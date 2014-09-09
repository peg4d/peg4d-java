package org.peg4d;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Stack;

import org.peg4d.vm.MachineInstruction;
import org.peg4d.vm.Opcode;

public class SimpleGrammarFormatter extends GrammarFormatter {
	UList<Opcode> opList = new UList<Opcode>(new Opcode[256]);
	HashMap<String, Integer> nonTerminalMap = new HashMap<String, Integer>();
	public SimpleGrammarFormatter() {
		super();
	}
	public SimpleGrammarFormatter(Grammar peg, StringBuilder sb) {
		super(peg, sb);
	}
}

class SimpleCodeGenerator extends SimpleGrammarFormatter {
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
	
	int labelId = 0;
	private int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	private void writeLabel(int label) {
		sb.append(" L" + label + ":\n");
		labelMap.put(label, opList.size());
	}
	
	private void writeCode(MachineInstruction mi) {
		sb.append("\t" + mi + "\n");
		opList.add(new Opcode(mi));
	}

	private void writeJumpCode(MachineInstruction mi, int labelId) {
		sb.append("\t" + mi + " L" + labelId + "\n");
		opList.add(new Opcode(mi, labelId));
	}

	private void writeCode(MachineInstruction mi, String op) {
		sb.append("\t" + mi + " " + op + "\n");
		Opcode opcode = new Opcode(mi);
		if(mi == MachineInstruction.opMatchCharset | mi == MachineInstruction.opMatchText) {
			op = op.substring(1, op.length() - 1);
		}
		opcode.bdata = op.getBytes();
		opList.add(opcode);
	}

	@Override
	public void formatHeader(StringBuilder sb) {
		this.sb = sb;
		opList =  new UList<Opcode>(new Opcode[256]);
		labelMap = new HashMap<Integer,Integer>();
		this.writeCode(MachineInstruction.EXIT);
		this.sb = null;
	}

	@Override
	public void formatRule(String ruleName, PExpression e, StringBuilder sb) {
		this.sb = sb;
		this.formatRule(ruleName, e);
		this.writeCode(MachineInstruction.RET);
		this.sb = null;
	}
	
	@Override
	public void formatFooter(StringBuilder sb) {
		Stack<Integer> RetPosStack = new Stack<Integer>();
		for(int i = 0; i < opList.size(); i++) {
			Opcode op = opList.ArrayValues[i];
			if(op.isJumpCode()) {
				switch(op.opcode) {
				case CALL:
					String labelName;
					try {
						labelName = new String(op.bdata, "UTF-8");
						op.ndata = nonTerminalMap.get(labelName) - 1;
						RetPosStack.push(i);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					break;
				case RET:
					break;
				default:
					op.ndata = labelMap.get(op.ndata) - 1;
				}
			}
			sb.append("["+i+"] " + op + "\n");
		}
	}

	private void formatRule(String ruleName, PExpression e) {
		sb.append(ruleName + ":\n");
		e.visit(this);
	}
	
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		this.writeCode(MachineInstruction.CALL, e.symbol);
	}
	@Override
	public void visitString(PString e) {
		this.writeCode(MachineInstruction.opMatchText, ParsingCharset.quoteString('\'', e.text, '\''));
	}
	@Override
	public void visitCharacter(PCharacter e) {
		this.writeCode(MachineInstruction.opMatchCharset, e.charset.toString());
	}
	@Override
	public void visitAny(PAny e) {
		this.writeCode(MachineInstruction.opMatchAnyChar);
	}
	@Override
	public void visitTagging(PTagging e) {
		this.writeCode(MachineInstruction.opTagging, e.tag.toString());
	}
	@Override
	public void visitMessage(PMessage e) {
		this.writeCode(MachineInstruction.opValue, ParsingCharset.quoteString('\'', e.symbol, '\''));
	}
	@Override
	public void visitIndent(ParsingIndent e) {
		this.writeCode(MachineInstruction.opIndent);
	}
	@Override
	public void visitOptional(POptional e) {
		int labelL = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opRememberFailurePosition);
		writeCode(MachineInstruction.opStoreObject);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeCode(MachineInstruction.opDropStoredObject);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
		writeCode(MachineInstruction.opRestoreObject);
		writeLabel(labelL);
		//writeCode(MachineInstruction.opRestoreObjectIfFailure);
		writeCode(MachineInstruction.opForgetFailurePosition);
	}
	@Override
	public void visitRepetition(PRepetition e) {
		int labelL = newLabel();
		int labelE = newLabel();
		int labelE2 = newLabel();
		/*if(e.atleast == 1) {
			e.inner.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL,labelE2);
		}*/
		writeLabel(labelL);
		writeCode(MachineInstruction.opStoreObject);
		writeCode(MachineInstruction.opRememberPosition);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeCode(MachineInstruction.opDropStoredObject);
		writeCode(MachineInstruction.opForgetFailurePosition);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeCode(MachineInstruction.opRestoreObject);
		writeLabel(labelE2);
	}
	
	@Override
	public void visitAnd(PAnd e) {
		writeCode(MachineInstruction.opRememberPosition);
		e.inner.visit(this);
		writeCode(MachineInstruction.opBacktrackPosition);
	}

	@Override
	public void visitNot(PNot e) {
		int labelL = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opRememberPosition);
		writeCode(MachineInstruction.opRememberFailurePosition);
		writeCode(MachineInstruction.opStoreObject);
		e.inner.visit(this);
		//writeCode(MachineInstruction.opRestoreNegativeObject);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeCode(MachineInstruction.opForgetFailurePosition);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeCode(MachineInstruction.opDropStoredObject);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
		writeCode(MachineInstruction.opCommitPosition);
		writeCode(MachineInstruction.opForgetFailurePosition);
		writeCode(MachineInstruction.opRestoreObject);
		writeLabel(labelL);
	}

	@Override
	public void visitConnector(PConnector e) {
		int labelF = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opStoreObject);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelF);
		writeCode(MachineInstruction.opConnectObject, ""+e.index);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.opDropStoredObject);
		writeLabel(labelE);
	}

	@Override
	public void visitSequence(PSequence e) {
		int labelF = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.opRememberPosition);
		for(int i = 0; i < e.size(); i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF);
		}
		writeCode(MachineInstruction.opCommitPosition);
		//writeCode(MachineInstruction.opCommitSequencePosition);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeLabel(labelE);
	}

	@Override
	public void visitChoice(PChoice e) {
		int labelS = newLabel();
		int labelE1 = newLabel();
		for(int i = 0; i < e.size(); i++) {
			writeCode(MachineInstruction.opStoreObject);
			writeCode(MachineInstruction.opRememberPosition);
			e.get(i).visit(this);
			writeJumpCode(MachineInstruction.IFSUCC, labelS);
			if(i != e.size() - 1) {
				writeCode(MachineInstruction.opRestoreObject);
			}
			writeCode(MachineInstruction.opBacktrackPosition);
		}
		writeCode(MachineInstruction.opDropStoredObject);
		writeJumpCode(MachineInstruction.JUMP, labelE1);
		writeLabel(labelS);
		writeCode(MachineInstruction.opDropStoredObject);
		writeCode(MachineInstruction.opForgetFailurePosition);
		writeLabel(labelE1);
	}

	@Override
	public void visitConstructor(PConstructor e) {
		int labelF = newLabel();
		int labelE = newLabel();
		if(e.leftJoin) {
			writeCode(MachineInstruction.opLeftJoinObject);
		}
		else {
			writeCode(MachineInstruction.opNewObject);
		}
		writeCode(MachineInstruction.opRememberPosition);
		for(int i = 0; i < e.size(); i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF);
		}
		writeCode(MachineInstruction.opCommitPosition);
		//writeCode(MachineInstruction.opCommitSequencePosition);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.opBacktrackPosition);
		writeLabel(labelE);
		//writeCode(MachineInstruction.opCommitObject);
	}

	@Override
	public void visitOperation(POperator e) {
		if(e instanceof PMatch) {
			sb.append("<match ");
			e.inner.visit(this);
			sb.append(">");
		}
		else {
			e.inner.visit(this);
		}
	}
}

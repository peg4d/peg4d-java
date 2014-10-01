//package org.peg4d.vm;
//
//import java.io.UnsupportedEncodingException;
//import java.util.HashMap;
//import java.util.Stack;
//
//import org.peg4d.Grammar;
//import org.peg4d.GrammarFormatter;
//import org.peg4d.NonTerminal;
//import org.peg4d.ParsingAnd;
//import org.peg4d.ParsingAny;
//import org.peg4d.ParsingByteRange;
//import org.peg4d.ParsingCharset;
//import org.peg4d.ParsingChoice;
//import org.peg4d.ParsingConnector;
//import org.peg4d.ParsingConstructor;
//import org.peg4d.ParsingExpression;
//import org.peg4d.ParsingIndent;
//import org.peg4d.ParsingMatch;
//import org.peg4d.ParsingNot;
//import org.peg4d.ParsingOperation;
//import org.peg4d.ParsingOption;
//import org.peg4d.ParsingRepetition;
//import org.peg4d.ParsingSequence;
//import org.peg4d.ParsingString;
//import org.peg4d.ParsingTagging;
//import org.peg4d.ParsingValue;
//import org.peg4d.UList;
//
//public class SimpleGrammarFormatter extends GrammarFormatter {
//	UList<Opcode> opList = new UList<Opcode>(new Opcode[256]);
//	HashMap<String, Integer> nonTerminalMap = new HashMap<String, Integer>();
//	public SimpleGrammarFormatter() {
//		super();
//	}
//	public SimpleGrammarFormatter(Grammar peg, StringBuilder sb) {
//		super(peg, sb);
//	}
//}
//
//class SimpleCodeGenerator extends SimpleGrammarFormatter {
//	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
//	
//	int labelId = 0;
//	private int newLabel() {
//		int l = labelId;
//		labelId++;
//		return l;
//	}
//	private void writeLabel(int label) {
//		sb.append(" L" + label + ":\n");
//		labelMap.put(label, opList.size());
//	}
//	
//	private void writeCode(MachineInstruction mi) {
//		sb.append("\t" + mi + "\n");
//		opList.add(new Opcode(mi));
//	}
//
//	private void writeJumpCode(MachineInstruction mi, int labelId) {
//		sb.append("\t" + mi + " L" + labelId + "\n");
//		opList.add(new Opcode(mi, labelId));
//	}
//
//	private void writeCode(MachineInstruction mi, String op) {
//		sb.append("\t" + mi + " " + op + "\n");
//		Opcode opcode = new Opcode(mi);
//		if(mi == MachineInstruction.opMatchCharset | mi == MachineInstruction.opMatchText) {
//			op = op.substring(1, op.length() - 1);
//		}
//		opcode.bdata = op.getBytes();
//		opList.add(opcode);
//	}
//
//	@Override
//	public void formatHeader(StringBuilder sb) {
//		this.sb = sb;
//		opList =  new UList<Opcode>(new Opcode[256]);
//		labelMap = new HashMap<Integer,Integer>();
//		this.writeCode(MachineInstruction.EXIT);
//		this.sb = null;
//	}
//
//	@Override
//	public void formatRule(String ruleName, ParsingExpression e, StringBuilder sb) {
//		this.sb = sb;
//		this.formatRule(ruleName, e);
//		this.writeCode(MachineInstruction.RET);
//		this.sb = null;
//	}
//	
//	@Override
//	public void formatFooter(StringBuilder sb) {
//		Stack<Integer> RetPosStack = new Stack<Integer>();
//		for(int i = 0; i < opList.size(); i++) {
//			Opcode op = opList.ArrayValues[i];
//			if(op.isJumpCode()) {
//				switch(op.opcode) {
//				case CALL:
//					String labelName;
//					try {
//						labelName = new String(op.bdata, "UTF-8");
//						op.ndata = nonTerminalMap.get(labelName) - 1;
//						RetPosStack.push(i);
//					} catch (UnsupportedEncodingException e) {
//						e.printStackTrace();
//					}
//					break;
//				case RET:
//					break;
//				default:
//					op.ndata = labelMap.get(op.ndata) - 1;
//				}
//			}
//			sb.append("["+i+"] " + op + "\n");
//		}
//	}
//
//	private void formatRule(String ruleName, ParsingExpression e) {
//		sb.append(ruleName + ":\n");
//		e.visit(this);
//	}
//	
//	@Override
//	public void visitNonTerminal(NonTerminal e) {
//		this.writeCode(MachineInstruction.CALL, e.ruleName);
//	}
//	@Override
//	public void visitString(ParsingString e) {
//		this.writeCode(MachineInstruction.opMatchText, ParsingCharset.quoteString('\'', e.text, '\''));
//	}
//	@Override
//	public void visitByteRange(ParsingByteRange e) {
//		this.writeCode(MachineInstruction.opMatchCharset, e.toString());
//	}
//	@Override
//	public void visitAny(ParsingAny e) {
//		this.writeCode(MachineInstruction.opMatchAnyChar);
//	}
//	@Override
//	public void visitTagging(ParsingTagging e) {
//		this.writeCode(MachineInstruction.opTagging, e.tag.toString());
//	}
//	@Override
//	public void visitValue(ParsingValue e) {
//		this.writeCode(MachineInstruction.opValue, ParsingCharset.quoteString('\'', e.value, '\''));
//	}
//	@Override
//	public void visitIndent(ParsingIndent e) {
//		this.writeCode(MachineInstruction.opIndent);
//	}
//	@Override
//	public void visitOptional(ParsingOption e) {
//		int labelL = newLabel();
//		int labelE = newLabel();
//		writeCode(MachineInstruction.opRememberFailurePosition);
//		writeCode(MachineInstruction.opStoreObject);
//		e.inner.visit(this);
//		writeJumpCode(MachineInstruction.IFFAIL, labelE);
//		writeCode(MachineInstruction.opDropStoredObject);
//		writeJumpCode(MachineInstruction.JUMP, labelL);
//		writeLabel(labelE);
//		writeCode(MachineInstruction.opRestoreObject);
//		writeLabel(labelL);
//		//writeCode(MachineInstruction.opRestoreObjectIfFailure);
//		writeCode(MachineInstruction.opForgetFailurePosition);
//	}
//	@Override
//	public void visitRepetition(ParsingRepetition e) {
//		int labelL = newLabel();
//		int labelE = newLabel();
//		int labelE2 = newLabel();
////		if(e.atleast == 1) {
////			writeCode(MachineInstruction.opRememberPosition);
////			e.inner.visit(this);
////			writeJumpCode(MachineInstruction.IFFAIL,labelE2);
////			writeCode(MachineInstruction.opCommitPosition);
////		}
//		writeLabel(labelL);
//		writeCode(MachineInstruction.opRememberPosition);
//		writeCode(MachineInstruction.opStoreObject);
//		e.inner.visit(this);
//		writeJumpCode(MachineInstruction.IFFAIL, labelE);
//		writeCode(MachineInstruction.opDropStoredObject);
//		writeCode(MachineInstruction.opCommitPosition);
//		writeJumpCode(MachineInstruction.JUMP, labelL);
//		writeLabel(labelE);
//		writeCode(MachineInstruction.opRestoreObject);
//		writeLabel(labelE2);
//		writeCode(MachineInstruction.opBacktrackPosition);
//	}
//	
//	@Override
//	public void visitAnd(ParsingAnd e) {
//		writeCode(MachineInstruction.opRememberPosition);
//		e.inner.visit(this);
//		writeCode(MachineInstruction.opBacktrackPosition);
//	}
//
//	@Override
//	public void visitNot(ParsingNot e) {
//		int labelL = newLabel();
//		int labelE = newLabel();
//		writeCode(MachineInstruction.opRememberFailurePosition);
//		writeCode(MachineInstruction.opRememberPosition);
//		writeCode(MachineInstruction.opStoreObject);
//		e.inner.visit(this);
//		writeCode(MachineInstruction.opRestoreNegativeObject);
//		writeJumpCode(MachineInstruction.IFFAIL, labelE);
//		writeCode(MachineInstruction.opCommitPosition);
//		writeCode(MachineInstruction.opForgetFailurePosition);
//		writeJumpCode(MachineInstruction.JUMP, labelL);
//		writeLabel(labelE);
//		writeCode(MachineInstruction.opBacktrackPosition);
//		writeCode(MachineInstruction.opCommitPosition);
//		writeLabel(labelL);
//	}
//
//	@Override
//	public void visitConnector(ParsingConnector e) {
//		int labelF = newLabel();
//		int labelE = newLabel();
//		writeCode(MachineInstruction.opStoreObject);
//		e.inner.visit(this);
//		writeJumpCode(MachineInstruction.IFFAIL, labelF);
//		writeCode(MachineInstruction.opConnectObject, ""+e.index);
//		writeJumpCode(MachineInstruction.JUMP, labelE);
//		writeLabel(labelF);
//		writeCode(MachineInstruction.opDropStoredObject);
//		writeLabel(labelE);
//	}
//
//	@Override
//	public void visitSequence(ParsingSequence e) {
//		int labelF = newLabel();
//		int labelE = newLabel();
//		writeCode(MachineInstruction.opRememberPosition);
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = e.get(i);
//			se.visit(this);
//			writeJumpCode(MachineInstruction.IFFAIL, labelF);
//		}
//		writeCode(MachineInstruction.opCommitPosition);
//		//writeCode(MachineInstruction.opCommitSequencePosition);
//		writeJumpCode(MachineInstruction.JUMP, labelE);
//		writeLabel(labelF);
//		writeCode(MachineInstruction.opBacktrackPosition);
//		writeLabel(labelE);
//	}
//
//	@Override
//	public void visitChoice(ParsingChoice e) {
//		int labelS = newLabel();
//		int labelE1 = newLabel();
//		for(int i = 0; i < e.size(); i++) {
//			writeCode(MachineInstruction.opStoreObject);
//			writeCode(MachineInstruction.opRememberPosition);
//			writeCode(MachineInstruction.opRememberPosition);
//			e.get(i).visit(this);
//			writeJumpCode(MachineInstruction.IFSUCC, labelS);
//			if(i != e.size() - 1) {
//				writeCode(MachineInstruction.opRestoreObject);
//				writeCode(MachineInstruction.opCommitPosition);
//			}
//			writeCode(MachineInstruction.opBacktrackPosition);
//		}
//		writeCode(MachineInstruction.opForgetFailurePosition);
//		writeCode(MachineInstruction.opDropStoredObject);
//		writeJumpCode(MachineInstruction.JUMP, labelE1);
//		writeLabel(labelS);
//		writeCode(MachineInstruction.opDropStoredObject);
//		writeCode(MachineInstruction.opCommitPosition);
//		writeCode(MachineInstruction.opCommitPosition);
//		writeLabel(labelE1);
//	}
//
//	@Override
//	public void visitConstructor(ParsingConstructor e) {
//		int labelF = newLabel();
//		int labelE = newLabel();
//		if(e.leftJoin) {
//			writeCode(MachineInstruction.opLeftJoinObject);
//		}
//		else {
//			writeCode(MachineInstruction.opNewObject);
//		}
//		writeCode(MachineInstruction.opRememberPosition);
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = e.get(i);
//			se.visit(this);
//			writeJumpCode(MachineInstruction.IFFAIL, labelF);
//		}
//		writeCode(MachineInstruction.opCommitPosition);
//		//writeCode(MachineInstruction.opCommitSequencePosition);
//		writeJumpCode(MachineInstruction.JUMP, labelE);
//		writeLabel(labelF);
//		writeCode(MachineInstruction.opBacktrackPosition);
//		writeLabel(labelE);
//		//writeCode(MachineInstruction.opCommitObject);
//	}
//
//	@Override
//	public void visitParsingOperation(ParsingOperation e) {
//		if(e instanceof ParsingMatch) {
//			sb.append("<match ");
//			e.inner.visit(this);
//			sb.append(">");
//		}
//		else {
//			e.inner.visit(this);
//		}
//	}
//}

package org.peg4d.pegcode;

import java.util.HashMap;

import org.peg4d.Grammar;
import org.peg4d.ParsingRule;
import org.peg4d.UList;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingApply;
import org.peg4d.expression.ParsingAssert;
import org.peg4d.expression.ParsingBlock;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingCatch;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingName;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;
import org.peg4d.vm.Opcode;
import org.peg4d.vm.Instruction;

public class CodeGenerator extends GrammarFormatter {
	
	int codeIndex = 0;
	
	UList<Opcode> codeList = new UList<Opcode>(new Opcode[256]);	
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
	HashMap<String, Integer> callMap = new HashMap<String, Integer>();
	
	private Opcode newCode(Instruction inst) {
		Opcode code = new Opcode(inst);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata) {
		Opcode code = new Opcode(inst, ndata);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata1, int ndata2) {
		Opcode code = new Opcode(inst, ndata1, ndata2);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, String name) {
		Opcode code = new Opcode(inst, name);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	public final void writeCode(Instruction inst) {
		codeList.add(newCode(inst));
	}
	
	public final void writeCode(Instruction inst, int ndata) {
		codeList.add(newCode(inst, ndata));
	}
	
	public final void writeCode(Instruction inst, int ndata1, int ndata2) {
		codeList.add(newCode(inst, ndata1, ndata2));
	}
	
	public final void writeCode(Instruction inst, String name) {
		codeList.add(newCode(inst, name));
	}
	
	class FailurePoint {
		int id;
		FailurePoint prev;
		FailurePoint(int id, FailurePoint prev) {
			this.prev = prev;
			this.id = id;
		}
	}
	
	int labelId = 0;
	FailurePoint fLabel = null;
	private void pushFailureJumpPoint() {
		fLabel = new FailurePoint(labelId, fLabel);
		labelId += 1;
	}
	
	private int popFailureJumpPoint(ParsingRule r) {
		FailurePoint fLabel = this.fLabel;
		this.fLabel = this.fLabel.prev;
		labelMap.put(fLabel.id, codeIndex);
		System.out.println("L" + fLabel.id);
		return fLabel.id;
	}
	
	private int popFailureJumpPoint(ParsingExpression e) {
		FailurePoint fLabel = this.fLabel;
		this.fLabel = this.fLabel.prev;
		labelMap.put(fLabel.id, codeIndex);
		System.out.println("L" + fLabel.id);
		return fLabel.id;
	}
	
	private int jumpFailureJump() {
		return this.fLabel.id;
	}
	
	private int jumpPrevFailureJump() {
		return this.fLabel.prev.id;
	}
	
	public final int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	
	public final void writeLabel(int label) {
		labelMap.put(label, codeIndex);
		System.out.println("L" + label);
	}
	
	public final void writeJumpCode(Instruction inst, int labelId) {
		codeList.add(newCode(inst, labelId));
	}
	
	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.ruleName.startsWith("\"")) {
				this.formatRule(r, sb);		//string builder is not used.
			}
		}
		this.formatFooter();
	}

	@Override
	public void formatHeader() {
		System.out.println("\nGenerate Byte Code\n");
		writeCode(Instruction.EXIT);
	}
	
	@Override
	public void formatFooter() {
		System.out.println();
		for (int i = 0; i < codeList.size(); i++) {
			Opcode code = codeList.ArrayValues[i];
			if (code.isJumpCode()) {
				switch (code.inst) {
				case CALL:
					code.ndata[0] = this.callMap.get(code.name);
					break;
				case RET:
					break;
				case EXIT:
					break;
				default:
					code.ndata[0] = this.labelMap.get(code.ndata[0]);
					break;
				}
			}
			System.out.println("[" + i + "] " + code);
		}
	}

	@Override
	public String getDesc() {
		return "vm";
	}

	@Override
	public void visitRule(ParsingRule e) {
		this.callMap.put(e.ruleName, this.codeIndex);
		this.pushFailureJumpPoint();
		e.expr.visit(this);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.RET);
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		writeCode(Instruction.CALL, 0);
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		writeCode(Instruction.FAIL);
	}

	@Override
	public void visitByte(ParsingByte e) {
		writeCode(Instruction.BYTE, e.byteChar);
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		writeCode(Instruction.CHAR, e.startByteChar, e.endByteChar);
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeCode(Instruction.ANY);
	}

	@Override
	public void visitString(ParsingString e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitNot(ParsingNot e) {
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHp);
		e.inner.visit(this);
		writeJumpCode(Instruction.IFSUCC, this.jumpFailureJump());
		writeCode(Instruction.STOREp);
		writeJumpCode(Instruction.JUMP, this.jumpPrevFailureJump());
		this.popFailureJumpPoint(e);
		writeCode(Instruction.STOREp);
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitOptional(ParsingOption e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitValue(ParsingValue e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitExport(ParsingExport e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitName(ParsingName e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitApply(ParsingApply e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

}

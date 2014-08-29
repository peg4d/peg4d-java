package org.peg4d;

import java.util.HashMap;

import org.peg4d.vm.MachineInstruction;
import org.peg4d.vm.Opcode;

class GrammarFormatter extends ParsingVisitor {
	protected StringBuilder sb = null;
	public GrammarFormatter() {
		this.sb = null;
	}
	public GrammarFormatter(Grammar peg, StringBuilder sb) {
		UList<PegRule> ruleList = peg.getRuleList();
		for(int i = 0; i < ruleList.size(); i++) {
			PegRule rule = ruleList.ArrayValues[i];
			formatRule(rule.ruleName, rule.expr, sb);
		}
	}
	
	public String getDesc() {
		return "PEG4d ";
	}
	public void format(PExpression e, StringBuilder sb) {
		this.sb = sb;
		e.visit(this);
		this.sb = null;
	}
	public void formatHeader(StringBuilder sb) {
	}
	public void formatFooter(StringBuilder sb) {
	}
	public void formatRule(String ruleName, PExpression e, StringBuilder sb) {
		this.sb = sb;
		this.formatRuleName(ruleName, e);
		sb.append(this.getNewLine());
		sb.append(this.getSetter());
		sb.append(" ");
		if(e instanceof PChoice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					sb.append(this.getNewLine());
					sb.append("/ ");
				}
				e.get(i).visit(this);
			}
		}
		else {
			e.visit(this);
		}
		sb.append("\n");
		this.sb = null;
	}
	public String getNewLine() {
		return "\n\t";
	}
	public String getSetter() {
		return "=";
	}
	public String getSemiColon() {
		return "";
	}
	public void formatRuleName(String ruleName, PExpression e) {
		sb.append(ruleName);
	}
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		this.formatRuleName(e.symbol, e);
	}
	@Override
	public void visitLazyNonTerminal(PLazyNonTerminal e) {
		sb.append("<lazy ");
		sb.append(e.symbol);
		sb.append(">");
	}
	@Override
	public void visitString(PString e) {
		char quote = '\'';
		if(e.text.indexOf("'") != -1) {
			quote = '"';
		}
		sb.append(ParsingCharset.quoteString(quote, e.text, quote));
	}
	@Override
	public void visitCharacter(PCharacter e) {
		sb.append(e.charset.toString());
	}
	@Override
	public void visitAny(PAny e) {
		sb.append(".");
	}
	@Override
	public void visitTagging(PTagging e) {
		sb.append("#");
		sb.append(e.tag.toString());
	}
	@Override
	public void visitMessage(PMessage e) {
		sb.append("`" + e.symbol + "`");
	}
	@Override
	public void visitIndent(PIndent e) {
		sb.append("indent");
	}
	protected void format(String prefix, PUnary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(e.inner instanceof PTerm || e.inner instanceof PConstructor) {
			e.inner.visit(this);
		}
		else {
			sb.append("(");
			e.inner.visit(this);
			sb.append(")");
		}
		if(suffix != null) {
			sb.append(suffix);
		}
	}
	@Override
	public void visitOptional(POptional e) {
		this.format( null, e, "?");
	}
	@Override
	public void visitRepetition(PRepetition e) {
		if(e.atleast == 1) {
			this.format( null, e, "+");
		}
		else {
			this.format(null, e, "*");
		}
	}
	@Override
	public void visitAnd(PAnd e) {
		this.format( "&", e, null);
	}

	@Override
	public void visitNot(PNot e) {
		this.format( "!", e, null);
	}

	@Override
	public void visitConnector(PConnector e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		this.format(predicate, e, null);
	}

	protected void format(PList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			PExpression e = l.get(i);
			if(e instanceof PChoice || e instanceof PSequence) {
				sb.append("( ");
				e.visit(this);
				sb.append(" )");
			}
			else {
				if(e == null) {
					System.out.println("@@@@ " + i + " "); 
					continue;
				}
				e.visit(this);
			}
		}
	}

	@Override
	public void visitSequence(PSequence e) {
		//sb.append("( ");
		this.format( e);
		//sb.append(" )");
	}

	@Override
	public void visitChoice(PChoice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitConstructor(PConstructor e) {
		if(e.leftJoin) {
			sb.append("{@ ");
		}
		else {
			sb.append("{ ");
		}
		this.format(e);
		sb.append(" }");
	}

	@Override
	public void visitOperation(POperator e) {
		if(e instanceof PMatch) {
			sb.append("<match ");
			e.inner.visit(this);
			sb.append(">");
		}
		else if(e instanceof PCommit) {
			sb.append("<commit ");
			e.inner.visit(this);
			sb.append(">");
		}
		else {
			e.inner.visit(this);
		}
	}
}

class CodeGenerator extends GrammarFormatter {

	UList<Opcode> opList = new UList<Opcode>(new Opcode[256]);
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();

//	public CodeGenerator() {
//		super();
//	}
	
	int labelId = 0;
	private int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	private void writeLabel(String label) {
		sb.append(label + ":\n");
	}
	private void writeLabel(int label) {
		sb.append(" L" + label + ":\n");
		labelMap.put(label, opList.size());
	}
//	private void writeCode(String code) {
//		sb.append("\t" + code + "\n");
//	}
	
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
		opList.add(new Opcode(mi));
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
		for(int i = 0; i < opList.size(); i++) {
			Opcode op = opList.ArrayValues[i];
			if(op.isJumpCode()) {
				op.ndata = labelMap.get(op.ndata) - 1;
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
	public void visitLazyNonTerminal(PLazyNonTerminal e) {

	}
	@Override
	public void visitString(PString e) {
		this.writeCode(MachineInstruction.TMATCH, ParsingCharset.quoteString('\'', e.text, '\''));
	}
	@Override
	public void visitCharacter(PCharacter e) {
		this.writeCode(MachineInstruction.AMATCH, e.charset.toString());
	}
	@Override
	public void visitAny(PAny e) {
		this.writeCode(MachineInstruction.UMATCH);
	}
	@Override
	public void visitTagging(PTagging e) {
		this.writeCode(MachineInstruction.TAG, e.tag.toString());
	}
	@Override
	public void visitMessage(PMessage e) {
		this.writeCode(MachineInstruction.REPLACE, ParsingCharset.quoteString('\'', e.symbol, '\''));
	}
	@Override
	public void visitIndent(PIndent e) {
		this.writeCode(MachineInstruction.INDENT);
	}
	@Override
	public void visitOptional(POptional e) {
		writeCode(MachineInstruction.PUSH_FPOS);
		writeCode(MachineInstruction.PUSH_LEFT);
		e.inner.visit(this);
		writeCode(MachineInstruction.POP_LEFT_IFFAIL);
		writeCode(MachineInstruction.POP_FPOS_FORGET);
	}
	@Override
	public void visitRepetition(PRepetition e) {
		int labelL = newLabel();
		int labelE = newLabel();
		int labelE2 = newLabel();
		if(e.atleast == 1) {
			e.inner.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL,labelE2);
		}
		writeCode(MachineInstruction.PUSH_FPOS);
		writeLabel(labelL);
		e.inner.visit(this);
		writeJumpCode(MachineInstruction.IFFAIL, labelE);
		writeJumpCode(MachineInstruction.JUMP, labelL);
		writeLabel(labelE);
		writeCode(MachineInstruction.POP_FPOS_FORGET);
		writeLabel(labelE2);
	}
	
	@Override
	public void visitAnd(PAnd e) {
		writeCode(MachineInstruction.PUSH_POS);
		e.inner.visit(this);
		writeCode(MachineInstruction.POP_POS);		
	}

	@Override
	public void visitNot(PNot e) {
		writeCode(MachineInstruction.PUSH_POS);
		writeCode(MachineInstruction.PUSH_LEFT);
		e.inner.visit(this);
		writeCode(MachineInstruction.POP_LEFT_NOT);
		writeCode(MachineInstruction.POP_POS);		
	}

	@Override
	public void visitConnector(PConnector e) {
		writeCode(MachineInstruction.PUSH_LEFT);
		e.inner.visit(this);
		writeCode(MachineInstruction.POP_LEFT_CONNECT, ""+e.index);
	}

	@Override
	public void visitSequence(PSequence e) {
		int labelF = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.PUSH_POS);
		writeCode(MachineInstruction.PUSH_BUFPOS);
		for(int i = 0; i < e.size(); i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF);
		}
		writeCode(MachineInstruction.POP_BUFPOS);
		writeCode(MachineInstruction.POP_POS);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF);
		writeCode(MachineInstruction.POP_BUFPOS_BACK);
		writeCode(MachineInstruction.POP_POS_BACK);
		writeLabel(labelE);
	}

	@Override
	public void visitChoice(PChoice e) {
		int labelS = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.PUSH_FPOS);
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
			writeJumpCode(MachineInstruction.IFSUCC, labelS);
		}
		writeCode(MachineInstruction.POP_FPOS);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelS);
		writeCode(MachineInstruction.POP_FPOS_FORGET);
		writeLabel(labelE);
	}

	@Override
	public void visitConstructor(PConstructor e) {
		int labelF = newLabel();
		int labelF2 = newLabel();
		int labelE = newLabel();
		writeCode(MachineInstruction.PUSH_POS);
		for(int i = 0; i < e.prefetchIndex; i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF);
		}
		writeCode(MachineInstruction.PUSH_BUFPOS);
		writeCode(MachineInstruction.NEW);
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(MachineInstruction.IFFAIL, labelF2);
		}
		writeCode(MachineInstruction.POP_BUFPOS);
		writeCode(MachineInstruction.POP_POS);
		writeJumpCode(MachineInstruction.JUMP, labelE);
		writeLabel(labelF2);
		writeCode(MachineInstruction.POP_BUFPOS);
		writeLabel(labelF);
		writeCode(MachineInstruction.POP_POS_BACK);
		writeLabel(labelE);
	}

	@Override
	public void visitOperation(POperator e) {
		if(e instanceof PMatch) {
			sb.append("<match ");
			e.inner.visit(this);
			sb.append(">");
		}
		else if(e instanceof PCommit) {
			sb.append("<commit ");
			e.inner.visit(this);
			sb.append(">");
		}
		else {
			e.inner.visit(this);
		}
	}
}


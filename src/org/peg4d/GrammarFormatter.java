package org.peg4d;

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
	public void formatHeader() {
	}
	public void formatFooter() {
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

	int labelId = 0;
	private String newLabel() {
		labelId++;
		return " L" + labelId;
	}
	private void writeLabel(String label) {
		sb.append(label + ":\n");
	}
	private void writeCode(String code) {
		sb.append("\t" + code + "\n");
	}
	@Override
	public void formatRule(String ruleName, PExpression e, StringBuilder sb) {
		this.sb = sb;
		this.formatRule(ruleName, e);
		this.writeCode("RET");
		this.sb = null;
	}
	private void formatRule(String ruleName, PExpression e) {
		sb.append(ruleName + ":\n");
		e.visit(this);
	}
	
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		this.writeCode("INVOKE " + e.symbol);
	}
	@Override
	public void visitLazyNonTerminal(PLazyNonTerminal e) {

	}
	@Override
	public void visitString(PString e) {
		this.writeCode("TMATCH " + ParsingCharset.quoteString('\'', e.text, '\''));
	}
	@Override
	public void visitCharacter(PCharacter e) {
		this.writeCode("AMATCH " + e.charset);
	}
	@Override
	public void visitAny(PAny e) {
		this.writeCode("UMATCH");
	}
	@Override
	public void visitTagging(PTagging e) {
		this.writeCode("TAG " + e.tag.toString());
	}
	@Override
	public void visitMessage(PMessage e) {
		this.writeCode("REPLACE " + ParsingCharset.quoteString('\'', e.symbol, '\''));
	}
	@Override
	public void visitIndent(PIndent e) {
		this.writeCode("INDENT");
	}
	@Override
	public void visitOptional(POptional e) {
		writeCode("PUSH_FAIL");
		writeCode("PUSH_LEFT");
		e.inner.visit(this);
		writeCode("POP_LEFT_IFFAIL");
		writeCode("POP_FAIL_AND_FORGET");
	}
	@Override
	public void visitRepetition(PRepetition e) {
		String labelL = newLabel();
		String labelE = newLabel();
		String labelE2 = newLabel();
		if(e.atleast == 1) {
			e.inner.visit(this);
			writeCode("IFFAIL" + labelE2);
		}
		writeCode("PUSH_FAIL");
		writeLabel(labelL);
		e.inner.visit(this);
		writeCode("IFFAIL" + labelE);
		writeCode("JUMP" + labelL);
		writeLabel(labelE);
		writeCode("POP_FAIL_AND_FORGET");
		writeLabel(labelE2);
	}
	
	@Override
	public void visitAnd(PAnd e) {
		writeCode("PUSH_POS");
		e.inner.visit(this);
		writeCode("POP_POS");		
	}

	@Override
	public void visitNot(PNot e) {
		writeCode("PUSH_POS");
		writeCode("PUSH_LEFT");
		e.inner.visit(this);
		writeCode("POP_LEFT_AND_NOT");
		writeCode("POP_POS");		
	}

	@Override
	public void visitConnector(PConnector e) {
		writeCode("PUSH_LEFT");
		e.inner.visit(this);
		writeCode("POP_LEFT_AND_CONNECT " + e.index);
	}

	@Override
	public void visitSequence(PSequence e) {
		String labelF = newLabel();
		String labelE = newLabel();
		writeCode("PUSH_POS");
		for(int i = 0; i < e.size(); i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeCode("IFFAIL" + labelF);
		}
		writeCode("POP_POS");
		writeCode("JUMP" + labelE);
		writeLabel(labelF);
		writeCode("POP_POS_AND_ROLLBACK");
		writeLabel(labelE);
	}

	@Override
	public void visitChoice(PChoice e) {
		String labelS = newLabel();
		String labelE = newLabel();
		writeCode("PUSH_FAIL");
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
			writeCode("IFSUCC" + labelS);

		}
		writeCode("POP_FAIL");
		writeCode("JUMP" + labelE);
		writeLabel(labelS);
		writeCode("POP_FAIL_AND_FORGET");
		writeLabel(labelE);
	}

	@Override
	public void visitConstructor(PConstructor e) {
		String labelF = newLabel();
		String labelF2 = newLabel();
		String labelE = newLabel();
		writeCode("PUSH_POS");
		for(int i = 0; i < e.prefetchIndex; i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeCode("IFFAIL" + labelF);
		}
		writeCode("PUSH_STACK");
		writeCode("NEW");
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			PExpression se = e.get(i);
			se.visit(this);
			writeCode("IFFAIL" + labelF2);
		}
		writeCode("POP_STACK_LINK");
		writeCode("POP_POS");
		writeCode("JUMP" + labelE);
		writeLabel(labelF2);
		writeCode("POP_STACK");
		writeLabel(labelF);
		writeCode("POP_POS_AND_ROLLBACK");
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


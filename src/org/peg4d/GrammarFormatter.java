package org.peg4d;

import java.util.HashMap;

import org.peg4d.vm.Instruction2;
import org.peg4d.vm.Opcode2;

class GrammarFormatter extends ExpressionVisitor {
	protected StringBuilder sb = null;
	public GrammarFormatter() {
		this.sb = null;
	}
	public GrammarFormatter(Grammar peg, StringBuilder sb) {
		UList<ParsingRule> ruleList = peg.getRuleList();
		for(int i = 0; i < ruleList.size(); i++) {
			ParsingRule rule = ruleList.ArrayValues[i];
			formatRule(rule.ruleName, rule.expr, sb);
		}
	}
	
	public String getDesc() {
		return "PEG4d ";
	}
	public void format(ParsingExpression e, StringBuilder sb) {
		this.sb = sb;
		e.visit(this);
		this.sb = null;
	}
	public void formatHeader(StringBuilder sb) {
	}
	public void formatFooter(StringBuilder sb) {
	}
	public void formatRule(String ruleName, ParsingExpression e, StringBuilder sb) {
		this.sb = sb;
		this.formatRuleName(ruleName, e);
		sb.append(this.getNewLine());
		sb.append(this.getSetter());
		sb.append(" ");
		if(e instanceof ParsingChoice) {
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
	public void formatRuleName(String ruleName, ParsingExpression e) {
		sb.append(ruleName);
	}
	@Override
	public void visitNonTerminal(NonTerminal e) {
		this.formatRuleName(e.ruleName, e);
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
		sb.append("''");
	}

	public final static String stringfyByte(int ch) {
		char c = (char)ch;
		switch(c) {
		case '\n' : return("'\\n'"); 
		case '\t' : return("'\\t'"); 
		case '\r' : return("'\\r'"); 
		case '\'' : return("'\\''"); 
		case '\\' : return("'\\\\'"); 
		}
		if(Character.isISOControl(c) || c > 127) {
			return(String.format("0x%02x", (int)c));
		}
		return("'" + c + "'");
	}
	
	@Override
	public void visitByte(ParsingByte e) {
		sb.append(stringfyByte(e.byteChar));
	}

	@Override
	public void visitString(ParsingString e) {
		char quote = '\'';
		sb.append(ParsingCharset.quoteString(quote, e.text, quote));
	}
	public final static String stringfyByte2(int ch) {
		char c = (char)ch;
		switch(c) {
		case '\n' : return("\\n"); 
		case '\t' : return("\\t"); 
		case '\r' : return("\\r"); 
		case '\'' : return("\\'"); 
		case ']' : return("\\]"); 
		case '-' : return("\\-"); 
		case '\\' : return("\\\\"); 
		}
		if(Character.isISOControl(c) || c > 127) {
			return(String.format("\\x%02x", (int)c));
		}
		return("" + c);
	}
	@Override
	public void visitByteRange(ParsingByteRange e) {
		sb.append("[");
		sb.append(stringfyByte2(e.startByteChar));
		sb.append("-");
		sb.append(stringfyByte2(e.endByteChar));
		sb.append("]");
	}
	@Override
	public void visitAny(ParsingAny e) {
		sb.append(".");
	}
	@Override
	public void visitTagging(ParsingTagging e) {
		sb.append("#");
		sb.append(e.tag.toString());
	}
	@Override
	public void visitValue(ParsingValue e) {
		sb.append("`" + e.value + "`");
	}
	@Override
	public void visitIndent(ParsingIndent e) {
		sb.append("indent");
	}
	protected void format(String prefix, ParsingUnary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(e.inner instanceof ParsingString || e.inner instanceof NonTerminal || e.inner instanceof ParsingConstructor) {
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
	public void visitOptional(ParsingOption e) {
		this.format( null, e, "?");
	}
	@Override
	public void visitRepetition(ParsingRepetition e) {
		this.format(null, e, "*");
	}
	@Override
	public void visitAnd(ParsingAnd e) {
		this.format( "&", e, null);
	}

	@Override
	public void visitNot(ParsingNot e) {
		this.format( "!", e, null);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		this.format(predicate, e, null);
	}

	protected void formatSequence(ParsingList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			int n = formatString(l, i);
			if(n > i) {
				i = n;
				continue;
			}
			ParsingExpression e = l.get(i);
			if(e instanceof ParsingChoice || e instanceof ParsingSequence) {
				sb.append("( ");
				e.visit(this);
				sb.append(" )");
				continue;
			}
			e.visit(this);
		}
	}

	private int formatString(ParsingList l, int start) {
		int end = l.size();
		String s = "";
		for(int i = start; i < end; i++) {
			ParsingExpression e = l.get(i);
			if(e instanceof ParsingByte) {
				char c = (char)(((ParsingByte) e).byteChar);
				if(c >= ' ' && c < 127) {
					s += c;
					continue;
				}
			}
			end = i;
			break;
		}
		if(s.length() > 1) {
			sb.append(ParsingCharset.quoteString('\'', s, '\''));
		}
		return end - 1;
	}
	
	@Override
	public void visitSequence(ParsingSequence e) {
		//sb.append("( ");
		this.formatSequence( e);
		//sb.append(" )");
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		if(e.leftJoin) {
			sb.append("{@ ");
		}
		else {
			sb.append("{ ");
		}
		this.formatSequence(e);
		sb.append(" }");
	}

	@Override
	public void visitParsingFunction(ParsingFunction e) {
		sb.append("<");
		sb.append(e.funcName);
		sb.append(e.getParameters());
		sb.append(">");
	}

	@Override
	public void visitParsingOperation(ParsingOperation e) {
		sb.append("<");
		sb.append(e.funcName);
		sb.append(e.getParameters());
		sb.append(" ");
		e.inner.visit(this);
		sb.append(">");
	}	

}

class CodeGenerator extends ExpressionVisitor {
	
	int opIndex = 0;
	UList<Opcode2> opList = new UList<Opcode2>(new Opcode2[256]);	
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();	
	HashMap<String,Integer> ruleMap = new HashMap<String,Integer>();
	
	
	private String uniqueRuleName(Grammar peg, String ruleName) {
		return ruleName;
	}
	
	public final void makeRule(Grammar peg, String ruleName) {
		this.ruleMap.put(uniqueRuleName(peg, ruleName), opIndex);
	}
	
	private Opcode2 newOpcode(Instruction2 mi) {
		Opcode2 c = new Opcode2(opIndex, mi);
		opIndex++;
		opList.add(c);
		return c;
	}

	public final void writeCode(Instruction2 mi) {
		newOpcode(mi);
	}
	
	int labelId = 0;
	public final int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	
	public final void writeLabel(int label) {
		Opcode2 c = newOpcode(Instruction2.NOP);
		c.n = label;
	}
	
	public final void writeJumpCode(Instruction2 mi, int labelId) {
		Opcode2 c = newOpcode(mi);
		c.n = labelId;
	}

	private void writeCode(Instruction2 mi, int n, Object v) {
		Opcode2 c = newOpcode(mi);
		c.n = n;
		c.v = c;
	}

	public void begin() {

	}

	public void end() {

	}

	public void init() {
		opList   =  new UList<Opcode2>(new Opcode2[256]);
		labelMap = new HashMap<Integer,Integer>();
		this.writeCode(Instruction2.EXIT);
	}

	public void generateRule(ParsingRule rule) {
		makeRule(rule.peg, rule.ruleName);
		rule.expr.visit(this);
	}
	
	public void finish() {
		for(int i = 0; i < opList.size(); i++) {
			Opcode2 op = opList.ArrayValues[i];
			if(op.isJumpCode()) {
				op.n = labelMap.get(op.n) - 1;
			}
			//sb.append("["+i+"] " + op + "\n");
		}
	}
	
	@Override
	public void visitNonTerminal(NonTerminal e) {
		this.writeCode(Instruction2.CALL, 0, uniqueRuleName(e.peg, e.ruleName));
	}
	
	@Override
	public void visitString(ParsingString e) {
		int labelFAIL = newLabel();
		int labelEXIT = newLabel();
		this.writeCode(Instruction2.TEXT, e.utf8.length, e.utf8);
		this.writeJumpCode(Instruction2.IFZERO, labelFAIL); {
			this.writeCode(Instruction2.CONSUME);
			this.writeJumpCode(Instruction2.JUMP, labelEXIT);
		}
		this.writeLabel(labelFAIL); {
			this.writeCode(Instruction2.FAIL);
		}
	}
	@Override
	public void visitByteRange(ParsingByteRange e) {
		int labelFAIL = newLabel();
		int labelEXIT = newLabel();
		// FIXME this.writeCode(Instruction2.CHAR, 0, e.charset);
		this.writeJumpCode(Instruction2.IFZERO, labelFAIL); {
			this.writeCode(Instruction2.CONSUME);
			this.writeJumpCode(Instruction2.JUMP, labelEXIT);
		}
		this.writeLabel(labelFAIL); {
			this.writeCode(Instruction2.FAIL);
		}
	}
	@Override
	public void visitAny(ParsingAny e) {
		this.writeCode(Instruction2.ANY);
	}
	@Override
	public void visitTagging(ParsingTagging e) {
		this.writeCode(Instruction2.TAG, 0, e.tag);
	}
	@Override
	public void visitValue(ParsingValue e) {
		this.writeCode(Instruction2.VALUE, 0, e.value);
	}
	@Override
	public void visitIndent(ParsingIndent e) {
		//this.writeCode(Instruction2.opIndent);
	}
	@Override
	public void visitOptional(ParsingOption e) {
		int labelEXIT = newLabel();
		writeCode(Instruction2.PUSHf); //-2
		writeCode(Instruction2.PUSHo); //-1
		e.inner.visit(this);
		writeJumpCode(Instruction2.IFSUCC, labelEXIT); {
			this.writeCode(Instruction2.STOREf, -2, null);
			this.writeCode(Instruction2.STOREo, -1, null);
		}
		writeLabel(labelEXIT);
		writeCode(Instruction2.POP, 2, null);
	}
	@Override
	public void visitRepetition(ParsingRepetition e) {
		int labelLOOP = newLabel();
		int labelEXIT = newLabel();

		writeCode(Instruction2.PUSHf); //-3
		writeCode(Instruction2.PUSHo); //-2
		writeCode(Instruction2.PUSHp); //-1
		writeLabel(labelLOOP); {
			this.writeCode(Instruction2.STOREp, -3, null);			
			e.inner.visit(this);
			writeJumpCode(Instruction2.IFFAIL, labelEXIT);
			writeJumpCode(Instruction2.IFp, labelEXIT);
			this.writeCode(Instruction2.LOADo, -2, null);
		}
		writeJumpCode(Instruction2.JUMP, labelLOOP);
		writeLabel(labelEXIT);
		this.writeCode(Instruction2.STOREf, -3, null);
		this.writeCode(Instruction2.STOREo, -2, null);
		writeCode(Instruction2.POP, 3, null);
	}
	
	@Override
	public void visitAnd(ParsingAnd e) {
		writeCode(Instruction2.PUSHp); //-1
		e.inner.visit(this);
		this.writeCode(Instruction2.STOREp, -1, null);		
		writeCode(Instruction2.POP, 1, null);
	}

	@Override
	public void visitNot(ParsingNot e) {
		int labelFAIL = newLabel();
		int labelEXIT = newLabel();
		writeCode(Instruction2.PUSHf); //-3
		writeCode(Instruction2.PUSHo); //-2
		writeCode(Instruction2.PUSHp); //-1
		e.inner.visit(this);
		writeJumpCode(Instruction2.IFFAIL, labelFAIL); {
			this.writeCode(Instruction2.STOREp, -1, null);		
			this.writeCode(Instruction2.FAIL);
			this.writeJumpCode(Instruction2.JUMP, labelEXIT);
		}
		writeLabel(labelFAIL); {
			this.writeCode(Instruction2.STOREo, -2, null);		
			this.writeCode(Instruction2.STOREf, -3, null);		
		}
		writeLabel(labelEXIT);
		writeCode(Instruction2.POP, 3, null);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		writeCode(Instruction2.PUSHo); //-1
		e.inner.visit(this);
		writeCode(Instruction2.LINK, e.index, null);
		writeCode(Instruction2.POP, 1, null);
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		int labelFAIL = newLabel();
		int labelEXIT = newLabel();
		writeCode(Instruction2.PUSHp); //-1
		for(int i = 0; i < e.size(); i++) {
			ParsingExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(Instruction2.IFFAIL, labelFAIL);
		}
		this.writeJumpCode(Instruction2.JUMP, labelEXIT);
		writeLabel(labelFAIL); {
			this.writeCode(Instruction2.STOREp, -1, null);
		}
		writeLabel(labelEXIT);
		writeCode(Instruction2.POP, 1, null);
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		int labelOK = newLabel();
		int labelEXIT = newLabel();
		writeCode(Instruction2.PUSHf); //-2
		writeCode(Instruction2.PUSHo); //-1
		for(int i = 0; i < e.size(); i++) {
			ParsingExpression se = e.get(i);
			se.visit(this);
			writeJumpCode(Instruction2.IFSUCC, labelOK);
			writeCode(Instruction2.STOREo, -1, null);
		}
		this.writeJumpCode(Instruction2.JUMP, labelEXIT);
		writeLabel(labelOK); {
			this.writeCode(Instruction2.STOREf, -2, null);
		}
		writeLabel(labelEXIT);
		writeCode(Instruction2.POP, 2, null);
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		int labelFAIL = newLabel();
		int labelEXIT = newLabel();
		writeCode(Instruction2.PUSHp); //-3
		writeCode(Instruction2.PUSHo); //-2
		if(e.leftJoin) {
			writeCode(Instruction2.NEWJOIN);
		}
		else {
			writeCode(Instruction2.NEW);   // -1
		}
		writeCode(Instruction2.STOREo, -2, null);
		for(int i = 0; i < e.size(); i++) {
			ParsingExpression se = e.get(i);
			writeCode(Instruction2.LOADo, -2, null); //-1
			se.visit(this);
			writeJumpCode(Instruction2.IFFAIL, labelFAIL);
		}
		writeCode(Instruction2.COMMIT);
		writeJumpCode(Instruction2.JUMP, labelEXIT);
		writeLabel(labelFAIL); {
			writeCode(Instruction2.ABORT);
		}
		writeLabel(labelEXIT);
		writeCode(Instruction2.POP, 2, null);
	}
}


package org.peg4d.pegcode;

import org.peg4d.ParsingCharset;
import org.peg4d.ParsingRule;
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
import org.peg4d.expression.ParsingCommand;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingFunction;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingList;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingName;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingUnary;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;

public class PEG4dFormatter extends GrammarFormatter {
	protected StringBuilder sb = null;
	
	public PEG4dFormatter() {
		this.sb = null;
	}
		
	@Override
	public String getDesc() {
		return "PEG4d";
	}
	
	@Override
	public void visitRule(ParsingRule rule) {
		ParsingExpression e = rule.expr;
		this.formatRuleName(rule.ruleName, e);
		this.formatString(this.getNewLine());
		this.formatString(this.getSetter());
		this.formatString(" ");
		if(e instanceof ParsingChoice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					this.formatString(this.getNewLine());
					this.formatString("/ ");
				}
				e.get(i).visit(this);
			}
		}
		else {
			e.visit(this);
		}
		this.formatString("\n");
	}

	public String getNewLine() {
		return "\n\t";
	}
	public String getSetter() {
		return "=";
	}
	
	public void formatRuleName(String ruleName, ParsingExpression e) {
		this.formatString(ruleName);
	}
	
	@Override
	public void visitEmpty(ParsingEmpty e) {
		this.formatString("''");
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		this.formatString("!''");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		this.formatRuleName(e.ruleName, e);
	}

	@Override
	public void visitByte(ParsingByte e) {
		this.formatString(GrammarFormatter.stringfyByte(e.byteChar));
	}

	@Override
	public void visitString(ParsingString e) {
		char quote = '\'';
		this.formatString(ParsingCharset.quoteString(quote, e.text, quote));
	}
	
	@Override
	public void visitByteRange(ParsingByteRange e) {
		this.formatString("[");
		this.formatString(GrammarFormatter.stringfyByte2(e.startByteChar));
		this.formatString("-");
		this.formatString(GrammarFormatter.stringfyByte2(e.endByteChar));
		this.formatString("]");
	}
	
	@Override
	public void visitAny(ParsingAny e) {
		this.formatString(".");
	}
	
	@Override
	public void visitTagging(ParsingTagging e) {
		this.formatString("#");
		this.formatString(e.tag.toString());
	}
	@Override
	public void visitValue(ParsingValue e) {
		this.formatString("`" + e.value + "`");
	}
	
	protected void format(String prefix, ParsingUnary e, String suffix) {
		if(prefix != null) {
			this.formatString(prefix);
		}
		if(e.inner instanceof ParsingString || e.inner instanceof NonTerminal || e.inner instanceof ParsingConstructor) {
			e.inner.visit(this);
		}
		else {
			this.formatString("(");
			e.inner.visit(this);
			this.formatString(")");
		}
		if(suffix != null) {
			this.formatString(suffix);
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
				this.formatString(" ");
			}
			int n = formatString(l, i);
			if(n > i) {
				i = n;
				continue;
			}
			ParsingExpression e = l.get(i);
			if(e instanceof ParsingChoice || e instanceof ParsingSequence) {
				this.formatString("( ");
				e.visit(this);
				this.formatString(" )");
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
			this.formatString(ParsingCharset.quoteString('\'', s, '\''));
		}
		return end - 1;
	}
	
	@Override
	public void visitSequence(ParsingSequence e) {
		//this.formatString("( ");
		this.formatSequence( e);
		//this.formatString(" )");
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				this.formatString(" / ");
			}
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		if(e.leftJoin) {
			this.formatString("{@ ");
		}
		else {
			this.formatString("{ ");
		}
		this.formatSequence(e);
		this.formatString(" }");
	}
	

	public void formatParsingCommand(ParsingCommand e) {
		this.formatString("<");
		this.formatString(e.funcName);
		this.formatString(e.getParameters());
		this.formatString(">");
	}

	public void formatParsingFunction(ParsingFunction e) {
		this.formatString("<");
		this.formatString(e.funcName);
		this.formatString(e.getParameters());
		this.formatString(" ");
		e.inner.visit(this);
		this.formatString(">");
	}


	@Override
	public void visitExport(ParsingExport e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		this.formatParsingFunction(e);
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		// TODO Auto-generated method stub
		this.formatParsingCommand(e);
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		this.formatParsingFunction(e);
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		// TODO Auto-generated method stub
		this.formatParsingCommand(e);
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		this.formatParsingFunction(e);
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		this.formatParsingFunction(e);
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		this.formatParsingFunction(e);
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		this.formatParsingCommand(e);
	}

	@Override
	public void visitName(ParsingName e) {
		this.formatParsingFunction(e);
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		// TODO Auto-generated method stub
		this.formatParsingCommand(e);
	}

	@Override
	public void visitApply(ParsingApply e) {
		//this.formatParsingFunction(e);
		this.formatParsingFunction(e);
	}	



}

//class CodeGenerator extends ParsingExpressionVisitor {
//	
//	int opIndex = 0;
//	UList<Opcode2> opList = new UList<Opcode2>(new Opcode2[256]);	
//	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();	
//	HashMap<String,Integer> ruleMap = new HashMap<String,Integer>();
//	
//	
//	private String uniqueRuleName(Grammar peg, String ruleName) {
//		return ruleName;
//	}
//	
//	public final void makeRule(Grammar peg, String ruleName) {
//		this.ruleMap.put(uniqueRuleName(peg, ruleName), opIndex);
//	}
//	
//	private Opcode2 newOpcode(Instruction2 mi) {
//		Opcode2 c = new Opcode2(opIndex, mi);
//		opIndex++;
//		opList.add(c);
//		return c;
//	}
//
//	public final void writeCode(Instruction2 mi) {
//		newOpcode(mi);
//	}
//	
//	int labelId = 0;
//	public final int newLabel() {
//		int l = labelId;
//		labelId++;
//		return l;
//	}
//	
//	public final void writeLabel(int label) {
//		Opcode2 c = newOpcode(Instruction2.NOP);
//		c.n = label;
//	}
//	
//	public final void writeJumpCode(Instruction2 mi, int labelId) {
//		Opcode2 c = newOpcode(mi);
//		c.n = labelId;
//	}
//
//	private void writeCode(Instruction2 mi, int n, Object v) {
//		Opcode2 c = newOpcode(mi);
//		c.n = n;
//		c.v = c;
//	}
//
//	public void begin() {
//
//	}
//
//	public void end() {
//
//	}
//
//	public void init() {
//		opList   =  new UList<Opcode2>(new Opcode2[256]);
//		labelMap = new HashMap<Integer,Integer>();
//		this.writeCode(Instruction2.EXIT);
//	}
//
//	public void generateRule(ParsingRule rule) {
//		makeRule(rule.peg, rule.ruleName);
//		rule.expr.visit(this);
//	}
//	
//	public void finish() {
//		for(int i = 0; i < opList.size(); i++) {
//			Opcode2 op = opList.ArrayValues[i];
//			if(op.isJumpCode()) {
//				op.n = labelMap.get(op.n) - 1;
//			}
//			//this.formatString("["+i+"] " + op + "\n");
//		}
//	}
//	
//	@Override
//	public void visitNonTerminal(NonTerminal e) {
//		this.writeCode(Instruction2.CALL, 0, uniqueRuleName(e.peg, e.ruleName));
//	}
//	
//	@Override
//	public void visitString(ParsingString e) {
//		int labelFAIL = newLabel();
//		int labelEXIT = newLabel();
//		this.writeCode(Instruction2.TEXT, e.utf8.length, e.utf8);
//		this.writeJumpCode(Instruction2.IFZERO, labelFAIL); {
//			this.writeCode(Instruction2.CONSUME);
//			this.writeJumpCode(Instruction2.JUMP, labelEXIT);
//		}
//		this.writeLabel(labelFAIL); {
//			this.writeCode(Instruction2.FAIL);
//		}
//	}
//	@Override
//	public void visitByteRange(ParsingByteRange e) {
//		int labelFAIL = newLabel();
//		int labelEXIT = newLabel();
//		// FIXME this.writeCode(Instruction2.CHAR, 0, e.charset);
//		this.writeJumpCode(Instruction2.IFZERO, labelFAIL); {
//			this.writeCode(Instruction2.CONSUME);
//			this.writeJumpCode(Instruction2.JUMP, labelEXIT);
//		}
//		this.writeLabel(labelFAIL); {
//			this.writeCode(Instruction2.FAIL);
//		}
//	}
//	@Override
//	public void visitAny(ParsingAny e) {
//		this.writeCode(Instruction2.ANY);
//	}
//	@Override
//	public void visitTagging(ParsingTagging e) {
//		this.writeCode(Instruction2.TAG, 0, e.tag);
//	}
//	@Override
//	public void visitValue(ParsingValue e) {
//		this.writeCode(Instruction2.VALUE, 0, e.value);
//	}
//	@Override
//	public void visitIndent(ParsingIndent e) {
//		//this.writeCode(Instruction2.opIndent);
//	}
//	@Override
//	public void visitOptional(ParsingOption e) {
//		int labelEXIT = newLabel();
//		writeCode(Instruction2.PUSHf); //-2
//		writeCode(Instruction2.PUSHo); //-1
//		e.inner.visit(this);
//		writeJumpCode(Instruction2.IFSUCC, labelEXIT); {
//			this.writeCode(Instruction2.STOREf, -2, null);
//			this.writeCode(Instruction2.STOREo, -1, null);
//		}
//		writeLabel(labelEXIT);
//		writeCode(Instruction2.POP, 2, null);
//	}
//	@Override
//	public void visitRepetition(ParsingRepetition e) {
//		int labelLOOP = newLabel();
//		int labelEXIT = newLabel();
//
//		writeCode(Instruction2.PUSHf); //-3
//		writeCode(Instruction2.PUSHo); //-2
//		writeCode(Instruction2.PUSHp); //-1
//		writeLabel(labelLOOP); {
//			this.writeCode(Instruction2.STOREp, -3, null);			
//			e.inner.visit(this);
//			writeJumpCode(Instruction2.IFFAIL, labelEXIT);
//			writeJumpCode(Instruction2.IFp, labelEXIT);
//			this.writeCode(Instruction2.LOADo, -2, null);
//		}
//		writeJumpCode(Instruction2.JUMP, labelLOOP);
//		writeLabel(labelEXIT);
//		this.writeCode(Instruction2.STOREf, -3, null);
//		this.writeCode(Instruction2.STOREo, -2, null);
//		writeCode(Instruction2.POP, 3, null);
//	}
//	
//	@Override
//	public void visitAnd(ParsingAnd e) {
//		writeCode(Instruction2.PUSHp); //-1
//		e.inner.visit(this);
//		this.writeCode(Instruction2.STOREp, -1, null);		
//		writeCode(Instruction2.POP, 1, null);
//	}
//
//	@Override
//	public void visitNot(ParsingNot e) {
//		int labelFAIL = newLabel();
//		int labelEXIT = newLabel();
//		writeCode(Instruction2.PUSHf); //-3
//		writeCode(Instruction2.PUSHo); //-2
//		writeCode(Instruction2.PUSHp); //-1
//		e.inner.visit(this);
//		writeJumpCode(Instruction2.IFFAIL, labelFAIL); {
//			this.writeCode(Instruction2.STOREp, -1, null);		
//			this.writeCode(Instruction2.FAIL);
//			this.writeJumpCode(Instruction2.JUMP, labelEXIT);
//		}
//		writeLabel(labelFAIL); {
//			this.writeCode(Instruction2.STOREo, -2, null);		
//			this.writeCode(Instruction2.STOREf, -3, null);		
//		}
//		writeLabel(labelEXIT);
//		writeCode(Instruction2.POP, 3, null);
//	}
//
//	@Override
//	public void visitConnector(ParsingConnector e) {
//		writeCode(Instruction2.PUSHo); //-1
//		e.inner.visit(this);
//		writeCode(Instruction2.LINK, e.index, null);
//		writeCode(Instruction2.POP, 1, null);
//	}
//
//	@Override
//	public void visitSequence(ParsingSequence e) {
//		int labelFAIL = newLabel();
//		int labelEXIT = newLabel();
//		writeCode(Instruction2.PUSHp); //-1
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = e.get(i);
//			se.visit(this);
//			writeJumpCode(Instruction2.IFFAIL, labelFAIL);
//		}
//		this.writeJumpCode(Instruction2.JUMP, labelEXIT);
//		writeLabel(labelFAIL); {
//			this.writeCode(Instruction2.STOREp, -1, null);
//		}
//		writeLabel(labelEXIT);
//		writeCode(Instruction2.POP, 1, null);
//	}
//
//	@Override
//	public void visitChoice(ParsingChoice e) {
//		int labelOK = newLabel();
//		int labelEXIT = newLabel();
//		writeCode(Instruction2.PUSHf); //-2
//		writeCode(Instruction2.PUSHo); //-1
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = e.get(i);
//			se.visit(this);
//			writeJumpCode(Instruction2.IFSUCC, labelOK);
//			writeCode(Instruction2.STOREo, -1, null);
//		}
//		this.writeJumpCode(Instruction2.JUMP, labelEXIT);
//		writeLabel(labelOK); {
//			this.writeCode(Instruction2.STOREf, -2, null);
//		}
//		writeLabel(labelEXIT);
//		writeCode(Instruction2.POP, 2, null);
//	}
//
//	@Override
//	public void visitConstructor(ParsingConstructor e) {
//		int labelFAIL = newLabel();
//		int labelEXIT = newLabel();
//		writeCode(Instruction2.PUSHp); //-3
//		writeCode(Instruction2.PUSHo); //-2
//		if(e.leftJoin) {
//			writeCode(Instruction2.NEWJOIN);
//		}
//		else {
//			writeCode(Instruction2.NEW);   // -1
//		}
//		writeCode(Instruction2.STOREo, -2, null);
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = e.get(i);
//			writeCode(Instruction2.LOADo, -2, null); //-1
//			se.visit(this);
//			writeJumpCode(Instruction2.IFFAIL, labelFAIL);
//		}
//		writeCode(Instruction2.COMMIT);
//		writeJumpCode(Instruction2.JUMP, labelEXIT);
//		writeLabel(labelFAIL); {
//			writeCode(Instruction2.ABORT);
//		}
//		writeLabel(labelEXIT);
//		writeCode(Instruction2.POP, 2, null);
//	}
//}
//

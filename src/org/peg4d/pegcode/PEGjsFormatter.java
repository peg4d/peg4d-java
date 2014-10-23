package org.peg4d.pegcode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class PEGjsFormatter extends GrammarFormatter {
	protected StringBuilder sb = null;
	
	private Map<String, Integer> doubleQuotedToken = new HashMap<String, Integer>();
	private Set<String> definedDoubleQuotedTokenSet = new HashSet<String>();
	private int doubleQuotedTokenCount = 0;
	
	private String getDoubleQuotedTokenName(String quotedString){
		if(!this.doubleQuotedToken.containsKey(quotedString)){
			doubleQuotedToken.put(quotedString, ++doubleQuotedTokenCount);
		}
		return "tk" + doubleQuotedToken.get(quotedString);
	}
	
	public PEGjsFormatter() {
		this.sb = null;
	}
		
	@Override
	public String getDesc() {
		return "PEG.js";
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
		if(ruleName.equals("File")){
			this.formatString("start");
		}else if(ruleName.startsWith("\"")){
			this.formatString(this.getDoubleQuotedTokenName(ruleName));
		}
		else{
			this.formatString(ruleName);
		}
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
		String hexString = Integer.toHexString(e.byteChar);
		hexString = hexString.length() == 1 ? ("0" + hexString) : hexString;
		this.formatString("\"\\x" + hexString + "\"");
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
//		this.formatString("#");
//		this.formatString(e.tag.toString());
	}
	@Override
	public void visitValue(ParsingValue e) {
		this.formatString("{ return '" + e.value + "'; }");
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
//		String predicate = "@";
//		if(e.index != -1) {
//			predicate += "[" + e.index + "]";
//		}
		this.format(null, e, null);
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
			this.formatString("(");
		}
		else {
			this.formatString("(");
		}
		this.formatSequence(e);
		this.formatString(")");
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

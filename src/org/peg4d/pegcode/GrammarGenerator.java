package org.peg4d.pegcode;

import org.peg4d.Grammar;
import org.peg4d.ParsingRule;
import org.peg4d.expression.ParsingExpression;

public abstract class GrammarGenerator extends GrammarVisitor {
	protected StringBuilder sb = null;
	
	public GrammarGenerator() {
		this.sb = null;
	}
	
	public abstract String getDesc();

	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.sb = sb;
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			this.formatRule(r, sb);
		}
		this.formatFooter();
		System.out.println(sb.toString());
	}
	
	public void formatHeader() {
	}
	public void formatFooter() {
	}
	
	public final void formatRule(ParsingRule rule, StringBuilder sb) {
		this.sb = sb;
		this.visitRule(rule);
	}

	public final void formatExpression(ParsingExpression e, StringBuilder sb) {
		this.sb = sb;
		e.visit(this);
	}
	
	protected final void formatString(String s) {
		if(sb == null) {
			System.out.print(s);
		}
		else {
			this.sb.append(s);
		}
	}

}

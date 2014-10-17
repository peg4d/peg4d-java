package org.peg4d.pegcode;

import org.peg4d.Grammar;
import org.peg4d.ParsingRule;
import org.peg4d.expression.ParsingExpression;

public abstract class GrammarFormatter extends GrammarVisitor {
	protected StringBuilder sb = null;
	
	public GrammarFormatter() {
		this.sb = null;
	}
	
	public GrammarFormatter(Grammar peg, StringBuilder sb) {
		this.formatHeader(sb);
		for(ParsingRule r : peg.getRuleList()) {
			this.formatRule(r, sb);
		}
		this.formatFooter(sb);
	}

	public abstract String getDesc();
	
	public void formatHeader(StringBuilder sb) {
	}
	
	public final void formatRule(ParsingRule rule, StringBuilder sb) {
		this.sb = sb;
		this.visitRule(rule);
	}

	public void formatFooter(StringBuilder sb) {
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

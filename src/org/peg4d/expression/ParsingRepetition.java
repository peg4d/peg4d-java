package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingRepetition extends ParsingUnary {
	ParsingRepetition(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("*\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newRepetition(e);
	}
	@Override
	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		this.inner.checkLength(ruleName, start, minlen, stack);
//		if(!(this.inner.minlen > 0) && !(this.inner instanceof NonTerminal)) {
//			this.report(ReportLevel.warning, "uncosumed repetition: " + this.inner.minlen);
//		}
		this.minlen = 0;
		return this.minlen + minlen;
	}

	@Override public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long ppos = -1;
		long pos = context.getPosition();
//		long f = context.rememberFailure();
		while(ppos < pos) {
			ParsingObject left = context.left;
			if(!this.inner.matcher.simpleMatch(context)) {
				context.left = left;
				left = null;
				break;
			}
			ppos = pos;
			pos = context.getPosition();
			left = null;
		}
//		context.forgetFailure(f);
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitRepetition(this);
	}
}
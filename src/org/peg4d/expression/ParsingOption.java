package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingOption extends ParsingUnary {
	ParsingOption(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("?\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newOption(e);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitOptional(this);
	}
	@Override 
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
		if(!this.inner.matcher.simpleMatch(context)) {
			context.left = left;
			context.forgetFailure(f);
		}
		left = null;
		return true;
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingAnd extends ParsingUnary {
	ParsingAnd(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("&\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newAnd(e);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitAnd(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		this.inner.matcher.simpleMatch(context);
		context.rollback(pos);
		return !context.isFailure();
	}
	
	@Override
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept || r == LazyAccept) {
			return LazyAccept;
		}
		return r;
	}
	
}
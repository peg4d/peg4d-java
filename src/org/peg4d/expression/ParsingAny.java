package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingAny extends ParsingExpression {
	ParsingAny() {
		super();
		this.minlen = 1;
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression(".\b", this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitAny(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.charAt(context.pos) != -1) {
			int len = context.source.charLength(context.pos);
			context.consume(len);
			return true;
		}
		context.failure(this);
		return false;
	}
}
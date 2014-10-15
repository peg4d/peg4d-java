package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingEmpty extends ParsingExpression {
	ParsingEmpty() {
		super();
		this.minlen = 0;
	}
	@Override ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("\b", this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitEmpty(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingValue extends ParsingExpression {
	public String value;
	ParsingValue(String value) {
		super();
		this.value = value;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("`\b" + this.value, this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(lexOnly || this.isRemovedOperation()) {
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitValue(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setValue(this.value);
		return true;
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

public abstract class ParsingFunction extends ParsingExpression {
	public String funcName;
	ParsingFunction(String funcName) {
		super();
		this.funcName = funcName;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	public String getParameters() {
		return "";
	}
	@Override final ParsingExpression uniquefyImpl() {
		return 	ParsingExpression.uniqueExpression("<"+this.funcName+this.getParameters(), this);
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitParsingFunction(this);
	}
}
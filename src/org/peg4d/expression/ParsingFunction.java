package org.peg4d.expression;


public abstract class ParsingFunction extends ParsingUnary {
	public String funcName;
	ParsingFunction(String funcName, ParsingExpression inner) {
		super(inner);
		this.funcName = funcName;
		this.inner = inner;
	}
	@Override final ParsingExpression uniquefyImpl() {
		return 	ParsingExpression.uniqueExpression("<"+this.funcName+this.getParameters()+"+"+this.uniqueKey(), this);
	}
	public String getParameters() {
		return "";
	}
	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

}
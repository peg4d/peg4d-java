package org.peg4d.expression;


public abstract class ParsingFunction extends ParsingUnary {
	public String funcName;
	ParsingFunction(String funcName, ParsingExpression inner) {
		super(inner);
		this.funcName = funcName;
		this.inner = inner;
	}
	@Override
	public final String getInternKey() {
		return "<"+this.funcName+this.getParameters();
	}
	public String getParameters() {
		return "";
	}
	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

}
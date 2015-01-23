package org.peg4d.expression;

import java.util.TreeMap;


public abstract class ParsingFunction extends ParsingUnary {
	public String funcName;
	ParsingFunction(String funcName, ParsingExpression inner) {
		super(inner);
		this.funcName = funcName;
		this.inner = inner;
	}
	@Override
	public final String getInterningKey() {
		return "<"+this.funcName+this.getParameters();
	}
	public String getParameters() {
		return "";
	}
	@Override
	public short acceptByte(int ch) {
		if(this.inner != null) {
			return this.inner.acceptByte(ch);
		}
		return Accept;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> undefedFlags) {
		return null;
	}
}
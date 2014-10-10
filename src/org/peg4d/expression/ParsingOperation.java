package org.peg4d.expression;


public abstract class ParsingOperation extends ParsingUnary {
	public String funcName;
	ParsingOperation(String funcName, ParsingExpression inner) {
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
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitParsingOperation(this);
	}
	@Override
	public ParsingExpression getExpression() {
		return this.inner;
	}
}
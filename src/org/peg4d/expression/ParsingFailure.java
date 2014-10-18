package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingFailure extends ParsingExpression {
	String message;
	ParsingFailure(String message) {
		super();
		this.message = message;
		this.minlen = 0;
	}
	public ParsingFailure(ParsingMatcher m) {
		super();
		this.message = "expecting " + m;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("!!\b"+message, this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.failure(this);
		return false;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitFailure(this);
	}
}
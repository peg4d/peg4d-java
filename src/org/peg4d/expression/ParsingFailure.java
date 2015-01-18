package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingFailure extends ParsingExpression {
	String message;
	ParsingFailure(String message) {
		super();
		this.message = message;
		this.minlen = 0;
	}
	public ParsingFailure(Recognizer m) {
		super();
		this.message = "expecting " + m;
	}
	@Override
	public
	String getInterningKey() {
		return "!!";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
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
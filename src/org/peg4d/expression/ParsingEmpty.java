package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

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
	public void visit(GrammarVisitor visitor) {
		visitor.visitEmpty(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return LazyAccept;
	}
}
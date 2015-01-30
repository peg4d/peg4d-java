package org.peg4d.expression;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIs extends ParsingFunction {
	int tagId;
	ParsingIs(int tagId) {
		super("is", null);
		this.tagId = tagId;
		this.minlen = 1;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean match(ParsingContext context) {
		return context.matchSymbolTableTop(tagId);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIs(this);
	}
	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tagId);
	}
}
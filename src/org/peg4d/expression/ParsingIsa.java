package org.peg4d.expression;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIsa extends ParsingFunction {
	int tableType;
	ParsingIsa(int tableType) {
		super("isa", null);
		this.tableType = tableType;
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
		return context.matchSymbolTable(tableType);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIsa(this);
	}
	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tableType);
	}
}
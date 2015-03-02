package org.peg4d.expression;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingCatch extends ParsingFunction {
	ParsingCatch() {
		super("catch", null);
		this.minlen = 0;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		throw new RuntimeException("TODO");
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		throw new RuntimeException("TODO");
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		throw new RuntimeException("TODO");
	}

	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		//context.left.setSourcePosition(context.fpos);
		context.left.setValue(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitCatch(this);
	}
}
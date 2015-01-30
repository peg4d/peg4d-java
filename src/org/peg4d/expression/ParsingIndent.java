package org.peg4d.expression;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIndent extends ParsingFunction {
	ParsingIndent() {
		super("indent", null);
		this.minlen = 0;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
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
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		return context.matchSymbolTableTop(PEG4d.Indent);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIndent(this);
	}
}
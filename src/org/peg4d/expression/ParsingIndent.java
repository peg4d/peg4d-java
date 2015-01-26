package org.peg4d.expression;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
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
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
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
	public boolean simpleMatch(ParsingContext context) {
		return context.matchSymbolTableTop(PEG4d.Indent);
	}
	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitIndent(this);
	}
}
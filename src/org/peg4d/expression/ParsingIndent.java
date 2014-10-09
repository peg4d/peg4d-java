package org.peg4d.expression;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;

public class ParsingIndent extends ParsingFunction {
	ParsingIndent() {
		super("indent");
	}
	@Override
	public short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return CheckNextFlow;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchTokenStackTop(PEG4d.Indent);
	}
}
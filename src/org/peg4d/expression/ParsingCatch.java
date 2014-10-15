package org.peg4d.expression;

import org.peg4d.ParsingContext;

public class ParsingCatch extends ParsingFunction {
	ParsingCatch() {
		super("catch");
		this.minlen = 0;
	}
	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setSourcePosition(context.fpos);
		context.left.setValue(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
		return true;
	}
}
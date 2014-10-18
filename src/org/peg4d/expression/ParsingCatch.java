package org.peg4d.expression;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingCatch extends ParsingCommand {
	ParsingCatch() {
		super("catch");
		this.minlen = 0;
	}
	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setSourcePosition(context.fpos);
		context.left.setValue(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitCatch(this);
	}
}
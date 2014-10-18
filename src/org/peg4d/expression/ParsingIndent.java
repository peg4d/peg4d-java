package org.peg4d.expression;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIndent extends ParsingCommand {
	ParsingIndent() {
		super("indent");
		this.minlen = 0;
	}
	@Override
	public short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchTokenStackTop(PEG4d.Indent);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIndent(this);
	}
}
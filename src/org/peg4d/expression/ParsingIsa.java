package org.peg4d.expression;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIsa extends ParsingCommand {
	int tagId;
	ParsingIsa(int tagId) {
		super("isa");
		this.tagId = tagId;
		this.minlen = 1;
	}
	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tagId);
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchTokenStack(tagId);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIsa(this);
	}
}
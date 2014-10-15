package org.peg4d.expression;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;

public class ParsingIsa extends ParsingFunction {
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
	public boolean simpleMatch(ParsingContext context) {
		return context.matchTokenStack(tagId);
	}
}
package org.peg4d.expression;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;

public class ParsingIsa extends ParsingFunction {
	int tagId;
	ParsingIsa(int tagId) {
		super("if");
		this.tagId = tagId;
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
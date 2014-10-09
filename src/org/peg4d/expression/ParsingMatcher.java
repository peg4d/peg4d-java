package org.peg4d.expression;

import org.peg4d.ParsingContext;

public abstract class ParsingMatcher {
	public abstract boolean simpleMatch(ParsingContext context);
	public String expectedToken() {
		return toString();
	}
}
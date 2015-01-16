package org.peg4d.expression;

import org.peg4d.ParsingContext;

public interface ParsingMatcher {
	public boolean simpleMatch(ParsingContext context);
}

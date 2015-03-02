package org.peg4d.expression;

import org.peg4d.ParsingContext;

public interface Recognizer {
	public boolean match(ParsingContext context);
}

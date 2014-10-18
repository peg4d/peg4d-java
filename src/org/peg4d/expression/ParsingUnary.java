package org.peg4d.expression;

import org.peg4d.UList;

public abstract class ParsingUnary extends ParsingExpression {
	public ParsingExpression inner;
	ParsingUnary(ParsingExpression e) {
		super();
		this.inner = e;
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final ParsingExpression get(int index) {
		return this.inner;
	}
	protected final int uniqueKey() {
		this.inner = inner.uniquefy();
		assert(this.inner.uniqueId != 0);
		return this.inner.uniqueId;
	}
	@Override
	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		int lmin = this.inner.checkLength(ruleName, start, minlen, stack);
		if(this instanceof ParsingOption || this instanceof ParsingRepetition || this instanceof ParsingNot || this instanceof ParsingAnd ) {
			this.minlen = 0;
		}
		else {
			this.minlen = lmin - minlen;
		}
		return this.minlen + minlen;
	}

	@Override
	public
	boolean hasObjectOperation() {
		return this.inner.hasObjectOperation();
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

import nez.util.UList;

public abstract class ParsingUnary extends ParsingExpression {
	public ParsingExpression inner;
	ParsingUnary(ParsingExpression e) {
		super();
		this.inner = e;
	}
	@Override
	public final int size() {
		return this.inner == null ? 0 : 1;
	}
	@Override
	public final ParsingExpression get(int index) {
		return this.inner;
	}
	@Override
	public final ParsingExpression set(int index, ParsingExpression e) {
		ParsingExpression old = this.inner;
		this.inner = e;
		return old;
	}
	@Override
	public ParsingExpression removeNodeOperator() {
		if(inner == null) {
			return this;
		}
		return ParsingExpression.dupUnary(this, inner.removeNodeOperator());
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String,String> undefedFlags) {
		if(inner == null) {
			return this;
		}
		return ParsingExpression.dupUnary(this, inner.removeFlag(undefedFlags));
	}

//	protected final int uniqueKey() {
//		this.inner = inner.intern();
//		assert(this.inner.internId != 0);
//		return this.inner.internId;
//	}
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
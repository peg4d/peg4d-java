package org.peg4d.expression;

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
	boolean hasObjectOperation() {
		return this.hasObjectOperation();
	}
	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}
}
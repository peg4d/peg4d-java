package org.peg4d.expression;

import org.peg4d.UList;

public abstract class ParsingList extends ParsingExpression {
	//UList<ParsingExpression> inners;
	ParsingExpression[] inners;
	ParsingList(UList<ParsingExpression> list) {
		super();
		this.inners = new ParsingExpression[list.size()];
		for(int i = 0; i < list.size(); i++) {
			this.inners[i] = list.get(i);
		}
	}
	@Override
	public final int size() {
		return this.inners.length;
	}
	@Override
	public final ParsingExpression get(int index) {
		return this.inners[index];
	}
	final void set(int index, ParsingExpression e) {
		this.inners[index] = e;
	}
	protected final String uniqueKey() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = this.get(i).uniquefy();
			set(i, e);
			sb.append(e.uniqueId);
			sb.append(":");
		}
		return sb.toString();
	}
	
	@Override
	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		int local = minlen;
		for(int i = 0; i < this.size(); i++) {
			minlen = this.get(i).checkLength(ruleName, start, minlen, stack);
		}
		this.minlen = minlen - local;
		return minlen;
	}

	@Override
	public
	boolean hasObjectOperation() {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).hasObjectOperation()) {
				return true;
			}
		}
		return false;
	}
	
	public final void swap(int i, int j) {
		ParsingExpression e = this.inners[i];
		this.inners[i] = this.inners[j];
		this.inners[j] = e;
	}
}
package org.peg4d.expression;

import nez.util.UList;

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
	@Override
	public ParsingExpression set(int index, ParsingExpression e) {
		ParsingExpression oldExpresion = this.inners[index];
		this.inners[index] = e;
		return oldExpresion;
	}
	public final void swap(int i, int j) {
		ParsingExpression e = this.inners[i];
		this.inners[i] = this.inners[j];
		this.inners[j] = e;
	}
	protected final UList<ParsingExpression> newList() {
		return new UList<ParsingExpression>(new ParsingExpression[this.size()]);
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		for(ParsingExpression e: this) {
			if(e.checkAlwaysConsumed(startNonTerminal, stack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ParsingExpression removeNodeOperator() {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(ParsingExpression e : this) {
			ParsingExpression.addSequence(l, e.removeNodeOperator());
		}
		return ParsingExpression.newSequence(l);
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
	
}
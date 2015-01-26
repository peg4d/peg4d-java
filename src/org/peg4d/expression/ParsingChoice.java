package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingChoice extends ParsingList {
	ParsingChoice(UList<ParsingExpression> list) {
		super(list);
	}
	@Override
	public
	String getInterningKey() {
		return "/";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		boolean afterAll = true;
		for(ParsingExpression e: this) {
			if(!e.checkAlwaysConsumed(startNonTerminal, stack)) {
				if(stack == null) {  // reconfirm 
					return false;
				}
				afterAll = false;
			}
		}
		return afterAll;
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		if(this.size() > 0) {
			return this.get(0).inferPEG4dTranstion(visited);
		}
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		int required = c.required;
		UList<ParsingExpression> l = newList();
		for(ParsingExpression e : this) {
			c.required = required;
			ParsingExpression.addChoice(l, e.checkPEG4dTransition(c));
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	public ParsingExpression removePEG4dOperator() {
		UList<ParsingExpression> l = newList();
		for(ParsingExpression e : this) {
			ParsingExpression.addChoice(l, e.removePEG4dOperator());
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(ParsingExpression e : this) {
			ParsingExpression.addChoice(l, e.removeParsingFlag(undefedFlags));
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, undefedFlags);
			ParsingExpression.addChoice(l, e);
		}
		return ParsingExpression.newChoice(l);
	}

	@Override
	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		int lmin = Integer.MAX_VALUE;
		for(int i = 0; i < this.size(); i++) {
			int nc = this.get(i).checkLength(ruleName, start, minlen, stack);
			if(nc < lmin) {
				lmin = nc;
			}
		}
		this.minlen = lmin - minlen;
		return minlen + this.minlen;
	}

	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitChoice(this);
	}
	@Override
	public short acceptByte(int ch) {
		boolean hasLazyAccept = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r == Accept) {
				return r;
			}
			if(r == Unconsumed) {
				hasLazyAccept = true;
			}
		}
		return hasLazyAccept ? Unconsumed : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingTree left = context.left;
		for(int i = 0; i < this.size(); i++) {
			context.left = left;
			if(this.get(i).matcher.simpleMatch(context)) {
				context.forgetFailure(f);
				left = null;
				return true;
			}
		}
		assert(context.isFailure());
		left = null;
		return false;
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
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
	public int inferNodeTransition(UMap<String> visited) {
		if(this.size() > 0) {
			return this.get(0).inferNodeTransition(visited);
		}
		return NodeTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		int required = c.required;
		UList<ParsingExpression> l = newList();
		for(ParsingExpression e : this) {
			c.required = required;
			ParsingExpression.addChoice(l, e.checkNodeTransition(c));
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	public ParsingExpression removeNodeOperator() {
		UList<ParsingExpression> l = newList();
		for(ParsingExpression e : this) {
			ParsingExpression.addChoice(l, e.removeNodeOperator());
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(ParsingExpression e : this) {
			ParsingExpression.addChoice(l, e.removeFlag(undefedFlags));
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
	public void visit(GrammarVisitor visitor) {
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
	public boolean match(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingTree left = context.left;
		for(int i = 0; i < this.size(); i++) {
			context.left = left;
			if(this.get(i).matcher.match(context)) {
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
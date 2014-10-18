package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingChoice extends ParsingList {
	ParsingChoice(UList<ParsingExpression> list) {
		super(list);
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
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("|\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, withoutMap);
			ParsingExpression.addChoice(l, e);
		}
		return ParsingExpression.newChoice(l);
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
			if(r == LazyAccept) {
				hasLazyAccept = true;
			}
		}
		return hasLazyAccept ? LazyAccept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
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
package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;

public class ParsingSequence extends ParsingList {
	ParsingSequence(UList<ParsingExpression> l) {
		super(l);
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression(" \b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitSequence(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).matcher.simpleMatch(context))) {
				context.abortLinkLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}
}
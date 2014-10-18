package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingSequence extends ParsingList {
	ParsingSequence(UList<ParsingExpression> l) {
		super(l);
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression(" \b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public short acceptByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r != LazyAccept) {
				return r;
			}
		}
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		int mark = context.markLogStack();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).matcher.simpleMatch(context))) {
				context.abortLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitSequence(this);
	}
}
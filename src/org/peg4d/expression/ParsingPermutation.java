package org.peg4d.expression;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingPermutation extends ParsingList {

	private Set<Integer> p;
	ParsingPermutation(UList<ParsingExpression> l) {
		super(l);
		this.p = new HashSet<Integer>();
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitPermutation(this);
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
		long ppos = context.getPosition();
		int pmark = context.markLogStack();
		int count = 0;
		for(int j = 0; j < this.size(); j++) {
			long f = context.rememberFailure();
			ParsingObject left = context.left;
			for(int i = 0; i < this.size(); i++) {
				context.left = left;
				if(p.contains(this.get(i).uniqueId)) {
					continue;
				}
				if((this.get(i).matcher.simpleMatch(context))) {
					p.add(this.get(i).uniqueId);
					context.forgetFailure(f);
					left = null;
					++count;
					break;
				}
			}
			if(context.isFailure()) {
				p.clear();
				left = null;
				return false;
			}
		}
		p.clear();
		if(count < this.size()) {
			context.abortLog(pmark);
			context.rollback(ppos);
			return false;
		}
		return true;
	}

	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newPermutation(l);
	}

	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("<perm " + this.uniqueKey() + " >", this);
	}

}

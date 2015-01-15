package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingRepeat extends ParsingFunction {

	ParsingRepeat(ParsingExpression inner) {
		super("repeat", inner);
	}

	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if (e == inner) {
			return this;
		}
		return ParsingExpression.newRepeat(e);
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitRepeat(this);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		try {
			int repeat = context.getRepeatValue(this.inner.toString());
			for(int i = 0; i < repeat; i++) {
				if(!this.inner.matcher.simpleMatch(context)) {
					context.rollback(pos);
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
}

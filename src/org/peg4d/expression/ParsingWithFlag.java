package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingWithFlag extends ParsingFunction {
	String flagName;
	ParsingWithFlag(String flagName, ParsingExpression inner) {
		super("with", inner);
		this.flagName = flagName;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		boolean removeWithout = false;
		if(withoutMap != null && withoutMap.containsKey(flagName)) {
			withoutMap.remove(flagName);
			removeWithout = true;
		}
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(removeWithout) {
			withoutMap.put(flagName, flagName);
		}
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newWithFlag(flagName, e);
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(ParsingIf.OldFlag) {
			final boolean currentFlag = context.getFlag(this.flagName);
			context.setFlag(this.flagName, true);
			boolean res = this.inner.matcher.simpleMatch(context);
			context.setFlag(this.flagName, currentFlag);
			return res;
		}
		return this.inner.matcher.simpleMatch(context);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitWithFlag(this);
	}

}
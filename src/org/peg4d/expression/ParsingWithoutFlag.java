package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingWithoutFlag extends ParsingOperation {
	String flagName;
	ParsingWithoutFlag(String flagName, ParsingExpression inner) {
		super("without", inner);
		this.flagName = flagName;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		boolean addWithout = false;
		if(withoutMap != null && !withoutMap.containsKey(flagName)) {
			withoutMap.put(flagName, flagName);
			addWithout = true;
		}
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(addWithout) {
			withoutMap.remove(flagName);
		}
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newWithoutFlag(flagName, e);
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(ParsingIf.OldFlag) {
			final boolean currentFlag = context.getFlag(this.flagName);
			context.setFlag(this.flagName, false);
			boolean res = this.inner.matcher.simpleMatch(context);
			context.setFlag(this.flagName, currentFlag);
			return res;
		}
		return this.inner.matcher.simpleMatch(context);
	}
}
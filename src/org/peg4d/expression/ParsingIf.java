package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingIf extends ParsingFunction {
	public final static boolean OldFlag = false;
	String flagName;
	ParsingIf(String flagName) {
		super("if");
		this.flagName = flagName;
		this.minlen = 0;
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(ParsingIf.OldFlag) {
			Boolean f = context.getFlag(this.flagName);
			if(!context.isFlag(f)) {
				context.failure(null);
			}
			return !(context.isFailure());
		}
		return true;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(withoutMap != null && withoutMap.containsKey(flagName)) {
			return ParsingExpression.newFailure(this);
		}
		return this;
	}
}
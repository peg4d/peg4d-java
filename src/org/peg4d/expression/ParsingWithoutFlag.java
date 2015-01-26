package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingWithoutFlag extends ParsingFunction {
	String flagName;
	ParsingWithoutFlag(String flagName, ParsingExpression inner) {
		super("without", inner);
		this.flagName = flagName;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		return this.inner.inferPEG4dTranstion(visited);
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		this.inner = this.inner.checkPEG4dTransition(c);
		return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String,String> undefedFlags) {
		boolean addWithout = false;
		if(undefedFlags != null && !undefedFlags.containsKey(flagName)) {
			undefedFlags.put(flagName, flagName);
			addWithout = true;
		}
		ParsingExpression e = inner.removeParsingFlag(undefedFlags);
		if(addWithout) {
			undefedFlags.remove(flagName);
		}
		return e;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		boolean addWithout = false;
		if(undefedFlags != null && !undefedFlags.containsKey(flagName)) {
			undefedFlags.put(flagName, flagName);
			addWithout = true;
		}
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if(addWithout) {
			undefedFlags.remove(flagName);
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
	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitWithoutFlag(this);
	}

}
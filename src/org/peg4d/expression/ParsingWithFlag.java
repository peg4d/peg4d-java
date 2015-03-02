package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingWithFlag extends ParsingFunction {
	String flagName;
	ParsingWithFlag(String flagName, ParsingExpression inner) {
		super("with", inner);
		this.flagName = flagName;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return this.inner.inferNodeTransition(visited);
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		this.inner = this.inner.checkNodeTransition(c);
		return this;
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String,String> undefedFlags) {
		boolean removeWithout = false;
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			undefedFlags.remove(flagName);
			removeWithout = true;
		}
		ParsingExpression e = inner.removeFlag(undefedFlags);
		if(removeWithout) {
			undefedFlags.put(flagName, flagName);
		}
		return e;
	}

	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		boolean removeWithout = false;
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			undefedFlags.remove(flagName);
			removeWithout = true;
		}
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if(removeWithout) {
			undefedFlags.put(flagName, flagName);
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
	public boolean match(ParsingContext context) {
		if(ParsingIf.OldFlag) {
			final boolean currentFlag = context.getFlag(this.flagName);
			context.setFlag(this.flagName, true);
			boolean res = this.inner.matcher.match(context);
			context.setFlag(this.flagName, currentFlag);
			return res;
		}
		return this.inner.matcher.match(context);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitWithFlag(this);
	}

}
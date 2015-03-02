package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIf extends ParsingFunction {
	public final static boolean OldFlag = false;
	String flagName;
	ParsingIf(String flagName) {
		super("if", null);
		this.flagName = flagName;
		this.minlen = 0;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String,String> undefedFlags) {
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			return ParsingExpression.newFailure(this);
		}
		return this;
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean match(ParsingContext context) {
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
	public short acceptByte(int ch) {
		return Unconsumed;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			return ParsingExpression.newFailure(this);
		}
		return this;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIfFlag(this);
	}

}
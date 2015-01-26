package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
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
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String,String> undefedFlags) {
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
	public void accept(GrammarVisitor visitor) {
		visitor.visitIfFlag(this);
	}

}
package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingMatch extends ParsingFunction {
	ParsingMatch(ParsingExpression inner) {
		super("match", inner);
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return this.get(0).checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		this.inner = this.inner.removePEG4dOperator();
		return this;
	}

	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
	//		ParsingExpression e = inner.normalizeImpl(true, flagMap, undefedFlags);
	//		if(e == inner) {
	//			return this;
	//		}
	//		return ParsingExpression.newMatch(e);
		return inner.norm(true, undefedFlags);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		boolean oldMode = context.setRecognitionMode(true);
//		ParsingObject left = context.left;
//		if(this.inner.matcher.simpleMatch(context)) {
//			context.setRecognitionMode(oldMode);
//			context.left = left;
//			left = null;
//			return true;
//		}
//		context.setRecognitionMode(oldMode);
//		left = null;
//		return false;
		return this.inner.matcher.simpleMatch(context);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitMatch(this);
	}
}
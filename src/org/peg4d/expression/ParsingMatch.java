package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
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
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		this.inner = this.inner.removeNodeOperator();
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
	public boolean match(ParsingContext context) {
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
		return this.inner.matcher.match(context);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitMatch(this);
	}
}
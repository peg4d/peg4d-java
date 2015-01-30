package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingApply extends ParsingFunction {
	ParsingApply(ParsingExpression inner) {
		super("apply", inner);
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		throw new RuntimeException("TODO");
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		throw new RuntimeException("TODO");
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		throw new RuntimeException("TODO");
	}

	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		//TODO;
		return null;
	}
	@Override
	public boolean match(ParsingContext context) {
//		ParsingContext s = new ParsingContext(context.left);
//		
//		this.inner.matcher.simpleMatch(s);
//		context.opRememberPosition();
//		context.opRememberFailurePosition();
//		context.opStoreObject();
//		this.inner.matcher.simpleMatch(context);
//		context.opDebug(this.inner);
		return !(context.isFailure());

	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitApply(this);
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingAssert extends ParsingFunction {
	protected ParsingAssert(ParsingExpression inner) {
		super("assert", inner);
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		throw new RuntimeException("TODO");
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		throw new RuntimeException("TODO");
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		throw new RuntimeException("TODO");
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newDebug(e);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		ParsingTree left = context.left;
		this.inner.matcher.simpleMatch(context);
		if(context.isFailure()) {
			assert(pos == context.getPosition());
			System.out.println(context.source.formatPositionLine("debug", context.getPosition(), "failure at pos=" + pos  + " in " + inner));
			left = null;
			return false;
		}
//		if(context.left != left) {
//			System.out.println(context.source.formatPositionLine("debug", pos,
//				"transition #" + context.left.getTag() + " => #" + left.getTag() + " in " + inner));
//			return true;
//		}
		else if(context.getPosition() != pos) {
			System.out.println(context.source.formatPositionMessage("debug", pos,
				"consumed pos=" + pos + " => " + context.getPosition() + " in " + inner));
		}
		else {
			System.out.println(context.source.formatPositionLine("debug", pos, "pass and unconsumed at pos=" + pos + " in " + inner));
		}
		left = null;
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitAssert(this);
	}

}
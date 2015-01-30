package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingScan extends ParsingFunction {
	public int number; 
	public ParsingExpression repeatExpression;
	ParsingScan(int number, ParsingExpression scan, ParsingExpression repeat) {
		super("scan", scan);
		this.number = number;
		this.repeatExpression = repeat;
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
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> undefedFlags) {
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if (e == inner) {
			return this;
		}
		return ParsingExpression.newScan(number, e, repeatExpression);
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitScan(this);
	}

	@Override
	public boolean match(ParsingContext context) {
		long start = context.pos;
		if(this.inner.matcher.match(context)) {
			String value = context.getRepeatByteString(start);
			context.setRepeatExpression(this.repeatExpression.toString(), Integer.parseInt(value, this.number));
			return true;
		}
		return false;
	}
	
}

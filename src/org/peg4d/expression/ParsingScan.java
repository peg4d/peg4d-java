package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
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
	public int inferPEG4dTranstion(UMap<String> visited) {
		throw new RuntimeException("TODO");
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
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
	public void accept(GrammarVisitor visitor) {
		visitor.visitScan(this);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		long start = context.pos;
		if(this.inner.matcher.simpleMatch(context)) {
			String value = context.getRepeatByteString(start);
			context.setRepeatExpression(this.repeatExpression.toString(), Integer.parseInt(value, this.number));
			return true;
		}
		return false;
	}
	
}

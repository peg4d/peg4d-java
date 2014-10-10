package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;

public class ParsingBlock extends ParsingOperation {
	ParsingBlock(ParsingExpression e) {
		super("block", e);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newBlock(e);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		String indent = context.source.getIndentText(context.pos);
		int stackTop = context.pushTokenStack(PEG4d.Indent, indent);
		boolean b = this.inner.matcher.simpleMatch(context);
		context.popTokenStack(stackTop);
		return b;
	}
}
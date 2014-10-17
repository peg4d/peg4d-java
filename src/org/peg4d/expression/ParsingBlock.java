package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingBlock extends ParsingFunction {
	ParsingBlock(ParsingExpression e) {
		super("block", e);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
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
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitBlock(this);
	}

}
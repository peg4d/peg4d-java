package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.PEG4d;
import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingBlock extends ParsingFunction {
	ParsingBlock(ParsingExpression e) {
		super("block", e);
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		return this.inner.inferPEG4dTranstion(visited);
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		this.inner = this.inner.checkPEG4dTransition(c);
		return this;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newBlock(e);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int stateValue = context.stateValue;
		String indent = context.source.getIndentText(context.pos);
		int stackTop = context.pushSymbolTable(PEG4d.Indent, indent);
		boolean b = this.inner.matcher.simpleMatch(context);
		context.popSymbolTable(stackTop);
		context.stateValue = stateValue;
		return b;
	}
	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitBlock(this);
	}

}
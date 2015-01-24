package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingSource;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingAny extends ParsingExpression {
	ParsingAny() {
		super();
		this.minlen = 1;
	}
	@Override
	public String getInterningKey() { 
		return ".";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
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
	public ParsingExpression removePEG4dOperator() {
		return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return (ch == ParsingSource.EOF) ? Reject : Accept;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitAny(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.charAt(context.pos) != -1) {
			int len = context.source.charLength(context.pos);
			context.consume(len);
			return true;
		}
		context.failure(this);
		return false;
	}
}
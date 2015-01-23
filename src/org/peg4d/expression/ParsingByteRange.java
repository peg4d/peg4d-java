package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingByteRange extends ParsingExpression {
	public int startByteChar;
	public int endByteChar;
	ParsingByteRange(int startByteChar, int endByteChar) {
		super();
		this.startByteChar = startByteChar;
		this.endByteChar = endByteChar;
		this.minlen = 1;
	}
	@Override
	public 
	String getInterningKey() { 
		return "[" + startByteChar + "-" + endByteChar;
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
	public ParsingExpression transformPEG() {
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
	void setCount(int[] count) {
		for(int c = startByteChar; c <= endByteChar; c++) {
			count[c]++;
		}
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitByteRange(this);
	}
	@Override 
	public short acceptByte(int ch) {
		return (startByteChar <= ch && ch <= endByteChar) ? Accept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int ch = context.source.byteAt(context.pos);
		if(startByteChar <= ch && ch <= endByteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}
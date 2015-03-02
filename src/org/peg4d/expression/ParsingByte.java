package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingByte extends ParsingExpression {
	public int byteChar;
	String errorToken = null;
	ParsingByte(int ch) {
		super();
		this.byteChar = ch;
		this.minlen = 1;
	}
	@Override
	public String getInterningKey() { 
		return "'" + byteChar;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public ParsingExpression removeNodeOperator() {
		return this;
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> undefedFlags) {
		return this;
	}
//	public String expectedToken() {
//		if(this.errorToken != null) {
//			return this.errorToken;
//		}
//		return this.toString();
//	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitByte(this);
	}
	@Override
	public short acceptByte(int ch) {
		return (byteChar == ch) ? Accept : Reject;
	}
	@Override
	public boolean match(ParsingContext context) {
		if(context.source.byteAt(context.pos) == this.byteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}
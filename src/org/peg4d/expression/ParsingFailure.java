package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingFailure extends ParsingExpression {
	String message;
	ParsingFailure(String message) {
		super();
		this.message = message;
		this.minlen = 0;
	}
	public ParsingFailure(Recognizer m) {
		super();
		this.message = "expecting " + m;
	}
	@Override
	public
	String getInterningKey() {
		return "!!";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
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
	@Override
	public short acceptByte(int ch) {
		return Reject;
	}
	@Override
	public boolean match(ParsingContext context) {
		context.failure(this);
		return false;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitFailure(this);
	}
}
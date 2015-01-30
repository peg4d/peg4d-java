package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingString extends ParsingExpression {
	public String text;
	public byte[] utf8;
	ParsingString(String text, byte[] utf8) {
		super();
		this.text = text;
		this.utf8 = utf8;
		this.minlen = utf8.length;
	}
	@Override
	public
	String getInterningKey() { 
		return "''" + text;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return utf8.length > 0;
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
	public void visit(GrammarVisitor visitor) {
		visitor.visitString(this);
	}
	@Override public short acceptByte(int ch) {
		if(this.utf8.length == 0) {
			return Unconsumed;
		}
		return ((this.utf8[0] & 0xff) == ch) ? Accept : Reject;
	}
	@Override
	public boolean match(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(this.utf8.length);
			return true;
		}
		else {
			context.failure(this);
			return false;
		}
	}
}
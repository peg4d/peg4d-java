package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingSequence extends ParsingList {
	ParsingSequence(UList<ParsingExpression> l) {
		super(l);
	}
	@Override
	public
	String getInterningKey() {
		return " ";
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		for(ParsingExpression e: this) {
			int t = e.inferNodeTransition(visited);
			if(t == NodeTransition.ObjectType || t == NodeTransition.OperationType) {
				return t;
			}
		}
		return NodeTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		UList<ParsingExpression> l = newList();
		for(ParsingExpression e : this) {
			ParsingExpression.addSequence(l, e.checkNodeTransition(c));
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<ParsingExpression> l = newList();
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).removeFlag(undefedFlags);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, undefedFlags);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public short acceptByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r != Unconsumed) {
				return r;
			}
		}
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		long pos = context.getPosition();
		int mark = context.markLogStack();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).matcher.match(context))) {
				context.abortLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitSequence(this);
	}
}
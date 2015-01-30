package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingNot extends ParsingUnary {
	ParsingNot(ParsingExpression e) {
		super(e);
	}
	@Override
	public String getInterningKey() { 
		return "!";
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
		int t = this.inner.inferNodeTransition(null);
		if(t == NodeTransition.ObjectType || t == NodeTransition.OperationType) {
			this.inner = this.inner.removeNodeOperator();
		}
		return this;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		ParsingExpression e = inner.norm(true, undefedFlags);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newNot(e);
	}
	@Override
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		/* the code below works only if a single character in !(e) */
		/* we must accept 'i' for !'int' 'i' */
//		if(r == Accept || r == LazyAccept) {
//			return Reject;
//		}
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingTree left = context.left;
		if(this.inner.matcher.match(context)) {
			context.rollback(pos);
			context.failure(this);
			left = null;
			return false;
		}
		else {
			context.rollback(pos);
			context.forgetFailure(f);
			context.left = left;
			left = null;
			return true;
		}
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitNot(this);
	}
}
package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.UList;
import org.peg4d.UMap;
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
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		int t = this.inner.inferPEG4dTranstion(null);
		if(t == PEG4dTransition.ObjectType || t == PEG4dTransition.OperationType) {
			this.inner = this.inner.removePEG4dOperator();
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
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingTree left = context.left;
		if(this.inner.matcher.simpleMatch(context)) {
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
	public void accept(GrammarVisitor visitor) {
		visitor.visitNot(this);
	}
}
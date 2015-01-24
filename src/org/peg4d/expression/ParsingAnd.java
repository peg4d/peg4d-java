package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingAnd extends ParsingUnary {
	ParsingAnd(ParsingExpression e) {
		super(e);
	}
	@Override
	public String getInterningKey() { 
		return "&";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		int t = this.inner.inferPEG4dTranstion(visited);
		if(t == PEG4dTransition.ObjectType) {  // typeCheck needs to report error
			return PEG4dTransition.BooleanType;
		}
		return t;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		if(c.required == PEG4dTransition.ObjectType) {
			c.required = PEG4dTransition.BooleanType;
			this.inner = this.inner.checkPEG4dTransition(c);
			c.required = PEG4dTransition.ObjectType;
		}
		else {
			this.inner = this.inner.checkPEG4dTransition(c);
		}
		return this;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newAnd(e);
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitAnd(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		this.inner.matcher.simpleMatch(context);
		context.rollback(pos);
		return !context.isFailure();
	}
	
	@Override
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept || r == Unconsumed) {
			return Unconsumed;
		}
		return r;
	}
	
}
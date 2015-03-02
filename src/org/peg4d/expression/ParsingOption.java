package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingOption extends ParsingUnary {
	ParsingOption(ParsingExpression e) {
		super(e);
	}
	@Override
	public String getInterningKey() { 
		return "?";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		int t = this.inner.inferNodeTransition(visited);
		if(t == NodeTransition.ObjectType) {
			return NodeTransition.BooleanType;
		}
		return t;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		int required = c.required;
		ParsingExpression inn = this.inner.checkNodeTransition(c);
		if(required != NodeTransition.OperationType && c.required == NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "unable to create objects in repetition");
			this.inner = inn.removeNodeOperator();
			c.required = required;
		}
		else {
			this.inner = inn;
		}
		return this;
	}
	
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newOption(e);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitOptional(this);
	}
	@Override 
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingTree left = context.left;
		if(!this.inner.matcher.match(context)) {
			context.left = left;
			context.forgetFailure(f);
		}
		left = null;
		return true;
	}
}
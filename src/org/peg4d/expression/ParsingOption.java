package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
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
	public int inferPEG4dTranstion(UMap<String> visited) {
		int t = this.inner.inferPEG4dTranstion(visited);
		if(t == PEG4dTransition.ObjectType) {
			return PEG4dTransition.BooleanType;
		}
		return t;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		int required = c.required;
		ParsingExpression inn = this.inner.checkPEG4dTransition(c);
		if(required != PEG4dTransition.OperationType && c.required == PEG4dTransition.OperationType) {
			this.report(ReportLevel.warning, "unable to create objects in repetition");
			this.inner = inn.transformPEG();
			c.required = required;
		}
		else {
			this.inner = inn;
		}
		return this;
	}
	
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
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
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingTree left = context.left;
		if(!this.inner.matcher.simpleMatch(context)) {
			context.left = left;
			context.forgetFailure(f);
		}
		left = null;
		return true;
	}
}
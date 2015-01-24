package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingRepetition extends ParsingUnary {
	ParsingRepetition(ParsingExpression e) {
		super(e);
	}
	@Override
	public String getInterningKey() { 
		return "*";
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
		if(!this.inner.checkAlwaysConsumed(null, null)) {
			this.report(ReportLevel.warning, "empty repetition");
		}
		ParsingExpression inn = this.inner.checkPEG4dTransition(c);
		if(required != PEG4dTransition.OperationType && c.required == PEG4dTransition.OperationType) {
			this.report(ReportLevel.warning, "unable to create objects in repetition");
			this.inner = inn.removePEG4dOperator();
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
		return ParsingExpression.newRepetition(e);
	}
	@Override
	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		this.inner.checkLength(ruleName, start, minlen, stack);
//		if(!(this.inner.minlen > 0) && !(this.inner instanceof NonTerminal)) {
//			this.report(ReportLevel.warning, "uncosumed repetition: " + this.inner.minlen);
//		}
		this.minlen = 0;
		return this.minlen + minlen;
	}

	@Override public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long ppos = -1;
		long pos = context.getPosition();
//		long f = context.rememberFailure();
		while(ppos < pos) {
			ParsingTree left = context.left;
			if(!this.inner.matcher.simpleMatch(context)) {
				context.left = left;
				left = null;
				break;
			}
			ppos = pos;
			pos = context.getPosition();
			left = null;
		}
//		context.forgetFailure(f);
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitRepetition(this);
	}
}
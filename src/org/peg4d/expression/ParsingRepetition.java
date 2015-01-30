package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
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
		if(!this.inner.checkAlwaysConsumed(null, null)) {
			this.report(ReportLevel.warning, "empty repetition");
		}
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
	public boolean match(ParsingContext context) {
		long ppos = -1;
		long pos = context.getPosition();
//		long f = context.rememberFailure();
		while(ppos < pos) {
			ParsingTree left = context.left;
			if(!this.inner.matcher.match(context)) {
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
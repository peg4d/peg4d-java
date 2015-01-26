package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingConnector extends ParsingUnary {
	public int index;
	ParsingConnector(ParsingExpression e, int index) {
		super(e);
		this.index = index;
	}
	@Override
	public
	String getInterningKey() {
		return (index != -1) ? "@" + index : "@";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.OperationType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		if(c.required != PEG4dTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected connector");
			return this.inner.removePEG4dOperator();
		}
		c.required = PEG4dTransition.ObjectType;
		ParsingExpression inn = inner.checkPEG4dTransition(c);
		if(c.required != PEG4dTransition.OperationType) {
			this.report(ReportLevel.warning, "no object created");
			c.required = PEG4dTransition.OperationType;
			return inn;
		}
		c.required = PEG4dTransition.OperationType;
		this.inner = inn;
		return this;
	}
	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		if(this.isRemovedOperation()) {
			lexOnly = true;
		}
		ParsingExpression e = this.inner.norm(lexOnly, undefedFlags);
		if(this.isNothingConnected() || lexOnly) {
			return e;
		}
		return ParsingExpression.newConnector(e, this.index);
	}

	@Override
	public short acceptByte(int ch) {
		return inner.acceptByte(ch);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingTree left = context.left;
		int mark = context.markLogStack();
		if(this.inner.matcher.simpleMatch(context)) {
			if(context.left != left) {
				context.commitLog(mark, context.left);
				context.lazyLink(left, this.index, context.left);
			}
			context.left = left;
			left = null;
			return true;
		}
		context.abortLog(mark);			
		left = null;
		return false;
	}
	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitConnector(this);
	}
}
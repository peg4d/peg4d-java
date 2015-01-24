package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingRepeat extends ParsingFunction {
	ParsingRepeat(ParsingExpression inner) {
		super("repeat", inner);
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(startNonTerminal, stack);
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
			this.report(ReportLevel.warning, "unable to create objects in repeat");
			this.inner = inn.removePEG4dOperator();
			c.required = required;
		}
		else {
			this.inner = inn;
		}
		return this;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> undefedFlags) {
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if (e == inner) {
			return this;
		}
		return ParsingExpression.newRepeat(e);
	}

	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitRepeat(this);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		try {
			int repeat = context.getRepeatValue(this.inner.toString());
			for(int i = 0; i < repeat; i++) {
				if(!this.inner.matcher.simpleMatch(context)) {
					context.rollback(pos);
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
}

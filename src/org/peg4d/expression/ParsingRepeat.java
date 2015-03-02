package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
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
			this.report(ReportLevel.warning, "unable to create objects in repeat");
			this.inner = inn.removeNodeOperator();
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
	public boolean match(ParsingContext context) {
		long pos = context.getPosition();
		try {
			int repeat = context.getRepeatValue(this.inner.toString());
			for(int i = 0; i < repeat; i++) {
				if(!this.inner.matcher.match(context)) {
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

package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public class Sequence extends ExpressionList {
	Sequence(SourcePosition s, UList<Expression> l) {
		super(s, l);
	}
	@Override
	public String getPredicate() {
		return "seq";
	}	
	@Override
	public String getInterningKey() {
		return " ";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		for(Expression e: this) {
			if(e.checkAlwaysConsumed(startNonTerminal, stack)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public Expression removeNodeOperator() {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			Factory.addSequence(l, e.removeNodeOperator());
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		for(Expression e: this) {
			int t = e.inferNodeTransition(visited);
			if(t == NodeTransition.ObjectType || t == NodeTransition.OperationType) {
				return t;
			}
		}
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		UList<Expression> l = newList();
		for(Expression e : this) {
			Factory.addSequence(l, e.checkNodeTransition(c));
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = newList();
		for(int i = 0; i < this.size(); i++) {
			Expression e = get(i).removeFlag(undefedFlags);
			Factory.addSequence(l, e);
		}
		return Factory.newSequence(s, l);
	}
	@Override
	public short acceptByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r != Unconsumed) {
				return r;
			}
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		long pos = context.getPosition();
		int mark = context.markLogStack();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).matcher.match(context))) {
				context.abortLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}
}

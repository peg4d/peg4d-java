package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public class Choice extends ExpressionList {
	Choice(SourcePosition s, UList<Expression> l) {
		super(s, l);
	}
	@Override
	public String getPredicate() {
		return "/";
	}
	@Override
	public String getInterningKey() {
		return "/";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		boolean afterAll = true;
		for(Expression e: this) {
			if(!e.checkAlwaysConsumed(startNonTerminal, stack)) {
				if(stack == null) {  // reconfirm 
					return false;
				}
				afterAll = false;
			}
		}
		return afterAll;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		if(this.size() > 0) {
			return this.get(0).inferNodeTransition(visited);
		}
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		int required = c.required;
		UList<Expression> l = newList();
		for(Expression e : this) {
			c.required = required;
			Factory.addChoice(l, e.checkNodeTransition(c));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public Expression removeNodeOperator() {
		UList<Expression> l = newList();
		for(Expression e : this) {
			Factory.addChoice(l, e.removeNodeOperator());
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			Factory.addChoice(l, e.removeFlag(undefedFlags));
		}
		return Factory.newChoice(this.s, l);
	}
	@Override
	public short acceptByte(int ch) {
		boolean hasUnconsumed = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r == Accept) {
				return r;
			}
			if(r == Unconsumed) {
				hasUnconsumed = true;
			}
		}
		return hasUnconsumed ? Unconsumed : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		//long f = context.rememberFailure();
		Node left = context.left;
		for(int i = 0; i < this.size(); i++) {
			context.left = left;
			if(this.get(i).matcher.match(context)) {
				//context.forgetFailure(f);
				left = null;
				return true;
			}
		}
		assert(context.isFailure());
		left = null;
		return false;
	}
}

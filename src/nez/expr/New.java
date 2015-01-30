package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

public class New extends ExpressionList {
	int prefetchIndex = 0;
	New(SourcePosition s, UList<Expression> list) {
		super(s, list);
	}
	@Override
	public String getPredicate() { 
		return "new";
	}
	@Override
	public String getInterningKey() {
		return "{}";
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
		return NodeTransition.ObjectType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		if(c.required != NodeTransition.ObjectType) {
			this.report(ReportLevel.warning, "unexpected constructor");
			return this.removeNodeOperator();
		}
		c.required = NodeTransition.OperationType;
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			Expression e = get(i).removeFlag(undefedFlags);
			Factory.addSequence(l, e);
		}
		return Factory.newNew(s, l);
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
		long startIndex = context.getPosition();
//		ParsingObject left = context.left;
		for(int i = 0; i < this.prefetchIndex; i++) {
			if(!this.get(i).matcher.match(context)) {
				context.rollback(startIndex);
				return false;
			}
		}
		int mark = context.markLogStack();
		Node newnode = context.newNode(startIndex, this);
		context.left = newnode;
		for(int i = this.prefetchIndex; i < this.size(); i++) {
			if(!this.get(i).matcher.match(context)) {
				context.abortLog(mark);
				context.rollback(startIndex);
				newnode = null;
				return false;
			}
		}
		newnode.setEndingPosition(context.getPosition());
		context.left = newnode;
		return true;
	}
}
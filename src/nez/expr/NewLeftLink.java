package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;

public class NewLeftLink extends New {
	NewLeftLink(SourcePosition s, UList<Expression> list) {
		super(s, list);
	}
	@Override
	public String getPredicate() { 
		return "new-left-link";
	}
	@Override
	public String getInterningKey() {
		return "{@}";
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected left-associative constructor");
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
		return Factory.newNewLeftLink(this.s, l);
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
		context.lazyJoin(context.left);
		context.lazyLink(newnode, 0, context.left);
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
		//context.commitLinkLog2(newnode, startIndex, mark);
		//System.out.println("newnode: " + newnode.oid);
		context.left = newnode;
		newnode = null;
		return true;
	}
}
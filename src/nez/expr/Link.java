package nez.expr;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

public class Link extends Unary {
	public int index;
	Link(SourcePosition s, Expression e, int index) {
		super(s, e);
		this.index = index;
	}
	@Override
	public String getPredicate() { 
		return (index != -1) ? "link " + index : "link";
	}
	@Override
	public String getInterningKey() {
		return (index != -1) ? "@" + index : "@";
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newLink(this.s, e, this.index) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.OperationType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected connector");
			return this.inner.removeNodeOperator();
		}
		c.required = NodeTransition.ObjectType;
		Expression inn = inner.checkNodeTransition(c);
		if(c.required != NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "no object created");
			c.required = NodeTransition.OperationType;
			return inn;
		}
		c.required = NodeTransition.OperationType;
		this.inner = inn;
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return inner.acceptByte(ch);
	}
	@Override
	public boolean match(SourceContext context) {
		Node left = context.left;
		int mark = context.markLogStack();
		if(this.inner.matcher.match(context)) {
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
}
package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public class And extends Unary {
	And(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() {
		return "&";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		int t = this.inner.inferNodeTransition(visited);
		if(t == NodeTransition.ObjectType) {  // typeCheck needs to report error
			return NodeTransition.BooleanType;
		}
		return t;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		if(c.required == NodeTransition.ObjectType) {
			c.required = NodeTransition.BooleanType;
			this.inner = this.inner.checkNodeTransition(c);
			c.required = NodeTransition.ObjectType;
		}
		else {
			this.inner = this.inner.checkNodeTransition(c);
		}
		return this;
	}

	@Override
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept || r == Unconsumed) {
			return Unconsumed;
		}
		return r;
	}
	@Override
	public boolean match(SourceContext context) {
		long pos = context.getPosition();
		this.inner.matcher.match(context);
		context.rollback(pos);
		return !context.isFailure();
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newAnd(this.s, e) : this;
	}
	
	
}
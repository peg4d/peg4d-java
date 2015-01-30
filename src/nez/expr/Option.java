package nez.expr;

import nez.SourceContext;
import nez.ast.Node;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

public class Option extends Unary {
	Option(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newOption(this.s, e) : this;
	}
	@Override
	public String getPredicate() { 
		return "*";
	}
	@Override
	public String getInterningKey() { 
		return "?";
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
	public Expression checkNodeTransition(NodeTransition c) {
		int required = c.required;
		Expression inn = this.inner.checkNodeTransition(c);
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
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		//long f = context.rememberFailure();
		Node left = context.left;
		if(!this.inner.matcher.match(context)) {
			context.left = left;
			//context.forgetFailure(f);
		}
		left = null;
		return true;
	}

}
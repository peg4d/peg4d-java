package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public class Match extends Unary {
	Match(SourcePosition s, Expression inner) {
		super(s, inner);
	}
	@Override
	public String getPredicate() { 
		return "~";
	}
	@Override
	public String getInterningKey() { 
		return "~";
	}	
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newMatch(this.s, e) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		return this.inner.removeNodeOperator();
	}
	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}
	@Override
	public boolean match(SourceContext context) {
		return this.inner.matcher.match(context);
	}
}
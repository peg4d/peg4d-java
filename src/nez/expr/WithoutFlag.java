package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public class WithoutFlag extends Unary {
	String flagName;
	WithoutFlag(SourcePosition s, String flagName, Expression inner) {
		super(s, inner);
		this.flagName = flagName;
		this.matcher = inner.matcher;
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newWithoutFlag(this.s, this.flagName, e) : this;
	}
	@Override
	public String getPredicate() {
		return "without " + this.flagName;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return this.inner.inferNodeTransition(visited);
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		this.inner = this.inner.checkNodeTransition(c);
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		boolean addWithout = false;
		if(undefedFlags != null && !undefedFlags.containsKey(flagName)) {
			undefedFlags.put(flagName, flagName);
			addWithout = true;
		}
		Expression e = inner.removeFlag(undefedFlags);
		if(addWithout) {
			undefedFlags.remove(flagName);
		}
		return e;
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
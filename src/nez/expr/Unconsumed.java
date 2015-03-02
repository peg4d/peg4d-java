package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

abstract class Unconsumed extends Expression {
	protected Unconsumed(SourcePosition s) {
		super(s);
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final Expression get(int index) {
		return null;
	}
	@Override
	public String getInterningKey() {
		return this.getPredicate();
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		return true;
	}
}

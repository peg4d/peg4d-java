package nez.expr;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UMap;

public abstract class Terminal extends Expression {
	Terminal(SourcePosition s) {
		super(s);
	}
	@Override
	public final Expression get(int index) {
		return null;
	}
	@Override
	public final int size() {
		return 0;
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

}

package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;

public class Failure extends Unconsumed {
	Failure(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "fail";
	}
	@Override
	public String getInterningKey() {
		return "!!";
	}
	@Override
	public short acceptByte(int ch) {
		return Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.failure2(this);
	}
}
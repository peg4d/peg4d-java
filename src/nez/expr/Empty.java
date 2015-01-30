package nez.expr;

import nez.ast.SourcePosition;

public class Empty extends Unconsumed {
	Empty(SourcePosition s) {
		super(s);
	}	
	@Override
	public String getPredicate() {
		return "empty";
	}
	
	@Override
	public String getInterningKey() {
		return "";
	}
}

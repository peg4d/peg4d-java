package nez.expr;

import java.util.TreeMap;

import nez.ast.SourcePosition;

public class IfFlag extends Unconsumed {
	String flagName;
	IfFlag(SourcePosition s, String flagName) {
		super(s);
		this.flagName = flagName;
	}
	@Override
	public String getPredicate() {
		return "if " + this.flagName;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			return Factory.newFailure(this.s);
		}
		return this;
	}

}
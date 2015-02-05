package nez;

import nez.ast.SourcePosition;
import nez.expr.Expression;
import nez.expr.Rule;
import nez.util.UList;
import nez.util.UMap;

public class Grammar {
	String               name;
	UMap<Rule>           ruleMap;
	public UList<String> nameList;
	public boolean foundError = false;

	public Grammar(String name) {
		this.name = name;
		this.ruleMap = new UMap<Rule>();
	}

	public String uniqueName(String rulename) {
		return this.name + ":" + rulename;
	}
	
	public final Rule newRule(String name, Expression e) {
		Rule r = new Rule(null, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}

	public final Rule defineRule(SourcePosition s, String name, Expression e) {
		if(!hasRule(name)) {
			nameList.add(name);
		}
		Rule r = new Rule(s, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}
	
//	public int getRuleSize() {
//		return this.ruleMap.size();
//	}

	public final boolean hasRule(String ruleName) {
		return this.ruleMap.get(ruleName) != null;
	}

	public final Rule getRule(String ruleName) {
		return this.ruleMap.get(ruleName);
	}

	public final Production getProduction(String name) {
		Rule r = this.getRule(name);
		if(r != null) {
			return new Production(r);
		}
		return null;
	}


}

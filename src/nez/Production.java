package nez;

import nez.expr.Expression;
import nez.expr.NonTerminal;
import nez.expr.Rule;
import nez.util.UMap;

public class Production {
	Rule startingRule;
	UMap<Rule>           ruleMap;

	Production(Rule rule) {
		this.startingRule = rule;
		add(rule);
	}

	private void add(Rule r) {
		if(!ruleMap.hasKey(r.getUniqueName())) {
			ruleMap.put(r.getUniqueName(), r);
			add(r.getExpression());
		}
	}
	
	private void add(Expression expression) {
		for(Expression e : expression) {
			add(e);
		}
		if(expression instanceof NonTerminal) {
			add(((NonTerminal) expression).getRule());
		}
	}

	public final boolean match(SourceContext s) {
		return this.startingRule.match(s);
	}
	
	public final boolean match(String str) {
		SourceContext sc = SourceContext.newStringSourceContext(str);
		if(this.startingRule.match(sc)) {
			return (sc.getPosition() == sc.length());
		}
		return false;
	}
	
}

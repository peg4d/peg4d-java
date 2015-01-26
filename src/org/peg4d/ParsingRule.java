package org.peg4d;

import java.util.TreeMap;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.Optimizer;
import org.peg4d.expression.PEG4dTransition;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingRule extends ParsingExpression {
	public Grammar           peg;
	public String            localName;
	public ParsingRule       definedRule;
	public ParsingExpression expr;

	public int minlen = -1;

	@Override
	public boolean isAlwaysConsumed() {
		return this.checkAlwaysConsumed(null, null);
	}
	
	@Override
	public final boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		if(stack != null && this.minlen != 0 && stack.size() > 0) {
			for(String n : stack) { // Check Unconsumed Recursion
				String uName = this.getUniqueName();
				if(uName.equals(n)) {
					this.minlen = 0;
					break;
				}
			}
		}
		if(minlen == -1) {
			if(stack == null) {
				stack = new UList<String>(new String[4]);
			}
			if(startNonTerminal == null) {
				startNonTerminal = this.getUniqueName();
			}
			stack.add(this.getUniqueName());
			this.minlen = this.expr.checkAlwaysConsumed(startNonTerminal, stack) ? 1 : 0;
			stack.pop();
		}
		return minlen > 0;
	}
	
	public int transType = PEG4dTransition.Undefined;

	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		if(this.transType != PEG4dTransition.Undefined) {
			return this.transType;
		}
		String uname = this.getUniqueName();
		if(visited != null) {
			if(visited.hasKey(uname)) {
				this.transType = PEG4dTransition.BooleanType;
				return this.transType;
			}
		}
		else {
			visited = new UMap<String>();
		}
		visited.put(uname, uname);
		int t = expr.inferPEG4dTranstion(visited);
		assert(t != PEG4dTransition.Undefined);
		if(this.transType == PEG4dTransition.Undefined) {
			this.transType = t;
		}
		else {
			assert(transType == t);
		}
		return this.transType;
	}

	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		int t = checkNamingConvention(this.localName);
		c.required = this.inferPEG4dTranstion();
		if(t != PEG4dTransition.Undefined && c.required != t) {
			this.report(ReportLevel.warning, "invalid naming convention: " + this.localName);
		}
		this.expr = this.expr.checkPEG4dTransition(c);
		return this;
	}

	public final static int checkNamingConvention(String ruleName) {
		int start = 0;
		if(ruleName.startsWith("~") || ruleName.startsWith("\"")) {
			return PEG4dTransition.BooleanType;
		}
		for(;ruleName.charAt(start) == '_'; start++) {
			if(start + 1 == ruleName.length()) {
				return PEG4dTransition.BooleanType;
			}
		}
		boolean firstUpperCase = Character.isUpperCase(ruleName.charAt(start));
		for(int i = start+1; i < ruleName.length(); i++) {
			char ch = ruleName.charAt(i);
			if(ch == '!') break; // option
			if(Character.isUpperCase(ch) && !firstUpperCase) {
				return PEG4dTransition.OperationType;
			}
			if(Character.isLowerCase(ch) && firstUpperCase) {
				return PEG4dTransition.ObjectType;
			}
		}
		return firstUpperCase ? PEG4dTransition.BooleanType : PEG4dTransition.Undefined;
	}

	@Override
	public ParsingExpression removePEG4dOperator() {
		if(this.inferPEG4dTranstion() == PEG4dTransition.BooleanType) {
			return this;
		}
		String name = "~" + this.localName;
		ParsingRule r = this.peg.getRule(name);
		if(r == null) {
			r = this.peg.newRule(name, this.expr);
			r.definedRule = this;
			r.transType = PEG4dTransition.BooleanType;
			r.expr = this.expr.removePEG4dOperator();
		}
		return r;
	}

	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
		if(undefedFlags.size() > 0) {
			StringBuilder sb = new StringBuilder();
			int loc = localName.indexOf('!');
			if(loc > 0) {
				sb.append(this.localName.substring(0, loc));
			}
			else {
				sb.append(this.localName);
			}
			for(String flag: undefedFlags.keySet()) {
				if(ParsingExpression.hasReachableFlag(this.expr, flag)) {
					sb.append("!");
					sb.append(flag);
				}
			}
			String rName = sb.toString();
			System.out.println("creating new rule: " + rName);
			ParsingRule rRule = peg.getRule(rName);
			if(rRule == null) {
				rRule = peg.newRule(rName, ParsingExpression.newEmpty());
				rRule.expr = expr.removeParsingFlag(undefedFlags).intern();
			}
			return rRule;
		}
		return this;
	}
	
	@Override
	public String getInterningKey() {
		return "=";
	}

	@Override
	public ParsingExpression norm(boolean lexOnly,
			TreeMap<String, String> undefedFlags) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void accept(GrammarVisitor visitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public short acceptByte(int ch) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		return this.expr.simpleMatch(context);
	}

	public ParsingRule(Grammar peg, String ruleName, ParsingObject po, ParsingExpression e) {
		this.peg = peg;
		this.po = po;
		this.baseName = ruleName;
		this.localName = ruleName;
		this.expr = e;
		this.type = ParsingRule.checkNamingConvention(ruleName);
	}

	public final String getUniqueName() {
		return this.peg.uniqueRuleName(localName);
	}

	public final static int LexicalRule   = PEG4dTransition.BooleanType;
	public final static int ObjectRule    = PEG4dTransition.ObjectType;
	public final static int OperationRule = PEG4dTransition.OperationType;
	public final static int ReservedRule  = PEG4dTransition.Undefined;
	String baseName;

	ParsingObject po;
	public int type;
	
	public int refc = 0;

	@Override
	public String toString() {
		String t = "";
		switch(this.type) {
		case LexicalRule:   t = "boolean "; break;
		case ObjectRule:    t = "Object "; break;
		case OperationRule: t = "void "; break;
		}
		return t + this.localName + "[" + this.minlen + "]" + "=" + this.expr;
	}
		
	Grammar getGrammar() {
		return this.peg;
	}
	
	class PegRuleAnnotation {
		String key;
		ParsingObject value;
		PegRuleAnnotation next;
		PegRuleAnnotation(String key, ParsingObject value, PegRuleAnnotation next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
	}

	PegRuleAnnotation annotation;
	
	public void addAnotation(String key, ParsingObject value) {
		this.annotation = new PegRuleAnnotation(key,value, this.annotation);
	}
	
	public final void testExample1(Grammar peg, ParsingContext context) {
		PegRuleAnnotation a = this.annotation;
		while(a != null) {
			boolean isExample = a.key.equals("example");
			boolean isBadExample = a.key.equals("bad-example");
			if(isExample || isBadExample) {
				boolean ok = true;
				ParsingSource s = ParsingSource.newStringSource(a.value);
				context.resetSource(s, 0);
				context.parse2(peg, this.localName, new ParsingObject(), null);
				//System.out.println("@@ fail? " + context.isFailure() + " unconsumed? " + context.hasByteChar() + " example " + isExample + " " + isBadExample);
				if(context.isFailure() || context.hasByteChar()) {
					if(isExample) ok = false;
				}
				else {
					if(isBadExample) ok = false;
				}
				String msg = ( ok ? "[PASS]" : "[FAIL]" ) + " " + this.localName + " " + a.value.getText();
				if(Main.TestMode && !ok) {	
					Main._Exit(1, "[FAIL] tested " + a.value.getText() + " by " + peg.getRule(this.localName));
				}
				Main.printVerbose("Testing", msg);
			}
			a = a.next;
		}
	}
	
	boolean isObjectType() {
		return this.type == ParsingRule.ObjectRule;
	}


	public static boolean isLexicalName(String ruleName) {
		return checkNamingConvention(ruleName) == ParsingRule.LexicalRule;
	}

	public static String toOptionName(ParsingRule rule, boolean lexOnly, TreeMap<String,String> undefedFlags) {
		String ruleName = rule.baseName;
		if(lexOnly && !isLexicalName(ruleName)) {
			ruleName = "__" + ruleName.toUpperCase();
		}
		if(undefedFlags != null) {
			for(String flag : undefedFlags.keySet()) {
				ParsingExpression.containFlag(rule.expr, flag);
				ruleName += "!" + flag;
			}
		}
		return ruleName;
	}
	
	public ParsingExpression resolveNonTerminal() {
		return Optimizer.resolveNonTerminal(this.expr);
	}
	
	public UList<ParsingRule> subRule() {
		UMap<ParsingRule> visitedMap = new UMap<ParsingRule>();
		visitedMap.put(this.getUniqueName(), this);
		makeSubRule(this.expr, visitedMap);
		return visitedMap.values(new ParsingRule[visitedMap.size()]);
	}

	private void makeSubRule(ParsingExpression e, UMap<ParsingRule>  visited) {
		for(int i = 0; i < e.size(); i++) {
			makeSubRule(e.get(i), visited);
		}
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal)e;
			assert(e.isInterned());
			String un = ne.getUniqueName();
			ParsingRule memoed = visited.get(un);
			if(memoed == null) {
				memoed = ne.getRule();
				visited.put(un, memoed);
				makeSubRule(memoed.expr, visited);
			}
		}
	}


}

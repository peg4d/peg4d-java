package org.peg4d;

import java.util.TreeMap;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingExpression;

public class ParsingRule {
	public final static int LexicalRule   = 0;
	public final static int ObjectRule    = 1;
	public final static int OperationRule = 1 << 1;
	public final static int ReservedRule  = 1 << 15;
	
	public Grammar  peg;
	String baseName;
	public String ruleName;

	ParsingObject po;
	public int type;
	public ParsingExpression expr;
	
	public int minlen = -1;
	public int refc = 0;

	public ParsingRule(Grammar peg, String ruleName, ParsingObject po, ParsingExpression e) {
		this.peg = peg;
		this.po = po;
		this.baseName = ruleName;
		this.ruleName = ruleName;
		this.expr = e;
		this.type = ParsingRule.typeOf(ruleName);
	}
	
	final String getUniqueName() {
		return this.peg.uniqueRuleName(ruleName);
	}
	
	@Override
	public String toString() {
		String t = "";
		switch(this.type) {
		case LexicalRule:   t = "boolean "; break;
		case ObjectRule:    t = "Object "; break;
		case OperationRule: t = "void "; break;
		}
		return t + this.ruleName + "[" + this.minlen + "]" + "=" + this.expr;
	}
	
	
	public final void report(ReportLevel level, String msg) {
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
		}
		else {
			System.out.println("" + level.toString() + ": " + msg);
		}
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
				ParsingSource s = ParsingObjectUtils.newStringSource(a.value);
				context.resetSource(s, 0);
				context.parse(peg, this.ruleName, null);
//				System.out.println("@@ " + context.isFailure() + " " + context.hasByteChar() + " " + isExample + " " + isBadExample);
				if(context.isFailure() || context.hasByteChar()) {
					if(isExample) ok = false;
				}
				else {
					if(isBadExample) ok = false;
				}
				String msg = ( ok ? "[PASS]" : "[FAIL]" ) + " " + this.ruleName + " " + a.value.getText();
				if(Main.TestMode && !ok) {	
					Main._Exit(1, "[FAIL] tested " + a.value.getText() + " by " + peg.getRule(this.ruleName));
				}
				Main.printVerbose("Testing", msg);
			}
			a = a.next;
		}
	}
	
	boolean isObjectType() {
		return this.type == ParsingRule.ObjectRule;
	}

	public final static int typeOf(String ruleName) {
		int start = 0;
		for(;ruleName.charAt(start) == '_'; start++) {
			if(start + 1 == ruleName.length()) {
				return LexicalRule;
			}
		}
		boolean firstUpperCase = Character.isUpperCase(ruleName.charAt(start));
		for(int i = start+1; i < ruleName.length(); i++) {
			char ch = ruleName.charAt(i);
			if(ch == '!') break; // option
			if(Character.isUpperCase(ch) && !firstUpperCase) {
				return OperationRule;
			}
			if(Character.isLowerCase(ch) && firstUpperCase) {
				return ObjectRule;
			}
		}
		return firstUpperCase ? LexicalRule : ReservedRule;
	}

	public static boolean isLexicalName(String ruleName) {
		return typeOf(ruleName) == ParsingRule.LexicalRule;
	}

	public static String toOptionName(ParsingRule rule, boolean lexOnly, TreeMap<String,String> withoutMap) {
		String ruleName = rule.baseName;
		if(lexOnly && !isLexicalName(ruleName)) {
			ruleName = "__" + ruleName.toUpperCase();
		}
		if(withoutMap != null) {
			for(String flag : withoutMap.keySet()) {
				ParsingExpression.containFlag(rule.expr, flag);
				ruleName += "!" + flag;
			}
		}
		return ruleName;
	}
	
	public ParsingExpression resolveNonTerminal() {
		return Optimizer2.resolveNonTerminal(this.expr);
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
			assert(e.isUnique());
			String un = ne.getUniqueName();
			ParsingRule memoed = visited.get(un);
			if(memoed == null) {
				memoed = ne.getRule();
				visited.put(un, memoed);
				makeSubRule(memoed.expr, visited);
			}
		}
	}

//	private static final boolean hasNonTerminal(ParsingExpression e, String name, UMap<ParsingRule>  visited) {
//		for(int i = 0; i < e.size(); i++) {
//			if(hasNonTerminal(e.get(i), name, visited)) {
//				return true;
//			}
//		}
//		if(e instanceof NonTerminal) {
//			NonTerminal ne = (NonTerminal)e;
//			assert(e.isUnique());
//			if(name != null && name.equals(ne.ruleName)) {
//				return true;
//			}
//			String un = ne.getUniqueName();
//			ParsingRule memoed = visited.get(un);
//			if(memoed == null) {
//				memoed = ne.getRule();
//				visited.put(un, memoed);
//				return hasNonTerminal(memoed.expr, name, visited);
//			}
//		}
//		return false;
//	}
//
//	public void newReplacedNonTerminal(String oldName, String newName) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	private static final void replacedNonTerminal(Grammar peg, ParsingExpression e, String oldName, String newName, UMap<ParsingRule>  visited) {
//		for(int i = 0; i < e.size(); i++) {
//			if(e.get(i) instanceof NonTerminal) {
//				NonTerminal ne = (NonTerminal)e.get(i);
//				if(ne.ruleName.equals(oldName)) {
//					e.set(i, peg.newNonTerminal(newName).uniquefy());
//					continue;
//				}
//				ParsingExpression ref = ne.deReference();
//				visited.clear();
//				if(hasNonTerminal(ref, oldName, visited)) {
//					e.set(i, peg.newNonTerminal(replacedName(ne.ruleName, oldName, newName)).uniquefy());
//					continue;
//				}
//			}
//			else {
//				replacedNonTerminal(peg, e.get(i), oldName, newName, visited);
//			}
//		}
//	}
//
//	private static String replacedName(String ruleName, String oldName, String newName) {
//		return ruleName + "[" + oldName + "->" + newName + "]";
//	}

	
	
}

package org.peg4d;

class ParsingRule {
	public final static int LexicalRule   = 0;
	public final static int ObjectRule    = 1;
	public final static int OperationRule = 1 << 1;
	public final static int ReservedRule  = 1 << 15;
	
	Grammar  peg;
	String ruleName;

	ParsingObject po;
	int type;
	ParsingExpression expr;
	
	int minlen = -1;
	int refc = 0;

	ParsingRule(Grammar peg, String ruleName, ParsingObject po, ParsingExpression e) {
		this.peg = peg;
		this.po = po;
		this.ruleName = ruleName;
		this.expr = e;
		this.type = ParsingRule.typeOf(ruleName);
	}
	
	final String getUniqueName() {
		return this.peg.uniqueRuleName(ruleName);
	}
	
	@Override
	public String toString() {
		return type + " " + this.ruleName + "[" + this.minlen + "]" + "=" + this.expr;
	}
	
	final void report(ReportLevel level, String msg) {
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
			if(Character.isUpperCase(ruleName.charAt(i)) && !firstUpperCase) {
				return OperationRule;
			}
			if(Character.isLowerCase(ruleName.charAt(i)) && firstUpperCase) {
				return ObjectRule;
			}
		}
		return firstUpperCase ? LexicalRule : ReservedRule;
	}

	public static boolean isLexicalName(String ruleName) {
		return typeOf(ruleName) == ParsingRule.LexicalRule;
	}

	public static String toLexicalName(String ruleName) {
		if(typeOf(ruleName) != ParsingRule.LexicalRule) {
			return "__" + ruleName.toUpperCase();
		}
		return ruleName;
	}
	
	public ParsingExpression resolveNonTerminal() {
		return this.expr;
	}
	
}
package nez.expr;

import java.util.TreeMap;

import nez.Grammar;
import nez.SourceContext;
import nez.ast.AST;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

public class Rule extends Expression {
	Grammar    grammar;
	String     name;
	String     uname;
	Expression body;
	
	public Rule(SourcePosition s, Grammar grammar, String name, Expression body) {
		super(s);
		this.grammar = grammar;
		this.name = name;
		this.uname = grammar.uniqueName(name);
		this.body = (body == null) ? Factory.newEmpty(s) : body;
		this.definedRule = definedRule;
	}
	
	@Override
	public Expression get(int index) {
		return body;
	}
	
	@Override
	public int size() {
		return 1;
	}
	
	public final String getLocalName() {
		return this.name;
	}
	
	public final String getUniqueName() {
		return this.uname;
	}
	
	public final Expression getExpression() {
		return this.body;
	}

	public int minlen = -1;
	
	@Override
	public final boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		if(stack != null && this.minlen != 0 && stack.size() > 0) {
			for(String n : stack) { // Check Unconsumed Recursion
				if(uname.equals(n)) {
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
				startNonTerminal = this.uname;
			}
			stack.add(this.uname);
			this.minlen = this.body.checkAlwaysConsumed(startNonTerminal, stack) ? 1 : 0;
			stack.pop();
		}
		return minlen > 0;
	}
	
	public int transType = NodeTransition.Undefined;
	private Rule definedRule;  // defined

	@Override
	public int inferNodeTransition(UMap<String> visited) {
		if(this.transType != NodeTransition.Undefined) {
			return this.transType;
		}
		if(visited != null) {
			if(visited.hasKey(uname)) {
				this.transType = NodeTransition.BooleanType;
				return this.transType;
			}
		}
		else {
			visited = new UMap<String>();
		}
		visited.put(uname, uname);
		int t = body.inferNodeTransition(visited);
		assert(t != NodeTransition.Undefined);
		if(this.transType == NodeTransition.Undefined) {
			this.transType = t;
		}
		else {
			assert(transType == t);
		}
		return this.transType;
	}

	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		int t = checkNamingConvention(this.name);
		c.required = this.inferNodeTransition(null);
		if(t != NodeTransition.Undefined && c.required != t) {
			this.report(ReportLevel.warning, "invalid naming convention: " + this.name);
		}
		this.body = this.body.checkNodeTransition(c);
		return this;
	}

	public final static int checkNamingConvention(String ruleName) {
		int start = 0;
		if(ruleName.startsWith("~") || ruleName.startsWith("\"")) {
			return NodeTransition.BooleanType;
		}
		for(;ruleName.charAt(start) == '_'; start++) {
			if(start + 1 == ruleName.length()) {
				return NodeTransition.BooleanType;
			}
		}
		boolean firstUpperCase = Character.isUpperCase(ruleName.charAt(start));
		for(int i = start+1; i < ruleName.length(); i++) {
			char ch = ruleName.charAt(i);
			if(ch == '!') break; // option
			if(Character.isUpperCase(ch) && !firstUpperCase) {
				return NodeTransition.OperationType;
			}
			if(Character.isLowerCase(ch) && firstUpperCase) {
				return NodeTransition.ObjectType;
			}
		}
		return firstUpperCase ? NodeTransition.BooleanType : NodeTransition.Undefined;
	}

	@Override
	public Expression removeNodeOperator() {
		if(this.inferNodeTransition(null) == NodeTransition.BooleanType) {
			return this;
		}
		String name = "~" + this.name;
		Rule r = this.grammar.getRule(name);
		if(r == null) {
			r = this.grammar.newRule(name, this.body);
			r.definedRule = this;
			r.transType = NodeTransition.BooleanType;
			r.body = this.body.removeNodeOperator();
		}
		return r;
	}

	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		if(undefedFlags.size() > 0) {
			StringBuilder sb = new StringBuilder();
			int loc = name.indexOf('!');
			if(loc > 0) {
				sb.append(this.name.substring(0, loc));
			}
			else {
				sb.append(this.name);
			}
			for(String flag: undefedFlags.keySet()) {
				if(Expression.hasReachableFlag(this.body, flag)) {
					sb.append("!");
					sb.append(flag);
				}
			}
			String rName = sb.toString();
			Rule rRule = grammar.getRule(rName);
			if(rRule == null) {
				rRule = grammar.newRule(rName, Factory.newEmpty(null));
				rRule.body = body.removeFlag(undefedFlags).intern();
			}
			return rRule;
		}
		return this;
	}
	
	@Override
	public String getInterningKey() {
		return this.getUniqueName() + "=";
	}
	
	@Override
	public boolean match(SourceContext context) {
		return body.match(context);
	}

	@Override
	public String getPredicate() {
		return this.getUniqueName() + "=";
	}

	@Override
	public short acceptByte(int ch) {
		return this.body.acceptByte(ch);
	}

	public void addAnotation(String textAt, AST ast) {
		// TODO Auto-generated method stub
		
	}


}

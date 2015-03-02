package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.Grammar;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingRule;
import org.peg4d.pegcode.GrammarVisitor;

public class NonTerminal extends ParsingExpression {
	public Grammar peg;
	public String  ruleName;
	String  uniqueName;
	public NonTerminal(Grammar peg, String ruleName) {
		super();
		this.peg = peg;
		this.ruleName = ruleName;
		this.uniqueName = this.peg.uniqueRuleName(this.ruleName);
	}
	@Override
	public String getInterningKey() {
		return getUniqueName();
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		ParsingRule r = this.getRule();
		if(r == null) {
			this.report(ReportLevel.warning, "undefined rule: " + this.ruleName);
			r = new ParsingRule(this.peg, this.ruleName, null, ParsingExpression.newEmpty());
			this.peg.setRule(this.ruleName, r);
		}
		if(startNonTerminal != null && startNonTerminal.equals(this.uniqueName)) {
			this.report(ReportLevel.error, "left recursion: " + this.ruleName);
			this.peg.foundError = true;
			return false;
		}
		return r.checkAlwaysConsumed(startNonTerminal, stack);
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		ParsingRule r = this.getRule();
		return r.inferNodeTransition(visited);
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		ParsingRule r = this.getRule();
		int t = r.inferNodeTransition();
		if(t == NodeTransition.BooleanType) {
			return this;
		}
		if(c.required == NodeTransition.ObjectType) {
			if(t == NodeTransition.OperationType) {
				this.report(ReportLevel.warning, "unexpected operation");
				return this.removeNodeOperator();
			}
			c.required = NodeTransition.OperationType;
			return this;
		}
		if(c.required == NodeTransition.OperationType) {
			if(t == NodeTransition.ObjectType) {
				this.report(ReportLevel.warning, "expected connector");
				return ParsingExpression.newConnector(this, -1);
			}
		}
		return this;
	}
	@Override
	public ParsingExpression removeNodeOperator() {
		ParsingRule r = (ParsingRule)this.getRule().removeNodeOperator();
		if(!this.ruleName.equals(r.localName)) {
			return new NonTerminal(peg, r.localName);
		}
		return this;
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String,String> undefedFlags) {
		ParsingRule r = (ParsingRule)this.getRule().removeFlag(undefedFlags);
		if(!this.ruleName.equals(r.localName)) {
			return new NonTerminal(peg, r.localName);
		}
		return this;
	}
	
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		NonTerminal ne = this;
		ParsingRule rule = ne.getRule();
		String optName = ParsingRule.toOptionName(rule, lexOnly, undefedFlags);
		if(ne.peg.getRule(optName) != rule) {
			ne.peg.makeOptionRule(rule, optName, lexOnly, undefedFlags);
			ne = ne.peg.newNonTerminal(optName);
			//System.out.println(rule.ruleName + "@=>" + optName);
		}
		if(isExpectedConnector()) {
			 return ParsingExpression.newConnector(ne, -1);
		}
		return ne;
	}
	@Override
	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		NonTerminal ne = this;
		ne.checkReference();
		if(minlen == 0) {
			String n = this.getUniqueName();
			ParsingRule r = this.getRule();
			if(n.equals(ruleName) && !this.is(LeftRecursion)) {
				this.set(LeftRecursion);
				this.report(ReportLevel.error, "left recursion: " + r);
				r.peg.foundError = true;
			}
			if(!checkRecursion(n, stack)) {
				int pos = stack.size();
				stack.add(n);
				int nc = this.deReference().checkLength(ruleName, start, minlen, stack);
				this.minlen = nc - minlen;
				stack.clear(pos);
			}
			if(this.minlen == -1) {
				this.minlen = 1; // FIXME: assuming no left recursion
			}
		}
		else if(this.minlen == -1) {
			this.minlen = 0;
		}
		return minlen + this.minlen;
	}
	
	void checkReference() {
		ParsingRule r = this.getRule();
		if(r == null) {
			this.report(ReportLevel.error, "undefined rule: " + this.ruleName);
			r = new ParsingRule(this.peg, this.ruleName, null, new ParsingIf(this.ruleName));
			this.peg.setRule(this.ruleName, r);
		}
		this.minlen = r.minlen != -1 ? r.minlen : -1 /* FIXME: assuming no left recursion */;
		r.refc += 1;
	}
	
	private boolean checkRecursion(String uName, UList<String> stack) {
		for(int i = 0; i < stack.size() - 1; i++) {
			if(uName.equals(stack.ArrayValues[i])) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public
	boolean hasObjectOperation() {
		ParsingRule r = this.getRule();
		return r.type == ParsingRule.OperationRule;
	}

	public final String getUniqueName() {
		return this.uniqueName;
	}
	
	public final ParsingRule getRule() {
		return this.peg.getRule(this.ruleName);
	}
	
	public final ParsingExpression deReference() {
		return this.peg.getExpression(this.ruleName);
	}
	
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitNonTerminal(this);
	}
	@Override
	public short acceptByte(int ch) {
		if(this.deReference() != null && !this.is(LeftRecursion)) {
			return this.deReference().acceptByte(ch);
		}
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		return context.matchNonTerminal(this);
	}

}

package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.Grammar;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingRule;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
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
		//System.out.println("uName: " + startNonTerminal + " " + this.uniqueName);
		if(startNonTerminal != null && startNonTerminal.equals(this.uniqueName)) {
			this.report(ReportLevel.error, "left recursion: " + this.ruleName);
			this.peg.foundError = true;
			return false;
		}
		return r.checkAlwaysConsumed(startNonTerminal, stack);
	}
	public int typeCheck(int typeStatus) {
		ParsingRule r = this.getRule();
		int ruleType = r.type;
		if(ruleType == ParsingRule.ObjectRule) {
			return checkObjectConstruction(this, typeStatus);
		}
		if(ruleType == ParsingRule.OperationRule) {
			checkObjectOperation(this, typeStatus);
		}
		return typeStatus;
	}
	@Override
	public ParsingExpression transformPEG() {
		ParsingRule rule = this.getRule();
		throw new RuntimeException("TODO");
//		ParsingRule pureRule = this.peg.getPureRule(this.ruleName);
//		if(rule != pureRule) {
//			return new NonTerminal(peg, pureRule.ruleName).intern();
//		}
	//	return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String,String> withoutMap) {
		if(withoutMap != null && withoutMap.size() > 0) {
			ParsingRule rule = this.getRule();
			StringBuilder sb = new StringBuilder();
			int loc = this.ruleName.indexOf('!');
			if(loc > 0) {
				sb.append(this.ruleName.substring(0, loc));
			}
			else {
				sb.append(this.ruleName);
			}
			for(String flag: withoutMap.keySet()) {
				if(ParsingExpression.hasReachableIf(rule.expr, flag)) {
					sb.append("!");
					sb.append(this.ruleName);
				}
			}
			String rName = sb.toString();
			ParsingRule rRule = peg.getRule(rName);
			if(rRule == null) {
				rRule = peg.newRule(rName, ParsingExpression.newEmpty());
				rRule.expr = rule.expr.removeParsingFlag(withoutMap);
			}
			return (rRule == rule) ? this : new NonTerminal(peg, rRule.ruleName).intern();
		}
		return this;
	}
	
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		NonTerminal ne = this;
		ParsingRule rule = ne.getRule();
		String optName = ParsingRule.toOptionName(rule, lexOnly, withoutMap);
		if(ne.peg.getRule(optName) != rule) {
			ne.peg.makeOptionRule(rule, optName, lexOnly, withoutMap);
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
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchNonTerminal(this);
	}

}

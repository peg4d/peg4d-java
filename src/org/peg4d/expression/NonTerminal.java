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

	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression(getUniqueName(), this);
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
		int stackTop = context.pushCallStack(this);
		boolean b = this.deReference().matcher.simpleMatch(context);
		context.popCallStack(stackTop);
		return b;
	}
}

// Name[Old->New]

class ParamNonTerminal extends NonTerminal {
	String baseName;
	String oldName;
	String newName;
	
	ParamNonTerminal(Grammar peg, String baseName, String oldName, String newName) {
		super(peg, baseName);
		this.baseName = baseName;
		this.oldName = oldName;
		this.newName = newName;

	}

//	@Override
//	void checkReference() {
//		if(baseName.equals(baseName)) {
//			UList<ParsingRule> l = this.getRule().subRule();
//			for(ParsingRule r: l) {
//				if(r.hasNonTerminal(oldName)) {
//					r.newReplacedNonTerminal(oldName, newName);
//				}
//			}
//		}
//	}

	
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return null;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		// TODO Auto-generated method stub
		return false;
	}
}

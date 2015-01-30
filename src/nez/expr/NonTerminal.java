package nez.expr;

import java.util.TreeMap;

import nez.Grammar;
import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

public class NonTerminal extends Expression {
	public Grammar peg;
	public String  ruleName;
	String  uniqueName;
	public NonTerminal(SourcePosition s, Grammar peg, String ruleName) {
		super(s);
		this.peg = peg;
		this.ruleName = ruleName;
		this.uniqueName = this.peg.uniqueName(this.ruleName);
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Expression get(int index) {
		return null;
	}
	
	@Override
	public String getInterningKey() {
		return getUniqueName();
	}

	@Override
	public String getPredicate() {
		return getUniqueName();
	}

	public final String getLocalName() {
		return ruleName;
	}

	public final String getUniqueName() {
		return this.uniqueName;
	}
	
	public final Rule getRule() {
		return this.peg.getRule(this.ruleName);
	}
	
	public final Expression deReference() {
		return this.peg.getRule(this.ruleName).get(0);
	}
	
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		Rule r = this.getRule();
		if(r == null) {
			this.report(ReportLevel.warning, "undefined rule: " + this.ruleName);
			r = this.peg.newRule(this.ruleName, Factory.newEmpty(s));
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
		Rule r = this.getRule();
		return r.inferNodeTransition(visited);
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		Rule r = this.getRule();
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
				return Factory.newLink(this.s, this, -1);
			}
		}
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		Rule r = (Rule)this.getRule().removeNodeOperator();
		if(!this.ruleName.equals(r.getLocalName())) {
			return Factory.newNonTerminal(this.s, peg, r.getLocalName());
		}
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		Rule r = (Rule)this.getRule().removeFlag(undefedFlags);
		if(!this.ruleName.equals(r.getLocalName())) {
			return Factory.newNonTerminal(this.s, peg, r.getLocalName());
		}
		return this;
	}
	
	@Override
	public short acceptByte(int ch) {
		return this.deReference().acceptByte(ch);
	}

	@Override
	public boolean match(SourceContext context) {
		return context.matchNonTerminal(this);
	}

}

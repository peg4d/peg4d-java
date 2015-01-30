package org.peg4d.expression;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingValue extends ParsingExpression {
	public String value;
	ParsingValue(String value) {
		super();
		this.value = value;
		this.minlen = 0;
	}
	@Override
	public String getInterningKey() {
		return "`" + this.value;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.OperationType;
	}
	@Override
	public ParsingExpression checkNodeTransition(NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected value");
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	public ParsingExpression removeNodeOperator() {
		return ParsingExpression.newEmpty();
	}
	@Override
	public ParsingExpression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		if(lexOnly || this.isRemovedOperation()) {
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Unconsumed;
	}
	@Override
	public boolean match(ParsingContext context) {
		context.left.setValue(this.value);
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitValue(this);
	}
}
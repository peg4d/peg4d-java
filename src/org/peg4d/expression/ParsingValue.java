package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
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
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.OperationType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		if(c.required != PEG4dTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected value");
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	public ParsingExpression removePEG4dOperator() {
		return ParsingExpression.newEmpty();
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
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
	public boolean simpleMatch(ParsingContext context) {
		context.left.setValue(this.value);
		return true;
	}
	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitValue(this);
	}
}
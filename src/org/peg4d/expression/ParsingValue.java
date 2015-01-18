package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingValue extends ParsingExpression {
	public String value;
	ParsingValue(String value) {
		super();
		this.value = value;
		this.minlen = 0;
	}
	@Override
	public String getInternKey() {
		return "`" + this.value;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(lexOnly || this.isRemovedOperation()) {
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return LazyAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setValue(this.value);
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitValue(this);
	}
}
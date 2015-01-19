package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.UList;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingTagging extends ParsingExpression {
	public ParsingTag tag;
	ParsingTagging(ParsingTag tag) {
		super();
		this.tag = tag;
		this.minlen = 0;
	}
	@Override
	public String getInterningKey() {
		return "#" + this.tag.toString();
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public ParsingExpression transformPEG() {
		return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> withoutMap) {
		return this;
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
		context.left.setTag(this.tag);
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitTagging(this);
	}
}
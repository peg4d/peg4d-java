package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingTagging extends ParsingExpression {
	public ParsingTag tag;
	ParsingTagging(ParsingTag tag) {
		super();
		this.tag = tag;
		this.minlen = 0;
	}
	@Override
	public
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("#\b" + this.tag.key(), this);
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
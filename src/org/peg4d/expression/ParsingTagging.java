package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;

public class ParsingTagging extends ParsingExpression {
	public ParsingTag tag;
	ParsingTagging(ParsingTag tag) {
		super();
		this.tag = tag;
	}
	@Override
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
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitTagging(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setTag(this.tag);
		return true;
	}
}
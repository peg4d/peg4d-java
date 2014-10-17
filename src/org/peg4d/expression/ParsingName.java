package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingName extends ParsingFunction {
	int tagId;
	ParsingName(int tagId, ParsingExpression inner) {
		super("name", inner);
		this.tagId = tagId; //ParsingTag.tagId(flagName);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newName(tagId, e);
	}

	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tagId);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
		if(this.inner.matcher.simpleMatch(context)) {
			long endIndex = context.getPosition();
			String s = context.source.substring(startIndex, endIndex);
			context.pushTokenStack(tagId, s);
			return true;
		}
		return false;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitName(this);
	}
}
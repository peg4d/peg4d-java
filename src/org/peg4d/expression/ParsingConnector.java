package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingExpressionVisitor;
import org.peg4d.ParsingObject;

public class ParsingConnector extends ParsingUnary {
	public int index;
	ParsingConnector(ParsingExpression e, int index) {
		super(e);
		this.index = index;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		if(index != -1) {
			return ParsingExpression.uniqueExpression("@" + index + "\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniqueExpression("@\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(this.isRemovedOperation()) {
			lexOnly = true;
		}
		ParsingExpression e = this.inner.normalizeImpl(lexOnly, withoutMap);
		if(this.isNothingConnected() || lexOnly) {
			return e;
		}
		return ParsingExpression.newConnector(e, this.index);
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitConnector(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingObject left = context.left;
		int mark = context.markObjectStack();
		if(this.inner.matcher.simpleMatch(context)) {
			if(context.left != left) {
				context.commitLinkLog(mark, context.left);
				context.logLink(left, this.index, context.left);
			}
			context.left = left;
			left = null;
			return true;
		}
		context.abortLinkLog(mark);			
		left = null;
		return false;
	}
}
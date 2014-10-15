package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;

public class ParsingNot extends ParsingUnary {
	ParsingNot(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("!\b" + this.uniqueKey(), this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.norm(true, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newNot(e);
	}
	@Override
	public void visit(ParsingExpressionVisitor visitor) {
		visitor.visitNot(this);
	}
	@Override
	public short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == StringAccept) {
			return StringReject;
		}
		if(r == StringReject) {
			return StringAccept;
		}
		return r;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingObject left = context.left;
		if(this.inner.matcher.simpleMatch(context)) {
			context.rollback(pos);
			context.failure(this);
			left = null;
			return false;
		}
		else {
			context.rollback(pos);
			context.forgetFailure(f);
			context.left = left;
			left = null;
			return true;
		}
	}
}
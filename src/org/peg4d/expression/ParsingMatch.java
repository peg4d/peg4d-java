package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingMatch extends ParsingFunction {
	ParsingMatch(ParsingExpression inner) {
		super("match", inner);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
//		ParsingExpression e = inner.normalizeImpl(true, flagMap, withoutMap);
//		if(e == inner) {
//			return this;
//		}
//		return ParsingExpression.newMatch(e);
		return inner.norm(true, withoutMap);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		boolean oldMode = context.setRecognitionMode(true);
//		ParsingObject left = context.left;
//		if(this.inner.matcher.simpleMatch(context)) {
//			context.setRecognitionMode(oldMode);
//			context.left = left;
//			left = null;
//			return true;
//		}
//		context.setRecognitionMode(oldMode);
//		left = null;
//		return false;
		return this.inner.matcher.simpleMatch(context);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitMatch(this);
	}
}
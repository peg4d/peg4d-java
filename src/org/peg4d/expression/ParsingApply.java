package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;

public class ParsingApply extends ParsingOperation {
	ParsingApply(ParsingExpression inner) {
		super("apply", inner);
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		//TODO;
		return null;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		ParsingContext s = new ParsingContext(context.left);
//		
//		this.inner.matcher.simpleMatch(s);
//		context.opRememberPosition();
//		context.opRememberFailurePosition();
//		context.opStoreObject();
//		this.inner.matcher.simpleMatch(context);
//		context.opDebug(this.inner);
		return !(context.isFailure());

	}
}
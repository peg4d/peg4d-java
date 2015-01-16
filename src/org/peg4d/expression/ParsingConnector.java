package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingConnector extends ParsingUnary {
	public int index;
	ParsingConnector(ParsingExpression e, int index) {
		super(e);
		this.index = index;
	}
	@Override
	public
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	public
	String getInternKey() {
		return (index != -1) ? "@" + index : "@";
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(this.isRemovedOperation()) {
			lexOnly = true;
		}
		ParsingExpression e = this.inner.norm(lexOnly, withoutMap);
		if(this.isNothingConnected() || lexOnly) {
			return e;
		}
		return ParsingExpression.newConnector(e, this.index);
	}

	@Override
	public short acceptByte(int ch) {
		return inner.acceptByte(ch);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingTree left = context.left;
		int mark = context.markLogStack();
		if(this.inner.matcher.simpleMatch(context)) {
			if(context.left != left) {
				context.commitLog(mark, context.left);
				context.lazyLink(left, this.index, context.left);
			}
			context.left = left;
			left = null;
			return true;
		}
		context.abortLog(mark);			
		left = null;
		return false;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitConnector(this);
	}
}
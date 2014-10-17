package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingExport extends ParsingUnary {
	ParsingExport(ParsingExpression e) {
		super(e);
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return new ParsingExport(inner);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> withoutMap) {
		return inner.norm(lexOnly, withoutMap);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitExport(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
}
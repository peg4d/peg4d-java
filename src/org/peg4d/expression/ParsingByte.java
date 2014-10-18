package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingByte extends ParsingExpression {
	public int byteChar;
	String errorToken = null;
	ParsingByte(int ch) {
		super();
		this.byteChar = ch;
		this.minlen = 1;
	}
	@Override ParsingExpression uniquefyImpl() { 
		if(this.errorToken == null) {
			return ParsingExpression.uniqueExpression("'\b" + byteChar, this);
		}
		return ParsingExpression.uniqueExpression("'\b" + this.errorToken + "\b" + byteChar, this);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	public String expectedToken() {
		if(this.errorToken != null) {
			return this.errorToken;
		}
		return this.toString();
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitByte(this);
	}
	@Override
	public short acceptByte(int ch) {
		return (byteChar == ch) ? Accept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.byteAt(context.pos) == this.byteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}
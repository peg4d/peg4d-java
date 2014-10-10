package org.peg4d.expression;

import org.peg4d.UMap;
import org.peg4d.expression.*;


public class ParsingExpressionVisitor {
	private UMap<String> visitedMap = new UMap<String>();
	boolean isVisited(String name) {
		if(this.visitedMap != null) {
			return this.visitedMap.hasKey(name);
		}
		return true;
	}
	void visited(String name) {
		if(this.visitedMap != null) {		
			this.visitedMap.put(name, name);
		}
	}
	void initVisitor() {
		if(this.visitedMap != null) {
			this.visitedMap.clear();
		}
	}
	public void visitNonTerminal(NonTerminal e) {
		if(!this.isVisited(e.ruleName)) {
			visited(e.ruleName);
			e.peg.getExpression(e.ruleName).visit(this);
		}
	}
	public void visitEmpty(ParsingEmpty e) {
	}
	public void visitFailure(ParsingFailure e) {
	}
	public void visitByte(ParsingByte e) {
	}
	public void visitByteRange(ParsingByteRange e) {
	}
	public void visitString(ParsingString e) {
	}
	public void visitAny(ParsingAny e) {
	}
	public void visitTagging(ParsingTagging e) {
	}
	public void visitValue(ParsingValue e) {
	}
	
	
	public void visitIndent(ParsingIndent e) {
	}
	public void visitUnary(ParsingUnary e) {
		e.inner.visit(this);
	}
	public void visitNot(ParsingNot e) {
		this.visitUnary(e);
	}
	public void visitAnd(ParsingAnd e) {
		this.visitUnary(e);
	}
	public void visitOptional(ParsingOption e) {
		this.visitUnary(e);
	}
	public void visitRepetition(ParsingRepetition e) {
		this.visitUnary(e);
	}
	public void visitConnector(ParsingConnector e) {
		this.visitUnary(e);
	}
	public void visitExport(ParsingExport e) {
		this.visitUnary(e);
	}
	public void visitList(ParsingList e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}
	public void visitSequence(ParsingSequence e) {
		this.visitList(e);
	}
	public void visitChoice(ParsingChoice e) {
		this.visitList(e);
	}
	public void visitConstructor(ParsingConstructor e) {
		this.visitList(e);
	}

	public void visitParsingFunction(ParsingFunction parsingFunction) {
	}
	
	public void visitParsingOperation(ParsingOperation e) {
		e.inner.visit(this);
	}
	public void visitParsingIfFlag(ParsingIf e) {
	}
}


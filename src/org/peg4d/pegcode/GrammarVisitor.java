package org.peg4d.pegcode;

import org.peg4d.ParsingRule;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingApply;
import org.peg4d.expression.ParsingAssert;
import org.peg4d.expression.ParsingBlock;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingCatch;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingName;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;


public abstract class GrammarVisitor {
//	private UMap<String> visitedMap = new UMap<String>();
//	protected final boolean isVisited(String name) {
//		if(this.visitedMap != null) {
//			return this.visitedMap.hasKey(name);
//		}
//		return true;
//	}
//	protected final void visited(String name) {
//		if(this.visitedMap != null) {		
//			this.visitedMap.put(name, name);
//		}
//	}
//	protected final void initVisitor() {
//		if(this.visitedMap != null) {
//			this.visitedMap.clear();
//		}
//	}
//	
//	public abstract void visitNonTerminal(NonTerminal e) {
//		if(!this.isVisited(e.ruleName)) {
//			visited(e.ruleName);
//			e.peg.getExpression(e.ruleName).visit(this);
//		}
//	}
	
	public abstract void visitRule(ParsingRule e);

	public abstract void visitNonTerminal(NonTerminal e);
	public abstract void visitEmpty(ParsingEmpty e);
	public abstract void visitFailure(ParsingFailure e);
	public abstract void visitByte(ParsingByte e);
	public abstract void visitByteRange(ParsingByteRange e);
	public abstract void visitAny(ParsingAny e);
	public abstract void visitString(ParsingString e);

	public abstract void visitNot(ParsingNot e);
	public abstract void visitAnd(ParsingAnd e);
	public abstract void visitOptional(ParsingOption e);
	public abstract void visitRepetition(ParsingRepetition e);

	public abstract void visitSequence(ParsingSequence e);
	public abstract void visitChoice(ParsingChoice e);

	public abstract void visitConstructor(ParsingConstructor e);
	public abstract void visitConnector(ParsingConnector e);
	public abstract void visitTagging(ParsingTagging e);
	public abstract void visitValue(ParsingValue e);

	public abstract void visitExport(ParsingExport e);
	
	public abstract void visitMatch(ParsingMatch e);
	public abstract void visitCatch(ParsingCatch e);
	public abstract void visitAssert(ParsingAssert e);

	public abstract void visitIfFlag(ParsingIf e);
	public abstract void visitWithFlag(ParsingWithFlag e);
	public abstract void visitWithoutFlag(ParsingWithoutFlag e);
	
	public abstract void visitBlock(ParsingBlock e);
	public abstract void visitIndent(ParsingIndent e);
	public abstract void visitName(ParsingName e);
	public abstract void visitIsa(ParsingIsa e);

	public abstract void visitApply(ParsingApply e);

	
}


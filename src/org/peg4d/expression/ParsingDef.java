package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingDef extends ParsingFunction {
	int tableId;
	ParsingDef(int tableId, ParsingExpression inner) {
		super("def", inner);
		this.tableId = tableId;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		if(!this.inner.checkAlwaysConsumed(startNonTerminal, stack)) {
			this.report(ReportLevel.warning, "unconsumed expression: " + this.inner);
		}
		return true;
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		int t = this.inner.inferPEG4dTranstion(null);
		if(t != PEG4dTransition.BooleanType) {
			this.inner = this.inner.removePEG4dOperator();
		}
		return this;
	}	
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		ParsingExpression e = inner.norm(lexOnly, undefedFlags);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newDef(tableId, e);
	}

	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tableId);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
		if(this.inner.matcher.simpleMatch(context)) {
			long endIndex = context.getPosition();
			String s = context.source.substring(startIndex, endIndex);
			context.pushSymbolTable(tableId, s);
			return true;
		}
		return false;
	}
	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitDef(this);
	}
	
	// Utilities
	
	public static boolean checkContextSensitivity(ParsingExpression e, UMap<String> visitedMap) {
		if(e.size() > 0) {
			for(int i = 0; i < e.size(); i++) {
				if(checkContextSensitivity(e.get(i), visitedMap)) {
					return true;
				}
			}
			return false;
		}
		if(e instanceof NonTerminal) {
			String un = ((NonTerminal) e).getUniqueName();
			if(visitedMap.get(un) == null) {
				visitedMap.put(un, un);
				return checkContextSensitivity(((NonTerminal) e).getRule().expr, visitedMap);
			}
			return false;
		}
		if(e instanceof ParsingIndent || e instanceof ParsingIs || e instanceof ParsingIsa) {
			return true;
		}
		return false;
	}
	
}
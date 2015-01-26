package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingSequence extends ParsingList {
	ParsingSequence(UList<ParsingExpression> l) {
		super(l);
	}
	@Override
	public
	String getInterningKey() {
		return " ";
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		for(ParsingExpression e: this) {
			int t = e.inferPEG4dTranstion(visited);
			if(t == PEG4dTransition.ObjectType || t == PEG4dTransition.OperationType) {
				return t;
			}
		}
		return PEG4dTransition.BooleanType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		UList<ParsingExpression> l = newList();
		for(ParsingExpression e : this) {
			ParsingExpression.addSequence(l, e.checkPEG4dTransition(c));
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
		UList<ParsingExpression> l = newList();
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).removeParsingFlag(undefedFlags);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, undefedFlags);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	@Override
	public short acceptByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r != Unconsumed) {
				return r;
			}
		}
		return Unconsumed;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		int mark = context.markLogStack();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).matcher.simpleMatch(context))) {
				context.abortLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}

	@Override
	public void accept(GrammarVisitor visitor) {
		visitor.visitSequence(this);
	}
}
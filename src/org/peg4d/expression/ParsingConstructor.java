package org.peg4d.expression;

import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTree;
import org.peg4d.ReportLevel;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingConstructor extends ParsingList {
	public boolean leftJoin = false;
	public int prefetchIndex = 0;
	ParsingConstructor(boolean leftJoin, UList<ParsingExpression> list) {
		super(list);
		this.leftJoin = leftJoin;
	}
	@Override
	public
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	public
	String getInterningKey() {
		return (leftJoin) ? "{@}" : "{}";
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		return PEG4dTransition.ObjectType;
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		if(this.leftJoin) {
			if(c.required != PEG4dTransition.OperationType) {
				this.report(ReportLevel.warning, "unexpected left-associative constructor");
				return this.removePEG4dOperator();
			}
		}
		else {
			if(c.required != PEG4dTransition.ObjectType) {
				this.report(ReportLevel.warning, "unexpected constructor");
				return this.removePEG4dOperator();
			}
		}
		c.required = PEG4dTransition.OperationType;
		return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).removeParsingFlag(undefedFlags);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newConstructor(this.leftJoin, l);
	}
	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).norm(lexOnly, undefedFlags);
			ParsingExpression.addSequence(l, e);
		}
		ParsingExpression ne = (lexOnly) ? ParsingExpression.newSequence(l) : ParsingExpression.newConstructor(this.leftJoin, l);
		if(this.isExpectedConnector()) {
			ne = ParsingExpression.newConnector(ne, -1);
		}
		return ne;
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
		long startIndex = context.getPosition();
//		ParsingObject left = context.left;
		for(int i = 0; i < this.prefetchIndex; i++) {
			if(!this.get(i).matcher.simpleMatch(context)) {
				context.rollback(startIndex);
				return false;
			}
		}
		int mark = context.markLogStack();
		ParsingTree newnode = context.newParsingTree(startIndex, this);
		if(this.leftJoin) {
			context.lazyJoin(context.left);
			context.lazyLink(newnode, 0, context.left);
		}
		context.left = newnode;
		for(int i = this.prefetchIndex; i < this.size(); i++) {
			if(!this.get(i).matcher.simpleMatch(context)) {
				context.abortLog(mark);
				context.rollback(startIndex);
				newnode = null;
				return false;
			}
		}
		newnode.setEndingPosition(context.getPosition());
		//context.commitLinkLog2(newnode, startIndex, mark);
		//System.out.println("newnode: " + newnode.oid);
		context.left = newnode;
		newnode = null;
		return true;
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitConstructor(this);
	}
}
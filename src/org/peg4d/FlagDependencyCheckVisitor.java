package org.peg4d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
import org.peg4d.expression.ParsingDef;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIs;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepeat;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingScan;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;
import org.peg4d.pegcode.GrammarVisitor;

public class FlagDependencyCheckVisitor extends GrammarVisitor{
	
	Map<String, List<String>> flagDependencyMap;
	String visitingRuleName;
	
	public FlagDependencyCheckVisitor(){
		this.flagDependencyMap = new HashMap<>();
	}

	public FlagDependencyCheckVisitor(Map<String, List<String>> map){
		this.flagDependencyMap = map;
	}
	
	@Override
	public void visitRule(ParsingRule e) {
		String oldVisitingRuleName = visitingRuleName;
		String newVisitingRuleName = e.baseName;
		if(!this.flagDependencyMap.containsKey(newVisitingRuleName)){
			this.flagDependencyMap.put(newVisitingRuleName, new ArrayList<String>());
		}
		visitingRuleName = newVisitingRuleName;
		e.expr.visit(this);
		visitingRuleName = oldVisitingRuleName;
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		ParsingRule r = e.getRule();
		if(!this.flagDependencyMap.containsKey(r.baseName)){
			this.visitRule(r);
		}
		List<String> visitingFlag = this.flagDependencyMap.get(visitingRuleName);
		for(String flag : this.flagDependencyMap.get(r.baseName)){
			if(!visitingFlag.contains(flag)){
				visitingFlag.add(flag);
			}
		}
}

	@Override
	public void visitEmpty(ParsingEmpty e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitByte(ParsingByte e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitAny(ParsingAny e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitString(ParsingString e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitNot(ParsingNot e) {
		e.inner.visit(this);
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		e.inner.visit(this);
	}

	@Override
	public void visitOptional(ParsingOption e) {
		e.inner.visit(this);
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		e.inner.visit(this);
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		e.inner.visit(this);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitValue(ParsingValue e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		String flag = e.getFlagName();
		List<String> visitingFlag = this.flagDependencyMap.get(visitingRuleName);
		if(!visitingFlag.contains(flag)){
			visitingFlag.add(flag);
		}
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		e.inner.visit(this);
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		e.inner.visit(this);
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		e.inner.visit(this);
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitDef(ParsingDef e) {
		e.inner.visit(this);
	}

	@Override
	public void visitIs(ParsingIs e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitApply(ParsingApply e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitScan(ParsingScan e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitRepeat(ParsingRepeat e) {
		// TODO Auto-generated method stub
		
	}

}

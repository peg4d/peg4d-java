package org.peg4d;

class ExpressionVisitor {
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

//abstract class Checker {
//	public final static int False    = 0;
//	public final static int True     = 1;
//	public final static int Unknown  = 2;
//	abstract int check(ParsingExpression e);
//}
//
//class ParsingExpressionChecker {
//	private UMap<ParsingExpression> visitedMap;
//	private void initVisitor() {
//		if(this.visitedMap != null) {
//			this.visitedMap.clear();
//		}
//		else {
//			visitedMap = new UMap<ParsingExpression>();
//		}
//	}
//	private boolean isVisited(String name) {
//		if(this.visitedMap != null) {
//			return this.visitedMap.hasKey(name);
//		}
//		return true;
//	}
//	private void visited(String name, ParsingExpression next) {
//		if(this.visitedMap != null) {		
//			this.visitedMap.put(name, next);
//		}
//	}
//
//	void makeList(ParsingExpression e) {
//		if(e instanceof PNonTerminal) {
//			PNonTerminal ne = (PNonTerminal)e;
//			if(!this.isVisited(ne.getUniqueName())) {
//				visited(ne.getUniqueName(), ne.getNext());
//				makeList(ne.getNext());
//			}
//		}
//		for(int i = 0; i < e.size(); i++) {
//			makeList(e.get(i));
//		}
//	}
//		
//	
//}

class ListMaker extends ExpressionVisitor {
	private UList<String> nameList;
	private Grammar peg;
	UList<String> make(Grammar peg, String startPoint) {
		this.nameList = new UList<String>(new String[8]);
		this.peg = peg;
		this.initVisitor();
		this.visitImpl(startPoint);
		return this.nameList;
	}
	void visitImpl(String name) {
		this.visited(name);
		this.nameList.add(name);
		ParsingExpression next = peg.getExpression(name);
		next.visit(this);
	}
	@Override
	public void visitNonTerminal(NonTerminal e) {
		if(	e.peg == peg && !this.isVisited(e.ruleName)) {
			this.visitImpl(e.ruleName);
		}
	}
}

//class Importer extends ExpressionVisitor {
//	private Grammar dst;
//	private Grammar src;
//	private UMap<String> params;
//	private ParsingExpression returnExpression = null;
//	Importer(Grammar dst, Grammar src, String startPoint, UMap<String> params) {
//		this.dst = dst;
//		this.src = src;
//		this.initVisitor();
//		this.visitRule(startPoint);
//	}
//	
//	ParsingExpression importRule(Grammar dst, Grammar src, String startPoint, UMap<String> params) {
//		this.dst = dst;
//		this.src = src;
//		this.initVisitor();
//		this.visitRule(startPoint);
//		return returnExpression;
//	}
//	
//	private void visitRule(String name) {
//		this.visited(name);
//		ParsingExpression next = src.getExpression(name);
//		next.visit(this);
//	}
//	
//	@Override
//	public void visitNonTerminal(PNonTerminal e) {
//		if( e.peg == src && !this.isVisited(e.ruleName)) {
//			this.visitRule(e.ruleName);
//		}
//	}
//}

class Optimizer extends ExpressionVisitor {
	Grammar peg;
	Optimizer(Grammar peg) {
		this.peg = peg;
	}
	void optimize() {
		UList<ParsingExpression> pegList = this.peg.getExpressionList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}
//	@Override
//	public void visitNonTerminal(PNonTerminal e) {
//		if(!this.isVisited(e.symbol)) {
//			visited(e.symbol);
//			e.base.getExpression(e.symbol).visit(this);
//		}
//	}
	
	private ParsingExpression removeOperation(ParsingExpression e) {
		if(e instanceof ParsingMatch) {
			ParsingExpression inner = ((ParsingMatch) e).inner;
//			if(!inner.hasObjectOperation()) {
//				this.peg.InterTerminalOptimization += 1;
//				//System.out.println("removed: " + e);
//				return inner;
//			}
			//System.out.println("unremoved: " + e);
		}
//		if(e instanceof PCommit) {
//			ParsingExpression inner = ((PCommit) e).inner;
//			if(!inner.is(ParsingExpression.HasConnector)) {
//				this.peg.InterTerminalOptimization += 1;
//				//System.out.println("removed: " + e);
//				return inner;
//			}
//			//System.out.println("unremoved: " + e);
//		}
		return e;
	}

	@Override
	public void visitUnary(ParsingUnary e) {
		e.inner = removeOperation(e.inner);
		e.inner.visit(this);
	}
	@Override
	public void visitParsingOperation(ParsingOperation e) {
		e.inner = removeOperation(e.inner);
		e.inner.visit(this);
	}

	@Override
	public void visitList(ParsingList e) {
		for(int i = 0; i < e.size(); i++) {
			e.set(i, removeOperation(e.get(i)));
			e.get(i).visit(this);
		}
	}
	
//	private boolean needsObjectContext(ParsingExpression e) {
//		if(e.is(ParsingExpression.HasConstructor) || e.is(ParsingExpression.HasConnector) || e.is(ParsingExpression.HasTagging) || e.is(ParsingExpression.HasMessage) || e.is(ParsingExpression.HasContext)) {
//			return true;
//		}
//		return false;
//	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		this.visitList(e);
		int prefetchIndex = 0;
		for(int i = 0; i < e.size(); i++) {
			ParsingExpression sub = e.get(i);
////			if(needsObjectContext(sub)) {
////				break;
////			}
//			prefetchIndex = i + 1;
		}
		if(prefetchIndex > 0) {
			if(this.peg.optimizationLevel > 1) {
				this.peg.InterTerminalOptimization += 1;
//				System.out.println("prefetchIndex: " + prefetchIndex + " e = " + e);
				e.prefetchIndex = prefetchIndex;
			}
		}
	}
}

//class MemoRemover extends ExpressionVisitor {
//	UList<ParsingExpression> pegList;
//	int cutMiss = -1;
//	int RemovedCount = 0;
//	
//	MemoRemover(Grammar peg) {
//		UList<String> nameList = peg.ruleMap.keys();
//		this.pegList = new UList<ParsingExpression>(new ParsingExpression[nameList.size()]);
//		for(int i = 0; i < nameList.size(); i++) {
//			String ruleName = nameList.ArrayValues[i];
//			this.pegList.add(peg.getExpression(ruleName));
//		}
//	}
//
//	void removeDisabled() {
//		this.cutMiss = -1;
//		for(int i = 0; i < pegList.size(); i++) {
//			pegList.ArrayValues[i].visit(this);
//		}
//	}
//
//	void remove(int cutMiss) {
//		this.cutMiss = cutMiss;
//		for(int i = 0; i < pegList.size(); i++) {
//			pegList.ArrayValues[i].visit(this);
//		}
//	}
//
//	final boolean isRemoved(ParsingMemo pm) {
//		if(pm.memoMiss < this.cutMiss) {
//			return true;
//		}
//		return !(pm.enableMemo);
//	}
//	
//	ParsingExpression removeMemo(ParsingExpression e) {
//		if(e instanceof ParsingMemo) {
//			ParsingMemo pm = (ParsingMemo)e;
//			if(this.isRemoved(pm)) {
//				this.RemovedCount += 1;
//				return pm.inner;
//			}
//		}
//		return e;
//	}
//	
//	@Override
//	public void visitNonTerminal(PNonTerminal e) {
//		if(e.calling != null) {
//			e.calling = this.removeMemo(e.calling);
//		}
//	}
//
//	@Override
//	public void visitUnary(ParsingUnary e) {
//		e.inner = this.removeMemo(e.inner);
//		e.inner.visit(this);
//	}
//
//	@Override
//	public void visitList(ParsingList e) {
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = this.removeMemo(e.get(i));
//			e.set(i, se);
//			se.visit(this);
//		}
//	}
//	
//	@Override
//	public void visitChoice(ParsingChoice e) {
//		this.visitList(e);
////		if(e instanceof PegSelectiveChoice) {
////			for(int i = 0; i < UCharset.MAX; i++) {
////				if(e.caseOf[i] != null) {
////					Peg se = this.removeMemo(e.caseOf[i]);
////					e.caseOf[i] = se;
////					se.visit(this);
////				}
////			}
////		}
//	}
//}

//class ObjectRemover extends ParsingExpressionVisitor {
//	ObjectRemover() {
//	}
//	
//	private ParsingExpression returnPeg = null;
//	ParsingExpression removeObjectOperation(ParsingExpression e) {
//		ParsingExpression stack = this.returnPeg;
//		this.returnPeg = e;
//		e.visit(this);
//		ParsingExpression r = this.returnPeg;
//		this.returnPeg = stack;
//		return r;
//	}
//	
//	@Override
//	public void visitNonTerminal(PNonTerminal e) {
//		if(e.resolvedExpression.hasObjectOperation()) {
//			this.returnPeg = e.base.newNonTerminal(e.symbol + "'");
//		}
//	}
////	@Override
////	public void visitNotAny(PNotAny e) {
////		ParsingExpression ne = this.removeObjectOperation(e.not);
////		if(ne == null) {
////			this.returnPeg = e.base.newAny(".");
////			return ;
////		}
////		e.not = (PNot)ne;
////		this.returnPeg = e;
////	}
//	@Override
//	public void visitTagging(ParsingTagging e) {
//		this.returnPeg = null;
//	}
//	@Override
//	public void visitMessage(ParsingValue e) {
//		this.returnPeg = null;
//	}
//	@Override
//	public void visitIndent(ParsingIndent e) {
//		this.returnPeg = null;
//	}
//	@Override
//	public void visitUnary(ParsingUnary e) {
//		ParsingExpression ne = this.removeObjectOperation(e.inner);
//		if(ne == null) {
//			this.returnPeg = null;			
//			return ;
//		}
//		e.inner = ne;
//		this.returnPeg = e;
//	}
//
//	@Override
//	public void visitParsingOperation(ParsingOperation e) {
//		ParsingExpression ne = this.removeObjectOperation(e.inner);
//		if(ne == null) {
//			this.returnPeg = null;			
//			return ;
//		}
//		e.inner = ne;
//		this.returnPeg = e;
//	}
//
//	@Override
//	public void visitConnector(PConnector e) {
//		ParsingExpression ne = this.removeObjectOperation(e.inner);
//		if(ne == null) {
//			this.returnPeg = null;
//			return ;
//		}
//		this.returnPeg = ne;
//	}
//
//	@Override
//	public void visitList(ParsingList e) {
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression se = e.get(i);
//			e.set(i, removeObjectOperation(se));
//		}
//		this.returnPeg = e.trim();
//	}
//
//	@Override
//	public void visitConstructor(PConstructor e) {
//		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[e.size()]);
//		for(int i = 0; i < e.size(); i++) {
//			ParsingExpression ne = removeObjectOperation(e.get(i));
//			if(ne != null) {
//				l.add(ne);
//			}
//		}
//		//System.out.println("@@@ size=" + l.size() + " e=" + e);
//		this.returnPeg = new PSequence(0, l);
//	}
//}
//

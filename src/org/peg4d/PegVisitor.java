package org.peg4d;

class PegVisitor {
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
	public void visitNonTerminal(PegNonTerminal e) {
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			e.base.getExpression(e.symbol).visit(this);
		}
	}
	public void visitString(PegString e) {
	}
	public void visitCharacter(PegCharacter e) {
	}
	public void visitAny(PegAny e) {
	}
	public void visitNotAny(PegNotAny e) {
		e.orig.visit(this);
	}
	public void visitTagging(PegTagging e) {
	}
	public void visitMessage(PegMessage e) {
	}
	public void visitIndent(PegIndent e) {
	}
	public void visitIndex(PegIndex e) {
	}
	public void visitUnary(PegUnary e) {
		e.inner.visit(this);
	}
	public void visitNot(PegNot e) {
		this.visitUnary(e);
	}
	public void visitAnd(PegAnd e) {
		this.visitUnary(e);
	}
	public void visitOptional(PegOptional e) {
		this.visitUnary(e);
	}
	public void visitRepeat(PegRepeat e) {
		this.visitUnary(e);
	}
	public void visitSetter(PegSetter e) {
		this.visitUnary(e);
	}
	public void visitExport(PegExport e) {
		this.visitUnary(e);
	}
	public void visitList(PegList e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}
	public void visitSequence(PegSequence e) {
		this.visitList(e);
	}
	public void visitChoice(PegChoice e) {
		this.visitList(e);
	}
	public void visitNewObject(PegNewObject e) {
		this.visitList(e);
	}
	public void visitOperation(PegOperation e) {
		e.inner.visit(this);
	}
}

class Formatter extends PegVisitor {
	UStringBuilder sb;
	
	public Formatter() {
		sb = new UStringBuilder();
	}

	public String getDesc() {
		return "PEG4d ";
	}
	
	public void format(Peg e, UStringBuilder sb) {
		this.sb = sb;
		e.visit(this);
		this.sb = null;
	}

	public void formatHeader() {
	}

	public void formatFooter() {
	}
	
	public void formatRule(String ruleName, Peg e, UStringBuilder sb) {
		this.sb = sb;
		sb.appendNewLine();
		this.formatRuleName(ruleName, e);
		sb.append(this.getNewLine(), this.getSetter(), " ");
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					sb.append(this.getNewLine(), "/ ");
				}
				e.get(i).visit(this);
			}
		}
		else {
			e.visit(this);
		}
		sb.append(getNewLine(), this.getSemiColon());
		this.sb = null;
	}
	
	public String getNewLine() {
		return "\n\t";
	}

	public String getSetter() {
		return "=";
	}

	public String getSemiColon() {
		return ";";
	}

	public void formatRuleName(String ruleName, Peg e) {
		if(e.is(Peg.HasNewObject)) {
			sb.append(ruleName);
		}
		else {
			sb.append(ruleName.toUpperCase());
		}
	}

	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		this.formatRuleName(e.symbol, e);
	}

	@Override
	public void visitString(PegString e) {
		char quote = '\'';
		if(e.text.indexOf("'") != -1) {
			quote = '"';
		}
		sb.append(UCharset._QuoteString(quote, e.text, quote));
	}

	@Override
	public void visitCharacter(PegCharacter e) {
		sb.append("[" + e.charset, "]");
	}

	@Override
	public void visitAny(PegAny e) {
		sb.append(".");
	}

	@Override
	public void visitTagging(PegTagging e) {
		sb.append(e.symbol);
	}
	
	@Override
	public void visitMessage(PegMessage e) {
		sb.append("`", e.symbol, "`");
	}

	@Override
	public void visitIndent(PegIndent e) {
		sb.append("indent");
	}

	@Override
	public void visitIndex(PegIndex e) {
		sb.appendInt(e.index);
	}

	
	protected void format(String prefix, PegUnary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(e.inner instanceof PegTerm || e.inner instanceof PegNewObject) {
			e.inner.visit(this);
		}
		else {
			sb.append("(");
			e.inner.visit(this);
			sb.append(")");
		}
		if(suffix != null) {
			sb.append(suffix);
		}
	}

	@Override
	public void visitOptional(PegOptional e) {
		this.format( null, e, "?");
	}

	@Override
	public void visitRepeat(PegRepeat e) {
		if(e.atleast == 1) {
			this.format( null, e, "+");
		}
		else {
			this.format(null, e, "*");
		}
	}

	@Override
	public void visitAnd(PegAnd e) {
		this.format( "&", e, null);
	}

	@Override
	public void visitNot(PegNot e) {
		this.format( "!", e, null);
	}

	@Override
	public void visitSetter(PegSetter e) {
		this.format( null, e, "^");
		if(e.index != -1) {
			sb.appendInt(e.index);
		}
	}

	@Override
	public void visitExport(PegExport e) {
		sb.append("<| ");
		if(e.inner instanceof PegNewObject) {
			this.format((PegNewObject)e.inner);
		}
		else {
			e.inner.visit(this);
		}
		sb.append(" |>");
	}

	protected void format(PegList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			Peg e = l.get(i);
			if(e instanceof PegChoice || e instanceof PegSequence) {
				sb.append("( ");
				e.visit(this);
				sb.append(" )");
			}
			else {
				if(e == null) {
					System.out.println("@@@@ " + i + " "); 
					continue;
				}
				e.visit(this);
			}
		}
	}

	@Override
	public void visitSequence(PegSequence e) {
		//sb.append("( ");
		this.format( e);
		//sb.append(" )");
	}

	@Override
	public void visitChoice(PegChoice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitNewObject(PegNewObject e) {
		if(e.leftJoin) {
			sb.append("<{^ ");
		}
		else {
			sb.append("<{ ");
		}
		this.format(e);
		sb.append(" }>");
	}
	

}

class ListMaker extends PegVisitor {
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
		Peg next = peg.getExpression(name);
		next.visit(this);
	}
	
	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		if(	!e.isForeignNonTerminal() && !this.isVisited(e.symbol)) {
			visited(e.symbol);
			this.visitImpl(e.symbol);
		}
	}
}



class NonTerminalChecker extends PegVisitor {
	String startPoint;
	Peg startRule;
	int consumedMinimumLength;
	boolean hasNext = false;

	void verify(String startPoint, Peg startRule) {
		this.initVisitor();
		this.startPoint = startPoint;
		this.verifyImpl(startRule);
	}

	void verifyImpl(Peg startRule) {
		this.startRule = startRule;
		this.consumedMinimumLength = 0;
		this.startRule.visit(this);
	}

	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		Peg next = e.base.getExpression(e.symbol);
		if(next == null) {
			Main._PrintLine(e.source.formatErrorMessage("error", e.sourcePosition, "undefined label: " + e.symbol));
			e.base.foundError = true;
			return;
		}
		e.jumpExpression = next;
		if(this.startPoint.equals(e.symbol)) {
//			if(this.length == 0) {
//				System.out.println("left recursion: " + e.symbol);
//			}
			if(e.length == -1) {
				e.length = this.consumedMinimumLength;
			}
			else {
				if(this.consumedMinimumLength < e.length) {
					e.length = this.consumedMinimumLength;
				}
			}
			if(!this.startRule.is(Peg.CyclicRule)) {
				Main.printVerbose("cyclic rule", e.symbol);
				this.startRule.set(Peg.CyclicRule);
			}
		}
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			Peg stackedRule = this.startRule;
			int stackedLength = this.consumedMinimumLength;
			this.verifyImpl(e.getNext());
			e.length = this.consumedMinimumLength;
			this.consumedMinimumLength = stackedLength;
			this.startRule = stackedRule;
			this.startRule.derived(e.getNext());
		}
		this.consumedMinimumLength += e.length;
	}
	
	@Override
	public void visitString(PegString e) {
		this.consumedMinimumLength += e.text.length();
	}
	
	@Override
	public void visitCharacter(PegCharacter e) {
		this.consumedMinimumLength += 1;
	}
	@Override
	public void visitAny(PegAny e) {
		this.consumedMinimumLength += 1;
	}
	@Override
	public void visitTagging(PegTagging e) {
	}
	@Override
	public void visitMessage(PegMessage e) {
	}
	@Override
	public void visitIndent(PegIndent e) {
	}
	@Override
	public void visitIndex(PegIndex e) {
	}
	@Override
	public void visitNot(PegNot e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitAnd(PegAnd e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitOptional(PegOptional e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitRepeat(PegRepeat e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		if(e.atleast == 0) {
			this.consumedMinimumLength = stackedLength;
		}
	}
	@Override
	public void visitSetter(PegSetter e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitExport(PegExport e) {
		this.visitUnary(e);
	}
	@Override
	public void visitSequence(PegSequence e) {
		int stackedLength = this.consumedMinimumLength;
		for(int i = 0; i < e.size(); i++) {
			Peg sub = e.get(i);
			sub.visit(this);
		}
		e.length = this.consumedMinimumLength - stackedLength;
	}
	@Override
	public void visitChoice(PegChoice e) {
		int stackedLength = this.consumedMinimumLength;
		int min = Integer.MAX_VALUE;
		for(int i = 0; i < e.size(); i++) {
			this.consumedMinimumLength = stackedLength;
			e.get(i).visit(this);
			if(this.consumedMinimumLength < min) {
				min = this.consumedMinimumLength;
			}
		}
		e.length = min;
		this.consumedMinimumLength = stackedLength + min;
	}
	
	@Override
	public void visitNewObject(PegNewObject e) {
		int stackedLength = this.consumedMinimumLength;
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		e.length = this.consumedMinimumLength - stackedLength;
	}
}

class Inliner extends PegVisitor {
	Grammar peg;
	Inliner(Grammar peg) {
		this.peg = peg;
	}
	void performInlining() {
		UList<Peg> pegList = this.peg.getExpressionList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}
	final boolean isInlinable(Peg e) {
		if(e instanceof PegNonTerminal && peg.optimizationLevel > 1) {
			return ! ((PegNonTerminal) e).jumpExpression.is(Peg.CyclicRule);
		}
		return false;
	}
	final Peg doInline(Peg parent, PegNonTerminal r) {
		//System.out.println("inlining: " + parent.getClass().getSimpleName() + " " + r.symbol +  " Memo? " + (r.nextRule1 instanceof PegMemo) + " e=" + r.nextRule1);
		this.peg.InliningCount += 1;
		if(parent instanceof PegRepeat) {
			System.out.println("inlining: " + parent.getClass().getSimpleName() + " " + r.symbol +  " Memo? " + (r.jumpExpression instanceof PegMemo) + " e=" + r.jumpExpression);
		}
		return r.jumpExpression;
	}
	@Override
	public void visitUnary(PegUnary e) {
		if(isInlinable(e.inner)) {
			e.inner = doInline(e, (PegNonTerminal)e.inner);
		}
		else {
			e.inner.visit(this);
		}
		e.derived(e.inner);
	}
	@Override
	public void visitList(PegList e) {
		for(int i = 0; i < e.size(); i++) {
			Peg se = e.get(i);
			if(isInlinable(se)) {
				e.set(i, doInline(e, (PegNonTerminal)se));
			}
			else {
				se.visit(this);
			}
			e.derived(se);
		}
	}
	@Override
	public void visitOperation(PegOperation e) {
		e.inner.visit(this);
		e.derived(e.inner);
	}
}

class Optimizer extends PegVisitor {
	Grammar peg;
	
	Optimizer(Grammar peg) {
		this.peg = peg;
	}

	void optimize() {
		UList<Peg> pegList = this.peg.getExpressionList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}
	
	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			e.base.getExpression(e.symbol).visit(this);
		}
	}
//	public void visitNotAny(PegNotAny e) {
//		e.orig.visit(this);
//	}

	private void removeMonad(PegUnary e) {
		if(e.inner instanceof PegMonad) {
			PegMonad pm = (PegMonad)e.inner;
			if(!pm.inner.hasObjectOperation()) {
				this.peg.InterTerminalOptimization += 1;
				e.inner = pm.inner;
			}
		}
	}

	private void removeCommit(PegUnary e) {
		if(e.inner instanceof PegCommit) {
			PegCommit pm = (PegCommit)e.inner;
			if(!pm.inner.hasObjectOperation()) {
				this.peg.InterTerminalOptimization += 1;
				e.inner = pm.inner;
			}
		}
	}

	@Override
	public void visitNot(PegNot e) {
		removeMonad(e);
		this.visitUnary(e);
	}
	
	@Override
	public void visitAnd(PegAnd e) {
		removeMonad(e);
		this.visitUnary(e);
	}
	
	@Override
	public void visitOptional(PegOptional e) {
		removeCommit(e);
		this.visitUnary(e);
	}
	@Override
	public void visitRepeat(PegRepeat e) {
		removeCommit(e);
		this.visitUnary(e);
	}
	
	@Override
	public void visitSetter(PegSetter e) {
		this.visitUnary(e);
	}
	
	@Override
	public void visitExport(PegExport e) {
		this.visitUnary(e);
	}
	@Override
	public void visitList(PegList e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}
	@Override
	public void visitSequence(PegSequence e) {
		this.visitList(e);
	}
	@Override
	public void visitChoice(PegChoice e) {
		this.visitList(e);
		if(e instanceof PegSelectiveChoice) {
			((PegSelectiveChoice) e).tryPrediction();
		}
	}
	
	private boolean needsObjectContext(Peg e) {
		if(e.is(Peg.HasNewObject) || e.is(Peg.HasSetter) || e.is(Peg.HasTagging) || e.is(Peg.HasMessage) || e.is(Peg.HasContext)) {
			return true;
		}
		return false;
	}

	@Override
	public void visitNewObject(PegNewObject e) {
		int prefetchIndex = 0;
		for(int i = 0; i < e.size(); i++) {
			Peg sub = e.get(i);
			if(needsObjectContext(sub)) {
				break;
			}
			prefetchIndex = i + 1;
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

class MemoRemover extends PegVisitor {
	UList<Peg> pegList;
	int cutMiss = -1;
	int RemovedCount = 0;
	
	MemoRemover(Grammar peg) {
		UList<String> nameList = peg.ruleMap.keys();
		this.pegList = new UList<Peg>(new Peg[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			this.pegList.add(peg.getExpression(ruleName));
		}
	}

	void removeDisabled() {
		this.cutMiss = -1;
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}

	void remove(int cutMiss) {
		this.cutMiss = cutMiss;
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}

	final boolean isRemoved(PegMemo pm) {
		if(pm.memoMiss < this.cutMiss) {
			return true;
		}
		return !(pm.enableMemo);
	}
	
	Peg removeMemo(Peg e) {
		if(e instanceof PegMemo) {
			PegMemo pm = (PegMemo)e;
			if(this.isRemoved(pm)) {
				this.RemovedCount += 1;
				return pm.inner;
			}
		}
		return e;
	}
	
	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		if(e.jumpExpression != null) {
			e.jumpExpression = this.removeMemo(e.jumpExpression);
		}
	}

	@Override
	public void visitUnary(PegUnary e) {
		e.inner = this.removeMemo(e.inner);
		e.inner.visit(this);
	}

	@Override
	public void visitList(PegList e) {
		for(int i = 0; i < e.size(); i++) {
			Peg se = this.removeMemo(e.get(i));
			e.set(i, se);
			se.visit(this);
		}
	}
	
	@Override
	public void visitChoice(PegChoice e) {
		this.visitList(e);
//		if(e instanceof PegSelectiveChoice) {
//			for(int i = 0; i < UCharset.MAX; i++) {
//				if(e.caseOf[i] != null) {
//					Peg se = this.removeMemo(e.caseOf[i]);
//					e.caseOf[i] = se;
//					se.visit(this);
//				}
//			}
//		}
	}
	
}

class ObjectRemover extends PegVisitor {
	
	ObjectRemover() {
	}

	private Peg returnPeg = null;
	
	Peg removeObjectOperation(Peg e) {
		Peg stack = this.returnPeg;
		this.returnPeg = e;
		e.visit(this);
		Peg r = this.returnPeg;
		this.returnPeg = stack;
		return r;
	}
	
	@Override
	public void visitNonTerminal(PegNonTerminal e) {
	}
	
	@Override
	public void visitNotAny(PegNotAny e) {
		Peg ne = this.removeObjectOperation(e.not);
		if(ne == null) {
			this.returnPeg = e.base.newAny();
			return ;
		}
		e.not = (PegNot)ne;
		this.returnPeg = e;
	}
	@Override
	public void visitTagging(PegTagging e) {
		this.returnPeg = null;
	}
	@Override
	public void visitMessage(PegMessage e) {
		this.returnPeg = null;
	}
	@Override
	public void visitIndent(PegIndent e) {
		this.returnPeg = null;
	}
	@Override
	public void visitIndex(PegIndex e) {
		this.returnPeg = null;
	}

	@Override
	public void visitUnary(PegUnary e) {
		Peg ne = this.removeObjectOperation(e.inner);
		if(ne == null) {
			this.returnPeg = null;			
			return ;
		}
		e.inner = ne;
		this.returnPeg = e;
	}

	@Override
	public void visitOperation(PegOperation e) {
		Peg ne = this.removeObjectOperation(e.inner);
		if(ne == null) {
			this.returnPeg = null;			
			return ;
		}
		e.inner = ne;
		this.returnPeg = e;
	}

	@Override
	public void visitSetter(PegSetter e) {
		Peg ne = this.removeObjectOperation(e.inner);
		if(ne == null) {
			this.returnPeg = null;
			return ;
		}
		this.returnPeg = ne;
	}

	@Override
	public void visitList(PegList e) {
		for(int i = 0; i < e.size(); i++) {
			Peg se = e.get(i);
			e.set(i, removeObjectOperation(se));
		}
		this.returnPeg = e.trim();
	}
	@Override
	public void visitNewObject(PegNewObject e) {
		UList<Peg> l = new UList<Peg>(new Peg[e.size()]);
		for(int i = 0; i < e.size(); i++) {
			Peg ne = removeObjectOperation(e.get(i));
			if(ne != null) {
				l.add(ne);
			}
		}
		//System.out.println("@@@ size=" + l.size() + " e=" + e);
		this.returnPeg = e.base.newSequence(l);
	}
	
}


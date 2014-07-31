package org.peg4d;

class PegProbe {
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
			e.base.getRule(e.symbol).visit(this);
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

class Formatter extends PegProbe {
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

class ListMaker extends PegProbe {
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
		Peg next = peg.getRule(name);
		next.visit(this);
	}
	
	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			this.visitImpl(e.symbol);
		}
	}
}



class NonTerminalChecker extends PegProbe {
	String startPoint;
	Peg startRule;
	int length;
	boolean hasNext = false;

	void verify(String startPoint, Peg startRule) {
		this.initVisitor();
		this.startPoint = startPoint;
		this.verifyImpl(startRule);
	}

	void verifyImpl(Peg startRule) {
		this.startRule = startRule;
		this.length = 0;
		this.startRule.visit(this);
	}

	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		Peg next = e.base.getRule(e.symbol);
		if(next == null) {
			System.out.println("undefined label: " + e.symbol);
			return;
		}
		e.nextRule = next;
		if(this.startPoint.equals(e.symbol)) {
//			if(this.length == 0) {
//				System.out.println("left recursion: " + e.symbol);
//			}
			if(e.length == -1) {
				e.length = this.length;
			}
			else {
				if(this.length < e.length) {
					e.length = this.length;
				}
			}
			this.startRule.set(Peg.CyclicRule);
		}
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			Peg stackedRule = this.startRule;
			int stackedLength = this.length;
			this.verifyImpl(e.nextRule);
			e.length = this.length;
			this.length = stackedLength;
			this.startRule = stackedRule;
			this.startRule.derived(e.nextRule);
		}
		this.length += e.length;
	}
	
	@Override
	public void visitString(PegString e) {
		this.length += e.text.length();
	}
	
	@Override
	public void visitCharacter(PegCharacter e) {
		this.length += 1;
	}
	@Override
	public void visitAny(PegAny e) {
		this.length += 1;
	}
	@Override
	public void visitTagging(PegTagging e) {
		this.length += 0;
	}
	@Override
	public void visitMessage(PegMessage e) {
		this.length += 0;
	}
	@Override
	public void visitIndent(PegIndent e) {
		this.length += 0;
	}
	@Override
	public void visitIndex(PegIndex e) {
		this.length += 0;
	}
	@Override
	public void visitNot(PegNot e) {
		int stackedLength = this.length;
		this.visitUnary(e);
		this.length = stackedLength;
	}
	@Override
	public void visitAnd(PegAnd e) {
		int stackedLength = this.length;
		this.visitUnary(e);
		this.length = stackedLength;
	}
	@Override
	public void visitOptional(PegOptional e) {
		int stackedLength = this.length;
		this.visitUnary(e);
		this.length = stackedLength;
	}
	@Override
	public void visitRepeat(PegRepeat e) {
		int stackedLength = this.length;
		this.visitUnary(e);
		if(e.atleast == 0) {
			this.length = stackedLength;
		}
	}
	@Override
	public void visitSetter(PegSetter e) {
		int stackedLength = this.length;
		this.visitUnary(e);
		this.length = stackedLength;
	}
	@Override
	public void visitExport(PegExport e) {
		this.visitUnary(e);
	}
	@Override
	public void visitSequence(PegSequence e) {
		int stackedLength = this.length;
		for(int i = 0; i < e.size(); i++) {
			Peg sub = e.get(i);
			sub.visit(this);
		}
		e.length = this.length - stackedLength;
	}
	@Override
	public void visitChoice(PegChoice e) {
		int stackedLength = this.length;
		int min = Integer.MAX_VALUE;
		for(int i = 0; i < e.size(); i++) {
			this.length = stackedLength;
			e.get(i).visit(this);
			if(this.length < min) {
				min = this.length;
			}
		}
		e.length = min;
		this.length = stackedLength + min;
	}
	
	@Override
	public void visitNewObject(PegNewObject e) {
		int stackedLength = this.length;
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		e.length = this.length - stackedLength;
	}
}

class Inliner extends PegProbe {
	Grammar peg;
	
	Inliner(Grammar peg) {
		this.peg = peg;
	}

	void performInlining() {
		UList<Peg> pegList = this.peg.getRuleList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}

	final boolean isInlinable(Peg e) {
		if(e instanceof PegNonTerminal && peg.optimizationLevel > 1) {
			return ! ((PegNonTerminal) e).nextRule.is(Peg.CyclicRule);
		}
		return false;
	}
	
	final Peg doInline(PegNonTerminal te) {
		//System.out.println("inlining: " + te.symbol +  " Memo? " + (te.nextRule instanceof PegMemo) + " e=" + te.nextRule);
		this.peg.InliningCount += 1;
		return te.nextRule;
	}
	
	@Override
	public void visitUnary(PegUnary e) {
		if(isInlinable(e.inner)) {
			e.inner = doInline((PegNonTerminal)e.inner);
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
				e.set(i, doInline((PegNonTerminal)se));
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

class Optimizer extends PegProbe {
	Grammar peg;
	
	Optimizer(Grammar peg) {
		this.peg = peg;
	}

	void optimize() {
		UList<Peg> pegList = this.peg.getRuleList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}
	
	@Override
	public void visitNonTerminal(PegNonTerminal e) {
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			e.base.getRule(e.symbol).visit(this);
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

class MemoRemover extends PegProbe {
	UList<Peg> pegList;
	int cutMiss = -1;
	int RemovedCount = 0;
	
	MemoRemover(Grammar peg) {
		UList<String> nameList = peg.pegMap.keys();
		this.pegList = new UList<Peg>(new Peg[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			this.pegList.add(peg.getRule(ruleName));
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
		e.nextRule = this.removeMemo(e.nextRule);
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
	
}


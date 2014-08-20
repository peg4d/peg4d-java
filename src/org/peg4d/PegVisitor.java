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
	public void visitNonTerminal(PNonTerminal e) {
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			e.base.getExpression(e.symbol).visit(this);
		}
	}
	public void visitString(PString e) {
	}
	public void visitCharacter(PCharacter e) {
	}
	public void visitAny(PAny e) {
	}
	public void visitNotAny(PNotAny e) {
		e.orig.visit(this);
	}
	public void visitTagging(PTagging e) {
	}
	public void visitMessage(PMessage e) {
	}
	public void visitIndent(PIndent e) {
	}
	public void visitUnary(PUnary e) {
		e.inner.visit(this);
	}
	public void visitNot(PNot e) {
		this.visitUnary(e);
	}
	public void visitAnd(PAnd e) {
		this.visitUnary(e);
	}
	public void visitOptional(POptional e) {
		this.visitUnary(e);
	}
	public void visitRepetition(PRepetition e) {
		this.visitUnary(e);
	}
	public void visitConnector(PConnector e) {
		this.visitUnary(e);
	}
	public void visitExport(PExport e) {
		this.visitUnary(e);
	}
	public void visitList(PList e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}
	public void visitSequence(PSequence e) {
		this.visitList(e);
	}
	public void visitChoice(PChoice e) {
		this.visitList(e);
	}
	public void visitConstructor(PConstructor e) {
		this.visitList(e);
	}
	public void visitOperation(POperator e) {
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
	public void format(PExpression e, UStringBuilder sb) {
		this.sb = sb;
		e.visit(this);
		this.sb = null;
	}
	public void formatHeader() {
	}
	public void formatFooter() {
	}
	public void formatRule(String ruleName, PExpression e, UStringBuilder sb) {
		this.sb = sb;
		sb.appendNewLine();
		this.formatRuleName(ruleName, e);
		sb.append(this.getNewLine(), this.getSetter(), " ");
		if(e instanceof PChoice) {
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
	public void formatRuleName(String ruleName, PExpression e) {
		sb.append(ruleName);
	}
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		this.formatRuleName(e.symbol, e);
	}
	@Override
	public void visitString(PString e) {
		char quote = '\'';
		if(e.text.indexOf("'") != -1) {
			quote = '"';
		}
		sb.append(UCharset._QuoteString(quote, e.text, quote));
	}
	@Override
	public void visitCharacter(PCharacter e) {
		sb.append("[" + e.charset, "]");
	}
	@Override
	public void visitAny(PAny e) {
		sb.append(".");
	}
	@Override
	public void visitTagging(PTagging e) {
		sb.append(e.symbol);
	}
	@Override
	public void visitMessage(PMessage e) {
		sb.append("`", e.symbol, "`");
	}
	@Override
	public void visitIndent(PIndent e) {
		sb.append("indent");
	}
	protected void format(String prefix, PUnary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(e.inner instanceof PTerm || e.inner instanceof PConstructor) {
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
	public void visitOptional(POptional e) {
		this.format( null, e, "?");
	}
	@Override
	public void visitRepetition(PRepetition e) {
		if(e.atleast == 1) {
			this.format( null, e, "+");
		}
		else {
			this.format(null, e, "*");
		}
	}
	@Override
	public void visitAnd(PAnd e) {
		this.format( "&", e, null);
	}

	@Override
	public void visitNot(PNot e) {
		this.format( "!", e, null);
	}

	@Override
	public void visitConnector(PConnector e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		this.format(predicate, e, null);
	}

	protected void format(PList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			PExpression e = l.get(i);
			if(e instanceof PChoice || e instanceof PSequence) {
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
	public void visitSequence(PSequence e) {
		//sb.append("( ");
		this.format( e);
		//sb.append(" )");
	}

	@Override
	public void visitChoice(PChoice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitConstructor(PConstructor e) {
		if(e.leftJoin) {
			sb.append("{@ ");
		}
		else {
			sb.append("{ ");
		}
		this.format(e);
		sb.append(" }");
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
		PExpression next = peg.getExpression(name);
		next.visit(this);
	}
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		if(	!e.isForeignNonTerminal() && !this.isVisited(e.symbol)) {
			visited(e.symbol);
			this.visitImpl(e.symbol);
		}
	}
}

class NonTerminalChecker extends PegVisitor {
	PegRule startRule;
	PegRule checking;
	int     consumedMinimumLength;

	void verify(PegRule rule) {
		this.initVisitor();
		if(Main.PackratStyleMemo && !(rule.expr instanceof PMemo)) {
			rule.expr = new PMemo(rule.expr);
			rule.expr.base.EnabledMemo += 1;
		}
		this.startRule = rule;
		this.verifyImpl(rule);
	}

	void verifyImpl(PegRule checking) {
		this.checking = checking;
		this.consumedMinimumLength = 0;
		this.checking.expr.visit(this);
		if(checking.length < this.consumedMinimumLength) {
			checking.length = this.consumedMinimumLength;
		}
	}
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		PegRule next = e.base.getRule(e.symbol);
		if(next == null) {
			checking.reportError("undefined label: " + e.symbol);
			e.base.foundError = true;
			return;
		}
		e.jumpExpression = next.expr;
		if(next == checking) {
			if(this.consumedMinimumLength == 0) {
				checking.reportError("left recursion: " + e.symbol);
				e.base.foundError = true;
				return;				
			}
			if(next.length < this.consumedMinimumLength) {
				next.length = this.consumedMinimumLength;
			}
			if(!this.checking.expr.is(PExpression.CyclicRule)) {
				Main.printVerbose("cyclic rule", e.symbol);
				this.checking.expr.set(PExpression.CyclicRule);
			}
		}
		if(next == startRule) {
//			if(this.consumedMinimumLength == 0) {
//				startRule.reportError("indirect left recursion: " + e.symbol);
//				e.base.foundError = true;
//				return;				
//			}
			if(next.length < this.consumedMinimumLength) {
				next.length = this.consumedMinimumLength;
			}
			if(!startRule.expr.is(PExpression.CyclicRule)) {
				Main.printVerbose("cyclic rule", e.symbol);
				startRule.expr.set(PExpression.CyclicRule);
			}
		}
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			PegRule _checking = this.checking;
			int _length = this.consumedMinimumLength;
			this.verifyImpl(next);
			this.consumedMinimumLength = _length;
			this.checking = _checking;
		}
		this.consumedMinimumLength += next.length;
		this.startRule.expr.derived(next.expr);
	}
	
	@Override
	public void visitString(PString e) {
		this.consumedMinimumLength += e.text.length();
	}
	
	@Override
	public void visitCharacter(PCharacter e) {
		this.consumedMinimumLength += 1;
	}
	@Override
	public void visitAny(PAny e) {
		this.consumedMinimumLength += 1;
	}
	@Override
	public void visitNot(PNot e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitAnd(PAnd e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitOptional(POptional e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitRepetition(PRepetition e) {
		int stackedLength = this.consumedMinimumLength;
		this.visitUnary(e);
		if(e.atleast == 0) {
			this.consumedMinimumLength = stackedLength;
		}
	}
	@Override
	public void visitConnector(PConnector e) {
		this.visitUnary(e);
	}
	@Override
	public void visitExport(PExport e) {
		this.visitUnary(e);
	}
	@Override
	public void visitSequence(PSequence e) {
		int stackedLength = this.consumedMinimumLength;
		for(int i = 0; i < e.size(); i++) {
			PExpression sub = e.get(i);
			sub.visit(this);
		}
		e.length = this.consumedMinimumLength - stackedLength;
	}
	@Override
	public void visitChoice(PChoice e) {
		int stackedLength = this.consumedMinimumLength;
		e.get(0).visit(this);		
		int min = this.consumedMinimumLength;
		for(int i = 1; i < e.size(); i++) {
			this.consumedMinimumLength = stackedLength;
			e.get(i).visit(this);
			if(this.consumedMinimumLength < min) {
				min = this.consumedMinimumLength;
			}
		}
		this.consumedMinimumLength = stackedLength;
	}
	@Override
	public void visitConstructor(PConstructor e) {
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
		UList<PExpression> pegList = this.peg.getExpressionList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}
	final boolean isInlinable(PExpression e) {
		if(e instanceof PNonTerminal && peg.optimizationLevel > 1) {
			return ! ((PNonTerminal) e).jumpExpression.is(PExpression.CyclicRule);
		}
		return false;
	}
	final PExpression doInline(PExpression parent, PNonTerminal r) {
		//System.out.println("inlining: " + parent.getClass().getSimpleName() + " " + r.symbol +  " Memo? " + (r.nextRule1 instanceof PegMemo) + " e=" + r.nextRule1);
		this.peg.InliningCount += 1;
		if(parent instanceof PRepetition) {
			System.out.println("inlining: " + parent.getClass().getSimpleName() + " " + r.symbol +  " Memo? " + (r.jumpExpression instanceof PMemo) + " e=" + r.jumpExpression);
		}
		return r.jumpExpression;
	}
	@Override
	public void visitUnary(PUnary e) {
		if(isInlinable(e.inner)) {
			e.inner = doInline(e, (PNonTerminal)e.inner);
		}
		else {
			e.inner.visit(this);
		}
		e.derived(e.inner);
	}
	@Override
	public void visitList(PList e) {
		for(int i = 0; i < e.size(); i++) {
			PExpression se = e.get(i);
			if(isInlinable(se)) {
				e.set(i, doInline(e, (PNonTerminal)se));
			}
			else {
				se.visit(this);
			}
			e.derived(se);
		}
	}
	@Override
	public void visitOperation(POperator e) {
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
		UList<PExpression> pegList = this.peg.getExpressionList();
		for(int i = 0; i < pegList.size(); i++) {
			pegList.ArrayValues[i].visit(this);
		}
	}
	
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		if(!this.isVisited(e.symbol)) {
			visited(e.symbol);
			e.base.getExpression(e.symbol).visit(this);
		}
	}
//	public void visitNotAny(PegNotAny e) {
//		e.orig.visit(this);
//	}

	private void removeMonad(PUnary e) {
		if(e.inner instanceof PMonad) {
			PMonad pm = (PMonad)e.inner;
			if(!pm.inner.hasObjectOperation()) {
				this.peg.InterTerminalOptimization += 1;
				e.inner = pm.inner;
			}
		}
	}

	private void removeCommit(PUnary e) {
		if(e.inner instanceof PCommit) {
			PCommit pm = (PCommit)e.inner;
			if(!pm.inner.hasObjectOperation()) {
				this.peg.InterTerminalOptimization += 1;
				e.inner = pm.inner;
			}
		}
	}

	@Override
	public void visitNot(PNot e) {
		removeMonad(e);
		this.visitUnary(e);
	}
	
	@Override
	public void visitAnd(PAnd e) {
		removeMonad(e);
		this.visitUnary(e);
	}
	
	@Override
	public void visitOptional(POptional e) {
		removeCommit(e);
		this.visitUnary(e);
	}
	@Override
	public void visitRepetition(PRepetition e) {
		removeCommit(e);
		this.visitUnary(e);
	}
	
	@Override
	public void visitConnector(PConnector e) {
		this.visitUnary(e);
	}
	
	@Override
	public void visitExport(PExport e) {
		this.visitUnary(e);
	}
	@Override
	public void visitList(PList e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}
	@Override
	public void visitSequence(PSequence e) {
		this.visitList(e);
	}
	@Override
	public void visitChoice(PChoice e) {
		this.visitList(e);
		if(e instanceof PMappedChoice) {
			((PMappedChoice) e).tryPrediction();
		}
	}
	
	private boolean needsObjectContext(PExpression e) {
		if(e.is(PExpression.HasConstructor) || e.is(PExpression.HasConnector) || e.is(PExpression.HasTagging) || e.is(PExpression.HasMessage) || e.is(PExpression.HasContext)) {
			return true;
		}
		return false;
	}

	@Override
	public void visitConstructor(PConstructor e) {
		int prefetchIndex = 0;
		for(int i = 0; i < e.size(); i++) {
			PExpression sub = e.get(i);
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
	UList<PExpression> pegList;
	int cutMiss = -1;
	int RemovedCount = 0;
	
	MemoRemover(Grammar peg) {
		UList<String> nameList = peg.ruleMap.keys();
		this.pegList = new UList<PExpression>(new PExpression[nameList.size()]);
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

	final boolean isRemoved(PMemo pm) {
		if(pm.memoMiss < this.cutMiss) {
			return true;
		}
		return !(pm.enableMemo);
	}
	
	PExpression removeMemo(PExpression e) {
		if(e instanceof PMemo) {
			PMemo pm = (PMemo)e;
			if(this.isRemoved(pm)) {
				this.RemovedCount += 1;
				return pm.inner;
			}
		}
		return e;
	}
	
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		if(e.jumpExpression != null) {
			e.jumpExpression = this.removeMemo(e.jumpExpression);
		}
	}

	@Override
	public void visitUnary(PUnary e) {
		e.inner = this.removeMemo(e.inner);
		e.inner.visit(this);
	}

	@Override
	public void visitList(PList e) {
		for(int i = 0; i < e.size(); i++) {
			PExpression se = this.removeMemo(e.get(i));
			e.set(i, se);
			se.visit(this);
		}
	}
	
	@Override
	public void visitChoice(PChoice e) {
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
	
	private PExpression returnPeg = null;
	PExpression removeObjectOperation(PExpression e) {
		PExpression stack = this.returnPeg;
		this.returnPeg = e;
		e.visit(this);
		PExpression r = this.returnPeg;
		this.returnPeg = stack;
		return r;
	}
	
	@Override
	public void visitNonTerminal(PNonTerminal e) {
		if(e.jumpExpression.hasObjectOperation()) {
			this.returnPeg = e.base.newNonTerminal(e.symbol + "'");
		}
	}
	
	@Override
	public void visitNotAny(PNotAny e) {
		PExpression ne = this.removeObjectOperation(e.not);
		if(ne == null) {
			this.returnPeg = e.base.newAny();
			return ;
		}
		e.not = (PNot)ne;
		this.returnPeg = e;
	}
	@Override
	public void visitTagging(PTagging e) {
		this.returnPeg = null;
	}
	@Override
	public void visitMessage(PMessage e) {
		this.returnPeg = null;
	}
	@Override
	public void visitIndent(PIndent e) {
		this.returnPeg = null;
	}
	@Override
	public void visitUnary(PUnary e) {
		PExpression ne = this.removeObjectOperation(e.inner);
		if(ne == null) {
			this.returnPeg = null;			
			return ;
		}
		e.inner = ne;
		this.returnPeg = e;
	}

	@Override
	public void visitOperation(POperator e) {
		PExpression ne = this.removeObjectOperation(e.inner);
		if(ne == null) {
			this.returnPeg = null;			
			return ;
		}
		e.inner = ne;
		this.returnPeg = e;
	}

	@Override
	public void visitConnector(PConnector e) {
		PExpression ne = this.removeObjectOperation(e.inner);
		if(ne == null) {
			this.returnPeg = null;
			return ;
		}
		this.returnPeg = ne;
	}

	@Override
	public void visitList(PList e) {
		for(int i = 0; i < e.size(); i++) {
			PExpression se = e.get(i);
			e.set(i, removeObjectOperation(se));
		}
		this.returnPeg = e.trim();
	}

	@Override
	public void visitConstructor(PConstructor e) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[e.size()]);
		for(int i = 0; i < e.size(); i++) {
			PExpression ne = removeObjectOperation(e.get(i));
			if(ne != null) {
				l.add(ne);
			}
		}
		//System.out.println("@@@ size=" + l.size() + " e=" + e);
		this.returnPeg = e.base.newSequence(l);
	}
}


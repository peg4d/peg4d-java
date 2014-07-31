package org.peg4d;



class PegOptimizer extends PegTransformer {
	Grammar peg;
	UMap<Peg> pegCache;
	private int optimizationLevel = 0;
	private int NonTerminalPrediction = 0;
	private int Inlining = 0;
	private int MappedChoice = 0;
	private int SelectiveChoice = 0;
	private int OptimizedPeg = 0;
	
	PegOptimizer(Grammar peg, UMap<Peg> cacheMap) {
		this.peg = peg;
		this.pegCache = cacheMap;
		this.optimizationLevel = peg.optimizationLevel;
	}
	
	void updateStat(Stat stat) {
		stat.setCount("OptimizedPeg", this.OptimizedPeg);
		stat.setCount("Inlining", this.Inlining);
		stat.setCount("NonTerminalPrediction", this.NonTerminalPrediction);
		stat.setCount("SelectiveChoice", this.SelectiveChoice);
		stat.setCount("MappedChoice", this.MappedChoice);
	}
	
	@Override
	public Peg transform(Grammar base, Peg e) {
		if(this.optimizationLevel > 0) {
			if(e instanceof PegNewObject) {
				return this.predictNewObject(base, ((PegNewObject) e));
			}
			if(e instanceof PegUnary) {
				Peg inner = ((PegUnary) e).inner;
				if(e instanceof PegNot) {
					return peepNot(base, e, inner.clone(base, this));
				}
				if(e instanceof PegSetter) {
					return peepSetter(base, e, inner);
				}
			}
			return this.peepHoleTransform(base, e);
		}
		return e;
	}
	
	private final boolean isTextMatchOnly(Peg e) {
		if(e.hasObjectOperation()) {
			return false;
		}
		if(this.optimizationLevel < 2) {
			if(e.is(Peg.HasTagging) || e.is(Peg.HasReserved) || e.is(Peg.HasMessage) || e.is(Peg.HasContext)) {
				return false;
			}
		}
		return true;
	}
		
//	private final Peg predictChoice(Grammar base, PegChoice orig) {
//		UList<Peg> flatList = new UList<Peg>(new Peg[orig.size()]);
//		for(int i = 0; i < orig.size(); i++) {
//			Peg sub = orig.get(i).clone(base, this);
//			if(sub instanceof PegChoice) {
//				log(orig, "flaten choice: " + sub);
//				for(int j = 0; j < sub.size(); j++) {
//					this.appendChoiceList(base, flatList, sub.get(j));
//				}
//			}
//			else {
//				this.appendChoiceList(base, flatList, sub);
//			}
//		}
//		if(flatList.size() == 1) {
//			log(orig, "removed choice: " + flatList.ArrayValues[0]);
//			return flatList.ArrayValues[0];
//		}
//		PegChoice newChoice = orig.isSame(flatList) ? orig : new PegChoice(base, orig.flag, flatList);
////		newChoice.getPrediction();
//		return newChoice;
//	}
	
	private final void appendChoiceList(Grammar base, UList<Peg> flatList, Peg e) {
		flatList.add(e);
	}

	private final Peg predictNewObject(Grammar base, PegNewObject orig) {
		PegNewObject ne = new PegNewObject(base, 0, orig.size(), orig.leftJoin);
		ne.flag = orig.flag;
		int predictionIndex = -1;
		for(int i = 0; i < orig.size(); i++) {
			Peg sub = orig.get(i).clone(base, this);
			ne.list.add(sub);
			if(isNeedsObjectCreation(sub) && predictionIndex == -1) {
				predictionIndex = i;
			}
		}
		if(predictionIndex == -1) {
			predictionIndex=0;
		}
		if(this.optimizationLevel >= 3 && predictionIndex > 0) {
			log(orig, "prediction index: " + predictionIndex);
			ne.prefetchIndex = predictionIndex;
		}
		return ne;
	}
	
	private boolean isNeedsObjectCreation(Peg e) {
		if(e.is(Peg.HasNewObject) || e.is(Peg.HasSetter) || e.is(Peg.HasTagging) || e.is(Peg.HasMessage) || e.is(Peg.HasContext)) {
			return true;
		}
		return false;
	}
	
	public Peg peepHoleTransform(Grammar base, Peg e) {
		return e;
	}
	
	private final Peg peepNot(Grammar base, Peg orig, Peg inner) {
		return new PegNot(base, orig.flag, inner);
	}

	private final Peg peepSetter(Grammar base, Peg orig, Peg inner) {
		if(!inner.is(Peg.HasNewObject)) {
			log(orig, "removed stupid peg: " + orig);
			return inner.clone(base, this);
		}
		return null;
	}

	final void log(Peg orig, String msg) {
		this.OptimizedPeg += 1;
	}

	final void log(int level, Peg orig, String msg) {
		if(Main.VerboseMode && optimizationLevel == level) {
			Main.printVerbose("-O" + level, msg);
		}
		this.OptimizedPeg  += 1;
	}

	final void log2(Peg orig, String msg) {
		if(Main.VerboseMode) {
			Main.printVerbose("-O" , msg);
		}
		this.OptimizedPeg += 1;
	}


//	private final boolean isCharacter(Peg e) {
//		if(e instanceof PegCharacter || e instanceof PegString1) {
//			return true;
//		}
//		if(e instanceof PegString) {
//			if(((PegString) e).text.length() == 1) {
//				return true;
//			}
//		}
//		return false;
//	}
	

}

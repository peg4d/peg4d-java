package org.peg4d;

import java.util.HashMap;


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
			if(e instanceof PegNonTerminal) {
				return this.performInlining(base, ((PegNonTerminal) e));
			}
			if(e instanceof PegChoice) {
				return this.predictChoice(base, ((PegChoice) e));
			}
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
	
	private final Peg performInlining(Grammar base, PegNonTerminal label) {
		String ruleName = label.symbol;
		Peg next = peg.getRule(ruleName);
		if(this.optimizationLevel > 3) {
			this.NonTerminalPrediction += 1;
		}
		if(next.is(Peg.CyclicRule) || !isTextMatchOnly(next)) {
			return label;  // 
		}
		Peg optimizedExpression = pegCache.get(ruleName);
		if(optimizedExpression == null) {
			optimizedExpression = next.clone(base, this);
			pegCache.put(ruleName, optimizedExpression);
		}				
		this.Inlining += 1;
		log(label, "inlining: " + ruleName);
		return optimizedExpression;
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
		
	private final Peg predictChoice(Grammar base, PegChoice orig) {
		UList<Peg> flatList = new UList<Peg>(new Peg[orig.size()]);
		for(int i = 0; i < orig.size(); i++) {
			Peg sub = orig.get(i).clone(base, this);
			if(sub instanceof PegChoice) {
				log(orig, "flaten choice: " + sub);
				for(int j = 0; j < sub.size(); j++) {
					this.appendChoiceList(base, flatList, sub.get(j));
				}
			}
			else {
				this.appendChoiceList(base, flatList, sub);
			}
		}
		if(flatList.size() == 1) {
			log(orig, "removed choice: " + flatList.ArrayValues[0]);
			return flatList.ArrayValues[0];
		}
		PegChoice newChoice = orig.isSame(flatList) ? orig : new PegChoice(base, orig.flag, flatList);
		newChoice.getPrediction();
		if(newChoice.predictableChoice > 0) {
			if(this.optimizationLevel >= 3 && newChoice.predictableChoice == newChoice.size()) {
//				if(newChoice.prefixSize > 1 && newChoice.predictableChoice > 1) {
//					PegMappedChoice choice = new PegMappedChoice(newChoice);
//					log2(orig, "mapped choice: " + choice.map);
//					this.MappedChoice += 1;
//					return choice;
//				}
//				if(newChoice.prefixSize == 1) {
					PegMappedCharacterChoice choice = new PegMappedCharacterChoice(newChoice);
					log2(orig, "mapped choice: " + choice.keyList);
					this.MappedChoice += 1;
					return choice;
//				}
			}
			else {
				//System.out.println("@@ " + newChoice +"\n\t p=" + newChoice.predictable + "<" + newChoice.size() + " size=" + newChoice.pretextSize);
				if(this.optimizationLevel >= 4 && newChoice.predictableChoice > (newChoice.size() / 2)) {
					PegSelectiveChoice choice = new PegSelectiveChoice(newChoice);
					log2(orig, "selective choice: " + choice.predictableChoice + ", " + choice.size());
					this.SelectiveChoice += 1;
				}
			}
		}
		return newChoice;
	}
	
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
	
	class PegEmpty extends PegOptimized {
		public PegEmpty(Peg orig) {
			super(orig);
		}
		@Override
		public Pego simpleMatch(Pego left, ParserContext context) {
			return left;
		}
		@Override
		public int fastMatch(int left, MonadicParser context) {
			return left;
		}
	}

	
	class PegMappedChoice extends PegChoice {
		private HashMap<String, Peg> map;
		PegMappedChoice(PegChoice choice) {
			super(choice.base, choice.flag, choice.list);
			this.getPrediction();
			this.map = new HashMap<String, Peg>(this.size() * 4 + 1);
			for(int i = 0; i < this.size(); i++) {
				Peg sub = this.get(i);
				
				String key = sub.getPrediction().toString().substring(0, this.prefixSize);
				Peg defined = map.get(key);
				if(defined != null) {
					sub = defined.appendAsChoice(sub);
				}
				map.put(key, sub);
			}
		}

		@Override
		public Pego simpleMatch(Pego left, ParserContext context) {
//			char ch = context.getChar();
//			if(this.predicted.match(ch)) {
				long pos = context.getPosition();
				String token = context.substring(pos, pos + this.prefixSize);
				Peg e = this.map.get(token);
				if(e != null) {
					Pego node2 = e.simpleMatch(left, context);
//					if(node2.isFailure()) {
//						context.setPosition(pos);
//					}
					return node2;
				}
//			}
			return context.foundFailure(this);
		}
		
		@Override
		public int fastMatch(int left, MonadicParser context) {
			long pos = context.getPosition();
			String token = context.substring(pos, pos + this.prefixSize);
			Peg e = this.map.get(token);
			if(e != null) {
				int node2 = e.fastMatch(left, context);
				if(PEGUtils.isFailure(node2)) {
					context.setPosition(pos);
				}
				return node2;
			}
			//System.out.println("failure: " + pos + " token='"+token+"' in " + this.map.keys());
			return context.foundFailure2(this);
		}
	}

	class PegAlwaysFailure extends PegOptimized {
		public PegAlwaysFailure(Peg orig) {
			super(orig);
		}
		@Override
		public Pego simpleMatch(Pego left, ParserContext context) {
			return context.foundFailure(this);
		}
		@Override
		public int fastMatch(int left, MonadicParser context) {
			return context.foundFailure2(this);
		}
	}

	class PegMappedCharacterChoice extends PegChoice {
		Peg[] caseOf;
		boolean foundUnicode = false;
		Peg   alwaysFailure;
		UList<String> keyList;
		
		PegMappedCharacterChoice(PegChoice choice) {
			super(choice.base, choice.flag, choice.list);
			this.getPrediction();
			this.keyList = new UList<String>(new String[16]);
			this.caseOf = new Peg[128];
			for(int i = 0; i < this.list.size(); i++) {
				Peg sub = this.get(i);
				Object p = sub.getPrediction();
				if(p instanceof String) {
					int ch = ((String) p).charAt(0);
					if(ch < 128) {
						if(this.caseOf[ch] == null) {
							this.keyList.add(Main._CharToString((char)ch));
						}
						this.caseOf[ch] = Peg.appendAsChoice(this.caseOf[ch], sub);
					}
					else {
						this.foundUnicode = true;
					}
				}
				if(p instanceof UCharset) {
					UCharset u = ((UCharset) p);
					for(int ch = 0; ch < 128; ch ++) {
						if(u.hasChar((char)ch)) {
							if(this.caseOf[ch] == null) {
								this.keyList.add(Main._CharToString((char)ch));
							}
							this.caseOf[ch] = Peg.appendAsChoice(this.caseOf[ch], sub);
						}
					}
					if(u.hasUnicode()) {
						this.foundUnicode = true;
					}
				}
			}
		}

		@Override
		public Pego simpleMatch(Pego left, ParserContext context) {
			int ch = context.getChar();
			if(ch < 128) {
				if(caseOf[ch] != null) {
					return caseOf[ch].simpleMatch(left, context);
				}
			}
			else {
				if(this.foundUnicode) {
					return super.simpleMatch(left, context);
				}
			}
			return context.foundFailure(this);
		}
		@Override
		public int fastMatch(int left, MonadicParser context) {
			int ch = context.getChar();
			if(ch < 128) {
				if(caseOf[ch] != null) {
					return caseOf[ch].fastMatch(left, context);
				}
			}
			else {
				if(this.foundUnicode) {
					return super.fastMatch(left, context);
				}
			}
			return context.foundFailure2(this);
		}
	}
	
	class PegSelectiveChoice extends PegChoice {
		Peg[] caseOf;
		Peg   alwaysFailure;
		PegSelectiveChoice(PegChoice c) {
			super(c.base, c.flag, c.list);
			this.predictableChoice = c.predictableChoice;
			this.predicted = c.predicted;
			this.prefixSize = c.prefixSize;
			this.caseOf = new Peg[128];
			this.alwaysFailure = new PegAlwaysFailure(c);
		}
				
		@Override
		public Pego simpleMatch(Pego left, ParserContext context) {
			int ch = context.getChar();
			if(ch < 128) {
				if(caseOf[ch] == null) {
					caseOf[ch] = selectPeg(ch);
				}
//				long pos = context.getPosition();
				Pego node = caseOf[ch].simpleMatch(left, context);
//				context.setPosition(pos);
//				Pego node2 = super.simpleMatch(left, context);
//				if(node.equals(node2)) {
//					System.out.println("!! ch = '" + ch + "'");
//				}
				return node;
			}
			return super.simpleMatch(left, context);
		}

		private Peg selectPeg(int ch) {
			Peg e = null;
			for(int i = 0; i < this.list.size(); i++) {
				Peg sub = this.get(i);
				if(sub.accept(ch)) {
					e = Peg.appendAsChoice(e, sub);
				}
			}
			if(e == null) {
				e = this.alwaysFailure;
			}
			//System.out.println("selected: '" + ch + "': " + e.size() + " ** " + e);
			return e;
		}

		@Override
		public int fastMatch(int left, MonadicParser context) {
			int ch = context.getChar();
			if(ch < 128) {
				if(caseOf[ch] == null) {
					caseOf[ch] = selectPeg(ch);
				}
				return caseOf[ch].fastMatch(left, context);
			}
			return super.fastMatch(left, context);
		}
		
	}

}

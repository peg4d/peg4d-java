package org.peg4d;

public class GrammarComposer {
	UList<Peg> definedExpressionList = new UList<Peg>(new Peg[128]);
	UMap<Grammar> pegMap = new UMap<Grammar>();
	
	Grammar getGrammar(String filePath) {
		Grammar peg = pegMap.get(filePath);
		if(peg != null) {
			return peg;
		}
		peg = loadLibraryGrammar(filePath);
		if(peg != null) {
			setGrammar(filePath, peg);
		}
		return peg;
	}
	
	void setGrammar(String path, Grammar peg) {
		this.pegMap.put(path, peg);
	}
	
	private Grammar loadLibraryGrammar(String filePath) {
		if(!filePath.endsWith(".peg")) {
			filePath = "lib/" + filePath + ".peg";
		}
		if(Main.VerbosePeg) {
			System.out.println("importing " + filePath);
		}
		return Grammar.load(this, filePath);
	}
	
	short issue(Peg peg) {
		this.definedExpressionList.add(peg);
		return (short)this.definedExpressionList.size();
	}

	public final Peg getDefinedExpression(long oid) {
		int index = (short)oid;
		return this.definedExpressionList.ArrayValues[index-1];
	}

	// factory
	
	UMap<Peg> semMap = new UMap<Peg>();

	private static String prefixNonTerminal = "a\b";
	private static String prefixString = "t\b";
	private static String prefixCharacter = "c\b";
	private static String prefixNot = "!\b";
	private static String prefixAnd = "&\b";
	private static String prefixOneMore = "+\b";
	private static String prefixZeroMore = "*\b";
	private static String prefixOptional = "?\b";
	private static String prefixSequence = " \b";
	private static String prefixChoice = "/\b";
	
	private static String prefixSetter  = "S\b";
	private static String prefixTagging = "T\b";
	private static String prefixMessage = "M\b";
	
	private Peg getsem(String t) {
		Peg e = semMap.get(t);
		if(e != null) {
			e.refc += 1;
		}
		return e;
	}

	private Peg putsem(String t, Peg e) {
		if(Main.AllExpressionMemo && !e.is(Peg.NoMemo)) {
			e.base.EnabledMemo += 1;
			e = newMemo(e);
		}
		semMap.put(t, e);
		return e;
	}
	
	public final Peg newMemo(Peg e) {
		if(e instanceof PegMemo) {
			return e;
		}
		return new PegMemo(e);
	}

	public final Peg newNonTerminal(Grammar peg, String text) {
		Peg e = getsem(prefixNonTerminal + text);
		if(e == null) {
			e = new PegNonTerminal(peg, 0, text);
			e = putsem(prefixNonTerminal + text, e);
		}
		return e;
	}

	public final Peg newString(Grammar peg, String text) {
		Peg e = getsem(prefixString + text);
		if(e == null) {
			e = putsem(prefixString + text, this.newStringImpl(peg, text));
		}
		return e;
	}

	private Peg newStringImpl(Grammar peg, String text) {
		if(peg.optimizationLevel > 0) {
			if(UCharset.toUtf8(text).length == 1) {
				peg.LexicalOptimization += 1;
				return new PegString1(peg, 0, text);				
			}
			if(UCharset.toUtf8(text).length == 2) {
				peg.LexicalOptimization += 1;
				return new PegString2(peg, 0, text);				
			}
		}
		return new PegString(peg, 0, text);	
	}
	
	public final Peg newAny(Grammar peg) {
		Peg e = getsem("");
		if(e == null) {
			e = new PegAny(peg, 0);
			e = putsem("", e);
		}
		return e;
	}
	
	public final Peg newCharacter(Grammar peg, String text) {
		UCharset u = new UCharset(text);
		String key = prefixCharacter + u.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegCharacter(peg, 0, u);
			e = putsem(key, e);
		}
		return e;
	}

	public final Peg newOptional(Grammar peg, Peg p) {
		String key = prefixOptional + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newOptionalImpl(peg, p));
		}
		return e;
	}

	private Peg newOptionalImpl(Grammar peg, Peg p) {
		if(p instanceof PegString1) {
			peg.LexicalOptimization += 1;
			return new PegOptionalString1(peg, 0, (PegString1)p);
		}
		if(p instanceof PegString) {
			peg.LexicalOptimization += 1;
			return new PegOptionalString(peg, 0, (PegString)p);
		}
		if(p instanceof PegCharacter) {
			peg.LexicalOptimization += 1;
			return new PegOptionalCharacter(peg, 0, (PegCharacter)p);
		}
		return new PegOptional(peg, 0, newCommit(peg, p));
	}
	
	private Peg newCommit(Grammar peg, Peg p) {
		if(!p.is(Peg.HasConstructor) && !p.is(Peg.HasNonTerminal) && !p.is(Peg.HasConnector)) {
			return p;
		}
		return new PegCommit(p);
	}

	private Peg newMonad(Grammar peg, Peg p) {
		if(!p.is(Peg.HasConstructor) && !p.is(Peg.HasNonTerminal) && !p.is(Peg.HasConnector)) {
			return p;
		}
		return new PegMonad(p);
	}
	
	public final Peg newOneMore(Grammar peg, Peg p) {
		String key = prefixOneMore + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newOneMoreImpl(peg, p));
		}
		return e;
	}

	private Peg newOneMoreImpl(Grammar peg, Peg p) {
		if(peg.optimizationLevel > 0) {
			if(p instanceof PegCharacter) {
				peg.LexicalOptimization += 1;
				return new PegOneMoreCharacter(peg, 0, (PegCharacter)p);
			}
		}
		return new PegRepetition(peg, 0, newCommit(peg, p), 1);
	}
	
	public final Peg newZeroMore(Grammar peg, Peg p) {
		String key = prefixZeroMore + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newZeroMoreImpl(peg, p));
		}
		return e;
	}

	private Peg newZeroMoreImpl(Grammar peg, Peg p) {
		if(p instanceof PegCharacter) {
			peg.LexicalOptimization += 1;
			return new PegZeroMoreCharacter(peg, 0, (PegCharacter)p);
		}
		return new PegRepetition(peg, 0, newCommit(peg, p), 0);
	}

	public final Peg newAnd(Grammar peg, Peg p) {
		String key = prefixAnd + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newAndImpl(peg, p));
		}
		return e;
	}
	
	private Peg newAndImpl(Grammar peg, Peg p) {
		if(p instanceof PegOperation) {
			p = ((PegOperation)p).inner;
		}
		return new PegAnd(peg, 0, p);
	}

	public final Peg newNot(Grammar peg, Peg p) {
		String key = prefixNot + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newNotImpl(peg, p));
		}
		return e;
	}
	
	private Peg newNotImpl(Grammar peg, Peg p) {
		if(peg.optimizationLevel > 0) {
			if(p instanceof PegString) {
				peg.LexicalOptimization += 1;
				if(p instanceof PegString1) {
					return new PegNotString1(peg, 0, (PegString1)p);
				}
				if(p instanceof PegString2) {
					return new PegNotString2(peg, 0, (PegString2)p);
				}
				return new PegNotString(peg, 0, (PegString)p);
			}
			if(p instanceof PegCharacter) {
				peg.LexicalOptimization += 1;
				return new PegNotCharacter(peg, 0, (PegCharacter)p);
			}
		}
		if(p instanceof PegOperation) {
			p = ((PegOperation)p).inner;
		}
		return new PegNot(peg, 0, newMonad(peg, p));
	}
	
	public Peg newChoice(Grammar peg, UList<Peg> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		String key = prefixChoice;
		boolean isAllText = true;
		for(int i = 0; i < l.size(); i++) {
			Peg se = l.ArrayValues[i];
			key += se.key();
			if(!(se instanceof PegString) && !(se instanceof PegString)) {
				isAllText = false;
			}
		}
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newChoiceImpl(peg, l, isAllText));
		}
		return e;
	}

	private Peg newChoiceImpl(Grammar peg, UList<Peg> l, boolean isAllText) {
		if(peg.optimizationLevel > 0) {
			if(isAllText) {
				return new PegWordChoice(peg, 0, l);
			}
		}
		if(peg.optimizationLevel > 2) {
			return new PegSelectiveChoice(peg, 0, l);
		}		
		return new PegChoice(peg, 0, l);
	}
	
	public Peg mergeChoice(Grammar peg, Peg e, Peg e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<Peg> l = new UList<Peg>(new Peg[e.size()+e2.size()]);
		addChoice(peg, l, e);
		addChoice(peg, l, e2);
		return new PegChoice(peg, 0, l);
	}

	public Peg newSequence(Grammar peg, UList<Peg> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		if(l.size() == 0) {
			return this.newString(peg, "");
		}
		String key = prefixSequence;
		for(int i = 0; i < l.size(); i++) {
			key += l.ArrayValues[i].key();
		}
		Peg e = getsem(key);
		if(e == null) {
			e = newSequenceImpl(peg, l);
			e = putsem(key, e);
		}
		return e;
	}

	private Peg newSequenceImpl(Grammar peg, UList<Peg> l) {
		Peg orig = new PegSequence(peg, 0, l);
		if(peg.optimizationLevel > 1) {
			int nsize = l.size()-1;
			if(nsize > 0 && l.ArrayValues[nsize] instanceof PegAny) {
				boolean allNot = true;
				for(int i = 0; i < nsize; i++) {
					if(!(l.ArrayValues[nsize] instanceof PegNot)) {
						allNot = false;
						break;
					}
				}
				if(allNot) {
					return newNotAnyImpl(peg, orig, l, nsize);
				}
			}
		}
		return orig;
	}

	public Peg newConstructor(Grammar peg, String tagName, Peg p) {
		Peg e = new PegConstructor(peg, 0, false, "#"+tagName, toSequenceList(p));
		if(peg.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
			e = new PegMemo(e);
		}
		return e;
	}

	public Peg newJoinConstructor(Grammar peg, String tagName, Peg p) {
		Peg e = new PegConstructor(peg, 0, true, "#"+tagName, toSequenceList(p));
		if(peg.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
			e = new PegMemo(e);
		}
		return e;
	}
	
	public UList<Peg> toSequenceList(Peg e) {
		if(e instanceof PegSequence) {
			return ((PegSequence) e).list;
		}
		UList<Peg> l = new UList<Peg>(new Peg[1]);
		l.add(e);
		return l;
	}
	
	private Peg newNotAnyImpl(Grammar peg, Peg orig, UList<Peg> l, int nsize) {
		if(nsize == 1) {
			return new PegNotAny(peg, 0, (PegNot)l.ArrayValues[0], orig);
		}
		return orig;
	}
	
	public final Peg newConnector(Grammar peg, Peg p, int index) {
		String key = prefixSetter + index + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegSetter(peg, 0, p, index);
			e = putsem(key, e);
		}
		return e;
	}

	public final Peg newTagging(Grammar peg, String tag) {
		String key = prefixTagging + tag;
		Peg e = getsem(key);
		if(e == null) {
			e = new PegTagging(peg, 0, tag);
			e = putsem(key, e);
		}
		return e;
	}

	public final Peg newMessage(Grammar peg, String msg) {
		String key = prefixMessage + msg;
		Peg e = getsem(key);
		if(e == null) {
			e = new PegMessage(peg, 0, msg);
			e = putsem(key, e);
		}
		return e;
	}
		
	public void addChoice(Grammar peg, UList<Peg> l, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(peg, l, e.get(i));
			}
			return;
		}
		if(peg.optimizationLevel > 0) {
			if(l.size() > 0 && (e instanceof PegString1 || e instanceof PegCharacter)) {
				Peg prev = l.ArrayValues[l.size()-1];
				if(prev instanceof PegString1) {
					UCharset charset = new UCharset("");
					charset.append((char)((PegString1) prev).symbol1);
					PegCharacter c = new PegCharacter(e.base, 0, charset);
					l.ArrayValues[l.size()-1] = c;
					prev = c;
				}
				if(prev instanceof PegCharacter) {
					UCharset charset = ((PegCharacter) prev).charset;
					if(e instanceof PegCharacter) {
						charset.append(((PegCharacter) e).charset);
					}
					else {
						charset.append((char)((PegString1) e).symbol1);
					}
					return;
				}
			}
		}
		l.add(e);
	}

	public void addSequence(UList<Peg> l, Peg e) {
		if(e instanceof PegSequence) {
			for(int i = 0; i < e.size(); i++) {
				this.addSequence(l, e.get(i));
			}
		}
		else {
			l.add(e);
		}
	}
	
//	int memoId = 0;
//	PegMemo newMemo(Peg e) {
//		return null;
//	}
//	
//}
}

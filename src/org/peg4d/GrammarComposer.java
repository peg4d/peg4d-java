package org.peg4d;

public class GrammarComposer {
	UList<PExpression> definedExpressionList = new UList<PExpression>(new PExpression[128]);
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
		if(!filePath.endsWith(".p4d")) {
			filePath = filePath + ".p4d";
			if(!filePath.startsWith("lib/")) {
				filePath = "lib/" + filePath;
			}
		}
		if(Main.VerbosePeg) {
			System.out.println("importing " + filePath);
		}
		return Grammar.load(this, filePath);
	}
	
	short issue(PExpression peg) {
		this.definedExpressionList.add(peg);
		return (short)this.definedExpressionList.size();
	}

	public final PExpression getDefinedExpression(long oid) {
		int index = (short)oid;
		return this.definedExpressionList.ArrayValues[index-1];
	}

	// factory
	
	UMap<PExpression> semMap = new UMap<PExpression>();

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
	
	private PExpression getsem(String t) {
		PExpression e = semMap.get(t);
		if(e != null) {
		}
		return e;
	}

	private PExpression putsem(String t, PExpression e) {
		if(Main.AllExpressionMemo && !e.is(PExpression.NoMemo)) {
			e.base.EnabledMemo += 1;
			e = newMemo(e);
		}
		semMap.put(t, e);
		return e;
	}
	
	public final PExpression newMemo(PExpression e) {
		if(e instanceof PMemo) {
			return e;
		}
		return new PMemo(e);
	}

	public final PExpression newNonTerminal(Grammar peg, String text) {
		PExpression e = getsem(prefixNonTerminal + text);
		if(e == null) {
			e = new PNonTerminal(peg, 0, text);
			e = putsem(prefixNonTerminal + text, e);
		}
		return e;
	}

	public final PExpression newString(Grammar peg, String text) {
		PExpression e = getsem(prefixString + text);
		if(e == null) {
			e = putsem(prefixString + text, this.newStringImpl(peg, text));
		}
		return e;
	}

	private PExpression newStringImpl(Grammar peg, String text) {
		if(peg.optimizationLevel > 0) {
			if(UCharset.toUtf8(text).length == 1) {
				peg.LexicalOptimization += 1;
				return new PString1(peg, 0, text);				
			}
		}
		return new PString(peg, 0, text);	
	}
	
	public final PExpression newAny(Grammar peg) {
		PExpression e = getsem("");
		if(e == null) {
			e = new PAny(peg, 0);
			e = putsem("", e);
		}
		return e;
	}
	
	public final PExpression newCharacter(Grammar peg, String text) {
		UCharset u = new UCharset(text);
		String key = prefixCharacter + u.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = new PCharacter(peg, 0, u);
			e = putsem(key, e);
		}
		return e;
	}

	public final PExpression newOptional(Grammar peg, PExpression p) {
		String key = prefixOptional + p.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, newOptionalImpl(peg, p));
		}
		return e;
	}

	private PExpression newOptionalImpl(Grammar peg, PExpression p) {
		if(p instanceof PString1) {
			peg.LexicalOptimization += 1;
			return new POptionalString1(peg, 0, (PString1)p);
		}
		if(p instanceof PString) {
			peg.LexicalOptimization += 1;
			return new POptionalString(peg, 0, (PString)p);
		}
		if(p instanceof PCharacter) {
			peg.LexicalOptimization += 1;
			return new POptionalCharacter(peg, 0, (PCharacter)p);
		}
		return new POptional(peg, 0, newCommit(peg, p));
	}
	
	private PExpression newCommit(Grammar peg, PExpression p) {
//		if(!p.is(PExpression.HasConstructor) && !p.is(PExpression.HasNonTerminal) && !p.is(PExpression.HasConnector)) {
//			return p;
//		}
//		return new PCommit(p);
		return p;
	}

	public final PExpression newMatch(Grammar peg, PExpression p) {
		if(!p.hasObjectOperation() && !p.is(PExpression.HasNonTerminal)) {
			return p;
		}
		return new PMatch(p);
	}
	
	public final PExpression newOneMore(Grammar peg, PExpression p) {
		String key = prefixOneMore + p.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, newOneMoreImpl(peg, p));
		}
		return e;
	}

	private PExpression newOneMoreImpl(Grammar peg, PExpression p) {
		if(peg.optimizationLevel > 0) {
			if(p instanceof PCharacter) {
				peg.LexicalOptimization += 1;
				return new POneMoreCharacter(peg, 0, (PCharacter)p);
			}
		}
		return new PRepetition(peg, 0, newCommit(peg, p), 1);
	}
	
	public final PExpression newZeroMore(Grammar peg, PExpression p) {
		String key = prefixZeroMore + p.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, newZeroMoreImpl(peg, p));
		}
		return e;
	}

	private PExpression newZeroMoreImpl(Grammar peg, PExpression p) {
		if(p instanceof PCharacter) {
			peg.LexicalOptimization += 1;
			return new PZeroMoreCharacter(peg, 0, (PCharacter)p);
		}
		return new PRepetition(peg, 0, newCommit(peg, p), 0);
	}

	public final PExpression newAnd(Grammar peg, PExpression p) {
		String key = prefixAnd + p.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newAndImpl(peg, p));
		}
		return e;
	}
	
	private PExpression newAndImpl(Grammar peg, PExpression p) {
		if(p instanceof POperator) {
			p = ((POperator)p).inner;
		}
		return new PAnd(peg, 0, p);
	}

	public final PExpression newNot(Grammar peg, PExpression p) {
		String key = prefixNot + p.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newNotImpl(peg, p));
		}
		return e;
	}
	
	private PExpression newNotImpl(Grammar peg, PExpression p) {
		if(peg.optimizationLevel > 0) {
			if(p instanceof PString) {
				peg.LexicalOptimization += 1;
				if(p instanceof PString1) {
					return new PNotString1(peg, 0, (PString1)p);
				}
				return new PNotString(peg, 0, (PString)p);
			}
			if(p instanceof PCharacter) {
				peg.LexicalOptimization += 1;
				return new PNotCharacter(peg, 0, (PCharacter)p);
			}
		}
		if(p instanceof POperator) {
			p = ((POperator)p).inner;
		}
		return new PNot(peg, 0, newMatch(peg, p));
	}
	
	public PExpression newChoice(Grammar peg, UList<PExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		String key = prefixChoice;
		boolean isAllText = true;
		for(int i = 0; i < l.size(); i++) {
			PExpression se = l.ArrayValues[i];
			key += se.key();
			if(!(se instanceof PString) && !(se instanceof PString)) {
				isAllText = false;
			}
		}
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, newChoiceImpl(peg, l, isAllText));
		}
		return e;
	}

	private PExpression newChoiceImpl(Grammar peg, UList<PExpression> l, boolean isAllText) {
		if(peg.optimizationLevel > 0) {
			if(isAllText) {
				return new PegWordChoice(peg, 0, l);
			}
		}
		if(peg.optimizationLevel > 2) {
			return new PMappedChoice(peg, 0, l);
		}		
		return new PChoice(peg, 0, l);
	}
	
	public PExpression mergeChoice(Grammar peg, PExpression e, PExpression e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<PExpression> l = new UList<PExpression>(new PExpression[e.size()+e2.size()]);
		addChoice(peg, l, e);
		addChoice(peg, l, e2);
		return new PChoice(peg, 0, l);
	}

	public PExpression newSequence(Grammar peg, UList<PExpression> l) {
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
		PExpression e = getsem(key);
		if(e == null) {
			e = newSequenceImpl(peg, l);
			e = putsem(key, e);
		}
		return e;
	}

	private PExpression newSequenceImpl(Grammar peg, UList<PExpression> l) {
		PExpression orig = new PSequence(peg, 0, l);
		if(peg.optimizationLevel > 1) {
			int nsize = l.size()-1;
			if(nsize > 0 && l.ArrayValues[nsize] instanceof PAny) {
				boolean allNot = true;
				for(int i = 0; i < nsize; i++) {
					if(!(l.ArrayValues[nsize] instanceof PNot)) {
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

	public PExpression newConstructor(Grammar peg, String tagName, PExpression p) {
		PExpression e = new PConstructor(peg, 0, false, "#"+tagName, toSequenceList(p));
		if(peg.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
			e = new PMemo(e);
		}
		return e;
	}

	public PExpression newJoinConstructor(Grammar peg, String tagName, PExpression p) {
		PExpression e = new PConstructor(peg, 0, true, "#"+tagName, toSequenceList(p));
		if(peg.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
			e = new PMemo(e);
		}
		return e;
	}
	
	public UList<PExpression> toSequenceList(PExpression e) {
		if(e instanceof PSequence) {
			return ((PSequence) e).list;
		}
		UList<PExpression> l = new UList<PExpression>(new PExpression[1]);
		l.add(e);
		return l;
	}
	
	private PExpression newNotAnyImpl(Grammar peg, PExpression orig, UList<PExpression> l, int nsize) {
		if(nsize == 1) {
			return new PNotAny(peg, 0, (PNot)l.ArrayValues[0], orig);
		}
		return orig;
	}
	
	public final PExpression newConnector(Grammar peg, PExpression p, int index) {
		String key = prefixSetter + index + p.key();
		PExpression e = getsem(key);
		if(e == null) {
			e = new PConnector(peg, 0, p, index);
			e = putsem(key, e);
		}
		return e;
	}

	public final PExpression newTagging(Grammar peg, String tag) {
		String key = prefixTagging + tag;
		PExpression e = getsem(key);
		if(e == null) {
			e = new PTagging(peg, 0, tag);
			e = putsem(key, e);
		}
		return e;
	}

	public final PExpression newMessage(Grammar peg, String msg) {
		String key = prefixMessage + msg;
		PExpression e = getsem(key);
		if(e == null) {
			e = new PMessage(peg, 0, msg);
			e = putsem(key, e);
		}
		return e;
	}
		
	public void addChoice(Grammar peg, UList<PExpression> l, PExpression e) {
		if(e instanceof PChoice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(peg, l, e.get(i));
			}
			return;
		}
		if(peg.optimizationLevel > 0) {
			if(l.size() > 0 && (e instanceof PString1 || e instanceof PCharacter)) {
				PExpression prev = l.ArrayValues[l.size()-1];
				if(prev instanceof PString1) {
					UCharset charset = new UCharset("");
					charset.append((char)((PString1) prev).symbol1);
					PCharacter c = new PCharacter(e.base, 0, charset);
					l.ArrayValues[l.size()-1] = c;
					prev = c;
				}
				if(prev instanceof PCharacter) {
					UCharset charset = ((PCharacter) prev).charset;
					if(e instanceof PCharacter) {
						charset.append(((PCharacter) e).charset);
					}
					else {
						charset.append((char)((PString1) e).symbol1);
					}
					return;
				}
			}
		}
		l.add(e);
	}

	public void addSequence(UList<PExpression> l, PExpression e) {
		if(e instanceof PSequence) {
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

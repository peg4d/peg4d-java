package org.peg4d;

public class GrammarFactory {
	
	public GrammarFactory() {
	}

	public Grammar newGrammar(String name) {
		return new Grammar(this, name);
	}

	public Grammar newGrammar(String name, String fileName) {
		Grammar peg = new Grammar(this, name);
		peg.loadGrammarFile(fileName);
		return peg;
	}

	UMap<Grammar> grammarMap = new UMap<Grammar>();
	
	Grammar getGrammar(String filePath) {
		Grammar peg = grammarMap.get(filePath);
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
		this.grammarMap.put(path, peg);
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
		return this.newGrammar(filePath, filePath);
	}

	UMap<PExpression> uniqueExpressionMap = new UMap<PExpression>();

//	private static String prefixNonTerminal = "a\b";
	private static String prefixString = "t\b";
	private static String prefixByte   = "b\b";
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
		PExpression e = uniqueExpressionMap.get(t);
		if(e != null) {
			return e;
		}
		return null;
	}

	private PExpression putsem(String t, PExpression e) {
//		if(Main.AllExpressionMemo && !e.is(PExpression.NoMemo)) {
//			e.base.EnabledMemo += 1;
//			e = newMemo(e);
//		}
		uniqueExpressionMap.put(t, e);
		e.uniqueId = newExpressionId();
		return e;
	}
	
	private int uniqueId = 0;
	public final int newExpressionId() {
		uniqueId++;
		return uniqueId;
	}

//	public final PExpression newMemo(PExpression e) {
//		if(e instanceof ParsingMemo) {
//			return e;
//		}
//		return new ParsingMemo(e);
//	}

//	public final PExpression newNonTerminal(Grammar peg, String text) {
//		String key = prefixNonTerminal + text;
//		PExpression e = getsem(key);
//		if(e == null) {
//			e = putsem(key, new PNonTerminal(peg, 0, text));
//		}
//		return e;
//	}
	

	public final PExpression newString(Grammar peg, String text) {
		String key = prefixString + text;
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newStringImpl(peg, text));
		}
		return e;
	}

	private PExpression newStringImpl(Grammar peg, String text) {
		byte[] utf8 = ParsingCharset.toUtf8(text);
		if(peg.optimizationLevel > 0) {
			if(ParsingCharset.toUtf8(text).length == 1) {
				peg.LexicalOptimization += 1;
				return new PByteChar(0, utf8[0]);
			}
		}
		return new PString(0, text, utf8);	
	}

	public final PExpression newByte(Grammar peg, int ch, String text) {
		String key = prefixByte + text;
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, new PByteChar(0, ch));
		}
		return e;
	}

	public final PExpression newAny(Grammar peg, String text) {
		PExpression e = getsem(text);
		if(e == null) {
			e = new PAny(0);
			e = putsem(text, e);
		}
		return e;
	}
	
	public final PExpression newCharacter(Grammar peg, ParsingCharset u) {
//		ParsingCharset u = ParsingCharset.newParsingCharset(text);
//		String key = prefixCharacter + u.toString();
//		PExpression e = getsem(key);
//		if(e == null) {
//			e = new PCharacter(peg, 0, u);
//			e = putsem(key, e);
//		}
//		return e;
		return new PCharacter(0, u);
	}

	private String pkey(PExpression p) {
		return "#" + p.uniqueId;
	}
	
	public final PExpression newOptional(Grammar peg, PExpression p) {
		if(p.isUnique()) {
			String key = prefixOptional + pkey(p);
			PExpression e = getsem(key);
			if(e == null) {
				e = putsem(key, newOptionalImpl(peg, p));
			}
			return e;
		}
		return newOptionalImpl(peg, p);
	}

	private PExpression newOptionalImpl(Grammar peg, PExpression p) {
		if(p instanceof PByteChar) {
			peg.LexicalOptimization += 1;
			return new POptionalByteChar(0, (PByteChar)p);
		}
		if(p instanceof PString) {
			peg.LexicalOptimization += 1;
			return new POptionalString(0, (PString)p);
		}
		if(p instanceof PCharacter) {
			peg.LexicalOptimization += 1;
			return new POptionalCharacter(0, (PCharacter)p);
		}
		if(p instanceof PRepetition) {
			((PRepetition) p).atleast = 0;
			peg.LexicalOptimization += 1;
			return p;
		}
		return new POptional(0, p);
	}
	
	public final PExpression newMatch(Grammar peg, PExpression p) {
		if(!p.hasObjectOperation() && !p.is(PExpression.HasNonTerminal)) {
			return p;
		}
		return new ParsingMatch(p);
	}
	
	public final PExpression newOneMore(Grammar peg, PExpression p) {
		String key = prefixOneMore + pkey(p);
		PExpression e = getsem(key);
		if(e == null) {
			e = putsem(key, newOneMoreImpl(peg, p));
		}
		return e;
	}

	private PExpression newOneMoreImpl(Grammar peg, PExpression p) {
		if(peg.optimizationLevel > 0) {
//			if(p instanceof PCharacter) {
//				peg.LexicalOptimization += 1;
//				return new POneMoreCharacter(peg, 0, (PCharacter)p);
//			}
		}
		return new PRepetition(0, p, 1);
	}
	
	public final PExpression newZeroMore(Grammar peg, PExpression p) {
		if(p.isUnique()) {
			String key = prefixZeroMore + pkey(p);
			PExpression e = getsem(key);
			if(e == null) {
				e = putsem(key, newZeroMoreImpl(peg, p));
			}
			return e;
		}
		return newZeroMoreImpl(peg, p);
	}

	private PExpression newZeroMoreImpl(Grammar peg, PExpression p) {
		if(p instanceof PCharacter) {
			peg.LexicalOptimization += 1;
			return new PZeroMoreCharacter(0, (PCharacter)p);
		}
		return new PRepetition(0, p, 0);
	}

	public final PExpression newAnd(Grammar peg, PExpression p) {
		if(p.isUnique()) {
			String key = prefixAnd + pkey(p);
			PExpression e = getsem(key);
			if(e == null) {
				e = putsem(key, this.newAndImpl(peg, p));
			}
			return e;
		}
		return this.newAndImpl(peg, p);
	}
	
	private PExpression newAndImpl(Grammar peg, PExpression p) {
		return new PAnd(0, p);
	}

	public final PExpression newNot(Grammar peg, PExpression p) {
		if(p.isUnique()) {
			String key = prefixNot + pkey(p);
			PExpression e = getsem(key);
			if(e == null) {
				e = putsem(key, this.newNotImpl(peg, p));
			}
			return e;
		}
		return this.newNotImpl(peg, p);
	}
	
	private PExpression newNotImpl(Grammar peg, PExpression p) {
		if(peg.optimizationLevel > 0) {
			if(p instanceof PString) {
				peg.LexicalOptimization += 1;
				if(p instanceof PByteChar) {
					return new PNotByteChar(0, (PByteChar)p);
				}
				return new PNotString(0, (PString)p);
			}
			if(p instanceof PCharacter) {
				peg.LexicalOptimization += 1;
				return new PNotCharacter(0, (PCharacter)p);
			}
		}
		if(p instanceof ParsingOperation) {
			p = ((ParsingOperation)p).inner;
		}
		return new PNot(0, newMatch(peg, p));
	}
	
	private boolean isUnique(UList<PExpression> l) {
		for(int i = 0; i < l.size(); i++) {
			PExpression se = l.ArrayValues[i];
			if(!se.isUnique()) {
				return false;
			}
		}
		return true;
	}
	public PExpression newChoice(Grammar peg, UList<PExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		if(isUnique(l)) {
			String key = prefixChoice;
			boolean isAllText = true;
			for(int i = 0; i < l.size(); i++) {
				PExpression p = l.ArrayValues[i];
				key += pkey(p);
				if(!(p instanceof PString) && !(p instanceof PString)) {
					isAllText = false;
				}
			}
			PExpression e = getsem(key);
			if(e == null) {
				e = putsem(key, newChoiceImpl(peg, l, isAllText));
			}
			return e;
		}
		return newChoiceImpl(peg, l, false);
	}

	private PExpression newChoiceImpl(Grammar peg, UList<PExpression> l, boolean isAllText) {
		if(peg.optimizationLevel > 0) {
//			if(isAllText) {
//				return new PegWordChoice(peg, 0, l);
//			}
		}
		if(peg.optimizationLevel > 2) {
			return new PMappedChoice(0, l);
		}		
		return new PChoice(0, l);
	}
	
	public PExpression mergeChoice(Grammar peg, PExpression e, PExpression e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<PExpression> l = new UList<PExpression>(new PExpression[e.size()+e2.size()]);
		addChoice(peg, l, e);
		addChoice(peg, l, e2);
		return new PChoice(0, l);
	}

	public PExpression newSequence(Grammar peg, UList<PExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		if(l.size() == 0) {
			return this.newString(peg, "");
		}
		if(isUnique(l)) {
			String key = prefixSequence;
			for(int i = 0; i < l.size(); i++) {
				key += pkey(l.ArrayValues[i]);
			}
			PExpression e = getsem(key);
			if(e == null) {
				e = newSequenceImpl(peg, l);
				e = putsem(key, e);
			}
			return e;
		}
		return newSequenceImpl(peg, l);
	}

	private PExpression newSequenceImpl(Grammar peg, UList<PExpression> l) {
		PExpression orig = new PSequence(0, l);
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
		PExpression e = new PConstructor(0, false, toSequenceList(p));
//		if(peg.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
//			e = new ParsingMemo(e);
//		}
		return e;
	}

	public PExpression newJoinConstructor(Grammar peg, String tagName, PExpression p) {
		PExpression e = new PConstructor(0, true, toSequenceList(p));
//		if(peg.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
//			e = new ParsingMemo(e);
//		}
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
//		if(nsize == 1) {
//			return new PNotAny(peg, 0, (PNot)l.ArrayValues[0], orig);
//		}
		return orig;
	}
	
	public final PExpression newConnector(Grammar peg, PExpression p, int index) {
		if(p.isUnique()) {
			String key = prefixSetter + index + pkey(p);
			PExpression e = getsem(key);
			if(e == null) {
				e = new PConnector(0, p, index);
				e = putsem(key, e);
			}
			return e;
		}
		return new PConnector(0, p, index);
	}

	public final PExpression newTagging(Grammar peg, String tag) {
		String key = prefixTagging + tag;
		PExpression e = getsem(key);
		if(e == null) {
			e = new PTagging(0, peg.getModelTag(tag));
			e = putsem(key, e);
		}
		return e;
	}

	public final PExpression newMessage(Grammar peg, String msg) {
		String key = prefixMessage + msg;
		PExpression e = getsem(key);
		if(e == null) {
			e = new PMessage(0, msg);
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
			if(l.size() > 0 && (e instanceof PByteChar || e instanceof PCharacter)) {
				PExpression prev = l.ArrayValues[l.size()-1];
				if(prev instanceof PByteChar) {
					int ch = ((PByteChar) prev).byteChar;
					ParsingCharset charset = new ByteCharset(ch, ch);
					PCharacter c = new PCharacter(0, charset);
					l.ArrayValues[l.size()-1] = c;
					prev = c;
				}
				if(prev instanceof PCharacter) {
					ParsingCharset charset = ((PCharacter) prev).charset;
					if(e instanceof PCharacter) {
						charset = charset.merge(((PCharacter) e).charset);
					}
					else {
						int ch = ((PByteChar) e).byteChar;
						charset.appendByte(ch, ch);
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
}

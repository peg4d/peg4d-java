package org.peg4d;



class Optimizer2 {
	
	public static boolean InlineNonTerminal = false;
	public static boolean CharacterChoice   = false;
	public static boolean StringChoice      = false;
	public static boolean PredictedChoice   = false;
	
	static void enableOptimizer() {
		InlineNonTerminal = true;
		CharacterChoice   = true;
		StringChoice      = true;
//		PredictedChoice   = true;  // Don't enable. it bugs
	}

	public static int countOptimizedNonTerminal = 0;
	public static int countOptimizedCharacterChoice = 0;
	public static int countOptimizedStringChoice = 0;
	public static int countOptimizedChoice       = 0;
	
	final static void optimize(ParsingExpression e) {
		if(!e.isOptimized()) {
			if(e instanceof NonTerminal) {
				optimizeNonTerminal((NonTerminal)e);
			}
		}
		for(int i = 0; i < e.size(); i++) {
			optimize(e.get(i));
		}
		if(!e.isOptimized()) {
			if(e instanceof ParsingChoice) {
				optimizeChoice((ParsingChoice)e);
			}
		}
	}

	final static void optimizeNonTerminal(NonTerminal ne) {
		if(InlineNonTerminal) {
			ParsingExpression e = resolveNonTerminal(ne);
			ne.matcher = e.matcher;
			countOptimizedNonTerminal += 1;
		}
		//ne.report(ReportLevel.notice, "inlining " + e);
	}
	
	final static ParsingExpression resolveNonTerminal(ParsingExpression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	final static void optimizeChoice(ParsingChoice choice) {
		int[] c = new int[256];
		if(CharacterChoice && checkCharacterChoice(choice, c)) {
//			System.out.println("Optimized1: " + choice);
			for(int ch = 0; ch < 256; ch++) {
//				if(c[ch] > 0) {
//					System.out.println("|1 " + GrammarFormatter.stringfyByte(ch) + ": true" );
//				}
			}
			choice.matcher = new ByteChoiceMatcher(c);
			countOptimizedCharacterChoice += 1;
			countOptimizedChoice += 1;
			return;
		}
		for(int i = 0; i < c.length; i++) { 
			c[i] = 0; 
		}
		if(StringChoice && checkStringChoice(choice, c)) {
			ParsingExpression[] matchCase = new ParsingExpression[257];
			ParsingExpression empty = ParsingExpression.newEmpty();
			makeStringChoice(choice, matchCase, empty, empty);
			ParsingExpression f = new ParsingFailure(choice).uniquefy();
//			System.out.println("StringChoice: " + choice);
			for(int ch = 0; ch < matchCase.length; ch++) {
				if(matchCase[ch] == null) {
					matchCase[ch] = f;
				}
				else {
					matchCase[ch] = matchCase[ch].uniquefy();
//					System.out.println("|2 " + GrammarFormatter.stringfyByte(ch) + ":\t" + matchCase[ch]);
					if(matchCase[ch] instanceof ParsingChoice) {
						optimizeChoice((ParsingChoice)matchCase[ch]);
					}
				}
			}
			choice.matcher = new StringChoiceMatcher(matchCase);
			countOptimizedChoice += 1;
			countOptimizedStringChoice += 1;
			return;
		}
		if(PredictedChoice) {
			ParsingExpression[] matchCase = new ParsingExpression[257];
			matchCase[256] = new ParsingFailure(choice);  // EOF
			//System.out.println("Optimized3: " + choice);
			for(int ch = 0; ch < 256; ch++) {
				matchCase[ch] = selectChoice(choice, ch, matchCase[256]);
				matchCase[ch] = matchCase[ch].uniquefy();
//				if(matchCase[ch] != matchCase[256]) {
//					System.out.println("|3 " + GrammarFormatter.stringfyByte(ch) + ":\t" + matchCase[ch]);
//				}
				countOptimizedChoice += 1;
			}
			choice.matcher = new MappedChoiceMatcher(matchCase);
		}
	}
		
	final static boolean checkCharacterChoice(ParsingChoice choice, int[] c) {
		for(int i = 0; i < choice.size(); i++) {
			ParsingExpression e = resolveNonTerminal(choice.get(i));
			if(e instanceof ParsingByte) {
				c[((ParsingByte) e).byteChar]++;
				continue;
			}
			if(e instanceof ParsingByteRange) {
				ParsingByteRange r = ((ParsingByteRange) e);
				for(int c1 = r.startByteChar; c1 <= r.endByteChar; c1++) {
					c[c1]++;
				} 
				continue;
			}
			if(e instanceof ParsingChoice) {
				if(!checkCharacterChoice((ParsingChoice)e, c)) {
					return false;
				}
				continue;
			}
			return false;
		}
		return true;
	}

	final static ParsingExpression resolveSequence(ParsingExpression e) {
		e = resolveNonTerminal(e);
		while(e instanceof ParsingSequence) {
			if(e.size() == 0) {
				break;
			}
			e = resolveNonTerminal(e.get(0));
		}
		return e;
	}
	
	final static boolean checkStringChoice(ParsingChoice choice, int[] c) {
		for(int i = 0; i < choice.size(); i++) {
			ParsingExpression e = resolveSequence(choice.get(i));
			if(e instanceof ParsingByte) {
				c[((ParsingByte) e).byteChar]++;
				continue;
			}
			if(e instanceof ParsingByteRange) {
				ParsingByteRange r = ((ParsingByteRange) e);
				for(int c1 = r.startByteChar; c1 <= r.endByteChar; c1++) {
					c[c1]++;
				} 
				continue;
			}
			if(e instanceof ParsingChoice) {
				if(!checkCharacterChoice((ParsingChoice)e, c)) {
					return false;
				}
				continue;
			}
			//System.out.println("@@@@@ NotString: " + e);
			return false;
		}
		return true;
	}

	final static void makeStringChoice(ParsingChoice choice, ParsingExpression[] matchCase, ParsingExpression empty, ParsingExpression value) {
		for(int i = 0; i < choice.size(); i++) {
			appendCaseChoice(choice.get(i), matchCase, empty, value);
		}
	}
	
	final static void appendCaseChoice(ParsingExpression e, ParsingExpression[] matchCase, ParsingExpression empty, ParsingExpression value) {
		e = resolveNonTerminal(e);
		if(e instanceof ParsingByte) {
			int ch = ((ParsingByte) e).byteChar;
			matchCase[ch] = ParsingExpression.appendAsChoice(matchCase[ch], value);
		}
		if(e instanceof ParsingByteRange) {
			for(int c = ((ParsingByteRange) e).startByteChar; c <= ((ParsingByteRange) e).endByteChar; c++) {
				matchCase[c] = ParsingExpression.appendAsChoice(matchCase[c], value);
			}
		}
		if(e instanceof ParsingChoice) {
			makeStringChoice((ParsingChoice)e, matchCase, empty, value);
		}
		if(e instanceof ParsingSequence) {
			ParsingExpression e0 = e.get(0);
			ParsingExpression e1 = newLastSequence(e, empty);
//			System.out.println("sequence="+e);
//			System.out.println("         "+e0 + " as " + e0.getClass());
//			System.out.println("         "+e1);
			appendCaseChoice(e0, matchCase, empty, e1);
		}
	}

	private static ParsingExpression newLastSequence(ParsingExpression e, ParsingExpression empty) {
		if(e.size() == 1) {
			return empty;
		}
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[e.size()-1]);
		for(int i = 1; i < e.size(); i++) {
			l.add(e.get(i));
		}
		return ParsingExpression.newSequence(l);
	}
	
	private static ParsingExpression selectChoice(ParsingChoice choice, int ch, ParsingExpression failed) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[2]);
		for(int i = 0; i < choice.size(); i++) {
			if(choice.get(i).acceptByte(ch) != ParsingExpression.Reject) {
				l.add(choice.get(i));
			}
		}
		if(l.size() == 0) {
			l.add(failed);
		}
		return ParsingExpression.newChoice(l);
	}
}

class ByteChoiceMatcher implements Matcher {
	boolean bitMap[];

	ByteChoiceMatcher(int[] c) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < c.length; i++) { 
			if(c[i] > 0) {
				this.bitMap[i] = true;
			}
		}
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		if(this.bitMap[c]) {
			context.consume(1);
			return true;
		}
		context.opFailure();
		return false;
	}
}

class StringChoiceMatcher implements Matcher {
	ParsingExpression[] matchCase;

	StringChoiceMatcher(ParsingExpression[] matchCase) {
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		long pos = context.getPosition();
		context.consume(1);
		if(this.matchCase[c].matcher.simpleMatch(context)) {
			return true;
		}
		context.rollback(pos);
		return false;
	}
	
}

class MappedChoiceMatcher implements Matcher {
	ParsingExpression[] matchCase;

	MappedChoiceMatcher(ParsingExpression[] matchCase) {
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		return this.matchCase[c].matcher.simpleMatch(context);
	}
}

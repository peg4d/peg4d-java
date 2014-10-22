package org.peg4d;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingMatcher;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;

class Optimizer2 {
	public static boolean InlineNonTerminal     = true;

	public static boolean NonZeroByte           = false;
	public static boolean Specialization        = false;
	public static boolean StringSpecialization  = false;
	
	public static boolean CharacterChoice   = false;
	public static boolean StringChoice      = false;
	public static boolean PredictedChoice   = false;

	public static boolean LazyConstructor   = false;
	public static boolean NotCharacter      = false;

	static void enableOptimizer() {
		if(Main.DebugLevel > 0) {
			InlineNonTerminal    = false;
		}
		if(Main.OptimizationLevel > 0) {
			Specialization       = true;
			StringSpecialization = true;
			CharacterChoice      = true;
		}
		if(Main.OptimizationLevel > 1) {
			LazyConstructor   = true;
			StringChoice      = true;
			NotCharacter      = true;
		}
		if(Main.OptimizationLevel > 2) {
			PredictedChoice   = true;
		}
		if(Main.OptimizationLevel > 3) {
			NonZeroByte  = true;
		}
	}

	public static int countOptimizedNonTerminal = 0;

	public static int countSpecializedSequence  = 0;
	public static int countLazyConstructor      = 0;
	public static int countSpecializedNot       = 0;
	
	public static int countOptimizedCharacterChoice = 0;
	public static int countOptimizedStringChoice = 0;
	public static int countOptimizedChoice       = 0;

	final static void optimize(ParsingExpression e) {
		for(int i = 0; i < e.size(); i++) {
			optimize(e.get(i));
		}
		if(!e.isOptimized()) {
			if(e instanceof ParsingChoice) {
				optimizeChoice((ParsingChoice)e);
				return;
			}
			if(e instanceof ParsingSequence) {
				optimizeSequence((ParsingSequence)e);
				return;
			}
			if(e instanceof ParsingConstructor) {
				optimizeConstructor((ParsingConstructor)e);
				return;
			}
			if(NonZeroByte) {
				if(e instanceof ParsingByte && ((ParsingByte) e).byteChar != 0) {
					e.matcher = new NonZeroByteMatcher(((ParsingByte) e).byteChar);
					return;
				}
			}
			if(Specialization) {
				if(e instanceof ParsingNot) {
					optimizeNot((ParsingNot)e);
					return;
				}
				if(e instanceof ParsingOption) {
					optimizeOption((ParsingOption)e);
					return;
				}
				if(e instanceof ParsingRepetition) {
					optimizeRepetition((ParsingRepetition)e);
					return;
				}
			}
		}
	}

	final static void optimizeInline(ParsingExpression e) {
		if(!e.isOptimized()) {
			if(e instanceof NonTerminal) {
				optimizeNonTerminal((NonTerminal)e);
			}
		}
		for(int i = 0; i < e.size(); i++) {
			optimizeInline(e.get(i));
		}
	}

	final static void optimizeNonTerminal(NonTerminal ne) {
		ParsingExpression e = resolveNonTerminal(ne);
		ne.matcher = e.matcher;
		countOptimizedNonTerminal += 1;
	}

	final static ParsingExpression resolveNonTerminal(ParsingExpression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	final static void optimizeNot(ParsingNot holder) {
		ParsingExpression inner = holder.inner;
		if(InlineNonTerminal && inner instanceof NonTerminal) {
			inner = resolveNonTerminal(inner);
		}
		if(inner instanceof ParsingByte) {
			holder.matcher = new NotByteMatcher(((ParsingByte) inner).byteChar);
			countSpecializedNot += 1;
			return;
		}
		if(inner instanceof ParsingAny) {
			holder.matcher = new NotAnyMatcher();
			countSpecializedNot += 1;
			return;
		}
		ParsingMatcher m = inner.matcher;
		if(m instanceof ByteChoiceMatcher) {
			holder.matcher = new NotByteChoiceMatcher(((ByteChoiceMatcher) m).bitMap);
			countSpecializedNot += 1;
			return;
		}
		if(m instanceof StringSequenceMatcher) {
			holder.matcher = new NotStringSequenceMatcher(((StringSequenceMatcher) m).utf8);
			countSpecializedNot += 1;
			return;
		}
		//System.out.println("not " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
	}

	final static void optimizeOption(ParsingOption holder) {
		ParsingExpression inner = holder.inner;
		if(InlineNonTerminal && inner instanceof NonTerminal) {
			inner = resolveNonTerminal(inner);
		}
//		if(inner instanceof ParsingByte) {
//			holder.matcher = new NotByteMatcher(((ParsingByte) inner).byteChar);
//			countSpecializedNot += 1;
//			return;
//		}
//		if(inner instanceof ParsingAny) {
//			holder.matcher = new NotAnyMatcher();
//			countSpecializedNot += 1;
//			return;
//		}
//		ParsingMatcher m = inner.matcher;
//		if(m instanceof ByteChoiceMatcher) {
//			holder.matcher = new NotByteChoiceMatcher(((ByteChoiceMatcher) m).bitMap);
//			countSpecializedNot += 1;
//			return;
//		}
		//System.out.println("option " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
	}

	final static void optimizeRepetition(ParsingRepetition holder) {
		ParsingExpression inner = holder.inner;
		if(InlineNonTerminal && inner instanceof NonTerminal) {
			inner = resolveNonTerminal(inner);
		}
//		if(inner instanceof ParsingByte) {
//			holder.matcher = new NotByteMatcher(((ParsingByte) inner).byteChar);
//			countSpecializedNot += 1;
//			return;
//		}
//		if(inner instanceof ParsingAny) {
//			holder.matcher = new NotAnyMatcher();
//			countSpecializedNot += 1;
//			return;
//		}
		if(inner instanceof ParsingByteRange) {
			holder.matcher = new ZeroMoreByteRangeMatcher(((ParsingByteRange) inner).startByteChar, ((ParsingByteRange) inner).endByteChar);
			countSpecializedNot += 1;
			return;
		}
		ParsingMatcher m = inner.matcher;
		if(m instanceof ByteChoiceMatcher) {
			holder.matcher = new ZeroMoreByteChoiceMatcher(((ByteChoiceMatcher) m).bitMap);
			countSpecializedNot += 1;
			return;
		}
		//System.out.println("Unoptimized repetition " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
	}

	final static void optimizeConstructor(ParsingConstructor holder) {
		int prefetchIndex = 0;
		for(int i = 0; i < holder.size(); i++) {
			ParsingExpression sub = holder.get(i);
			if(sub.hasObjectOperation()) {
				break;
			}
			prefetchIndex = i + 1;
		}
		if(prefetchIndex > 0 && LazyConstructor) {
			countLazyConstructor += 1;
			holder.prefetchIndex = prefetchIndex;
		}
	}

	final static void optimizeSequence(ParsingSequence holder) {
		if(NotCharacter && holder.size() == 2 && holder.get(0) instanceof ParsingNot && holder.get(1) instanceof ParsingAny) {
			ParsingExpression inner = ((ParsingNot)holder.get(0)).inner;
			if(InlineNonTerminal && inner instanceof NonTerminal) {
				inner = resolveNonTerminal(inner);
			}
			if(inner instanceof ParsingByte) {
				holder.matcher = new ByteChoiceMatcher(((ParsingByte) inner).byteChar);
				countSpecializedNot += 1;
				return;
			}
			ParsingMatcher m = inner.matcher;
			if(m instanceof ByteChoiceMatcher) {
				holder.matcher = new ByteChoiceMatcher(((ByteChoiceMatcher) m), false);
				countSpecializedNot += 1;
				return;
			}
			//System.out.println("not any " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
		}
		if(StringSpecialization) {
			byte[] u = new byte[holder.size()];
			for(int i = 0; i < holder.size(); i++) {
				ParsingExpression inner = resolveNonTerminal(holder.get(i));				
				if(inner instanceof ParsingByte) {
					u[i] = (byte)((ParsingByte) inner).byteChar;
					continue;
				}
				return;
			}
			holder.matcher = new StringSequenceMatcher(u);
			countSpecializedSequence += 1;
			return;
		}
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
			boolean selfChoice = false;
			ParsingExpression[] matchCase = new ParsingExpression[257];
			ParsingExpression fails = new ParsingFailure(choice);
			//System.out.println("Optimized3: " + choice);
			for(int ch = 0; ch <= 256; ch++) {
				matchCase[ch] = selectChoice(choice, ch, fails);
				if(matchCase[ch] == choice) {
					/* this is a rare case where the selected choice is the parent choice */
					/* this cause the repeated calls of the same matchers */
					//System.out.println("SELF CHOICE: " + choice + " at " + GrammarFormatter.stringfyByte(ch) );
					selfChoice = true;
					//return; 
				}
				if(matchCase[ch] != fails) {
					//System.out.println("|3 " + GrammarFormatter.stringfyByte(ch) + ":\t" + matchCase[ch]);
				}
				countOptimizedChoice += 1;
			}
//			if(selfChoice) {
//				System.out.println("SELF CHOICE: " + choice);
//			}
			choice.matcher = selfChoice ? new MappedSelfChoiceMatcher(choice, matchCase) : new MappedChoiceMatcher(choice, matchCase);
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
		selectChoice(choice, ch, failed, l);
		if(l.size() == 0) {
			l.add(failed);
		}
		return ParsingExpression.newChoice(l).uniquefy();
	}

	private static void selectChoice(ParsingChoice choice, int ch, ParsingExpression failed, UList<ParsingExpression> l) {
		for(int i = 0; i < choice.size(); i++) {
			ParsingExpression e = resolveNonTerminal(choice.get(i));
			if(e instanceof ParsingChoice) {
				selectChoice((ParsingChoice)e, ch, failed, l);
			}
			else {
				short r = e.acceptByte(ch);
				//System.out.println("~ " + GrammarFormatter.stringfyByte(ch) + ": r=" + r + " in " + e);
				if(r != ParsingExpression.Reject) {
					l.add(e);
				}
			}
		}
	}
}

class NonZeroByteMatcher extends ParsingMatcher {
	int byteChar;
	NonZeroByteMatcher(int byteChar) {
		this.byteChar = byteChar;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.fastByteAt(context.pos);
		if(c == byteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}


class NotByteMatcher extends ParsingMatcher {
	int byteChar;

	NotByteMatcher(int byteChar) {
		this.byteChar = byteChar;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		if(c == byteChar) {
			context.failure(this);
			return false;
		}
		return true;
	}
}

class NotAnyMatcher extends ParsingMatcher {
	NotAnyMatcher() {
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		if(c == ParsingSource.EOF) {
			return true;
		}		
		context.failure(this);
		return false;
	}
}

class NotByteChoiceMatcher extends ParsingMatcher {
	boolean bitMap[];
	NotByteChoiceMatcher(boolean bitMap[]) {
		this.bitMap = bitMap;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		if(this.bitMap[c]) {
			context.failure(this);
			return false;			
		}
		return true;
	}
}

class ZeroMoreByteRangeMatcher extends ParsingMatcher {
	int startChar;
	int endChar;
	ZeroMoreByteRangeMatcher(int startChar, int endChar) {
		this.startChar = startChar;
		this.endChar = endChar;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		while(true) {
			int c = context.source.byteAt(context.pos);
			if(c < startChar || endChar < c) {
				break;
			}
			context.pos += 1;
		}
		return true;
	}
}

class ZeroMoreByteChoiceMatcher extends ParsingMatcher {
	boolean bitMap[];
	ZeroMoreByteChoiceMatcher(boolean bitMap[]) {
		this.bitMap = bitMap;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		while(true) {
			int c = context.source.byteAt(context.pos);
			if(!this.bitMap[c]) {
				break;
			}
			context.pos += 1;
		}
		return true;
	}
}

class StringSequenceMatcher extends ParsingMatcher {
	byte[] utf8;
	StringSequenceMatcher(byte[] utf8) {
		this.utf8 = utf8;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(utf8.length);
			return true;
		}
		context.failure(this);
		return false;
	}
}

class NotStringSequenceMatcher extends ParsingMatcher {
	byte[] utf8;
	NotStringSequenceMatcher(byte[] utf8) {
		this.utf8 = utf8;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.failure(this);
			return false;
		}
		return true;
	}
}

class OptionalStringSequenceMatcher extends ParsingMatcher {
	byte[] utf8;
	OptionalStringSequenceMatcher(byte[] utf8) {
		this.utf8 = utf8;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(utf8.length);
		}
		return true;
	}
}

class ByteChoiceMatcher extends ParsingMatcher {
	boolean bitMap[];
	ByteChoiceMatcher(int[] c) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < c.length; i++) { 
			if(c[i] > 0) {
				this.bitMap[i] = true;
			}
		}
	}
	ByteChoiceMatcher(int beginChar, int endChar) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < 256; i++) {
			if(beginChar <= i && i <= endChar) {
				this.bitMap[i] = true;
			}
			else {
				this.bitMap[i] = false;
			}
		}
	}
	ByteChoiceMatcher(int NotChar) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < 256; i++) { 
			this.bitMap[i] = true;
		}
		this.bitMap[NotChar] = false;
	}
	ByteChoiceMatcher(ByteChoiceMatcher notByteChoice, boolean eof) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < 256; i++) { 
			this.bitMap[i] = !notByteChoice.bitMap[i];
		}
		this.bitMap[256] = eof;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		if(this.bitMap[c]) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}

class StringChoiceMatcher extends ParsingMatcher {
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

class MappedChoiceMatcher extends ParsingMatcher {
	ParsingChoice choice;
	ParsingExpression[] matchCase;

	MappedChoiceMatcher(ParsingChoice choice, ParsingExpression[] matchCase) {
		this.choice = choice;
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		//System.out.println("pos="+context.pos + ", c=" + (char)c + " in " + choice);
		return this.matchCase[c].matcher.simpleMatch(context);
	}
}

class MappedSelfChoiceMatcher extends ParsingMatcher {
	ParsingChoice choice;
	ParsingExpression[] matchCase;

	MappedSelfChoiceMatcher(ParsingChoice choice, ParsingExpression[] matchCase) {
		this.choice = choice;
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		//System.out.println("pos="+context.pos + ", c=" + (char)c + " in " + choice);
		if(this.matchCase[c] == choice) {
			return choice.simpleMatch(context);
		}
		return this.matchCase[c].matcher.simpleMatch(context);
	}
}

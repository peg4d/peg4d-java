package org.peg4d.expression;

import nez.util.UList;

import org.peg4d.Grammar;
import org.peg4d.Main;
import org.peg4d.NezLogger;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingRule;

public class Optimizer {
	public int OptimizeLevel = 2;
	public int OptimizedMask  = 1;

	public static int O_Inline           = 1 << 0;	
	public static int O_SpecLexer        = 1 << 1;
	public static int O_SpecString       = 1 << 2;
	public static int O_ByteMap          = 1 << 3;
	public static int O_Prediction       = 1 << 4;
	public static int O_LazyObject       = 1 << 5;
	public static int O_Ext              = 1 << 30;

	//	public static int NotAnyFlag                = 1 << 1;
	
//	public static int PredictedChoiceFlag       = 1 << 7;
	
	private int CountInline = 0;
	private int CountSpecString  = 0;
	private int CountLazyObject      = 0;
	private int CountSpecLexer       = 0;
	
	private int CountByteMap    = 0;
	private int CountPrediction = 0;
//	public static int countOptimizedChoice       = 0;

	private Recognizer newByteChoiceMatcher(int[] c) {
		return new ByteMapMatcher(c);
	}

	public final void optimize(Grammar peg, NezLogger stats) {
		int OptimizationLevel = Main.OptimizationLevel;
		this.OptimizedMask = 0;
		if(Main.DebugLevel > 0) {
			OptimizationLevel = 0;
		}
		if(OptimizationLevel < 10) {
			if(OptimizationLevel >= 1) {
				this.OptimizedMask |= O_Inline;
				this.OptimizedMask |= O_SpecLexer;
				this.OptimizedMask |= O_SpecString;
			}
			if(OptimizationLevel >= 2) {
				this.OptimizedMask |= O_ByteMap;
				this.OptimizedMask |= O_Prediction;
				this.OptimizedMask |= O_LazyObject;
			}
		}
		else {
			switch(OptimizationLevel) {
				case 11: OptimizedMask = O_Inline; break;
				case 12: OptimizedMask = O_ByteMap; break;
				case 13: OptimizedMask = O_SpecLexer; break;
				case 14: OptimizedMask = O_SpecString; break;
				case 15: OptimizedMask = O_Prediction; break;
	
				case 22:
					this.OptimizedMask = O_Inline | O_ByteMap ;
					break;
				case 33:
					this.OptimizedMask = O_Inline | O_ByteMap | O_SpecLexer ;
					break;
				case 44:
					this.OptimizedMask = O_Inline | O_ByteMap | O_SpecLexer | O_SpecString ;
					break;
				case 55:
					this.OptimizedMask = O_Inline | O_SpecLexer | O_SpecString | O_ByteMap | O_Prediction;
					break;
	
				case 91:
					this.OptimizedMask = /*O_Inline |*/ O_SpecLexer | O_SpecString | O_ByteMap | O_Prediction;
					break;
				case 92:
					this.OptimizedMask = O_Inline | O_SpecLexer | O_SpecString /*| O_ByteMap*/ | O_Prediction;
					break;
				case 93:
					this.OptimizedMask = O_Inline /*| O_SpecLexer*/ | O_SpecString | O_ByteMap | O_Prediction;
					break;
				case 94:
					this.OptimizedMask = O_Inline | O_SpecLexer /*| O_SpecString*/ | O_ByteMap | O_Prediction;
					break;
				case 95:
					this.OptimizedMask = O_Inline | O_SpecLexer | O_SpecString | O_ByteMap /*| O_Prediction*/;
					break;
				case 99:
					this.OptimizedMask = O_Inline | O_SpecLexer | O_SpecString | O_ByteMap | O_Prediction | O_Ext;
					break;
					
			}
		}
		for(int i = 0; i < peg.definedNameList.size(); i++) {
			ParsingRule rule = peg.getRule(peg.definedNameList.ArrayValues[i]);
			this.optimize(rule.expr);
		}
		if(this.is(O_Inline)) {
			for(int i = 0; i < peg.definedNameList.size(); i++) {
				ParsingRule rule = peg.getRule(peg.definedNameList.ArrayValues[i]);
				this.optimizeInline(rule.expr);
			}
		}
		if(stats != null) {
			stats.setCount("O.Inline", this.CountInline);
			stats.setCount("O.Lexer",  this.CountSpecLexer);
			stats.setCount("O.String", this.CountSpecString);
			stats.setCount("O.ByteMap", this.CountByteMap);
			stats.setCount("O.Prediction", this.CountPrediction);
			stats.setCount("O.LazyObject", this.CountLazyObject);
		}
	}
	
	public final void optimize(ParsingExpression e) {
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
			if(is(O_ByteMap) && e instanceof ParsingByteRange) {
				e.matcher = new ByteMapMatcher(((ParsingByteRange) e).startByteChar, ((ParsingByteRange) e).endByteChar);
				this.CountByteMap += 1;
				return;
			}
			if(is(O_SpecLexer)) {
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

	private final boolean is(int flag) {
		return Main._IsFlag(this.OptimizedMask, flag);
	}
	
	public final static ParsingExpression resolveNonTerminal(ParsingExpression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}
	
	public final void optimizeInline(ParsingExpression e) {
		if(!e.isOptimized()) {
			if(e instanceof NonTerminal) {
				optimizeNonTerminal((NonTerminal)e);
			}
		}
		for(int i = 0; i < e.size(); i++) {
			optimizeInline(e.get(i));
		}
	}

	final void optimizeNonTerminal(NonTerminal ne) {
		ParsingExpression e = resolveNonTerminal(ne);
		ne.matcher = e.matcher;
		CountInline += 1;
	}

	final void optimizeNot(ParsingNot holder) {
		ParsingExpression inner = holder.inner;
		if(this.is(O_Inline) && inner instanceof NonTerminal) {
			inner = resolveNonTerminal(inner);
		}
		if(inner instanceof ParsingByte) {
			class NotByteMatcher implements Recognizer {
				int byteChar;
				NotByteMatcher(int byteChar) {
					this.byteChar = byteChar;
				}
				@Override
				public boolean match(ParsingContext context) {
					int c = context.source.byteAt(context.pos);
					if(c == byteChar) {
						context.failure(this);
						return false;
					}
					return true;
				}
			}
			holder.matcher = new NotByteMatcher(((ParsingByte) inner).byteChar);
			CountSpecLexer += 1;
			return;
		}
		Recognizer m = inner.matcher;
		if(m instanceof ByteMapMatcher) {
			class NotByteMapMatcher implements Recognizer {
				boolean bitMap[];
				NotByteMapMatcher(boolean bitMap[]) {
					this.bitMap = bitMap;
				}
				@Override
				public boolean match(ParsingContext context) {
					int c = context.source.byteAt(context.pos);
					if(this.bitMap[c]) {
						context.failure(this);
						return false;			
					}
					return true;
				}
			}
			holder.matcher = new NotByteMapMatcher(((ByteMapMatcher) m).bitMap);
			CountSpecLexer += 1;
			return;
		}
		if(m instanceof StringMatcher) {
			class NotStringMatcher implements Recognizer {
				byte[] utf8;
				NotStringMatcher(byte[] utf8) {
					this.utf8 = utf8;
				}
				@Override
				public boolean match(ParsingContext context) {
					if(context.source.match(context.pos, this.utf8)) {
						context.failure(this);
						return false;
					}
					return true;
				}
			}
			holder.matcher = new NotStringMatcher(((StringMatcher) m).utf8);
			CountSpecLexer += 1;
			return;
		}
		//System.out.println("not " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
	}

	final void optimizeOption(ParsingOption holder) {
		ParsingExpression inner = holder.inner;
		if(this.is(O_Inline) && inner instanceof NonTerminal) {
			inner = resolveNonTerminal(inner);
		}
		if(inner instanceof ParsingByte) {
			class OptionByteMatcher implements Recognizer {
				int byteChar;
				OptionByteMatcher(int byteChar) {
					this.byteChar = byteChar;
				}
				@Override
				public boolean match(ParsingContext context) {
					int c = context.source.byteAt(context.pos);
					if(c == byteChar) {
						context.consume(1);
					}
					return true;
				}
			}
			holder.matcher = new OptionByteMatcher(((ParsingByte) inner).byteChar);
			CountSpecLexer += 1;
			return;
		}
		Recognizer m = inner.matcher;
		if(m instanceof ByteMapMatcher) {
			class OptionByteMapMatcher implements Recognizer {
				boolean bitMap[];
				OptionByteMapMatcher(boolean bitMap[]) {
					this.bitMap = bitMap;
				}
				@Override
				public boolean match(ParsingContext context) {
					int c = context.source.byteAt(context.pos);
					if(this.bitMap[c]) {
						context.consume(1);
					}
					return true;
				}
			}
			holder.matcher = new OptionByteMapMatcher(((ByteMapMatcher) m).bitMap);
			CountSpecLexer += 1;
			return;
		}
		if(m instanceof StringMatcher) {
			class OptionStringMatcher implements Recognizer {
				byte[] utf8;
				OptionStringMatcher(byte[] utf8) {
					this.utf8 = utf8;
				}
				@Override
				public boolean match(ParsingContext context) {
					if(context.source.match(context.pos, this.utf8)) {
						context.consume(this.utf8.length);
					}
					return true;
				}
			}
			holder.matcher = new OptionStringMatcher(((StringMatcher) m).utf8);
			CountSpecLexer += 1;
			return;
		}
		//System.out.println("option " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
	}

	final void optimizeRepetition(ParsingRepetition holder) {
		ParsingExpression inner = holder.inner;
		if(this.is(O_Inline) && inner instanceof NonTerminal) {
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
//		if(inner instanceof ParsingByteRange) {
//			holder.matcher = new ZeroMoreByteRangeMatcher(((ParsingByteRange) inner).startByteChar, ((ParsingByteRange) inner).endByteChar);
//			CountSpecLexer += 1;
//			return;
//		}
		Recognizer m = inner.matcher;
		if(m instanceof ByteMapMatcher) {
			class ZeroMoreByteMapMatcher implements Recognizer {
				boolean bitMap[];
				ZeroMoreByteMapMatcher(boolean bitMap[]) {
					this.bitMap = bitMap;
				}
				@Override
				public boolean match(ParsingContext context) {
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
			holder.matcher = new ZeroMoreByteMapMatcher(((ByteMapMatcher) m).bitMap);
			CountSpecLexer += 1;
			return;
		}
		//System.out.println("Unoptimized repetition " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
	}

	final void optimizeConstructor(ParsingConstructor holder) {
		if(is(O_LazyObject)) {
			int prefetchIndex = 0;
			for(int i = 0; i < holder.size(); i++) {
				ParsingExpression sub = holder.get(i);
				if(sub.hasObjectOperation()) {
					break;
				}
				prefetchIndex = i + 1;
			}
			if(prefetchIndex > 0) {
				CountLazyObject += 1;
				holder.prefetchIndex = prefetchIndex;
			}
		}
	}

	final void optimizeSequence(ParsingSequence holder) {
		if(is(O_SpecLexer) && holder.size() == 2 && holder.get(0) instanceof ParsingNot && holder.get(1) instanceof ParsingAny) {
			ParsingExpression inner = ((ParsingNot)holder.get(0)).inner;
			if(this.is(O_Inline) && inner instanceof NonTerminal) {
				inner = resolveNonTerminal(inner);
			}
			if(inner instanceof ParsingByte) {
				holder.matcher = new ByteMapMatcher(((ParsingByte) inner).byteChar);
				CountSpecLexer += 1;
				return;
			}
			Recognizer m = inner.matcher;
			if(m instanceof ByteMapMatcher) {
				holder.matcher = new ByteMapMatcher(((ByteMapMatcher) m), false);
				CountSpecLexer += 1;
				return;
			}
			//System.out.println("not any " + holder + " " + inner.getClass().getSimpleName() + "/" + inner.matcher.getClass().getSimpleName());
		}
		if(is(O_SpecString)) {
			byte[] u = new byte[holder.size()];
			for(int i = 0; i < holder.size(); i++) {
				ParsingExpression inner = resolveNonTerminal(holder.get(i));				
				if(inner instanceof ParsingByte) {
					u[i] = (byte)((ParsingByte) inner).byteChar;
					continue;
				}
				return;
			}
			holder.matcher = new StringMatcher(u);
			CountSpecString += 1;
			return;
		}
	}
	
	final void optimizeChoice(ParsingChoice choice) {
		int[] c = new int[256];
		if(is(O_ByteMap) && checkCharacterChoice(choice, c)) {
//			System.out.println("Optimized1: " + choice);
			for(int ch = 0; ch < 256; ch++) {
//				if(c[ch] > 0) {
//					System.out.println("|1 " + GrammarFormatter.stringfyByte(ch) + ": true" );
//				}
			}
			choice.matcher = this.newByteChoiceMatcher(c);
			CountByteMap += 1;
			return;
		}
		for(int i = 0; i < c.length; i++) { 
			c[i] = 0; 
		}
		if(is(O_Prediction)) {
			if(is(O_Ext) && checkStringChoice(choice, c)) {
				ParsingExpression[] matchCase = new ParsingExpression[257];
				ParsingExpression empty = ParsingExpression.newEmpty();
				makeStringChoice(choice, matchCase, empty, empty);
				ParsingExpression f = new ParsingFailure(choice).intern();
	//			System.out.println("StringChoice: " + choice);
				for(int ch = 0; ch < matchCase.length; ch++) {
					if(matchCase[ch] == null) {
						matchCase[ch] = f;
					}
					else {
						matchCase[ch] = matchCase[ch].intern();
	//					System.out.println("|2 " + GrammarFormatter.stringfyByte(ch) + ":\t" + matchCase[ch]);
						if(matchCase[ch] instanceof ParsingChoice && !matchCase[ch].isOptimized()) {
							optimizeChoice((ParsingChoice)matchCase[ch]);
						}
					}
				}
				choice.matcher = new StringChoiceMatcher(matchCase);
			}
			else {
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
					else {
						if(matchCase[ch] instanceof ParsingChoice && !matchCase[ch].isOptimized()) {
							optimizeChoice((ParsingChoice)matchCase[ch]);
						}
					}
					if(matchCase[ch] != fails) {
						//System.out.println("|3 " + GrammarFormatter.stringfyByte(ch) + ":\t" + matchCase[ch]);
					}
				}
				choice.matcher = selfChoice ? new MappedSelfChoiceMatcher(choice, matchCase) : new MappedChoiceMatcher(choice, matchCase);
			}
			CountPrediction += 1;
		}			
	}
	
	final static boolean checkCharacterChoice(ParsingChoice choice, int[] c) {
		for(int i = 0; i < choice.size(); i++) {
			ParsingExpression e = resolveNonTerminal(choice.get(i));
			if(e instanceof ParsingByte) {
				c[((ParsingByte) e).byteChar]++;
				continue;
			}
			if(e.matcher instanceof ByteMapMatcher) {
				boolean[] bitMap = ((ByteMapMatcher)e.matcher).bitMap;
				for(int c1 = 0; c1 < c.length; c1++) {
					if(bitMap[c1]) {
						c[c1]++;
					}
				}
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
			if(e.matcher instanceof ByteMapMatcher) {
				boolean[] bitMap = ((ByteMapMatcher)e.matcher).bitMap;
				for(int c1 = 0; c1 < c.length; c1++) {
					if(bitMap[c1]) {
						c[c1]++;
					}
				}
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
		return ParsingExpression.newChoice(l).intern();
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

class NonZeroByteMatcher implements Recognizer {
	int byteChar;
	NonZeroByteMatcher(int byteChar) {
		this.byteChar = byteChar;
	}
	@Override
	public boolean match(ParsingContext context) {
		int c = context.source.fastByteAt(context.pos);
		if(c == byteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}

//class ZeroMoreByteRangeMatcher implements ParsingMatcher {
//	int startChar;
//	int endChar;
//	ZeroMoreByteRangeMatcher(int startChar, int endChar) {
//		this.startChar = startChar;
//		this.endChar = endChar;
//	}
//	
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		while(true) {
//			int c = context.source.byteAt(context.pos);
//			if(c < startChar || endChar < c) {
//				break;
//			}
//			context.pos += 1;
//		}
//		return true;
//	}
//}


class StringMatcher implements Recognizer {
	byte[] utf8;
	StringMatcher(byte[] utf8) {
		this.utf8 = utf8;
	}
	@Override
	public boolean match(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(utf8.length);
			return true;
		}
		context.failure(this);
		return false;
	}
}


class OptionalStringSequenceMatcher implements Recognizer {
	byte[] utf8;
	OptionalStringSequenceMatcher(byte[] utf8) {
		this.utf8 = utf8;
	}
	@Override
	public boolean match(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(utf8.length);
		}
		return true;
	}
}

class StringChoiceMatcher implements Recognizer {
	ParsingExpression[] matchCase;

	StringChoiceMatcher(ParsingExpression[] matchCase) {
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean match(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		long pos = context.getPosition();
		context.consume(1);
		if(this.matchCase[c].matcher.match(context)) {
			return true;
		}
		context.rollback(pos);
		return false;
	}
	
}

class MappedChoiceMatcher implements Recognizer {
	ParsingChoice choice;
	ParsingExpression[] matchCase;

	MappedChoiceMatcher(ParsingChoice choice, ParsingExpression[] matchCase) {
		this.choice = choice;
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean match(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		//System.out.println("pos="+context.pos + ", c=" + (char)c + " in " + choice);
		return this.matchCase[c].matcher.match(context);
	}
}

class MappedSelfChoiceMatcher implements Recognizer {
	ParsingChoice choice;
	ParsingExpression[] matchCase;

	MappedSelfChoiceMatcher(ParsingChoice choice, ParsingExpression[] matchCase) {
		this.choice = choice;
		this.matchCase = matchCase;
	}
	
	@Override
	public boolean match(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		//System.out.println("pos="+context.pos + ", c=" + (char)c + " in " + choice);
		if(this.matchCase[c] == choice) {
			return choice.match(context);
		}
		return this.matchCase[c].matcher.match(context);
	}
}

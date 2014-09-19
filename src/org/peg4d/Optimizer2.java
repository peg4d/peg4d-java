package org.peg4d;



class Optimizer2 {
	
	final static void optimize(ParsingExpression e) {
		for(int i = 0; i < e.size(); i++) {
			optimize(e.get(i));
		}
		if(e instanceof ParsingChoice) {
			optimizeChoice((ParsingChoice)e);
		}
	}
	
	final static void optimizeChoice(ParsingChoice choice) {
		int[] c = new int[256];
		if(checkCharacterChoice(choice, c)) {
			choice.matcher = new ByteChoiceMatcher(c);
		}
		for(int i = 0; i < c.length; i++) { 
			c[i] = 0; 
		}
		if(checkStringChoice(choice, c)) {
			ParsingExpression[] matchCase = new ParsingExpression[257];
			makeStringChoice(choice, matchCase, new ParsingEmpty());
			ParsingExpression f = new ParsingFailure(choice);
			for(int i = 0; i < matchCase.length; i++) {
				if(matchCase[i] == null) {
					matchCase[i] = f;
				}
			}
			choice.matcher = new StringChoiceMatcher(matchCase);
		}
		ParsingExpression[] matchCase = new ParsingExpression[257];
		matchCase[256] = new ParsingFailure(choice);  // EOF
		for(int ch = 0; ch < 256; ch++) {
			matchCase[ch] = selectChoice(choice, ch, matchCase[256]);
		}
		choice.matcher = new MappedChoiceMatcher(matchCase);
	}
	
	final static ParsingExpression resolveNonTerminal(ParsingExpression e) {
		while(e instanceof PNonTerminal) {
			PNonTerminal nterm = (PNonTerminal) e;
			e = nterm.calling;
		}
		return e;
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
			return false;
		}
		return true;
	}

	final static void makeStringChoice(ParsingChoice choice, ParsingExpression[] matchCase, ParsingExpression empty) {
		for(int i = 0; i < choice.size(); i++) {
			appendCaseChoice(choice.get(i), matchCase, empty, empty);
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
			makeStringChoice((ParsingChoice)e, matchCase, empty);
		}
		if(e instanceof ParsingSequence) {
			ParsingExpression e0 = e.get(0);
			ParsingExpression e1 = newLastSequence(e, empty);
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
			if(choice.get(i).acceptByte(ch, null) == ParsingExpression.Accept) {
				l.add(choice.get(i));
			}
		}
		if(l.size() == 0) {
			l.add(failed);
		}
		return ParsingExpression.newSequence(l);
	}
	
}

class ByteChoiceMatcher implements Matcher {
	boolean bitMap[];

	public ByteChoiceMatcher(int[] c) {
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
		if(this.matchCase[c].fastMatch(context)) {
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
		return this.matchCase[c].fastMatch(context);
	}
}

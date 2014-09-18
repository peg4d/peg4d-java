package org.peg4d;

import org.peg4d.ParsingContextMemo.ObjectMemo;

interface Matcher {
	boolean simpleMatch(ParsingContext context);
}

public abstract class PExpression implements Matcher {
	public final static int CyclicRule       = 1;
	public final static int HasNonTerminal    = 1 << 1;
	public final static int HasString         = 1 << 2;
	public final static int HasCharacter      = 1 << 3;
	public final static int HasAny            = 1 << 4;
	public final static int HasRepetition     = 1 << 5;
	public final static int HasOptional       = 1 << 6;
	public final static int HasChoice         = 1 << 7;
	public final static int HasAnd            = 1 << 8;
	public final static int HasNot            = 1 << 9;
	
	public final static int HasConstructor    = 1 << 10;
	public final static int HasConnector      = 1 << 11;
	public final static int HasTagging        = 1 << 12;
	public final static int HasMessage        = 1 << 13;
	public final static int HasContext        = 1 << 14;
	public final static int HasReserved       = 1 << 15;
	public final static int hasReserved2       = 1 << 16;
	public final static int Mask = HasNonTerminal | HasString | HasCharacter | HasAny
	                             | HasRepetition | HasOptional | HasChoice | HasAnd | HasNot
	                             | HasConstructor | HasConnector | HasTagging | HasMessage 
	                             | HasReserved | hasReserved2 | HasContext;
	public final static int HasLazyNonTerminal = Mask;

	public final static int LeftObjectOperation    = 1 << 17;
	public final static int PossibleDifferentRight = 1 << 18;
	
	public final static int NoMemo            = 1 << 20;

	public final static int HasSyntaxError    = 1 << 26;
	public final static int HasTypeError      = 1 << 27;

	
	int        flag       = 0;
	int        uniqueId   = 0;
	ParsingObject po      = null;
	PExpression flowNext  = null;
	Matcher matcher;
		
	protected PExpression(int flag) {
		this.flag = flag;
		this.matcher = this;
	}

	abstract PExpression dup();
	protected abstract void visit(ParsingExpressionVisitor visitor);

	public final boolean fastMatch(ParsingContext c) {
		return this.matcher.simpleMatch(c);
	}
	
//	public final boolean simpleMatch(ParsingContext context) {
//		int pos = (int)context.getPosition();
//		boolean b = this.fastMatch(context);
//		assert(context.isFailure() == !b);
//		System.out.println("["+pos+"] return: " + b + " by " + this);
//		return b;
//	}
	//public abstract    boolean simpleMatch(ParsingContext context);

	public final static short Reject        = 0;
	public final static short Accept        = 1;
	public final static short WeakAccept    = 1;
	public final static short CheckNextFlow = 2;
	
	abstract short acceptByte(int ch, PExpression stopped);
	
	protected final short checkNextFlow(short r, int ch, PExpression stopped) {
		if(r == CheckNextFlow) {
			if(this.flowNext == null) {
				return Reject;
			}
			if(stopped != this.flowNext ) {
				return this.flowNext.acceptByte(ch, stopped);
			}
			return CheckNextFlow;
		}
		return r;
	}

	public PExpression getExpression() {
		return this;
	}

	public final boolean is(int uflag) {
		return ((this.flag & uflag) == uflag);
	}

	public void set(int uflag) {
		this.flag = this.flag | uflag;
	}

	protected void derived(PExpression e) {
		this.flag |= (e.flag & PExpression.Mask);
	}
	
	public final boolean isUnique() {
		return this.uniqueId > 0;
	}
	
	public int size() {
		return 0;
	}
	public PExpression get(int index) {
		return this;  // to avoid NullPointerException
	}

	private final static GrammarFormatter DefaultFormatter = new GrammarFormatter();
	
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		DefaultFormatter.format(this, sb);
		return sb.toString();
	}

	public final String format(String name, GrammarFormatter fmt) {
		StringBuilder sb = new StringBuilder();
		fmt.formatRule(name, this, sb);
		return sb.toString();
	}
	public final String format(String name) {
		return this.format(name, new GrammarFormatter());
	}
	
	final void report(ReportLevel level, String msg) {
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
		}
		else {
			System.out.println("" + level.toString() + ": " + msg);
		}
	}

	public final boolean hasObjectOperation() {
		return this.is(PExpression.HasConstructor) 
				|| this.is(PExpression.HasConnector) 
				|| this.is(PExpression.HasTagging) 
				|| this.is(PExpression.HasMessage);
	}
	
	public static PExpression makeFlow(PExpression e, PExpression tail) {
		e.flowNext = tail;
		if(e instanceof PChoice) {
			for(int i = 0; i < e.size(); i++) {
				makeFlow(e.get(i), tail);
			}
			return e;
		}
		if(e instanceof PSequence || e instanceof PConstructor) {
			for(int i = e.size() - 1; i >=0; i--) {
				tail = makeFlow(e.get(i), tail);
			}
			return e;
		}
		if(e instanceof ParsingOperation) {
			tail = makeFlow(((ParsingOperation) e).inner, tail);
		}
		if(e instanceof ParsingUnary) {
			tail = makeFlow(((ParsingUnary) e).inner, tail);
		}
		return e;
	}

	private static boolean checkRecursion(String uName, UList<String> stack) {
		for(int i = 0; i < stack.size() - 1; i++) {
			if(uName.equals(stack.ArrayValues[i])) {
				return true;
			}
		}
		return false;
	}

	static int checkLeftRecursion(PExpression e, String uName, UList<String> stack, int consume) {
		//System.out.println("checking: " + uName + " " + e);
		if(e == null) {
			return consume;
		}
		if(e instanceof ParsingByte || e instanceof PCharacter) {
			consume += 1;
		}
		if(e instanceof PString) {
			consume += ((PString) e).utf8.length;
		}
		if(e instanceof PNonTerminal) {
			String n = ((PNonTerminal) e).getUniqueName();
			((PNonTerminal) e).checkReference();
			if(n.equals(uName) && consume == 0 && !e.is(HasSyntaxError)) {
				e.set(HasSyntaxError);
				e.report(ReportLevel.error, "left recursion: " + ((PNonTerminal) e).symbol);
			}
			if(!checkRecursion(n, stack)) {
				int pos = stack.size();
				stack.add(n);
				consume = checkLeftRecursion(((PNonTerminal) e).resolvedExpression, uName, stack, consume);
				stack.clear(pos);
			}
		}
		if(e instanceof PChoice) {
			int length = consume;
			consume = Integer.MAX_VALUE;
			for(int i = 0; i < e.size(); i++) {
				int nc = checkLeftRecursion(e.get(i), uName, stack, length);
				if(nc < consume) {
					consume = nc;
				}
			}
			return consume;
		}
		if(e instanceof ParsingUnary) {
			if(e instanceof POptional || e instanceof PRepetition) {
				checkLeftRecursion(((ParsingUnary) e).inner, uName, stack, consume); // skip count
				return checkLeftRecursion(e.flowNext, uName, stack, consume);
			}
			else {
				return checkLeftRecursion(((ParsingUnary) e).inner, uName, stack, consume);
			}
		}
		if(e instanceof ParsingOperation) {
			return checkLeftRecursion(((ParsingOperation) e).inner, uName, stack, consume);
		}
		if(e instanceof PList) {
			if(e.size() > 0) {
				return checkLeftRecursion(e.get(0), uName, stack, consume);
			}
		}
		return checkLeftRecursion(e.flowNext, uName, stack, consume);
	}

	static ParsingType typeCheck(PExpression e, UList<String> stack, ParsingType leftType, PExpression stopped) {
		if(e == null || e == stopped) {
			return leftType;
		}
		if(e instanceof PConnector) {
			ParsingType rightType = typeCheck(((PConnector) e).inner, stack, new ParsingType(), e.flowNext);
			if(!rightType.isObjectType() && !e.is(HasTypeError)) {
				e.set(HasTypeError);
				e.report(ReportLevel.warning, "nothing is connected: in " + e);
			}
			leftType.set(((PConnector) e).index, rightType, (PConnector)e);
			return typeCheck(e.flowNext, stack, leftType, stopped);
		}
		if(e instanceof PConstructor) {
			boolean LeftJoin = ((PConstructor) e).leftJoin;
			if(LeftJoin) {
				if(!leftType.isObjectType() && !e.is(HasTypeError)) {
					e.set(HasTypeError);
					e.report(ReportLevel.warning, "type error: unspecific left in " + e);
				}
			}
			else {
				if(leftType.isObjectType() && !e.is(HasTypeError)) {
					e.set(HasTypeError);
					e.report(ReportLevel.warning, "type error: object transition of " + leftType + " before " + e);
				}
			}
			if(((PConstructor) e).type == null) {
				ParsingType t = leftType.isEmpty() ? leftType : new ParsingType();
				if(LeftJoin) {
					t.set(0, leftType);
				}
				t.setConstructor((PConstructor)e);
				((PConstructor) e).type = typeCheck(e.get(0), stack, t, e.flowNext);
				
			}
			if(LeftJoin) {
				leftType.addUnionType(((PConstructor) e).type.dup());
			}
			else {
				leftType = ((PConstructor) e).type.dup();
			}
		}
		if(e instanceof PTagging) {
			leftType.addTagging(((PTagging) e).tag);
		}
		if(e instanceof PNonTerminal) {
			ParsingRule r = ((PNonTerminal) e).getRule();
			if(r.type == null) {
				String n = ((PNonTerminal) e).getUniqueName();
				if(!checkRecursion(n, stack)) {
					int pos = stack.size();
					stack.add(n);
					ParsingType t = new ParsingType();
					r.type = t;
					r.type = typeCheck(((PNonTerminal) e).resolvedExpression, stack, t, null);
					stack.clear(pos);
				}
				if(r.type == null) {
					e.report(ReportLevel.warning, "uninferred NonTerminal: " + n);				
				}
			}
			if(r.type != null) {
				if(r.type.isObjectType()) {
					leftType = r.type.dup();
				}
			}
		}
		if(e instanceof PChoice) {
			if(e.size() > 1) {
				ParsingType rightType = typeCheck(e.get(0), stack, leftType.dup(), e.flowNext);
				if(leftType.hasTransition(rightType)) {
					for(int i = 1; i < e.size(); i++) {
						ParsingType unionType = typeCheck(e.get(i), stack, leftType.dup(), e.flowNext);
						rightType.addUnionType(unionType);
					}					
				}
				else {
					for(int i = 1; i < e.size(); i++) {
						ParsingType lleftType = rightType;
						lleftType.enableUnionTagging();
						rightType = typeCheck(e.get(i), stack, lleftType, e.flowNext);
						if(lleftType.hasTransition(rightType)) {
							if(!e.get(i).is(HasTypeError)) {
								e.get(i).set(HasTypeError);
								e.report(ReportLevel.warning, "type error: mixed type: " + leftType + "/" + lleftType + "/" + rightType + " at " + e.get(i) + " in " + e);
							}
						}
					}
					rightType.disableUnionTagging();
					//System.out.println("CHOICE: " + e + "\n\t" + rightType);
				}
				leftType = rightType;
			}
		}
		if(e instanceof ParsingUnary) {
			leftType = typeCheck(((ParsingUnary) e).inner, stack, leftType, e.flowNext);
		}
		if(e instanceof ParsingOperation) {
			leftType = typeCheck(((ParsingOperation) e).inner, stack, leftType, e.flowNext);
		}
		if(e instanceof PSequence) {
			if(e.size() > 0) {
				leftType = typeCheck(e.get(0), stack, leftType, e.flowNext);
			}
		}
		return typeCheck(e.flowNext, stack, leftType, stopped);
	}
	
	// factory
	
	private static int unique = 0;
	
	private static boolean Conservative = true;
	private static boolean StringSpecialization = true;
	private static boolean CharacterChoice      = true;
	
	public final static int newExpressionId() {
		unique++;
		return unique;
	}
	
	public final static ParsingByte newByteChar(int ch) {
		return new ParsingByte(ch & 0xff);
	}
	
	public final static PExpression newByteChar(int ch, String token) {
		ParsingByte e = newByteChar(ch);
		e.errorToken = token;
		return e;
	}
	
	public final static PExpression newAny(String text) {
		return new ParsingAny();
	}
	

	public final static PExpression newSequence(UList<PExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		return new PSequence(0, l);
	}
	
	public final static void addSequence(UList<PExpression> l, PExpression e) {
		if(e instanceof PSequence) {
			for(int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
		}
		else {
			if(l.size() > 0 && e instanceof PNot) {
				PExpression pe = l.ArrayValues[l.size()-1];
				if(pe instanceof PNot) {
					((PNot) pe).inner = mergeChoice(((PNot) pe).inner, ((PNot) e).inner);
					Main.printVerbose("merging", pe);
					return;
				}
			}
			l.add(e);
		}
	}

	public final static PExpression mergeChoice(PExpression e, PExpression e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<PExpression> l = new UList<PExpression>(new PExpression[e.size()+e2.size()]);
		addChoice(l, e);
		addChoice(l, e2);
		return newChoice(l);
	}
	
	public static final PExpression newString(String text) {
		byte[] utf8 = ParsingCharset.toUtf8(text);
		if(Conservative) {
			return new PString(0, text, utf8);	
		}
		if(utf8.length == 1) {
			return newByteChar(utf8[0]);
		}
		return newByteSequence(utf8, text);
	}

	public final static PExpression newByteSequence(byte[] utf8, String token) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[utf8.length]);
		for(int i = 0; i < utf8.length; i++) {
			l.add(newByteChar(utf8[i], token));
		}
		return newSequence(l);
	}
	
	public final static PExpression newCharacter(ParsingCharset u) {
		if(u instanceof UnicodeRange) {
			return newUnicodeRange(((UnicodeRange) u).beginChar, ((UnicodeRange) u).endChar, u.key());
		}
		ByteCharset bc = (ByteCharset)u;
		PExpression e = null;
		int c = bc.size();
		if(c > 1) {
			e = new PCharacter(0, u);
		}
		else if(c == 1) {
			for(c = 0; c < 256; c++) {
				if(bc.bitMap[c]) {
					break;
				}
			}
			e = newByteChar(c);
		}
		if(bc.unicodeRangeList != null) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[2]);
			if(e != null) {
				l.add(e);
			}
			for(int i = 0; i < bc.unicodeRangeList.size(); i++) {
				UnicodeRange ur = bc.unicodeRangeList.ArrayValues[i];
				addChoice(l, newUnicodeRange(ur.beginChar, ur.endChar, ur.key()));
			}
			return newChoice(l);
		}
		return e;
	}

	public final static PExpression newUnicodeRange(int c, int c2, String token) {
		byte[] b = ParsingCharset.toUtf8(String.valueOf((char)c));
		byte[] b2 = ParsingCharset.toUtf8(String.valueOf((char)c2));
		if(equalsBase(b, b2)) {
			return newUnicodeRange(b, b2, token);
		}
		UList<PExpression> l = new UList<PExpression>(new PExpression[b.length]);
		b2 = b;
		for(int pc = c + 1; pc <= c2; pc++) {
			byte[] b3 = ParsingCharset.toUtf8(String.valueOf((char)pc));
			if(equalsBase(b, b3)) {
				b2 = b3;
				continue;
			}
			l.add(newUnicodeRange(b, b2, token));
			b = b3;
			b2 = b3;
		}
		b2 = ParsingCharset.toUtf8(String.valueOf((char)c2));
		l.add(newUnicodeRange(b, b2, token));
		return newChoice(l);
	}
	
	private final static boolean equalsBase(byte[] b, byte[] b2) {
		if(b.length == b2.length) {
			switch(b.length) {
			case 3: return b[0] == b2[0] && b[1] == b2[1];
			case 4: return b[0] == b2[0] && b[1] == b2[1] && b[2] == b2[2];
			}
			return b[0] == b2[0];
		}
		return false;
	}

	private final static PExpression newUnicodeRange(byte[] b, byte[] b2, String token) {
		if(b[b.length-1] == b2[b.length-1]) {
			return newByteSequence(b, token);
		}
		else {
			UList<PExpression> l = new UList<PExpression>(new PExpression[b.length]);
			for(int i = 0; i < b.length-1; i++) {
				l.add(newByteChar(b[i], token));
			}
			l.add(newByteRange(b[b.length-1] & 0xff, b2[b2.length-1] & 0xff, token));
			return newSequence(l);
		}
	}

	public final static PExpression newByteRange(int c, int c2, String token) {
		return new PCharacter(0, new ByteCharset(c, c2));
	}
	
	
	public final static PExpression newOptional(PExpression p) {
//		if(StringSpecialization) {
//			if(p instanceof PByteChar) {
//				return new POptionalByteChar(0, (PByteChar)p);
//			}
//			if(p instanceof PCharacter) {
//				return new POptionalCharacter(0, (PCharacter)p);
//			}
//			if(p instanceof PString) {
//				return new POptionalString(0, (PString)p);
//			}
//		}
		return new POptional(p);
	}
	
	public final static PExpression newMatch(PExpression p) {
		if(!p.hasObjectOperation() && !p.is(PExpression.HasNonTerminal)) {
			return p;
		}
		return new ParsingMatch(p);
	}
		
	public final static PExpression newRepetition(PExpression p) {
//		if(p instanceof PCharacter) {
//			return new PZeroMoreCharacter(0, (PCharacter)p);
//		}
		return new PRepetition(0, p);
	}

	public final static PExpression newAnd(PExpression p) {
		return new PAnd(0, p);
	}
	
	public final static PExpression newNot(PExpression p) {
//		if(StringSpecialization) {
//			if(p instanceof PByteChar) {
//				return new PNotByteChar(0, (PByteChar)p);
//			}
//			if(p instanceof PString) {
//				return new PNotString(0, (PString)p);
//			}
//			if(p instanceof PCharacter) {
//				return new PNotCharacter(0, (PCharacter)p);
//			}
//		}
//		if(p instanceof ParsingOperation) {
//			p = ((ParsingOperation)p).inner;
//		}
		return new PNot(0, p);
	}
		
	public final static PExpression newChoice(UList<PExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		return new PChoice(0, l);
//		if(isUnique(l)) {
//			String key = prefixChoice;
//			boolean isAllText = true;
//			for(int i = 0; i < l.size(); i++) {
//				PExpression p = l.ArrayValues[i];
//				key += pkey(p);
//				if(!(p instanceof PString) && !(p instanceof PString)) {
//					isAllText = false;
//				}
//			}
//			PExpression e = getUnique(key);
//			if(e == null) {
//				e = putUnique(key, newChoiceImpl(l, isAllText));
//			}
//			return e;
//		}
//		return newChoiceImpl(l, false);
	}

	
	public final static void addChoice(UList<PExpression> l, PExpression e) {
		if(e instanceof PChoice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if(CharacterChoice && l.size() > 0 && (e instanceof ParsingByte || e instanceof PCharacter)) {
			PExpression pe = l.ArrayValues[l.size()-1];
			if(pe instanceof ParsingByte || pe instanceof PCharacter) {
				ParsingCharset b = (e instanceof ParsingByte)
					? new ByteCharset(((ParsingByte) e).byteChar, ((ParsingByte) e).byteChar)
					: ((PCharacter)e).charset.dup();
				b = mergeUpdate(b, pe);
				l.ArrayValues[l.size()-1] = newCharacter(b);
				return;
			}
		}
		l.add(e);
	}
	
	private static final ParsingCharset mergeUpdate(ParsingCharset cu, PExpression e) {
		if(e instanceof ParsingByte) {
			return cu.appendByte(((ParsingByte) e).byteChar, ((ParsingByte) e).byteChar);
		}
		ParsingCharset u = ((PCharacter) e).charset;
		if(u instanceof UnicodeRange) {
			UnicodeRange r = (UnicodeRange)u;
			return cu.appendChar(r.beginChar, r.endChar);
		}
		ByteCharset ub = (ByteCharset)u;
		for(int i = 0; i < ub.bitMap.length; i++) {
			if(ub.bitMap[i]) {
				cu = cu.appendByte(i, i);
			}
		}
		if(ub.unicodeRangeList != null) {
			for(int i = 0; i < ub.unicodeRangeList.size(); i++) {
				UnicodeRange r = ub.unicodeRangeList.ArrayValues[i];
				cu = cu.appendChar(r.beginChar, r.endChar);
			}
		}
		return cu;
	}
	
	public final static PExpression newConstructor(PExpression p) {
		PExpression e = new PConstructor(0, false, toSequenceList(p));
		return e;
	}

	public final static PExpression newJoinConstructor(PExpression p) {
		PExpression e = new PConstructor(0, true, toSequenceList(p));
		return e;
	}
	
	public final static UList<PExpression> toSequenceList(PExpression e) {
		if(e instanceof PSequence) {
			return ((PSequence) e).list;
		}
		UList<PExpression> l = new UList<PExpression>(new PExpression[1]);
		l.add(e);
		return l;
	}
		
	public final static PExpression newConnector(PExpression p, int index) {
		return new PConnector(0, p, index);
	}

	public final static PExpression newTagging(ParsingTag tag) {
		return new PTagging(0, tag);
	}

	public final static PExpression newValue(String msg) {
		return new PMessage(0, msg);
	}
	
	public final static PExpression newDebug(PExpression e) {
		return new ParsingDebug(e);
	}

	public final static PExpression newFail(String message) {
		return new ParsingFail(0, message);
	}

	private static PExpression catchExpression = null;

	public final static PExpression newCatch() {
		if(catchExpression == null) {
			catchExpression = new ParsingCatch(0);
			catchExpression.uniqueId = PExpression.newExpressionId();
		}
		return catchExpression;
	}
	
	public final static PExpression newFlag(String flagName) {
		return new ParsingIfFlag(0, flagName);
	}

	public final static PExpression newEnableFlag(String flagName, PExpression e) {
		return new ParsingWithFlag(flagName, e);
	}

	public final static PExpression newDisableFlag(String flagName, PExpression e) {
		return new ParsingWithoutFlag(flagName, e);
	}

	private static PExpression indentExpression = null;

	public final static PExpression newIndent(PExpression e) {
		if(e == null) {
			if(indentExpression == null) {
				indentExpression = new ParsingIndent(0);
				indentExpression.uniqueId = PExpression.newExpressionId();
			}
			return indentExpression;
		}
		return new ParsingStackIndent(e);
	}
}

class PNonTerminal extends PExpression {
	Grammar base;
	String symbol;
	PExpression    resolvedExpression = null;
	PNonTerminal(Grammar base, int flag, String ruleName) {
		super(flag | PExpression.HasNonTerminal | PExpression.NoMemo);
		this.base = base;
		this.symbol = ruleName;
	}
	@Override
	PExpression dup() {
		return new PNonTerminal(base, flag, symbol);
	}
	String getUniqueName() {
		return this.base.uniqueRuleName(this.symbol);
	}
	final ParsingRule getRule() {
		return this.base.getRule(this.symbol);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitNonTerminal(this);
	}
	@Override short acceptByte(int ch, PExpression stopped) {
		if(this.resolvedExpression != null) {
			short r = this.resolvedExpression.acceptByte(ch, null);
			return this.checkNextFlow(r, ch, stopped);
		}
		return WeakAccept;
	}

	void checkReference() {
		if(this.resolvedExpression == null) {
			this.resolvedExpression = this.base.getExpression(this.symbol);
			//System.out.println("NonTerminal: " + this + " ref: " + this.resolvedExpression);
			if(this.resolvedExpression == null) {
				this.report(ReportLevel.error, "undefined rule: " + this.symbol);
				this.resolvedExpression = new ParsingIfFlag(0, this.symbol);
				ParsingRule rule = new ParsingRule(this.base, this.symbol, null, this.resolvedExpression);
				this.base.setRule(this.symbol, rule);
			}
		}
	}

	final PExpression getNext() {
		if(this.resolvedExpression == null) {
			return this.base.getExpression(this.symbol);
		}
		return this.resolvedExpression;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return this.resolvedExpression.fastMatch(context);
	}
}

abstract class ParsingTerminal extends PExpression {
	ParsingTerminal (int flag) {
		super(flag);
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final PExpression get(int index) {
		return this;  // just avoid NullPointerException
	}
}

class ParsingByte extends ParsingTerminal {
	int byteChar;
	String errorToken = null;
	ParsingByte(int ch) {
		super(0);
		this.byteChar = ch;
	}
	@Override PExpression dup() { 
		ParsingByte n = new ParsingByte(byteChar);
		n.errorToken = errorToken;
		return n;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitByteChar(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.byteAt(context.pos) == this.byteChar) {
			context.consume(1);
			return true;
		}
		context.opFailure();
		return false;
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return (byteChar == ch) ? Accept : Reject;
	}
}


class PString extends ParsingTerminal {
	String text;
	byte[] utf8;
	PString(int flag, String text, byte[] utf8) {
		super(PExpression.HasString | PExpression.NoMemo | flag);
		this.text = text;
		this.utf8 = utf8;
	}
	PString(int flag, String text) {
		this(flag, text, ParsingCharset.toUtf8(text));
	}
	PString(int flag, int ch) {
		super(PExpression.HasString | PExpression.NoMemo | flag);
		utf8 = new byte[1];
		utf8[0] = (byte)ch;
		if(ch >= ' ' && ch < 127) {
			this.text = String.valueOf((char)ch);
		}
		else {
			this.text = String.format("0x%x", ch);
		}
	}
	@Override
	PExpression dup() { 
		return new PString(flag, text, utf8); 
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitString(this);
	}
	@Override short acceptByte(int ch, PExpression stopped) {
		if(this.utf8.length == 0) {
			return this.checkNextFlow(CheckNextFlow, ch, stopped);
		}
		return ((this.utf8[0] & 0xff) == ch) ? Accept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.match(context.pos, this.utf8)) {
			context.consume(this.utf8.length);
			return true;
		}
		else {
			context.opFailure();
			return false;
		}
	}
}


class ParsingAny extends ParsingTerminal {
	ParsingAny() {
		super(PExpression.HasAny | PExpression.NoMemo);
	}
	@Override PExpression dup() { return new ParsingAny(); }
	@Override 
	short acceptByte(int ch, PExpression stopped) {
		return Accept;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitAny(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.charAt(context.pos) != -1) {
			int len = context.source.charLength(context.pos);
			context.consume(len);
			return true;
		}
		context.opFailure();
		return false;
	}
}

class PCharacter extends ParsingTerminal {
	ParsingCharset charset;
	PCharacter(int flag, ParsingCharset charset) {
		super(PExpression.HasCharacter | PExpression.NoMemo | flag);
		this.charset = charset;
	}
	@Override 
	PExpression dup() { return this; }
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitCharacter(this);
	}
	@Override 
	short acceptByte(int ch, PExpression stopped) {
		return this.charset.hasByte(ch) ? Accept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int consume = this.charset.consume(context.source, context.pos);
		if(consume > 0) {
			context.consume(consume);
			return true;
		}
		context.opFailure();
		return false;
	}
}

abstract class ParsingUnary extends PExpression {
	PExpression inner;
	ParsingUnary(int flag, PExpression e) {
		super(flag);
		this.inner = e;
		this.derived(e);
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final PExpression get(int index) {
		return this.inner;
	}
}

class POptional extends ParsingUnary {
	POptional(PExpression e) {
		super(PExpression.HasOptional | PExpression.NoMemo, e);
	}
	@Override PExpression dup() { 
		return new POptional(inner); 
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitOptional(this);
	}
	@Override 
	short acceptByte(int ch, PExpression stopped) {
		short r = this.inner.acceptByte(ch, this.flowNext);
		if(r == Accept) {
			return r;
		}
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
		if(!this.inner.fastMatch(context)) {
			context.left = left;
			context.forgetFailure(f);
		}
		return true;
	}
}

//class POptionalString extends POptional {
//	byte[] utf8;
//	POptionalString(int flag, PString e) {
//		super(e);
//		this.utf8 = e.utf8;
//	}
//	@Override PExpression dup() { 
//		return new POptionalString(flag, (PString)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		if(context.source.match(context.pos, this.utf8)) {
//			context.consume(this.utf8.length);
//		}
//	}
//}
//
//class POptionalByteChar extends POptional {
//	int byteChar;
//	POptionalByteChar(int flag, ParsingByte e) {
//		super(e);
//		this.byteChar = e.byteChar;
//	}
//	@Override PExpression dup() { 
//		return new POptionalByteChar(flag, (ParsingByte)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		context.opMatchOptionalByteChar(this.byteChar);
//	}
//}
//
//class POptionalCharacter extends POptional {
//	ParsingCharset charset;
//	POptionalCharacter(int flag, PCharacter e) {
//		super(e);
//		this.charset = e.charset;
//	}
//	@Override PExpression dup() { 
//		return new POptionalCharacter(flag, (PCharacter)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		context.opMatchOptionalCharset(this.charset);
//	}
//}

class PRepetition extends ParsingUnary {
//	public int atleast = 0; 
	PRepetition(int flag, PExpression e/*, int atLeast*/) {
		super(flag | PExpression.HasRepetition, e);
//		this.atleast = atLeast;
	}
	@Override PExpression dup() { 
		return new PRepetition(flag, inner/*, atleast*/); 
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitRepetition(this);
	}
	@Override short acceptByte(int ch, PExpression stopped) {
		short r = this.inner.acceptByte(ch, this.flowNext);
		if(r == Accept) {
			return r;
		}
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long ppos = -1;
		long pos = context.getPosition();
		long f = context.rememberFailure();
		while(ppos < pos) {
			ParsingObject left = context.left;
			if(!this.inner.fastMatch(context)) {
				context.left = left;
				break;
			}
			ppos = pos;
			pos = context.getPosition();
		}
		context.forgetFailure(f);
		return true;
	}
}

//class PZeroMoreCharacter extends PRepetition {
//	ParsingCharset charset;
//	PZeroMoreCharacter(int flag, PCharacter e) {
//		super(flag, e);
//		this.charset = e.charset;
//	}
//	@Override PExpression dup() { 
//		return new PZeroMoreCharacter(flag, (PCharacter)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		long pos = context.getPosition();
//		int consumed = 0;
//		do {
//			consumed = this.charset.consume(context.source, pos);
//			pos += consumed;
//		}
//		while(consumed > 0);
//		context.setPosition(pos);
//	}
//}

class PAnd extends ParsingUnary {
	PAnd(int flag, PExpression e) {
		super(flag | PExpression.HasAnd, e);
	}
	@Override PExpression dup() { 
		return new PAnd(flag, inner); 
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitAnd(this);
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		short r = this.inner.acceptByte(ch, this.flowNext);
		if(r == Reject) {
			return Reject;
		}
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		this.inner.fastMatch(context);
		context.rollback(pos);
		return !context.isFailure();
	}
}

class PNot extends ParsingUnary {
	PNot(int flag, PExpression e) {
		super(PExpression.HasNot | flag, e);
	}
	@Override PExpression dup() { 
		return new PNot(flag, inner); 
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitNot(this);
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		short r = this.inner.acceptByte(ch, this.flowNext);
		if(r == Accept) {
			return Reject;
		}
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingObject left = context.left;
		if(this.inner.fastMatch(context)) {
			context.rollback(pos);
			context.opFailure(this);
			return false;
		}
		else {
			context.rollback(pos);
			context.forgetFailure(f);
			context.left = left;
			return true;
		}
	}
}

//class PNotString extends PNot {
//	byte[] utf8;
//	PNotString(int flag, PString e) {
//		super(flag | PExpression.NoMemo, e);
//		this.utf8 = e.utf8;
//	}
//	@Override PExpression dup() { 
//		return new PNotString(flag, (PString)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		context.opMatchTextNot(utf8);
//	}
//}
//
//class PNotByteChar extends PNot {
//	int byteChar;
//	PNotByteChar(int flag, ParsingByte e) {
//		super(flag, e);
//		this.byteChar = e.byteChar;
//	}
//	@Override PExpression dup() { 
//		return new PNotByteChar(flag, (ParsingByte)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		context.opMatchByteCharNot(this.byteChar);
//	}
//}	
//
//class PNotCharacter extends PNot {
//	ParsingCharset charset;
//	PNotCharacter(int flag, PCharacter e) {
//		super(flag | PExpression.NoMemo, e);
//		this.charset = e.charset;
//	}
//	@Override PExpression dup() { 
//		return new PNotCharacter(flag, (PCharacter)inner); 
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		context.opMatchCharsetNot(this.charset);
//	}
//}

abstract class PList extends PExpression {
	UList<PExpression> list;
	int length = 0;
	PList(int flag, UList<PExpression> list) {
		super(flag);
		this.list = list;
	}
	@Override
	public final int size() {
		return this.list.size();
	}
	@Override
	public final PExpression get(int index) {
		return this.list.ArrayValues[index];
	}
	public final void set(int index, PExpression e) {
		this.list.ArrayValues[index] = e;
	}

	@Override
	short acceptByte(int ch, PExpression stopped) {
		short r = CheckNextFlow;
		if(this.size() > 0) {
			r = this.get(0).acceptByte(ch, null);
		}
		return this.checkNextFlow(r, ch, stopped);
	}

	public final PExpression trim() {
		int size = this.size();
		boolean hasNull = true;
		while(hasNull) {
			hasNull = false;
			for(int i = 0; i < size-1; i++) {
				if(this.get(i) == null && this.get(i+1) != null) {
					this.swap(i,i+1);
					hasNull = true;
				}
			}
		}
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i) == null) {
				size = i;
				break;
			}
		}
		if(size == 0) {
			return null;
		}
		if(size == 1) {
			return this.get(0);
		}
		this.list.clear(size);
		return this;
	}
	
	public final void swap(int i, int j) {
		PExpression e = this.list.ArrayValues[i];
		this.list.ArrayValues[i] = this.list.ArrayValues[j];
		this.list.ArrayValues[j] = e;
	}
}

class PSequence extends PList {
	PSequence(int flag, UList<PExpression> l) {
		super(flag, l);
	}
	@Override
	PExpression dup() {
		return new PSequence(flag, list);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitSequence(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			//System.out.println("Attmpt Sequence " + this.get(i) + " isFailure: " + context.isFailure());
			if(!(this.get(i).fastMatch(context))) {
				context.abortLinkLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}

}

class PChoice extends PList {
	PChoice(int flag, UList<PExpression> list) {
		super(flag | PExpression.HasChoice, list);
	}
	@Override
	PExpression dup() {
		return new PChoice(flag, list);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitChoice(this);
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		boolean checkNext = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch, stopped);
			if(r == Accept) {
				return r;
			}
			if(r == CheckNextFlow) {
				checkNext = true;
			}
		}
		return checkNext ? CheckNextFlow : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
		for(int i = 0; i < this.size(); i++) {
			context.left = left;
			if(this.get(i).fastMatch(context)) {
				context.forgetFailure(f);
				return true;
			}
		}
		assert(context.isFailure());
		return false;
	}

}

class PMappedChoice extends PChoice {
	PExpression[] caseOf = null;
	PMappedChoice(int flag, UList<PExpression> list) {
		super(flag, list);
	}
	@Override
	PExpression dup() {
		return new PMappedChoice(flag, list);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int ch = context.getByteChar();
		if(this.caseOf == null) {
			tryPrediction();
		}
		return caseOf[ch].fastMatch(context);
	}
	void tryPrediction() {
		if(this.caseOf == null) {
			this.caseOf = new PExpression[ParsingCharset.MAX];
			PExpression failed = new PAlwaysFailure(this);
			for(int ch = 0; ch < ParsingCharset.MAX; ch++) {
				this.caseOf[ch] = selectC1(ch, failed);
			}
//			this.base.PredictionOptimization += 1;
		}
	}
	private PExpression selectC1(int ch, PExpression failed) {
		PExpression e = null;
		UList<PExpression> l = null; // new UList<Peg>(new Peg[2]);
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).acceptByte(ch, null) == Accept) {
				if(e == null) {
					e = this.get(i);
				}
				else {
					if(l == null) {
						l = new UList<PExpression>(new PExpression[2]);
						l.add(e);
					}
					l.add(get(i));
				}
			}
		}
		if(l != null) {
			e = new PChoice(0, l);
//			e.base.UnpredicatedChoiceL1 += 1;
		}
		else {
			if(e == null) {
				e = failed;
			}
//			e.base.PredicatedChoiceL1 +=1;
		}
		return e;
	}
}

//class PegWordChoice extends PChoice {
//	ParsingCharset charset = null;
//	UList<byte[]> wordList = null;
//	PegWordChoice(Grammar base, int flag, UList<PExpression> list) {
//		super(base, flag | PExpression.HasChoice, list);
//		this.wordList = new UList<byte[]>(new byte[list.size()][]);
//		for(int i = 0; i < list.size(); i++) {
//			PExpression se = list.ArrayValues[i];
//			if(se instanceof PString1) {
//				if(charset == null) {
//					charset = new ParsingCharset("");
//				}
//				charset.append(((PString1)se).symbol1);
//			}
//			if(se instanceof PCharacter) {
//				if(charset == null) {
//					charset = new ParsingCharset("");
//				}
//				charset.append(((PCharacter)se).charset);
//			}
//			if(se instanceof PString) {
//				wordList.add(((PString)se).utf8);
//			}
//		}
//	}
//	
//	@Override
//	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
//		if(this.charset != null) {
//			if(context.match(this.charset)) {
//				return left;
//			}
//		}
//		for(int i = 0; i < this.wordList.size(); i++) {
//			if(context.match(this.wordList.ArrayValues[i])) {
//				return left;
//			}
//		}
//		return context.foundFailure(this);
//	}
//}

class PAlwaysFailure extends ParsingTerminal {
	PExpression dead;
	PAlwaysFailure(PExpression dead) {
		super(0);
		this.dead = dead;
	}
	@Override
	PExpression dup() {
		return new PAlwaysFailure(dead);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opFailure(dead);
		return false;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return Reject;
	}
}

class PConnector extends ParsingUnary {
	public int index;
	PConnector(int flag, PExpression e, int index) {
		super(flag | PExpression.HasConnector | PExpression.NoMemo, e);
		this.index = index;
	}
	@Override
	PExpression dup() {
		return new PConnector(flag, inner, index);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitConnector(this);
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.inner.acceptByte(ch, stopped);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingObject left = context.left;
		if(!this.inner.fastMatch(context)) {
			return false;
		}
		if(context.left != left && context.canTransCapture()) {
			context.logLink(left, this.index, context.left);
		}
		context.left = left;
		return true;
	}
}

class PTagging extends ParsingTerminal {
	ParsingTag tag;
	PTagging(int flag, ParsingTag tag) {
		super(PExpression.HasTagging | PExpression.NoMemo | flag);
		this.tag = tag;
	}
	@Override
	PExpression dup() {
		return new PTagging(flag, tag);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitTagging(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.canTransCapture()) {
			context.left.setTag(this.tag);
		}
		return true;
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
}

class PMessage extends ParsingTerminal {
	String symbol;
	PMessage(int flag, String message) {
		super(flag | PExpression.NoMemo | PExpression.HasMessage);
		this.symbol = message;
	}
	@Override
	PExpression dup() {
		return new PMessage(flag, symbol);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitMessage(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.canTransCapture()) {
			context.left.setValue(this.symbol);
		}
		return true;
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
}

class PConstructor extends PList {
	boolean leftJoin = false;
	int prefetchIndex = 0;
	ParsingType type;
	PConstructor(int flag, boolean leftJoin, UList<PExpression> list) {
		super(flag | PExpression.HasConstructor, list);
		this.leftJoin = leftJoin;
	}
	@Override
	PExpression dup() {
		return new PConstructor(flag, leftJoin, list);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitConstructor(this);
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
		ParsingObject left = context.left;
		if(context.isRecognitionMode()) {
			ParsingObject newone = context.newParsingObject(startIndex, this);
			context.left = newone;
			for(int i = 0; i < this.size(); i++) {
				if(!this.get(i).fastMatch(context)) {
					context.rollback(startIndex);
					return false;
				}
			}
			context.left = newone;
			return true;
		}
		else {
			for(int i = 0; i < this.prefetchIndex; i++) {
				if(!this.get(i).fastMatch(context)) {
					context.rollback(startIndex);
					return false;
				}
			}
			int mark = context.markObjectStack();
			ParsingObject newnode = context.newParsingObject(startIndex, this);
			context.left = newnode;
			if(this.leftJoin) {
				context.logLink(newnode, -1, left);
			}
			for(int i = this.prefetchIndex; i < this.size(); i++) {
				if(!this.get(i).fastMatch(context)) {
					context.abortLinkLog(mark);
					context.rollback(startIndex);
					return false;
				}
			}
			context.commitLinkLog(newnode, startIndex, mark);
			if(context.stat != null) {
				context.stat.countObjectCreation();
			}
			context.left = newnode;
			return true;
		}
	}
}

class PExport extends ParsingUnary {
	PExport(int flag, PExpression e) {
		super(flag | PExpression.NoMemo, e);
	}
	@Override
	PExpression dup() {
		return new PExport(flag, inner);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitExport(this);
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.checkNextFlow(this.inner.acceptByte(ch, this.flowNext), ch, stopped);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
}

abstract class ParsingFunction extends PExpression {
	String funcName;
	ParsingFunction(String funcName, int flag) {
		super(flag);
		this.funcName = funcName;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitParsingFunction(this);
	}
//	@Override
//	boolean checkFirstByte(int ch) {
//		return this.inner.checkFirstByte(ch);
//	}
	String getParameters() {
		return "";
	}
}

class ParsingIndent extends ParsingFunction {
	ParsingIndent(int flag) {
		super("indent", flag | PExpression.HasContext);
	}
	@Override PExpression dup() {
		return this;
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opIndent();
		return !context.isFailure();
	}
}

class ParsingFail extends ParsingFunction {
	String message;
	ParsingFail(int flag, String message) {
		super("fail", flag);
		this.message = message;
	}
	@Override PExpression dup() {
		return new ParsingFail(flag, message);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opFailure(this.message);
		return false;
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return Reject;
	}
}

class ParsingCatch extends ParsingFunction {
	ParsingCatch(int flag) {
		super("catch", flag);
	}
	@Override PExpression dup() {
		return this;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opCatch();
		return true;
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
}

abstract class ParsingOperation extends PExpression {
	String funcName;
	PExpression inner;
	protected ParsingOperation(String funcName, PExpression inner) {
		super(inner.flag);
		this.funcName = funcName;
		this.inner = inner;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitParsingOperation(this);
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.inner.acceptByte(ch, stopped);
	}
	@Override
	public PExpression getExpression() {
		return this.inner;
	}
	public String getParameters() {
		return "";
	}
}

class ParsingMemo extends ParsingOperation {
	static ParsingObject NonTransition = new ParsingObject(null, null, 0);

	boolean enableMemo = true;
	int memoId;
	int memoHit = 0;
	int memoMiss = 0;

	ParsingMemo(int memoId, PExpression inner) {
		super("memo", inner);
		this.memoId = memoId;
	}

	@Override PExpression dup() {
		return new ParsingMemo(0, inner);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(!this.enableMemo) {
			return this.inner.fastMatch(context);
		}
		long pos = context.getPosition();
		ParsingObject left = context.left;
		ObjectMemo m = context.getMemo(this, pos);
		if(m != null) {
			this.memoHit += 1;
			context.setPosition(pos + m.consumed);
			if(m.generated != NonTransition) {
				context.left = m.generated;
			}
			return !(context.isFailure());
		}
		this.inner.fastMatch(context);
		int length = (int)(context.getPosition() - pos);
		context.setMemo(pos, this, (context.left == left) ? NonTransition : context.left, length);
		this.memoMiss += 1;
		this.tryTracing();
		return !(context.isFailure());
	}

	private void tryTracing() {
		if(Main.TracingMemo) {
			if(this.memoMiss == 32) {
				if(this.memoHit < 2) {
					disabledMemo();
					return;
				}
			}
			if(this.memoMiss % 64 == 0) {
				if(this.memoHit == 0) {
					disabledMemo();
					return;
				}
				if(this.memoMiss / this.memoHit > 10) {
					disabledMemo();
					return;
				}
			}
		}		
	}
	
	private void disabledMemo() {
		//this.show();
		this.enableMemo = false;
//		this.base.DisabledMemo += 1;
//		int factor = this.base.EnabledMemo / 10;
//		if(factor != 0 && this.base.DisabledMemo % factor == 0) {
//			this.base.memoRemover.removeDisabled();
//		}
	}

	void show() {
		if(Main.VerboseMode) {
			double f = (double)this.memoHit / this.memoMiss;
			System.out.println(this.inner.getClass().getSimpleName() + " #h/m=" + this.memoHit + "," + this.memoMiss + ", f=" + f + " " + this.inner);
		}
	}
}

class ParsingMatch extends ParsingOperation {
	ParsingMatch(PExpression inner) {
		super("match", inner);
	}
	@Override PExpression dup() {
		return new ParsingMatch(inner);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		boolean oldMode = context.setRecognitionMode(true);
		ParsingObject left = context.left;
		if(this.inner.fastMatch(context)) {
			context.setRecognitionMode(oldMode);
			context.left = left;
			return true;
		}
		context.setRecognitionMode(oldMode);
		return false;
	}
}

class ParsingStackIndent extends ParsingOperation {
	ParsingStackIndent(PExpression e) {
		super("indent", e);
	}
	@Override PExpression dup() {
		return new ParsingStackIndent(inner);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opPushIndent();
		this.inner.fastMatch(context);
		context.opPopIndent();
		return !(context.isFailure());
	}
}

class ParsingIfFlag extends ParsingFunction {
	String flagName;
	ParsingIfFlag(int flag, String flagName) {
		super("if", flag | PExpression.HasContext);
		this.flagName = flagName;
	}
	@Override PExpression dup() {
		return new ParsingIfFlag(flag, flagName);
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opCheckFlag(this.flagName);
		return !(context.isFailure());
	}
	@Override
	short acceptByte(int ch, PExpression stopped) {
		return this.checkNextFlow(CheckNextFlow, ch, stopped);
	}
}

class ParsingWithFlag extends ParsingOperation {
	String flagName;
	ParsingWithFlag(String flagName, PExpression inner) {
		super("with", inner);
		this.flagName = flagName;
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override PExpression dup() {
		return new ParsingWithFlag(flagName, inner);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opEnableFlag(this.flagName);
		this.inner.fastMatch(context);
		context.opPopFlag(this.flagName);
		return !(context.isFailure());
	}
}

class ParsingWithoutFlag extends ParsingOperation {
	String flagName;
	ParsingWithoutFlag(String flagName, PExpression inner) {
		super("without", inner);
		this.flagName = flagName;
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override PExpression dup() {
		return new ParsingWithoutFlag(flagName, inner);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opDisableFlag(this.flagName);
		this.inner.fastMatch(context);
		context.opPopFlag(this.flagName);
		return !(context.isFailure());
	}
}

class ParsingDebug extends ParsingOperation {
	protected ParsingDebug(PExpression inner) {
		super("debug", inner);
	}
	@Override PExpression dup() {
		return new ParsingDebug(inner);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opRememberPosition();
		context.opRememberFailurePosition();
		context.opStoreObject();
		this.inner.fastMatch(context);
		context.opDebug(this.inner);
		return !(context.isFailure());
	}
}

class ParsingApply extends ParsingOperation {
	ParsingApply(PExpression inner) {
		super("|apply", inner);
	}
	@Override PExpression dup() {
		return new ParsingApply(inner);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		ParsingContext s = new ParsingContext(context.left);
//		
//		this.inner.fastMatch(s);
//		context.opRememberPosition();
//		context.opRememberFailurePosition();
//		context.opStoreObject();
//		this.inner.fastMatch(context);
//		context.opDebug(this.inner);
		return !(context.isFailure());

	}
}


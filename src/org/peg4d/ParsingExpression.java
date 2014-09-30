package org.peg4d;

import java.util.TreeMap;


abstract class ParsingMatcher {
	abstract boolean simpleMatch(ParsingContext context);
	String expectedToken() {
		return toString();
	}
}

public abstract class ParsingExpression extends ParsingMatcher {

	public final static int LeftRecursion     = 1 << 10;
	public final static int HasSyntaxError    = 1 << 16;
	public final static int HasTypeError      = 1 << 17;

	public final static int DisabledOperation = 1 << 18;
	public final static int ExpectedConnector = 1 << 19;
	public final static int NothingConnected  = 1 << 20;
	public final static int RedundantUnary    = 1 << 21;

	int           flag       = 0;
	int           uniqueId   = 0;
	ParsingObject po      = null;
	int           minlen = -1;
	ParsingMatcher       matcher;
		
	protected ParsingExpression() {
		this.matcher = this;
	}
	
	public final boolean isOptimized() {
		return (this.matcher != this);
	}

	public final boolean isUnique() {
		return (this.uniqueId > 0);
	}

	final boolean isExpectedConnector() {
		return (this.uniqueId == 0 && this.is(ExpectedConnector));
	}

	final boolean isNothingConnected() {
		return (this.uniqueId == 0 && this.is(NothingConnected));
	}

	final boolean isRemovedOperation() {
		return (this.uniqueId == 0 && this.is(DisabledOperation));
	}
	
	boolean hasObjectOperation() {
		return false;
	}
	
	ParsingExpression uniquefy() {
		ParsingExpression e = this.uniquefyImpl();
		assert(e.getClass() == this.getClass());
//		if(e.getClass() != this.getClass()) {
//			System.out.println("@@@@ " + this.getClass() + " " + this);
//		}
		return e;
	}
	ParsingExpression uniquefyImpl() { return null; }
	
//	ParsingExpression reduceOperation() {
//		ParsingExpression reduced = this.normalizeImpl(true, null, null);
////		if(reduced.getClass() != this.getClass()) {
////			System.out.println("@ " + this.getClass().getSimpleName() + " " + this + "\n\t=> " + reduced.getClass().getSimpleName() + " " + reduced);
////		}
//		return reduced;
//	}
	
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		return this;
	}
	
	protected abstract void visit(ExpressionVisitor visitor);
	
	public final boolean debugMatch(ParsingContext c) {
//		int d = cc; cc++;
//		int dpos = dstack.size();
//		int pos = (int)c.getPosition() ;
//		if(pos % (1024 * 1024) == 0) {
//			System.out.println("["+(pos/(1024 * 1024))+"] calling: " + this + " mark=" + c.markObjectStack() + " free" + Runtime.getRuntime().freeMemory());
//		}
//		dstack.add(this);
		boolean b = this.matcher.simpleMatch(c);
//		if(this instanceof NonTerminal) {
////			c.dumpCallStack("["+pos+"] called: " + b + " ");
//		}
//		dstack.clear(dpos);
//		if(pos > 12717) {
//			System.out.println(dstack);
//		}
//		//assert(c.isFailure() == !b);
		return b;
	}
	
	public final static short Reject        = 0;
	public final static short Accept        = 1;
	public final static short WeakAccept    = 1;
	public final static short CheckNextFlow = 2;
	
	short acceptByte(int ch) {
		return CheckNextFlow;
	}
	
	public ParsingExpression getExpression() {
		return this;
	}

	public final boolean is(int uflag) {
		return ((this.flag & uflag) == uflag);
	}

	public void set(int uflag) {
		this.flag = this.flag | uflag;
	}
	
	public int size() {
		return 0;
	}
	
	public ParsingExpression get(int index) {
		return null;
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
	
	public static int WarningLevel = 1;

	final void report(ReportLevel level, String msg) {
		if(WarningLevel == 0 && level.compareTo(ReportLevel.error) != 0) {
			return;
		}
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
		}
		else {
			System.out.println("" + level.toString() + ": " + msg + " in " + this);
		}
	}
		
	private static boolean checkRecursion(String uName, UList<String> stack) {
		for(int i = 0; i < stack.size() - 1; i++) {
			if(uName.equals(stack.ArrayValues[i])) {
				return true;
			}
		}
		return false;
	}

	static int checkLeftRecursion(ParsingExpression e, String uName, int start, int minlen, UList<String> stack, UMap<String> flagMap) {
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal) e;
			ne.checkReference();
			if(minlen == 0) {
				String n = ne.getUniqueName();
				if(n.equals(uName) && !e.is(LeftRecursion)) {
					ParsingRule r = ne.getRule();
					e.set(LeftRecursion);
					e.report(ReportLevel.error, "left recursion: " + r);
					r.peg.foundError = true;
				}
				if(!checkRecursion(n, stack)) {
					int pos = stack.size();
					stack.add(n);
					int nc = checkLeftRecursion(ne.deReference(), uName, start, minlen, stack, flagMap);
					e.minlen = nc - minlen;
					stack.clear(pos);
				}
				if(e.minlen == -1) {
					e.minlen = 1; // FIXME: assuming no left recursion
				}
			}
			else if(e.minlen == -1) {
				e.minlen = 0;
			}
		}
		if(e instanceof ParsingChoice) {
			int lmin = Integer.MAX_VALUE;
			for(int i = 0; i < e.size(); i++) {
				int nc = checkLeftRecursion(e.get(i), uName, start, minlen, stack, flagMap);
				if(nc < lmin) {
					lmin = nc;
				}
			}
			e.minlen = lmin - minlen;
		}
		else if(e instanceof ParsingSequence || e instanceof ParsingConstructor) {
			int nc = minlen;
			for(int i = 0; i < e.size(); i++) {
				ParsingExpression eN = e.get(i);
				nc = checkLeftRecursion(eN, uName, start, nc, stack, flagMap);
			}
			e.minlen = nc - minlen;
		}
		else if(e instanceof ParsingUnary) {
			int lmin = checkLeftRecursion(((ParsingUnary) e).inner, uName, start, minlen, stack, flagMap); // skip count
			if(e instanceof ParsingOption || e instanceof ParsingRepetition || e instanceof ParsingNot || e instanceof ParsingAnd ) {
				e.minlen = 0;
			}
			else {
				e.minlen = lmin - minlen;
			}
		}
		else if(e instanceof ParsingIf) {
			String n = ((ParsingIf) e).flagName;
			flagMap.put(n, n);
			e.minlen = 0;
		}
		else {
			if(e.minlen == -1) {
				e.minlen = 0;
			}
		}
//		if(e.minlen == -1) {
//			System.out.println("@@@@ " + uName + "," + e);
//		}
		assert(e.minlen != -1);
		minlen += e.minlen;
		return minlen;
	}
	
	static boolean containFlag(ParsingExpression e, String flagName, UMap<String> visited) {
		for(int i = 0; i < e.size(); i++) {
			if(containFlag(e.get(i), flagName, visited)) {
				return true;
			}
		}
		if(e instanceof ParsingIf) {
			return flagName.equals(((ParsingIf) e).flagName);
		}
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal)e;
			String un = ne.getUniqueName();
			if(!visited.hasKey(un)) {
				visited.put(un, un);
				ParsingRule r = ne.getRule();
				return containFlag(r.expr, flagName, visited);
			}
		}
		return false;
	}

	static boolean containFlag(ParsingExpression e, String flagName) {
		return containFlag(e, flagName, new UMap<String>());
	}
	
	static int ObjectContext    = 1 << 0;
	static int OperationContext = 1 << 1;
	
	private static int checkObjectConstruction(ParsingExpression e, int status) {
		if(status == ParsingRule.ReservedRule) {
			return status;
		}
		if(status == OperationContext) {
			e.set(ExpectedConnector);
			e.report(ReportLevel.warning, "expected @");			
		}
		else if(status != ObjectContext) {
			e.set(HasTypeError);
			e.set(DisabledOperation);
			e.report(ReportLevel.warning, "unexpected object construction");
			return status;
		}
		return OperationContext;
	}

	private static void checkObjectOperation(ParsingExpression e, int status) {
		if(status == ParsingRule.ReservedRule) {
			return;
		}
		if(status != OperationContext) {
			e.set(HasTypeError);
			e.set(DisabledOperation);
			e.report(ReportLevel.warning, "unspecific left object");
		}
	}

	static int typeCheckImpl(ParsingExpression e, int status, UMap<String> flagMap) {
		if(e instanceof NonTerminal) {
			ParsingRule r = ((NonTerminal) e).getRule();
			int ruleType = r.type;
			if(ruleType == ParsingRule.ObjectRule) {
				return checkObjectConstruction(e, status);
			}
			if(ruleType == ParsingRule.OperationRule) {
				checkObjectOperation(e, status);
			}
			return status;
		}
		if(e instanceof ParsingConstructor) {
			boolean LeftJoin = ((ParsingConstructor) e).leftJoin;
			if(LeftJoin) {
				checkObjectOperation(e, status);
			}
			else {
				status = checkObjectConstruction(e, status);
			}
			int newstatus = OperationContext;
			for(int i = 0; i < e.size(); i++) {
				newstatus = typeCheckImpl(e.get(i), newstatus, flagMap);
			}
			return status;
		}
		if(e instanceof ParsingConnector) {
			checkObjectOperation(e, status);
			int scope = typeCheckImpl(e.get(0), ObjectContext, flagMap);
			if(scope != OperationContext) {
				e.set(NothingConnected);
				e.report(ReportLevel.warning, "nothing is connected");
			}
			return status;
		}
		if(e instanceof ParsingTagging || e instanceof ParsingValue) {
			checkObjectOperation(e, status);
			return status;
		}
		if(e instanceof ParsingSequence) {
			for(int i = 0; i < e.size(); i++) {
				status = typeCheckImpl(e.get(i), status, flagMap);
			}
			return status;
		}
		if(e instanceof ParsingOption || e instanceof ParsingRepetition) {
			int r = typeCheckImpl(((ParsingUnary) e).inner, status, flagMap);
			if(r != status) {
				e.report(ReportLevel.warning, "mixed results");
			}
			return status;
		}
		if(e instanceof ParsingChoice) {
			int first = typeCheckImpl(e.get(0), status, flagMap);
			for(int i = 1; i < e.size(); i++) {
				int r = typeCheckImpl(e.get(i), status, flagMap);
				if(r != first) {
					e.get(i).report(ReportLevel.warning, "mixed choice");
				}
			}
			return first;
		}
		if(e instanceof ParsingNot || e instanceof ParsingMatch) {
//			ParsingExpression reduced = ((ParsingUnary) e).get(0).reduceOperation().uniquefy();
//			if(reduced != e.get(0)) {
//				((ParsingNot) e).inner = reduced;
//			}
			int r = typeCheckImpl(e.get(0), ObjectContext, flagMap);
			return status;
		}
		if(e instanceof ParsingWithFlag) {
			ParsingWithFlag we = (ParsingWithFlag)e;
			if(!containFlag(we.inner, we.flagName)) {
				we.report(ReportLevel.warning, "no such a flag: " + we.flagName);
				we.set(RedundantUnary);
			}
			return typeCheckImpl(we.inner, status, flagMap);
		}
		if(e instanceof ParsingWithoutFlag) {
			ParsingWithoutFlag we = (ParsingWithoutFlag)e;
			if(!containFlag(we.inner, we.flagName)) {
				we.report(ReportLevel.warning, "no such a flag: " + we.flagName);
				we.set(RedundantUnary);
			}
			return typeCheckImpl(we.inner, status, flagMap);
		}
		if(e instanceof ParsingUnary) {
			return typeCheckImpl(((ParsingUnary) e).inner, status, flagMap);
		}
		return status;
	}

	static void typeCheck(ParsingRule rule, UMap<String> flagMap) {
		int result = typeCheckImpl(rule.expr, rule.type, flagMap);
		if(rule.type == ParsingRule.ObjectRule) {
			if(result != OperationContext) {
				rule.report(ReportLevel.warning, "object construction is expected: " + rule);
			}
		}
	}
	
	// factory
	
	private static boolean Conservative = false;
	private static boolean StringSpecialization = true;
	private static boolean CharacterChoice      = true;
	
	public final static ParsingEmpty newEmpty() {
		return new ParsingEmpty();
	}

	public final static ParsingFailure newFailure(ParsingExpression e) {
		return new ParsingFailure(e);
	}

	public final static ParsingByte newByte(int ch) {
		return new ParsingByte(ch & 0xff);
	}
	
	public final static ParsingExpression newByte(int ch, String token) {
		ParsingByte e = newByte(ch);
		e.errorToken = token;
		return e;
	}
	
	public final static ParsingExpression newAny(String text) {
		return new ParsingAny();
	}

	public final static ParsingExpression newSequence(UList<ParsingExpression> l) {
		if(l.size() == 0) {
			return newEmpty();
		}
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		return checkUnique(new ParsingSequence(l), isUnique(l));
	}
	
	public final static void addSequence(UList<ParsingExpression> l, ParsingExpression e) {
		if(e instanceof ParsingEmpty) {
			return;
		}
		if(e instanceof ParsingSequence) {
			for(int i = 0; i < e.size(); i++) {
				addSequence(l, e.get(i));
			}
			return;
		}
		if(l.size() > 0) {
			ParsingExpression pe = l.ArrayValues[l.size()-1];
			if(e instanceof ParsingNot && pe instanceof ParsingNot) {
				((ParsingNot) pe).inner = appendAsChoice(((ParsingNot) pe).inner, ((ParsingNot) e).inner);
				return;
			}
			if(pe instanceof ParsingFailure && pe instanceof ParsingFail) {
				return;
			}
		}
		l.add(e);
	}

	public final static ParsingExpression appendAsChoice(ParsingExpression e, ParsingExpression e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[e.size()+e2.size()]);
		addChoice(l, e);
		addChoice(l, e2);
		return newChoice(l);
	}

	public static final ParsingExpression newString(String text) {
		byte[] utf8 = ParsingCharset.toUtf8(text);
		if(utf8.length == 0) {
			return newEmpty();
		}
		if(Conservative) {
			return new ParsingString(text, utf8);	
		}
		if(utf8.length == 1) {
			return newByte(utf8[0]);
		}
		return newByteSequence(utf8, text);
	}

	public final static ParsingExpression newByteSequence(byte[] utf8, String token) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[utf8.length]);
		for(int i = 0; i < utf8.length; i++) {
			l.add(newByte(utf8[i], token));
		}
		return newSequence(l);
	}

	public final static ParsingExpression newUnicodeRange(int c, int c2, String token) {
		byte[] b = ParsingCharset.toUtf8(String.valueOf((char)c));
		byte[] b2 = ParsingCharset.toUtf8(String.valueOf((char)c2));
		if(equalsBase(b, b2)) {
			return newUnicodeRange(b, b2, token);
		}
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[b.length]);
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

	private final static ParsingExpression newUnicodeRange(byte[] b, byte[] b2, String token) {
		if(b[b.length-1] == b2[b.length-1]) {
			return newByteSequence(b, token);
		}
		else {
			UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[b.length]);
			for(int i = 0; i < b.length-1; i++) {
				l.add(newByte(b[i], token));
			}
			l.add(newByteRange(b[b.length-1] & 0xff, b2[b2.length-1] & 0xff, token));
			return newSequence(l);
		}
	}

	public final static ParsingExpression newByteRange(int c, int c2, String token) {
		if(c == c2) {
			return newByte(c, token);
		}
		return new ParsingByteRange(c, c2);
//		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[c2 - c + 1]);
//		while(c <= c2) {
//			l.add(newByteChar(c, token));
//			c++;
//		}
//		return newChoice(l);
	}
	
	public final static ParsingExpression newCharset(String t, String t2, String token) {
		int c = ParsingCharset.parseAscii(t);
		int c2 = ParsingCharset.parseAscii(t2);
		if(c != -1 && c2 != -1) {
			return newByteRange(c, c2, token);
		}
		c = ParsingCharset.parseUnicode(t);
		c2 = ParsingCharset.parseUnicode(t2);
		if(c < 128 && c2 < 128) {
			return newByteRange(c, c2, token);
		}
		else {
			return newUnicodeRange(c, c2, token);
		}
	}
	
	public final static ParsingExpression newCharacter(String text) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[text.length()]);
		CharacterReader r = new CharacterReader(text);
		char ch = r.readChar();
		while(ch != 0) {
			char next = r.readChar();
			if(next == '-') {
				int ch2 = r.readChar();
				if(ch > 0 && ch2 < 128) {
					l.add(newByteRange(ch, ch2, text));
				}
				ch = r.readChar();
			}
			else {
				if(ch > 0 && ch < 128) {
					l.add(newByte(ch, text));
				}
				ch = next; //r.readChar();
			}
		}
		return newChoice(l);
	}
	
	public final static ParsingExpression newOption(ParsingExpression p) {
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
		return checkUnique(new ParsingOption(p), p.isUnique());
	}
	
	public final static ParsingExpression newMatch(ParsingExpression p) {
		return checkUnique(new ParsingMatch(p), p.isUnique());
	}
		
	public final static ParsingExpression newRepetition(ParsingExpression p) {
//		if(p instanceof PCharacter) {
//			return new PZeroMoreCharacter(0, (PCharacter)p);
//		}
		return checkUnique(new ParsingRepetition(p), p.isUnique());
	}

	public final static ParsingExpression newAnd(ParsingExpression p) {
		return checkUnique(new ParsingAnd(p), p.isUnique());
	}
	
	public final static ParsingExpression newNot(ParsingExpression p) {
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
		return checkUnique(new ParsingNot(p), p.isUnique());
	}
		
	public final static ParsingExpression newChoice(UList<ParsingExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		return checkUnique(new ParsingChoice(l), isUnique(l));
	}

	public final static void addChoice(UList<ParsingExpression> l, ParsingExpression e) {
		if(e instanceof ParsingChoice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
		}
		else {
			l.add(e);
		}
	}

	final static boolean isUnique(UList<ParsingExpression> l) {
		for(int i = 0; i < l.size(); i++) {
			if(!l.ArrayValues[i].isUnique()) {
				return false;
			}
		}
		return true;
	}
	
	public final static ParsingExpression newConstructor(ParsingExpression p) {
		UList<ParsingExpression> l = toSequenceList(p);
		return checkUnique(new ParsingConstructor(false, l), isUnique(l));
	}

	public final static ParsingExpression newJoinConstructor(ParsingExpression p) {
		UList<ParsingExpression> l = toSequenceList(p);
		return checkUnique(new ParsingConstructor(true, l), isUnique(l));
	}
	
	public final static UList<ParsingExpression> toSequenceList(ParsingExpression e) {
		if(e instanceof ParsingSequence) {
			return ((ParsingSequence) e).inners;
		}
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[1]);
		l.add(e);
		return l;
	}
		
	public final static ParsingExpression newConnector(ParsingExpression p, int index) {
		return checkUnique(new ParsingConnector(p, index), p.isUnique());
	}

	public final static ParsingExpression newTagging(ParsingTag tag) {
		return new ParsingTagging(tag);
	}

	public final static ParsingExpression newValue(String msg) {
		return new ParsingValue(msg);
	}
	
	public final static ParsingExpression newDebug(ParsingExpression e) {
		return checkUnique(new ParsingDebug(e), e.isUnique());
	}

	public final static ParsingExpression newFail(String message) {
		return new ParsingFail(message);
	}

	public final static ParsingExpression newCatch() {
		return new ParsingCatch();
	}
	
	public final static ParsingExpression newIf(String flagName) {
		return new ParsingIf(flagName);
	}

	public final static ParsingExpression newWithFlag(String flagName, ParsingExpression e) {
		return checkUnique(new ParsingWithFlag(flagName, e), e.isUnique());
	}

	public final static ParsingExpression newWithoutFlag(String flagName, ParsingExpression e) {
		return checkUnique(new ParsingWithoutFlag(flagName, e), e.isUnique());
	}

	public final static ParsingExpression newBlock(ParsingExpression e) {
		return checkUnique(new ParsingBlock(e), e.isUnique());
	}

	public final static ParsingExpression newIndent() {
		return new ParsingIndent();
	}

	public static ParsingExpression newName(int tagId, ParsingExpression e) {
		return checkUnique(new ParsingName(tagId, e), e.isUnique());
	}

	public static ParsingExpression newIsa(int tagId) {
		return new ParsingIsa(tagId);
	}


	public final static UMap<ParsingExpression> uniqueMap = new UMap<ParsingExpression>();
	
	public static ParsingExpression uniquefy(String key, ParsingExpression e) {
		if(e.uniqueId == 0) {
			ParsingExpression u = uniqueMap.get(key);
			if(u == null) {
				u = e;
				e.po = null;
				e.uniqueId = uniqueMap.size() + 1;
				uniqueMap.put(key, e);
			}
			assert(u.getClass() == e.getClass());
			return u;
		}
		return e;
	}

	private static ParsingExpression checkUnique(ParsingExpression e, boolean unique) {
		if(unique) {
			e = e.uniquefy();
		}
		return e;
	}
	
}

abstract class ParsingUnary extends ParsingExpression {
	ParsingExpression inner;
	ParsingUnary(ParsingExpression e) {
		super();
		this.inner = e;
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final ParsingExpression get(int index) {
		return this.inner;
	}
	protected final int uniqueKey() {
		this.inner = inner.uniquefy();
		return this.inner.uniqueId;
	}
	@Override
	boolean hasObjectOperation() {
		return this.hasObjectOperation();
	}
	@Override
	short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}
}

abstract class ParsingList extends ParsingExpression {
	UList<ParsingExpression> inners;
	ParsingList(UList<ParsingExpression> list) {
		super();
		this.inners = list;
	}
	@Override
	public final int size() {
		return this.inners.size();
	}
	@Override
	public final ParsingExpression get(int index) {
		return this.inners.ArrayValues[index];
	}
	final void set(int index, ParsingExpression e) {
		this.inners.ArrayValues[index] = e;
	}
	protected final String uniqueKey() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = this.get(i).uniquefy();
			set(i, e);
			sb.append(e.uniqueId);
			sb.append(":");
		}
		return sb.toString();
	}

	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, flagMap, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}

	@Override
	boolean hasObjectOperation() {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).hasObjectOperation()) {
				return true;
			}
		}
		return false;
	}

	@Override
	short acceptByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if(r != CheckNextFlow) {
				return r;
			}
		}
		return CheckNextFlow;
	}

//	public final ParsingExpression trim() {
//		int size = this.size();
//		boolean hasNull = true;
//		while(hasNull) {
//			hasNull = false;
//			for(int i = 0; i < size-1; i++) {
//				if(this.get(i) == null && this.get(i+1) != null) {
//					this.swap(i,i+1);
//					hasNull = true;
//				}
//			}
//		}
//		for(int i = 0; i < this.size(); i++) {
//			if(this.get(i) == null) {
//				size = i;
//				break;
//			}
//		}
//		if(size == 0) {
//			return null;
//		}
//		if(size == 1) {
//			return this.get(0);
//		}
//		this.list.clear(size);
//		return this;
//	}
	
	public final void swap(int i, int j) {
		ParsingExpression e = this.inners.ArrayValues[i];
		this.inners.ArrayValues[i] = this.inners.ArrayValues[j];
		this.inners.ArrayValues[j] = e;
	}
}

class ParsingEmpty extends ParsingExpression {
	ParsingEmpty() {
		super();
		this.minlen = 0;
	}
	
	@Override ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy("\b", this);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitEmpty(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
}

class ParsingFailure extends ParsingExpression {
	ParsingExpression dead;
	ParsingFailure(ParsingExpression dead) {
		super();
		this.dead = dead;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return new ParsingFailure(dead);
	}
	@Override
	short acceptByte(int ch) {
		return Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.failure(dead);
		return false;
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
	}
}

class ParsingByte extends ParsingExpression {
	int byteChar;
	String errorToken = null;
	ParsingByte(int ch) {
		super();
		this.byteChar = ch;
		this.minlen = 1;
	}
	@Override ParsingExpression uniquefyImpl() { 
		if(this.errorToken == null) {
			return ParsingExpression.uniquefy("'\b" + byteChar, this);
		}
		return ParsingExpression.uniquefy("'\b" + this.errorToken + "\b" + byteChar, this);
	}
	@Override
	String expectedToken() {
		if(this.errorToken != null) {
			return this.errorToken;
		}
		return this.toString();
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitByte(this);
	}
	@Override
	short acceptByte(int ch) {
		return (byteChar == ch) ? Accept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.byteAt(context.pos) == this.byteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}

class ParsingAny extends ParsingExpression {
	ParsingAny() {
		super();
		this.minlen = 1;
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy(".\b", this);
	}
	@Override 
	short acceptByte(int ch) {
		return Accept;
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitAny(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.source.charAt(context.pos) != -1) {
			int len = context.source.charLength(context.pos);
			context.consume(len);
			return true;
		}
		context.failure(this);
		return false;
	}
}

class NonTerminal extends ParsingExpression {
	Grammar peg;
	String  ruleName;
	String  uniqueName;
	NonTerminal(Grammar base, String ruleName) {
		super();
		this.peg = base;
		this.ruleName = ruleName;
		this.uniqueName = this.peg.uniqueRuleName(this.ruleName);
	}

	@Override
	boolean hasObjectOperation() {
		ParsingRule r = this.getRule();
		return r.type == ParsingRule.OperationRule;
	}

	@Override
	ParsingExpression uniquefyImpl() {
		boolean expectedConnector = this.isExpectedConnector();
		ParsingExpression e = ParsingExpression.uniquefy(getUniqueName(), this);
		if(expectedConnector) {
			e = ParsingExpression.newConnector(e, -1);
		}
		return e;
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingRule rule = this.getRule();
		String optName = ParsingRule.toOptionName(rule, lexOnly, withoutMap);
		if(this.peg.getRule(optName) != rule) {
			this.peg.makeOptionRule(rule, optName, lexOnly, flagMap, withoutMap);
			return this.peg.newNonTerminal(optName);
		}
		return this;
	}

	final String getUniqueName() {
		return this.uniqueName;
	}
	
	final ParsingRule getRule() {
		return this.peg.getRule(this.ruleName);
	}
	
	final ParsingExpression deReference() {
		return this.peg.getExpression(this.ruleName);
	}
	
	void checkReference() {
		ParsingRule r = this.getRule();
		if(r == null) {
			this.report(ReportLevel.error, "undefined rule: " + this.ruleName);
			r = new ParsingRule(this.peg, this.ruleName, null, new ParsingIf(this.ruleName));
			this.peg.setRule(this.ruleName, r);
		}
		if(r.minlen != -1) {
			this.minlen = r.minlen;
		}
		r.refc += 1;
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitNonTerminal(this);
	}
	@Override short acceptByte(int ch) {
		if(this.deReference() != null && !this.is(LeftRecursion)) {
			return this.deReference().acceptByte(ch);
		}
		return WeakAccept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int stackTop = context.pushCallStack(this.uniqueName);
		boolean b = this.deReference().debugMatch(context);
		if(Main.VerboseMode && !b && this.peg != GrammarFactory.Grammar) {
			context.dumpCallStack("["+context.getPosition()+"] failure: ");
		}
		context.popCallStack(stackTop);
		return b;
	}
}

class ParsingString extends ParsingExpression {
	String text;
	byte[] utf8;
	ParsingString(String text, byte[] utf8) {
		super();
		this.text = text;
		this.utf8 = utf8;
		this.minlen = utf8.length;
	}
	@Override
	ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy("''\b" + text, this);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitString(this);
	}
	@Override short acceptByte(int ch) {
		if(this.utf8.length == 0) {
			return CheckNextFlow;
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
			context.failure(this);
			return false;
		}
	}
}

class ParsingByteRange extends ParsingExpression {
	int startByteChar;
	int endByteChar;
	ParsingByteRange(int startByteChar, int endByteChar) {
		super();
		this.startByteChar = startByteChar;
		this.endByteChar = endByteChar;
		this.minlen = 1;
	}
	@Override 
	ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy("[\b" + startByteChar + "-" + endByteChar, this);
	}
	void setCount(int[] count) {
		for(int c = startByteChar; c <= endByteChar; c++) {
			count[c]++;
		}
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitByteRange(this);
	}
	@Override 
	short acceptByte(int ch) {
		return (startByteChar <= ch && ch <= endByteChar) ? Accept : Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int ch = context.source.byteAt(context.pos);
		if(startByteChar <= ch && ch <= endByteChar) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}

class ParsingOption extends ParsingUnary {
	ParsingOption(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy("?\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newOption(e);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitOptional(this);
	}
	@Override 
	short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return r;
		}
		return CheckNextFlow;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
		if(!this.inner.debugMatch(context)) {
			context.left = left;
			context.forgetFailure(f);
		}
		left = null;
		return true;
	}
}

class ParsingRepetition extends ParsingUnary {
	ParsingRepetition(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy("*\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newRepetition(e);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitRepetition(this);
	}
	@Override short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return r;
		}
		return CheckNextFlow;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long ppos = -1;
		long pos = context.getPosition();
//		long f = context.rememberFailure();
		while(ppos < pos) {
			ParsingObject left = context.left;
			if(!this.inner.debugMatch(context)) {
				context.left = left;
				left = null;
				break;
			}
			ppos = pos;
			pos = context.getPosition();
			left = null;
		}
//		context.forgetFailure(f);
		return true;
	}
}

class ParsingAnd extends ParsingUnary {
	ParsingAnd(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy("&\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newAnd(e);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitAnd(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		this.inner.debugMatch(context);
		context.rollback(pos);
		return !context.isFailure();
	}
}

class ParsingNot extends ParsingUnary {
	ParsingNot(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniquefy("!\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(true, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newNot(e);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitNot(this);
	}
	@Override
	short acceptByte(int ch) {
		short r = this.inner.acceptByte(ch);
		if(r == Accept) {
			return Reject;
		}
		if(r == Reject) {
			return Accept;
		}
		return r;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingObject left = context.left;
		if(this.inner.debugMatch(context)) {
			context.rollback(pos);
			context.failure(this);
			left = null;
			return false;
		}
		else {
			context.rollback(pos);
			context.forgetFailure(f);
			context.left = left;
			left = null;
			return true;
		}
	}
}

class ParsingSequence extends ParsingList {
	ParsingSequence(UList<ParsingExpression> l) {
		super(l);
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy(" \b" + this.uniqueKey(), this);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitSequence(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			if(!(this.get(i).debugMatch(context))) {
				context.abortLinkLog(mark);
				context.rollback(pos);
				return false;
			}
		}
		return true;
	}
}

class ParsingChoice extends ParsingList {
	ParsingChoice(UList<ParsingExpression> list) {
		super(list);
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy("|\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, flagMap, withoutMap);
			ParsingExpression.addChoice(l, e);
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitChoice(this);
	}
	@Override
	short acceptByte(int ch) {
		boolean checkNext = false;
		for(int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
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
			if(this.get(i).debugMatch(context)) {
				context.forgetFailure(f);
				left = null;
				return true;
			}
		}
		assert(context.isFailure());
		left = null;
		return false;
	}
}

class ParsingConnector extends ParsingUnary {
	public int index;
	ParsingConnector(ParsingExpression e, int index) {
		super(e);
		this.index = index;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		if(index != -1) {
			return ParsingExpression.uniquefy("@" + index + "\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniquefy("@\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		if(this.isRemovedOperation()) {
			lexOnly = true;
		}
		if(this.isNothingConnected()) {
			return this.inner.uniquefy();
		}
		return this.inner.normalizeImpl(lexOnly, flagMap, withoutMap);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitConnector(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingObject left = context.left;
		if(context.canTransCapture()) {
			int mark = context.markObjectStack();
			if(this.inner.debugMatch(context)) {
				if(context.left != left) {
					context.commitLinkLog(mark, context.left);
					context.logLink(left, this.index, context.left);
				}
				context.left = left;
				left = null;
				return true;
			}
			context.abortLinkLog(mark);			
			return false;
		}
		else {
			if(this.inner.debugMatch(context)) {
				context.left = left;
				left = null;
				return true;			
			}				
		}
		left = null;
		return false;
	}
}

class ParsingTagging extends ParsingExpression {
	ParsingTag tag;
	ParsingTagging(ParsingTag tag) {
		super();
		this.tag = tag;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy("#\b" + this.tag.key(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		if(this.isRemovedOperation()) {
			return this.normalizeImpl(lexOnly, flagMap, withoutMap);
		}
		return ParsingExpression.newEmpty();
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitTagging(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.canTransCapture()) {
			context.left.setTag(this.tag);
		}
		return true;
	}
}

class ParsingValue extends ParsingExpression {
	String value;
	ParsingValue(String value) {
		super();
		this.value = value;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy("`\b" + this.value, this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		if(this.isRemovedOperation()) {
			return this.normalizeImpl(true, flagMap, withoutMap);
		}
		return ParsingExpression.newEmpty();
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitValue(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.canTransCapture()) {
			context.left.setValue(this.value);
		}
		return true;
	}
}

class ParsingConstructor extends ParsingList {
	boolean leftJoin = false;
	int prefetchIndex = 0;
	ParsingType type;
	ParsingConstructor(boolean leftJoin, UList<ParsingExpression> list) {
		super(list);
		this.leftJoin = leftJoin;
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		if(leftJoin) {
			return ParsingExpression.uniquefy("{@}\b" + this.uniqueKey(), this);
		}
		boolean expectedConnector = this.isExpectedConnector();
		ParsingExpression e = ParsingExpression.uniquefy("{}\b" + this.uniqueKey(), this);
		if(expectedConnector) {
			e = ParsingExpression.newConnector(e, -1);
		}
		return e;
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitConstructor(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
		if(context.isRecognitionMode()) {
			ParsingObject newone = context.newParsingObject(startIndex, this);
			context.left = newone;
			for(int i = 0; i < this.size(); i++) {
				if(!this.get(i).debugMatch(context)) {
					context.rollback(startIndex);
					return false;
				}
			}
			context.left = newone;
			return true;
		}
		else {
			ParsingObject left = context.left;
			for(int i = 0; i < this.prefetchIndex; i++) {
				if(!this.get(i).debugMatch(context)) {
					context.rollback(startIndex);
					left = null;
					return false;
				}
			}
			int mark = context.markObjectStack();
			if(this.leftJoin) {
				context.lazyCommit(left);
			}
			ParsingObject newnode = context.newParsingObject(startIndex, this);
			context.left = newnode;
			if(this.leftJoin) {
				context.logLink(newnode, 0, left);
			}
			for(int i = this.prefetchIndex; i < this.size(); i++) {
				if(!this.get(i).debugMatch(context)) {
					context.abortLinkLog(mark);
					context.rollback(startIndex);
					left = null;
					return false;
				}
			}
			newnode.setLength((int)(context.getPosition() - startIndex));
			//context.commitLinkLog2(newnode, startIndex, mark);
			context.left = newnode;
			left = null;
			return true;
		}
	}
}

// ------------------------------
// PEG4d Function, PEG4d Operator

abstract class ParsingFunction extends ParsingExpression {
	String funcName;
	ParsingFunction(String funcName) {
		super();
		this.funcName = funcName;
	}
	String getParameters() {
		return "";
	}
	@Override final ParsingExpression uniquefyImpl() {
		return 	ParsingExpression.uniquefy("<"+this.funcName+this.getParameters(), this);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitParsingFunction(this);
	}
}

abstract class ParsingOperation extends ParsingUnary {
	String funcName;
	ParsingOperation(String funcName, ParsingExpression inner) {
		super(inner);
		this.funcName = funcName;
		this.inner = inner;
	}
	@Override final ParsingExpression uniquefyImpl() {
		return 	ParsingExpression.uniquefy("<"+this.funcName+this.getParameters()+"+"+this.uniqueId, this);
	}
	public String getParameters() {
		return "";
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitParsingOperation(this);
	}
	@Override
	public ParsingExpression getExpression() {
		return this.inner;
	}
}

class ParsingFail extends ParsingFunction {
	String message;
	ParsingFail(String message) {
		super("fail");
		this.message = message;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.failure(this);
		return false;
	}
	@Override
	short acceptByte(int ch) {
		return Reject;
	}
}

class ParsingCatch extends ParsingFunction {
	ParsingCatch() {
		super("catch");
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(context.canTransCapture()) {
			context.left.setSourcePosition(context.fpos);
			context.left.setValue(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
		}
		return true;
	}
}

class ParsingExport extends ParsingUnary {
	ParsingExport(ParsingExpression e) {
		super(e);
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return new ParsingExport(inner);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		return inner.normalizeImpl(lexOnly, flagMap, withoutMap);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitExport(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
}

class ParsingMatch extends ParsingOperation {
	ParsingMatch(ParsingExpression inner) {
		super("match", inner);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
//		ParsingExpression e = inner.normalizeImpl(true, flagMap, withoutMap);
//		if(e == inner) {
//			return this;
//		}
//		return ParsingExpression.newMatch(e);
		return inner.normalizeImpl(true, flagMap, withoutMap);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		boolean oldMode = context.setRecognitionMode(true);
		ParsingObject left = context.left;
		if(this.inner.debugMatch(context)) {
			context.setRecognitionMode(oldMode);
			context.left = left;
			left = null;
			return true;
		}
		context.setRecognitionMode(oldMode);
		left = null;
		return false;
	}
}

class ParsingBlock extends ParsingOperation {
	ParsingBlock(ParsingExpression e) {
		super("block", e);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newBlock(e);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		String indent = context.source.getIndentText(context.pos);
		int stackTop = context.pushTokenStack(PEG4d.Indent, indent);
		boolean b = this.inner.debugMatch(context);
		context.popTokenStack(stackTop);
		return b;
	}
}

class ParsingIndent extends ParsingFunction {
	ParsingIndent() {
		super("indent");
	}
	@Override
	short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return CheckNextFlow;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchTokenStackTop(PEG4d.Indent);
	}
}

class ParsingName extends ParsingOperation {
	int tagId;
	ParsingName(int tagId, ParsingExpression inner) {
		super("name", inner);
		this.tagId = tagId; //ParsingTag.tagId(flagName);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newName(tagId, e);
	}

	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tagId);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
		if(this.inner.debugMatch(context)) {
			long endIndex = context.getPosition();
			String s = context.source.substring(startIndex, endIndex);
			context.pushTokenStack(tagId, s);
			return true;
		}
		return false;
	}
}

class ParsingIsa extends ParsingFunction {
	int tagId;
	ParsingIsa(int tagId) {
		super("if");
		this.tagId = tagId;
	}
	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tagId);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchTokenStack(tagId);
	}
}


class ParsingIf extends ParsingFunction {
	String flagName;
	ParsingIf(String flagName) {
		super("if");
		this.flagName = flagName;
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
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		if(flagMap != null && withoutMap.containsKey(flagName)) {
			return ParsingExpression.newFailure(this);
		}
		return this;
	}
}

class ParsingWithFlag extends ParsingOperation {
	String flagName;
	ParsingWithFlag(String flagName, ParsingExpression inner) {
		super("with", inner);
		this.flagName = flagName;
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		boolean removeWithout = false;
		if(flagMap != null && withoutMap.containsKey(flagName)) {
			withoutMap.remove(flagName);
			removeWithout = true;
		}
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(removeWithout) {
			withoutMap.put(flagName, flagName);
		}
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newWithFlag(flagName, e);
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		final boolean currentFlag = context.getFlag(this.flagName);
		context.setFlag(this.flagName, true);
		this.inner.debugMatch(context);
		context.setFlag(this.flagName, currentFlag);
		return !(context.isFailure());
	}
}

class ParsingWithoutFlag extends ParsingOperation {
	String flagName;
	ParsingWithoutFlag(String flagName, ParsingExpression inner) {
		super("without", inner);
		this.flagName = flagName;
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		boolean addWithout = false;
		if(flagMap != null && !withoutMap.containsKey(flagName)) {
			withoutMap.put(flagName, flagName);
			addWithout = true;
		}
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(addWithout) {
			withoutMap.remove(flagName);
		}
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newWithFlag(flagName, e);
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		final boolean currentFlag = context.getFlag(this.flagName);
		context.setFlag(this.flagName, false);
		this.inner.debugMatch(context);
		context.setFlag(this.flagName, currentFlag);
		return !(context.isFailure());
	}
}

class ParsingDebug extends ParsingOperation {
	protected ParsingDebug(ParsingExpression inner) {
		super("debug", inner);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, flagMap, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newDebug(e);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		ParsingObject left = context.left;
		this.inner.debugMatch(context);
		if(context.isFailure()) {
			assert(pos == context.getPosition());
			System.out.println(context.source.formatPositionLine("debug", context.getPosition(), "failure at pos=" + pos  + " in " + inner));
			left = null;
			return false;
		}
		if(context.left != left) {
			System.out.println(context.source.formatPositionLine("debug", pos,
				"transition #" + context.left.getTag() + " => #" + left.getTag() + " in " + inner));
			return true;
		}
		else if(context.getPosition() != pos) {
			System.out.println(context.source.formatPositionMessage("debug", pos,
				"consumed pos=" + pos + " => " + context.getPosition() + " in " + inner));
		}
		else {
			System.out.println(context.source.formatPositionLine("debug", pos, "pass and unconsumed at pos=" + pos + " in " + inner));
		}
		left = null;
		return true;
	}
}

class ParsingApply extends ParsingOperation {
	ParsingApply(ParsingExpression inner) {
		super("apply", inner);
	}
	@Override
	boolean hasObjectOperation() {
		return true;
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, UMap<String> flagMap, TreeMap<String,String> withoutMap) {
		//TODO;
		return null;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		ParsingContext s = new ParsingContext(context.left);
//		
//		this.inner.debugMatch(s);
//		context.opRememberPosition();
//		context.opRememberFailurePosition();
//		context.opStoreObject();
//		this.inner.debugMatch(context);
//		context.opDebug(this.inner);
		return !(context.isFailure());

	}
}



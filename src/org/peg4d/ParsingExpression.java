package org.peg4d;

import java.util.TreeMap;

abstract class ParsingMatcher {
	abstract boolean simpleMatch(ParsingContext context);
	String expectedToken() {
		return toString();
	}
}

public abstract class ParsingExpression extends ParsingMatcher {
	public  static boolean  VerboseStack = false;

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

	final boolean isAllUnique() {
		for(int i = 0; i < this.size(); i++) {
			if(!this.get(i).isUnique()) {
				return false;
			}
		}
		return true;
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
	
	static void dumpId(String indent, ParsingExpression e) {
		System.out.println(indent + e.uniqueId + " " + e);
		for(int i = 0; i < e.size(); i++) {
			dumpId(indent + " ", e.get(i));
		}
	}
	
//	ParsingExpression reduceOperation() {
//		ParsingExpression reduced = this.normalizeImpl(true, null, null);
////		if(reduced.getClass() != this.getClass()) {
////			System.out.println("@ " + this.getClass().getSimpleName() + " " + this + "\n\t=> " + reduced.getClass().getSimpleName() + " " + reduced);
////		}
//		return reduced;
//	}
	
	abstract ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap);
//	{
//		return this;
//	}
	
	protected abstract void visit(ParsingExpressionVisitor visitor);
	
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

	static int checkLeftRecursion(ParsingExpression e, String uName, int start, int minlen, UList<String> stack) {
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
					int nc = checkLeftRecursion(ne.deReference(), uName, start, minlen, stack);
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
				int nc = checkLeftRecursion(e.get(i), uName, start, minlen, stack);
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
				nc = checkLeftRecursion(eN, uName, start, nc, stack);
			}
			e.minlen = nc - minlen;
		}
		else if(e instanceof ParsingUnary) {
			int lmin = checkLeftRecursion(((ParsingUnary) e).inner, uName, start, minlen, stack); // skip count
			if(e instanceof ParsingOption || e instanceof ParsingRepetition || e instanceof ParsingNot || e instanceof ParsingAnd ) {
				e.minlen = 0;
			}
			else {
				e.minlen = lmin - minlen;
			}
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

	public final static ParsingFailure newFailure(ParsingMatcher e) {
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
			if(pe instanceof ParsingFailure) {
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
	
	public final static ParsingExpression newConstructor(boolean leftJoin, UList<ParsingExpression> l) {
		return checkUnique(new ParsingConstructor(leftJoin, l), isUnique(l));
	}

	public final static ParsingExpression newConstructor(boolean leftJoin, ParsingExpression p) {
		return newConstructor(leftJoin, toSequenceList(p));
	}

	public final static ParsingExpression newConstructor(ParsingExpression p) {
		return newConstructor(false, p);
	}

	public final static ParsingExpression newJoinConstructor(ParsingExpression p) {
		return newConstructor(true, p);
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
		return new ParsingFailure(message);
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
	
	static ParsingExpression uniqueExpression(String key, ParsingExpression e) {
		if(e.uniqueId == 0) {
			if(!e.isAllUnique()) {
				dumpId("debug ", e);
			}
			assert(e.isAllUnique());
			ParsingExpression u = uniqueMap.get(key);
			if(u == null) {
				u = e;
				e.po = null;
				e.uniqueId = uniqueMap.size() + 1;
				if(e instanceof NonTerminal) {
					
				}
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
		assert(this.inner.uniqueId != 0);
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
		return ParsingExpression.uniqueExpression("\b", this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitEmpty(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return true;
	}
}

class ParsingFailure extends ParsingExpression {
	String message;
	ParsingFailure(String message) {
		super();
		this.message = message;
	}
	ParsingFailure(ParsingMatcher m) {
		super();
		this.message = "expecting " + m;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniqueExpression("!!\b"+message, this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	short acceptByte(int ch) {
		return Reject;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.failure(this);
		return false;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitFailure(this);
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
			return ParsingExpression.uniqueExpression("'\b" + byteChar, this);
		}
		return ParsingExpression.uniqueExpression("'\b" + this.errorToken + "\b" + byteChar, this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	String expectedToken() {
		if(this.errorToken != null) {
			return this.errorToken;
		}
		return this.toString();
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
		return ParsingExpression.uniqueExpression(".\b", this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override 
	short acceptByte(int ch) {
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
		context.failure(this);
		return false;
	}
}

class NonTerminal extends ParsingExpression {
	String  uniqueName;
	Grammar peg;
	String  ruleName;
	boolean assertFlag = false;
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
		return ParsingExpression.uniqueExpression(getUniqueName(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		NonTerminal ne = this;
		ParsingRule rule = ne.getRule();
		String optName = ParsingRule.toOptionName(rule, lexOnly, withoutMap);
		if(ne.peg.getRule(optName) != rule) {
			ne.peg.makeOptionRule(rule, optName, lexOnly, withoutMap);
			ne = ne.peg.newNonTerminal(optName);
			//System.out.println(rule.ruleName + "@=>" + optName);
		}
		if(isExpectedConnector()) {
			 return ParsingExpression.newConnector(ne, -1);
		}
		return ne;
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
	protected void visit(ParsingExpressionVisitor visitor) {
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
		boolean b = this.deReference().matcher.simpleMatch(context);
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
		return ParsingExpression.uniqueExpression("''\b" + text, this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
		return ParsingExpression.uniqueExpression("[\b" + startByteChar + "-" + endByteChar, this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	void setCount(int[] count) {
		for(int c = startByteChar; c <= endByteChar; c++) {
			count[c]++;
		}
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
		return ParsingExpression.uniqueExpression("?\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newOption(e);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
		if(!this.inner.matcher.simpleMatch(context)) {
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
		return ParsingExpression.uniqueExpression("*\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newRepetition(e);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
			if(!this.inner.matcher.simpleMatch(context)) {
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
		return ParsingExpression.uniqueExpression("&\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newAnd(e);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitAnd(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		this.inner.matcher.simpleMatch(context);
		context.rollback(pos);
		return !context.isFailure();
	}
}

class ParsingNot extends ParsingUnary {
	ParsingNot(ParsingExpression e) {
		super(e);
	}
	@Override ParsingExpression uniquefyImpl() { 
		return ParsingExpression.uniqueExpression("!\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(true, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newNot(e);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
		if(this.inner.matcher.simpleMatch(context)) {
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
		return ParsingExpression.uniqueExpression(" \b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
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
			if(!(this.get(i).matcher.simpleMatch(context))) {
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
		return ParsingExpression.uniqueExpression("|\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, withoutMap);
			ParsingExpression.addChoice(l, e);
		}
		return ParsingExpression.newChoice(l);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
			if(this.get(i).matcher.simpleMatch(context)) {
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
			return ParsingExpression.uniqueExpression("@" + index + "\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniqueExpression("@\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(this.isRemovedOperation()) {
			lexOnly = true;
		}
		ParsingExpression e = this.inner.normalizeImpl(lexOnly, withoutMap);
		if(this.isNothingConnected() || lexOnly) {
			return e;
		}
		return ParsingExpression.newConnector(e, this.index);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitConnector(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingObject left = context.left;
		int mark = context.markObjectStack();
		if(this.inner.matcher.simpleMatch(context)) {
			if(context.left != left) {
				context.commitLinkLog(mark, context.left);
				context.logLink(left, this.index, context.left);
			}
			context.left = left;
			left = null;
			return true;
		}
		context.abortLinkLog(mark);			
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
		return ParsingExpression.uniqueExpression("#\b" + this.tag.key(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(lexOnly || this.isRemovedOperation()) {
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitTagging(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setTag(this.tag);
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
		return ParsingExpression.uniqueExpression("`\b" + this.value, this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(lexOnly || this.isRemovedOperation()) {
			return ParsingExpression.newEmpty();
		}
		return this;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitValue(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setValue(this.value);
		return true;
	}
}

class ParsingConstructor extends ParsingList {
	boolean leftJoin = false;
	int prefetchIndex = 0;
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
			return ParsingExpression.uniqueExpression("{@}\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniqueExpression("{}\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).normalizeImpl(lexOnly, withoutMap);
			ParsingExpression.addSequence(l, e);
		}
		ParsingExpression ne = (lexOnly) ? ParsingExpression.newSequence(l) : ParsingExpression.newConstructor(this.leftJoin, l);
		if(this.isExpectedConnector()) {
			ne = ParsingExpression.newConnector(ne, -1);
		}
		return ne;
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitConstructor(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long startIndex = context.getPosition();
//		ParsingObject left = context.left;
		for(int i = 0; i < this.prefetchIndex; i++) {
			if(!this.get(i).matcher.simpleMatch(context)) {
				context.rollback(startIndex);
				return false;
			}
		}
		int mark = context.markObjectStack();
		ParsingObject newnode = context.newParsingObject(startIndex, this);
		if(this.leftJoin) {
			context.lazyCommit(context.left);
			context.logLink(newnode, 0, context.left);
		}
		context.left = newnode;
		for(int i = this.prefetchIndex; i < this.size(); i++) {
			if(!this.get(i).matcher.simpleMatch(context)) {
				context.abortLinkLog(mark);
				context.rollback(startIndex);
				newnode = null;
				return false;
			}
		}
		newnode.setLength((int)(context.getPosition() - startIndex));
		//context.commitLinkLog2(newnode, startIndex, mark);
		//System.out.println("newnode: " + newnode.oid);
		context.left = newnode;
		newnode = null;
		return true;
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
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}
	String getParameters() {
		return "";
	}
	@Override final ParsingExpression uniquefyImpl() {
		return 	ParsingExpression.uniqueExpression("<"+this.funcName+this.getParameters(), this);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
		return 	ParsingExpression.uniqueExpression("<"+this.funcName+this.getParameters()+"+"+this.uniqueKey(), this);
	}
	public String getParameters() {
		return "";
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
		visitor.visitParsingOperation(this);
	}
	@Override
	public ParsingExpression getExpression() {
		return this.inner;
	}
}

//class ParsingFail extends ParsingFunction {
//	String message;
//	ParsingFail(String message) {
//		super("fail");
//		this.message = message;
//	}
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		context.failure(this);
//		return false;
//	}
//	@Override
//	short acceptByte(int ch) {
//		return Reject;
//	}
//}

class ParsingCatch extends ParsingFunction {
	ParsingCatch() {
		super("catch");
	}
	@Override
	public boolean hasObjectOperation() {
		return true;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.left.setSourcePosition(context.fpos);
		context.left.setValue(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
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
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		return inner.normalizeImpl(lexOnly, withoutMap);
	}
	@Override
	protected void visit(ParsingExpressionVisitor visitor) {
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
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
//		ParsingExpression e = inner.normalizeImpl(true, flagMap, withoutMap);
//		if(e == inner) {
//			return this;
//		}
//		return ParsingExpression.newMatch(e);
		return inner.normalizeImpl(true, withoutMap);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		boolean oldMode = context.setRecognitionMode(true);
//		ParsingObject left = context.left;
//		if(this.inner.matcher.simpleMatch(context)) {
//			context.setRecognitionMode(oldMode);
//			context.left = left;
//			left = null;
//			return true;
//		}
//		context.setRecognitionMode(oldMode);
//		left = null;
//		return false;
		return this.inner.matcher.simpleMatch(context);
	}
}

class ParsingIf extends ParsingFunction {
	public final static boolean OldFlag = false;
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
		if(ParsingIf.OldFlag) {
			Boolean f = context.getFlag(this.flagName);
			if(!context.isFlag(f)) {
				context.failure(null);
			}
			return !(context.isFailure());
		}
		return true;
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		if(withoutMap != null && withoutMap.containsKey(flagName)) {
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
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		boolean removeWithout = false;
		if(withoutMap != null && withoutMap.containsKey(flagName)) {
			withoutMap.remove(flagName);
			removeWithout = true;
		}
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
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
		if(ParsingIf.OldFlag) {
			final boolean currentFlag = context.getFlag(this.flagName);
			context.setFlag(this.flagName, true);
			boolean res = this.inner.matcher.simpleMatch(context);
			context.setFlag(this.flagName, currentFlag);
			return res;
		}
		return this.inner.matcher.simpleMatch(context);
	}
}

class ParsingWithoutFlag extends ParsingOperation {
	String flagName;
	ParsingWithoutFlag(String flagName, ParsingExpression inner) {
		super("without", inner);
		this.flagName = flagName;
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		boolean addWithout = false;
		if(withoutMap != null && !withoutMap.containsKey(flagName)) {
			withoutMap.put(flagName, flagName);
			addWithout = true;
		}
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(addWithout) {
			withoutMap.remove(flagName);
		}
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newWithoutFlag(flagName, e);
	}
	@Override
	public String getParameters() {
		return " " + this.flagName;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(ParsingIf.OldFlag) {
			final boolean currentFlag = context.getFlag(this.flagName);
			context.setFlag(this.flagName, false);
			boolean res = this.inner.matcher.simpleMatch(context);
			context.setFlag(this.flagName, currentFlag);
			return res;
		}
		return this.inner.matcher.simpleMatch(context);
	}
}

class ParsingBlock extends ParsingOperation {
	ParsingBlock(ParsingExpression e) {
		super("block", e);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newBlock(e);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		String indent = context.source.getIndentText(context.pos);
		int stackTop = context.pushTokenStack(PEG4d.Indent, indent);
		boolean b = this.inner.matcher.simpleMatch(context);
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
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
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
		if(this.inner.matcher.simpleMatch(context)) {
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


class ParsingDebug extends ParsingOperation {
	protected ParsingDebug(ParsingExpression inner) {
		super("debug", inner);
	}
	@Override
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		ParsingExpression e = inner.normalizeImpl(lexOnly, withoutMap);
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newDebug(e);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		if(ParsingExpression.VerboseStack) {
			long pos = context.getPosition();
			long fpos = context.fpos;
			boolean flag  = context.enableTrace;
			String htrace = context.headTrace;
			String ftrace = context.failureTrace;
			String finfo = context.failureInfo;
			context.fpos = 0;
			context.enableTrace = true;
			this.inner.matcher.simpleMatch(context);
			context.enableTrace = flag;
			if(context.isFailure()) {
				assert(pos == context.getPosition());
				System.out.println(context.source.formatPositionLine("trace", pos, "trying .. " + this.inner));				
				System.out.println(context.source.formatPositionLine("failed", context.fpos, "failed at " + context.failureInfo + " " + context.failureTrace));
				if(fpos >= context.fpos) {
					context.forgetFailure(fpos);
					context.failureTrace = ftrace;
					context.failureInfo = finfo;
				}
				return false;
			}
			context.fpos = fpos;
			context.headTrace = htrace;
			context.failureTrace = ftrace;
			context.failureInfo = finfo;
			return true;
		}
		else {
			return this.inner.matcher.simpleMatch(context);
		}
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
	ParsingExpression normalizeImpl(boolean lexOnly, TreeMap<String,String> withoutMap) {
		//TODO;
		return null;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
//		ParsingContext s = new ParsingContext(context.left);
//		
//		this.inner.matcher.simpleMatch(s);
//		context.opRememberPosition();
//		context.opRememberFailurePosition();
//		context.opStoreObject();
//		this.inner.matcher.simpleMatch(context);
//		context.opDebug(this.inner);
		return !(context.isFailure());

	}
}



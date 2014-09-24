package org.peg4d;

interface Matcher {
	boolean simpleMatch(ParsingContext context);
}

public abstract class ParsingExpression implements Matcher {
	public final static int LeftRecursion     = 1 << 20;
	public final static int HasSyntaxError    = 1 << 26;
	public final static int HasTypeError      = 1 << 27;

	int           flag       = 0;
	int           uniqueId   = 0;
	ParsingObject po      = null;
	int           minlen = -1;
	Matcher       matcher;
		
	protected ParsingExpression() {
		this.matcher = this;
	}
	
	public final boolean isOptimized() {
		return (this.matcher != this);
	}

	public final boolean isUnique() {
		return (this.uniqueId > 0);
	}

	ParsingExpression uniquefy() {
		ParsingExpression e = this.uniquefyImpl();
		assert(e.getClass() == this.getClass());
//		if(e.getClass() != this.getClass()) {
//			System.out.println("@@@@ " + this.getClass() + " " + this);
//		}
		return e;
	}
	abstract ParsingExpression uniquefyImpl();
	ParsingExpression reduceOperation() {
		ParsingExpression reduced = this.reduceOperationImpl();
//		if(reduced.getClass() != this.getClass()) {
//			System.out.println("@ " + this.getClass().getSimpleName() + " " + this + "\n\t=> " + reduced.getClass().getSimpleName() + " " + reduced);
//		}
		return reduced;
	}
	
	ParsingExpression reduceOperationImpl() {
		return this;
	}
	protected abstract void visit(ExpressionVisitor visitor);

//	static int cc = 0;
//	static UList<ParsingExpression> dstack = new UList<ParsingExpression>(new ParsingExpression[1024]);
	
	public final boolean debugMatch(ParsingContext c) {
//		int d = cc; cc++;
//		int dpos = dstack.size();
		int pos = (int)c.getPosition() ;
//		if(pos % (1024 * 1024) == 0) {
//			System.out.println("["+(pos/(1024 * 1024))+"] calling: " + this + " mark=" + c.markObjectStack() + " free" + Runtime.getRuntime().freeMemory());
//		}
//		dstack.add(this);
		boolean b = this.matcher.simpleMatch(c);
		if(this instanceof NonTerminal) {
//			System.out.println("["+pos+"] called: " + b + " "+ this);
		}
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
					//System.out.println(uName + " minlen=" + minlen + " @@ " + stack);
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
	
	static int ObjectContext    = 1 << 0;
	static int OperationContext = 1 << 1;
	static int FirstTransition  = 1 << 2;

	private static final boolean isStatus(int status, int uflag) {
		return ((status & uflag) == uflag);
	}

	private static final int setStatus(int status, int uflag) {
		return status | uflag;
	}
	
	static int typeCheckImpl(ParsingExpression e, int status) {
		if(e instanceof NonTerminal) {
			ParsingRule r = ((NonTerminal) e).getRule();
			int ruleType = r.type;
			if(ruleType == ParsingRule.ObjectRule) {
				if(isStatus(status, FirstTransition) && !e.is(HasTypeError)) {
					e.set(HasTypeError);
					e.report(ReportLevel.warning, "unexpected non-terminal transition");
				}
				status = setStatus(status, FirstTransition);
			}
			if(ruleType == ParsingRule.OperationRule) {
				if(!isStatus(status, OperationContext)) {
					e.report(ReportLevel.warning, "unexpected non-terminal operation");				
				}
				status = setStatus(status, FirstTransition);
			}
			return status;
		}
		if(e instanceof ParsingConstructor) {
			boolean LeftJoin = ((ParsingConstructor) e).leftJoin;
			if(!isStatus(status, ObjectContext) && !e.is(HasTypeError)) {
				e.set(HasTypeError);
				e.report(ReportLevel.warning, "unexpected constructor");
			}
			if(LeftJoin) {
				if(!isStatus(status, FirstTransition) && !e.is(HasTypeError)) {
					e.set(HasTypeError);
					e.report(ReportLevel.warning, "unspecific left object");
				}		
			}
			else {
				if(isStatus(status, FirstTransition) && !e.is(HasTypeError)) {
					e.set(HasTypeError);
					e.report(ReportLevel.warning, "unexpected constructor transition");
				}
			}
			int newstatus = OperationContext;
			for(int i = 0; i < e.size(); i++) {
				newstatus = typeCheckImpl(e.get(i), newstatus);
			}
			status = setStatus(status, FirstTransition);
		}
		if(e instanceof ParsingConnector) {
			if(!isStatus(status, OperationContext)) {
				e.report(ReportLevel.warning, "unexpected operation");				
			}
			int scope = typeCheckImpl(e.get(0), ObjectContext);
			if(!isStatus(scope, FirstTransition)) {
				e.report(ReportLevel.warning, "nothing is connected");
			}
			return status;
		}
		if(e instanceof ParsingTagging || e instanceof ParsingValue) {
			if(!isStatus(status, OperationContext)) {
				e.report(ReportLevel.warning, "unexpected operation");				
			}
			return status;
		}
		if(e instanceof ParsingSequence) {
			for(int i = 0; i < e.size(); i++) {
				status = typeCheckImpl(e.get(i), status);
			}
			return status;
		}
		if(e instanceof ParsingChoice) {
			int status0 = typeCheckImpl(e.get(0), status);
			if(!isStatus(status, FirstTransition) && isStatus(status0, FirstTransition)) {
				for(int i = 1; i < e.size(); i++) {
					int n = typeCheckImpl(e.get(i), status);
					if(!isStatus(n, FirstTransition) && !e.is(ParsingExpression.HasTypeError)) {
						e.set(ParsingExpression.HasTypeError);
						e.report(ReportLevel.warning, "expected transtion for " + e.get(i));
					}
				}
				return status0;
			}
			else if (!isStatus(status, FirstTransition)) {
				for(int i = 1; i < e.size(); i++) {
					int n = typeCheckImpl(e.get(i), status);
					if(isStatus(n, FirstTransition) && !e.is(ParsingExpression.HasTypeError)) {
						e.set(ParsingExpression.HasTypeError );
						e.report(ReportLevel.warning, "unexpected transtion for " + e.get(i));
					}
				}
				return status;
			}
			else {
				for(int i = 1; i < e.size(); i++) {
					typeCheckImpl(e.get(i), status);
				}
				return status0;
			}
		}
//		if(e instanceof ParsingMatch) {
//			((ParsingMatch) e).inner = ((ParsingMatch) e).inner.reduceOperation();
//			typeCheckImpl(((ParsingMatch) e).inner, 0);
//			return status;
//		}
		if(e instanceof ParsingNot || e instanceof ParsingMatch) {
			ParsingExpression reduced = ((ParsingNot) e).get(0).reduceOperation().uniquefy();
			if(reduced != e.get(0)) {
				((ParsingNot) e).inner = reduced;
			}
			typeCheckImpl(e.get(0), 0);
			return status;
		}
		if(e instanceof ParsingUnary) {
			return typeCheckImpl(((ParsingUnary) e).inner, status);
		}
		return status;
	}
	
	static void typeCheck(ParsingRule rule) {
		int result = typeCheckImpl(rule.expr, rule.type);
		if(rule.type == ParsingRule.ObjectRule) {
			if(!isStatus(result, FirstTransition)) {
				rule.report(ReportLevel.warning, "no constructor");
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
		return new ParsingSequence(l);
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
				Main.printVerbose("merging", pe);
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
		return new ParsingOption(p);
	}
	
	public final static ParsingExpression newMatch(ParsingExpression p) {
		return new ParsingMatch(p);
	}
		
	public final static ParsingExpression newRepetition(ParsingExpression p) {
//		if(p instanceof PCharacter) {
//			return new PZeroMoreCharacter(0, (PCharacter)p);
//		}
		return new ParsingRepetition(p);
	}

	public final static ParsingExpression newAnd(ParsingExpression p) {
		return new ParsingAnd(p);
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
		return new ParsingNot(p);
	}
		
	public final static ParsingExpression newChoice(UList<ParsingExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		return new ParsingChoice(l);
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
	
	public final static ParsingExpression newConstructor(ParsingExpression p) {
		ParsingExpression e = new ParsingConstructor(false, toSequenceList(p));
		return e;
	}

	public final static ParsingExpression newJoinConstructor(ParsingExpression p) {
		ParsingExpression e = new ParsingConstructor(true, toSequenceList(p));
		return e;
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
		return new ParsingConnector(p, index);
	}

	public final static ParsingExpression newTagging(ParsingTag tag) {
		return new ParsingTagging(tag);
	}

	public final static ParsingExpression newValue(String msg) {
		return new ParsingValue(msg);
	}
	
	public final static ParsingExpression newDebug(ParsingExpression e) {
		ParsingExpression e2 = new ParsingDebug(e);
		if(e.isUnique()) {
			e2 = e2.uniquefy();
		}
		return e2;
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
		return new ParsingWithFlag(flagName, e);
	}

	public final static ParsingExpression newWithoutFlag(String flagName, ParsingExpression e) {
		return new ParsingWithoutFlag(flagName, e);
	}

	public final static ParsingExpression newIndent() {
		return new ParsingIndent();
	}

	public final static ParsingExpression newBlock(ParsingExpression e) {
		return new ParsingBlock(e);
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
	ParsingExpression reduceOperationImpl() {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).reduceOperation();
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
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
		context.opFailure(dead);
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
		context.opFailure(this.errorToken);
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
		context.opFailure();
		return false;
	}
}

class NonTerminal extends ParsingExpression {
	Grammar peg;
	String  ruleName;
	private ParsingExpression    calling = null;
	NonTerminal(Grammar base, String ruleName) {
		super();
		this.peg = base;
		this.ruleName = ruleName;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy(getUniqueName(), this);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		if(!ParsingRule.isLexicalName(this.ruleName)) {
			String lexName = ParsingRule.toLexicalName(this.ruleName);
			NonTerminal ne = this.peg.newNonTerminal(lexName);
			if(ne.deReference() == null) {
				this.peg.getLexicalRule(this.ruleName);
			}
			return ne;
		}
		return this;
	}

	String getUniqueName() {
		return this.peg.uniqueRuleName(this.ruleName);
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
//		if(this.calling == null) {
//			System.out.println("Null Reference remains: " + this.ruleName + " next=" + this.flowNext);
//			//assert(this.calling != null);
//			this.checkReference();
//		}
		return this.deReference().debugMatch(context);
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
			context.opFailure();
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
		context.opFailure();
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
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
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
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
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
		long f = context.rememberFailure();
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
		context.forgetFailure(f);
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
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newAnd(e);
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitAnd(this);
	}
//	@Override
//	short acceptByte(int ch) {
//		short r = this.inner.acceptByte(ch);
//		if(r == Reject) {
//			return Reject;
//		}
//		return CheckNextFlow;
//	}
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
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
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
			context.opFailure(this);
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
	ParsingExpression reduceOperationImpl() {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[this.size()]);
		for(int i = 0; i < this.size(); i++) {
			ParsingExpression e = get(i).reduceOperation();
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
	ParsingExpression uniquefyImpl() {
		if(index != -1) {
			return ParsingExpression.uniquefy("@" + index + "\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniquefy("@\b" + this.uniqueKey(), this);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		return this.inner.reduceOperation();
	}
	@Override
	protected void visit(ExpressionVisitor visitor) {
		visitor.visitConnector(this);
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		ParsingObject left = context.left;
		if(!this.inner.debugMatch(context)) {
			return false;
		}
		if(context.canTransCapture() && context.left != left) {
			context.logLink(left, this.index, context.left);
		}
		context.left = left;
		left = null;
		return true;
	}
}

class ParsingTagging extends ParsingExpression {
	ParsingTag tag;
	ParsingTagging(ParsingTag tag) {
		super();
		this.tag = tag;
	}
	@Override
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy("#\b" + this.tag.key(), this);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		return new ParsingEmpty();
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
	ParsingExpression uniquefyImpl() {
		return ParsingExpression.uniquefy("`\b" + this.value, this);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		return new ParsingEmpty();
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
	ParsingExpression uniquefyImpl() {
		if(leftJoin) {
			return ParsingExpression.uniquefy("{@}\b" + this.uniqueKey(), this);
		}
		return ParsingExpression.uniquefy("{}\b" + this.uniqueKey(), this);
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
			ParsingObject newnode = context.newParsingObject(startIndex, this);
			context.left = newnode;
			if(this.leftJoin) {
				context.logLink(newnode, -1, left);
			}
			for(int i = this.prefetchIndex; i < this.size(); i++) {
				if(!this.get(i).debugMatch(context)) {
					context.abortLinkLog(mark);
					context.rollback(startIndex);
					left = null;
					return false;
				}
			}
			context.commitLinkLog(newnode, startIndex, mark);
			if(context.stat != null) {
				context.stat.countObjectCreation();
			}
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
		context.opIndent();
		return !context.isFailure();
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
		context.opFailure(this.message);
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
		context.opCatch();
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
	ParsingExpression reduceOperationImpl() {
		return inner.reduceOperation();
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

//class ParsingMemo extends ParsingOperation {
//	static ParsingObject NonTransition = new ParsingObject(null, null, 0);
//	boolean enableMemo = true;
//	int memoId;
//	int memoHit = 0;
//	int memoMiss = 0;
//
//	ParsingMemo(int memoId, ParsingExpression inner) {
//		super("memo", inner);
//		this.memoId = memoId;
//	}
//
//	@Override
//	ParsingExpression reduceOperation_() {
//		ParsingExpression e = inner.reduceOperation();
//		if(e == inner) {
//			return this;
//		}
//		System.out.println("TODO: memo");
//		return e;
//	}
//	
//	@Override
//	public boolean simpleMatch(ParsingContext context) {
//		if(!this.enableMemo) {
//			return this.inner.debugMatch(context);
//		}
//		long pos = context.getPosition();
//		MemoEntry m = context.getMemo(this, pos);
//		if(m != null) {
//			this.memoHit += 1;
//			context.setPosition(pos + m.consumed);
//			if(m.generated != NonTransition) {
//				context.left = m.generated;
//			}
//			return !(context.isFailure());
//		}
//		ParsingObject left = context.left;
//		this.inner.debugMatch(context);
//		int length = (int)(context.getPosition() - pos);
//		context.setMemo(pos, this, (context.left == left) ? NonTransition : context.left, length);
//		this.memoMiss += 1;
//		this.tryTracing();
//		left = null;
//		return !(context.isFailure());
//	}
//
//	private void tryTracing() {
//		if(Main.TracingMemo) {
//			if(this.memoMiss == 32) {
//				if(this.memoHit < 2) {
//					disabledMemo();
//					return;
//				}
//			}
//			if(this.memoMiss % 64 == 0) {
//				if(this.memoHit == 0) {
//					disabledMemo();
//					return;
//				}
//				if(this.memoMiss / this.memoHit > 10) {
//					disabledMemo();
//					return;
//				}
//			}
//		}		
//	}
//	
//	private void disabledMemo() {
//		//this.show();
//		this.enableMemo = false;
////		this.base.DisabledMemo += 1;
////		int factor = this.base.EnabledMemo / 10;
////		if(factor != 0 && this.base.DisabledMemo % factor == 0) {
////			this.base.memoRemover.removeDisabled();
////		}
//	}
//
//	void show() {
//		if(Main.VerboseMode) {
//			double f = (double)this.memoHit / this.memoMiss;
//			System.out.println(this.inner.getClass().getSimpleName() + " #h/m=" + this.memoHit + "," + this.memoMiss + ", f=" + f + " " + this.inner);
//		}
//	}
//}

class ParsingMatch extends ParsingOperation {
	ParsingMatch(ParsingExpression inner) {
		super("match", inner);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		return inner.reduceOperation();
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
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
		if(e == inner) {
			return this;
		}
		return ParsingExpression.newBlock(e);
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		context.opPushIndent();
		this.inner.debugMatch(context);
		context.opPopIndent();
		return !(context.isFailure());
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
}

class ParsingWithFlag extends ParsingOperation {
	String flagName;
	ParsingWithFlag(String flagName, ParsingExpression inner) {
		super("with", inner);
		this.flagName = flagName;
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
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
		context.opEnableFlag(this.flagName);
		this.inner.debugMatch(context);
		context.opPopFlag(this.flagName);
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
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
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
		context.opDisableFlag(this.flagName);
		this.inner.debugMatch(context);
		context.opPopFlag(this.flagName);
		return !(context.isFailure());
	}
}

class ParsingDebug extends ParsingOperation {
	protected ParsingDebug(ParsingExpression inner) {
		super("debug", inner);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
		ParsingExpression e = inner.reduceOperation();
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
		super("|apply", inner);
	}
	@Override
	ParsingExpression reduceOperationImpl() {
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


// --------------------------------------------------------------------------

//class POptionalString extends POptional {
//byte[] utf8;
//POptionalString(int flag, PString e) {
//	super(e);
//	this.utf8 = e.utf8;
//}
//@Override ParsingExpression dup() { 
//	return new POptionalString(flag, (PString)inner); 
//}
//@Override
//public boolean simpleMatch(ParsingContext context) {
//	if(context.source.match(context.pos, this.utf8)) {
//		context.consume(this.utf8.length);
//	}
//}
//}
//
//class POptionalByteChar extends POptional {
//int byteChar;
//POptionalByteChar(int flag, ParsingByte e) {
//	super(e);
//	this.byteChar = e.byteChar;
//}
//@Override ParsingExpression dup() { 
//	return new POptionalByteChar(flag, (ParsingByte)inner); 
//}
//@Override
//public boolean simpleMatch(ParsingContext context) {
//	context.opMatchOptionalByteChar(this.byteChar);
//}
//}
//
//class POptionalCharacter extends POptional {
//ParsingCharset charset;
//POptionalCharacter(int flag, PCharacter e) {
//	super(e);
//	this.charset = e.charset;
//}
//@Override ParsingExpression dup() { 
//	return new POptionalCharacter(flag, (PCharacter)inner); 
//}
//@Override
//public boolean simpleMatch(ParsingContext context) {
//	context.opMatchOptionalCharset(this.charset);
//}
//}
//class PZeroMoreCharacter extends PRepetition {
//	ParsingCharset charset;
//	PZeroMoreCharacter(int flag, PCharacter e) {
//		super(flag, e);
//		this.charset = e.charset;
//	}
//	@Override ParsingExpression dup() { 
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

//class PNotString extends PNot {
//byte[] utf8;
//PNotString(int flag, PString e) {
//	super(flag | ParsingExpression.NoMemo, e);
//	this.utf8 = e.utf8;
//}
//@Override ParsingExpression dup() { 
//	return new PNotString(flag, (PString)inner); 
//}
//@Override
//public boolean simpleMatch(ParsingContext context) {
//	context.opMatchTextNot(utf8);
//}
//}
//
//class PNotByteChar extends PNot {
//int byteChar;
//PNotByteChar(int flag, ParsingByte e) {
//	super(flag, e);
//	this.byteChar = e.byteChar;
//}
//@Override ParsingExpression dup() { 
//	return new PNotByteChar(flag, (ParsingByte)inner); 
//}
//@Override
//public boolean simpleMatch(ParsingContext context) {
//	context.opMatchByteCharNot(this.byteChar);
//}
//}	
//
//class PNotCharacter extends PNot {
//ParsingCharset charset;
//PNotCharacter(int flag, PCharacter e) {
//	super(flag | ParsingExpression.NoMemo, e);
//	this.charset = e.charset;
//}
//@Override ParsingExpression dup() { 
//	return new PNotCharacter(flag, (PCharacter)inner); 
//}
//@Override
//public boolean simpleMatch(ParsingContext context) {
//	context.opMatchCharsetNot(this.charset);
//}
//}


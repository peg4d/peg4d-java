package org.peg4d.expression;

import java.util.AbstractList;
import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.CharacterReader;
import org.peg4d.Main;
import org.peg4d.ParsingCharset;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingRule;
import org.peg4d.ParsingSource;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarGenerator;
import org.peg4d.pegcode.GrammarVisitor;
import org.peg4d.pegcode.PEG4dFormatter;

public abstract class ParsingExpression extends AbstractList<ParsingExpression> implements Recognizer {
		
	protected ParsingExpression() {
		this.matcher = this;
	}

	public Recognizer  matcher;
	public final boolean isOptimized() {
		return (this.matcher != this);
	}

	public int    internId   = 0;
	public ParsingObject po      = null;
	public final boolean isInterned() {
		return (this.internId > 0);
	}
	public abstract String getInterningKey();
	public final ParsingExpression intern() {
		return ParsingExpression.intern(this);
	}
	
	public boolean isAlwaysConsumed() {
		return this.checkAlwaysConsumed(null, null);
	}
	public abstract boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack);

	public final int inferNodeTransition() {
		return this.inferNodeTransition(null);
	}

	public abstract int inferNodeTransition(UMap<String> visited);
	public abstract ParsingExpression checkNodeTransition(NodeTransition c);
	
	public abstract ParsingExpression removeNodeOperator();
	public abstract ParsingExpression removeFlag(TreeMap<String,String> undefedFlags);	

	public static boolean hasReachableFlag(ParsingExpression e, String flagName, UMap<String> visited) {
		if(e instanceof ParsingWithFlag) {
			if(flagName.equals(((ParsingWithFlag) e).flagName)) {
				return false;
			}
		}
		for(ParsingExpression se : e) {
			if(hasReachableFlag(se, flagName, visited)) {
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
				return hasReachableFlag(r.expr, flagName, visited);
			}
		}
		return false;
	}

	public static boolean hasReachableFlag(ParsingExpression e, String flagName) {
		return hasReachableFlag(e, flagName, new UMap<String>());
	}
	
	
	public final static short Reject         = 0;
	public final static short Accept         = 1;
	public final static short Unconsumed     = 2;  // depending on the next
	
	public abstract short acceptByte(int ch);

	public abstract void visit(GrammarVisitor visitor);

	
	@Override
	public int size() {
		return 0;
	}
	
	@Override
	public ParsingExpression get(int index) {
		return null;
	}
	
	public final boolean debugMatch(ParsingContext c) {
//		int d = cc; cc++;
//		int dpos = dstack.size();
//		int pos = (int)c.getPosition() ;
//		if(pos % (1024 * 1024) == 0) {
//			System.out.println("["+(pos/(1024 * 1024))+"] calling: " + this + " mark=" + c.markObjectStack() + " free" + Runtime.getRuntime().freeMemory());
//		}
//		dstack.add(this);
		boolean b = this.matcher.match(c);
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
	
	private final static GrammarGenerator DefaultFormatter = new PEG4dFormatter();
	
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		DefaultFormatter.formatExpression(this, sb);
		return sb.toString();
	}
	
	public static int WarningLevel = 1;

	public final void report(ReportLevel level, String msg) {
		if(WarningLevel == 0 && level.compareTo(ReportLevel.error) != 0) {
			return;
		}
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
		}
//		else {
//			System.out.println("" + level.toString() + ": " + msg + " in " + this);
//		}
	}

	// ======================================================================
	
	
	// ======================================================================
	
	int           minlen = -1;

	public int checkLength(String ruleName, int start, int minlen, UList<String> stack) {
		return this.minlen + minlen;
	}

	static int ObjectContext    = 1 << 0;
	static int OperationContext = 1 << 1;
	
	public final static int LeftRecursion     = 1 << 10;
	public final static int HasSyntaxError    = 1 << 16;
	public final static int HasTypeError      = 1 << 17;

	public final static int DisabledOperation = 1 << 18;
	public final static int ExpectedConnector = 1 << 19;
	public final static int NothingConnected  = 1 << 20;
	public final static int RedundantUnary    = 1 << 21;

	int           flag       = 0;

	final boolean isExpectedConnector() {
		return (this.internId == 0 && this.is(ExpectedConnector));
	}

	final boolean isNothingConnected() {
		return (this.internId == 0 && this.is(NothingConnected));
	}

	final boolean isRemovedOperation() {
		return (this.internId == 0 && this.is(DisabledOperation));
	}
	
	public boolean hasObjectOperation() {
		return false;
	}
		
	static void dumpId(String indent, ParsingExpression e) {
		System.out.println(indent + e.internId + " " + e);
		for(int i = 0; i < e.size(); i++) {
			dumpId(indent + " ", e.get(i));
		}
	}

	public final boolean is(int uflag) {
		return ((this.flag & uflag) == uflag);
	}

	public void set(int uflag) {
		this.flag = this.flag | uflag;
	}

	public abstract ParsingExpression norm(boolean lexOnly, TreeMap<String,String> undefedFlags);	
		
//	private static boolean checkRecursion(String uName, UList<String> stack) {
//		for(int i = 0; i < stack.size() - 1; i++) {
//			if(uName.equals(stack.ArrayValues[i])) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public static int checkLeftRecursion(ParsingExpression e, String uName, int start, int minlen, UList<String> stack) {
//		if(e instanceof NonTerminal) {
//			NonTerminal ne = (NonTerminal) e;
//			ne.checkReference();
//			if(minlen == 0) {
//				String n = ne.getUniqueName();
//				if(n.equals(uName) && !e.is(LeftRecursion)) {
//					ParsingRule r = ne.getRule();
//					e.set(LeftRecursion);
//					e.report(ReportLevel.error, "left recursion: " + r);
//					r.peg.foundError = true;
//				}
//				if(!checkRecursion(n, stack)) {
//					int pos = stack.size();
//					stack.add(n);
//					int nc = checkLeftRecursion(ne.deReference(), uName, start, minlen, stack);
//					e.minlen = nc - minlen;
//					stack.clear(pos);
//				}
//				if(e.minlen == -1) {
//					e.minlen = 1; // FIXME: assuming no left recursion
//				}
//			}
//			else if(e.minlen == -1) {
//				e.minlen = 0;
//			}
//		}
//		if(e instanceof ParsingChoice) {
//			int lmin = Integer.MAX_VALUE;
//			for(int i = 0; i < e.size(); i++) {
//				int nc = checkLeftRecursion(e.get(i), uName, start, minlen, stack);
//				if(nc < lmin) {
//					lmin = nc;
//				}
//			}
//			e.minlen = lmin - minlen;
//		}
//		else if(e instanceof ParsingSequence || e instanceof ParsingConstructor) {
//			int nc = minlen;
//			for(int i = 0; i < e.size(); i++) {
//				ParsingExpression eN = e.get(i);
//				nc = checkLeftRecursion(eN, uName, start, nc, stack);
//			}
//			e.minlen = nc - minlen;
//		}
//		else if(e instanceof ParsingUnary) {
//			int lmin = checkLeftRecursion(((ParsingUnary) e).inner, uName, start, minlen, stack); // skip count
//			if(e instanceof ParsingOption || e instanceof ParsingRepetition || e instanceof ParsingNot || e instanceof ParsingAnd ) {
//				e.minlen = 0;
//			}
//			else {
//				e.minlen = lmin - minlen;
//			}
//		}
//		else {
//			if(e.minlen == -1) {
//				e.minlen = 0;
//			}
//		}
////		if(e.minlen == -1) {
////			System.out.println("@@@@ " + uName + "," + e);
////		}
//		assert(e.minlen != -1);
//		minlen += e.minlen;
//		return minlen;
//	}
	
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

	public static boolean containFlag(ParsingExpression e, String flagName) {
		return containFlag(e, flagName, new UMap<String>());
	}
	
	protected final static int checkObjectConstruction(ParsingExpression e, int status) {
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

	protected final static void checkObjectOperation(ParsingExpression e, int status) {
		if(status == ParsingRule.ReservedRule) {
			return;
		}
		if(status != OperationContext) {
			e.set(HasTypeError);
			e.set(DisabledOperation);
			e.report(ReportLevel.warning, "unspecific left object");
		}
	}

	static int typeCheckImpl(ParsingExpression e, int typeStatus, UMap<String> flagMap) {
		if(e instanceof NonTerminal) {
			ParsingRule r = ((NonTerminal) e).getRule();
			int ruleType = r.type;
			if(ruleType == ParsingRule.ObjectRule) {
				return checkObjectConstruction(e, typeStatus);
			}
			if(ruleType == ParsingRule.OperationRule) {
				checkObjectOperation(e, typeStatus);
			}
			return typeStatus;
		}
		if(e instanceof ParsingConstructor) {
			boolean LeftJoin = ((ParsingConstructor) e).leftJoin;
			if(LeftJoin) {
				checkObjectOperation(e, typeStatus);
			}
			else {
				typeStatus = checkObjectConstruction(e, typeStatus);
			}
			int newstatus = OperationContext;
			for(int i = 0; i < e.size(); i++) {
				newstatus = typeCheckImpl(e.get(i), newstatus, flagMap);
			}
			return typeStatus;
		}
		if(e instanceof ParsingConnector) {
			checkObjectOperation(e, typeStatus);
			int scope = typeCheckImpl(e.get(0), ObjectContext, flagMap);
			if(scope != OperationContext) {
				e.set(NothingConnected);
				e.report(ReportLevel.warning, "nothing is connected");
			}
			return typeStatus;
		}
		if(e instanceof ParsingTagging || e instanceof ParsingValue) {
			checkObjectOperation(e, typeStatus);
			return typeStatus;
		}
		if(e instanceof ParsingSequence) {
			for(int i = 0; i < e.size(); i++) {
				typeStatus = typeCheckImpl(e.get(i), typeStatus, flagMap);
			}
			return typeStatus;
		}
		if(e instanceof ParsingOption || e instanceof ParsingRepetition) {
			int r = typeCheckImpl(((ParsingUnary) e).inner, typeStatus, flagMap);
			if(r != typeStatus) {
				e.report(ReportLevel.warning, "mixed results");
			}
			return typeStatus;
		}
		if(e instanceof ParsingChoice) {
			int first = typeCheckImpl(e.get(0), typeStatus, flagMap);
			for(int i = 1; i < e.size(); i++) {
				int r = typeCheckImpl(e.get(i), typeStatus, flagMap);
				if(r != first) {
					e.get(i).report(ReportLevel.warning, "mixed type in the choice");
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
			return typeStatus;
		}
		if(e instanceof ParsingWithFlag) {
			ParsingWithFlag we = (ParsingWithFlag)e;
			if(!containFlag(we.inner, we.flagName)) {
				we.report(ReportLevel.warning, "no such a flag: " + we.flagName);
				we.set(RedundantUnary);
			}
			return typeCheckImpl(we.inner, typeStatus, flagMap);
		}
		if(e instanceof ParsingWithoutFlag) {
			ParsingWithoutFlag we = (ParsingWithoutFlag)e;
			if(!containFlag(we.inner, we.flagName)) {
				we.report(ReportLevel.warning, "no such a flag: " + we.flagName);
				we.set(RedundantUnary);
			}
			return typeCheckImpl(we.inner, typeStatus, flagMap);
		}
		if(e instanceof ParsingUnary) {
			return typeCheckImpl(((ParsingUnary) e).inner, typeStatus, flagMap);
		}
		return typeStatus;
	}

	public static void typeCheck(ParsingRule rule, UMap<String> flagMap) {
		int result = typeCheckImpl(rule.expr, rule.type, flagMap);
		if(rule.type == ParsingRule.ObjectRule) {
			if(result != OperationContext) {
				//rule.report(ReportLevel.warning, "object construction is expected: " + rule);
			}
		}
	}
	
	// ======================================================================
	// factory
	
	private static boolean Conservative = false;
	private static boolean StringSpecialization = true;
	private static boolean CharacterChoice      = true;
	
	public final static ParsingEmpty newEmpty() {
		return new ParsingEmpty();
	}

	public final static ParsingFailure newFailure(Recognizer e) {
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
		byte[] utf8 = ParsingSource.toUtf8(text);
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
		byte[] b = ParsingSource.toUtf8(String.valueOf((char)c));
		byte[] b2 = ParsingSource.toUtf8(String.valueOf((char)c2));
		if(equalsBase(b, b2)) {
			return newUnicodeRange(b, b2, token);
		}
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[b.length]);
		b2 = b;
		for(int pc = c + 1; pc <= c2; pc++) {
			byte[] b3 = ParsingSource.toUtf8(String.valueOf((char)pc));
			if(equalsBase(b, b3)) {
				b2 = b3;
				continue;
			}
			l.add(newUnicodeRange(b, b2, token));
			b = b3;
			b2 = b3;
		}
		b2 = ParsingSource.toUtf8(String.valueOf((char)c2));
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
		return checkUnique(new ParsingOption(p), p.isInterned());
	}
	
	public final static ParsingExpression newMatch(ParsingExpression p) {
		return checkUnique(new ParsingMatch(p), p.isInterned());
	}
		
	public final static ParsingExpression newRepetition(ParsingExpression p) {
		return checkUnique(new ParsingRepetition(p), p.isInterned());
	}

	public final static ParsingExpression newAnd(ParsingExpression p) {
		return checkUnique(new ParsingAnd(p), p.isInterned());
	}
	
	public final static ParsingExpression newNot(ParsingExpression p) {
		return checkUnique(new ParsingNot(p), p.isInterned());
	}
		
	public final static ParsingExpression newChoice(UList<ParsingExpression> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		if(l.size() == 2 && l.ArrayValues[1] instanceof ParsingEmpty) {
			return newOption(l.ArrayValues[0]);  //     e / '' => e?
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
			if(!l.ArrayValues[i].isInterned()) {
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
		UList<ParsingExpression> l;
		if(e instanceof ParsingSequence) {
			l = new UList<ParsingExpression>(new ParsingExpression[e.size()]);
			for(int i = 0; i < e.size(); i++) {
				l.add(e.get(i));
			}
		}
		else {
			l = new UList<ParsingExpression>(new ParsingExpression[1]);
			l.add(e);
		}
		return l;
	}
		
	public final static ParsingExpression newConnector(ParsingExpression p, int index) {
		return checkUnique(new ParsingConnector(p, index), p.isInterned());
	}

	public final static ParsingExpression newTagging(ParsingTag tag) {
		return new ParsingTagging(tag);
	}

	public final static ParsingExpression newValue(String msg) {
		return new ParsingValue(msg);
	}
	
	public final static ParsingExpression newDebug(ParsingExpression e) {
		return checkUnique(new ParsingAssert(e), e.isInterned());
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
		return checkUnique(new ParsingWithFlag(flagName, e), e.isInterned());
	}

	public final static ParsingExpression newWithoutFlag(String flagName, ParsingExpression e) {
		return checkUnique(new ParsingWithoutFlag(flagName, e), e.isInterned());
	}

	public final static ParsingExpression newBlock(ParsingExpression e) {
		return checkUnique(new ParsingBlock(e), e.isInterned());
	}

	public final static ParsingExpression newIndent() {
		return new ParsingIndent();
	}

//	public final static ParsingExpression newPermutation(UList<ParsingExpression> l) {
//		if(l.size() == 0) {
//			return newEmpty();
//		}
//		if(l.size() == 1) {
//			return l.ArrayValues[0];
//		}
//		return new ParsingPermutation(l);
//	}
	
	public final static ParsingExpression newScan(int number, ParsingExpression scan, ParsingExpression repeat) {
		return new ParsingScan(number, scan, repeat);
	}
	
	public final static ParsingExpression newRepeat(ParsingExpression e) {
		return new ParsingRepeat(e);
	}

	public static ParsingExpression newDef(int tagId, ParsingExpression e) {
		return checkUnique(new ParsingDef(tagId, e), e.isInterned());
	}

	public static ParsingExpression newIs(int tagId) {
		return new ParsingIs(tagId);
	}
	
	public static ParsingExpression newIsa(int tagId) {
		return new ParsingIsa(tagId);
	}

	public final static UMap<ParsingExpression> uniqueMap = new UMap<ParsingExpression>();
	
//	static ParsingExpression intern(String key, ParsingExpression e) {
//		if(e.internId == 0) {
//			if(!e.isAllUnique()) {
//				dumpId("debug ", e);
//			}
//			assert(e.isAllUnique());
//			ParsingExpression u = uniqueMap.get(key);
//			if(u == null) {
//				u = e;
//				e.po = null;
//				e.internId = uniqueMap.size() + 1;
//				uniqueMap.put(key, e);
//			}
//			assert(u.getClass() == e.getClass());
//			return u;
//		}
//		return e;
//	}

	static ParsingExpression intern(ParsingExpression e) {
		if(e.internId == 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(e.getInterningKey());
			for(int i = 0; i < e.size(); i++) {
				ParsingExpression sube = e.get(i);
				if(!sube.isInterned()) {
					sube = ParsingExpression.intern(sube);
					e.set(i, sube);
				}
				sb.append("#" + sube.internId);
			}
			String key = sb.toString();
			ParsingExpression u = uniqueMap.get(key);
			if(u == null) {
				u = e;
				e.po = null;
				e.internId = uniqueMap.size() + 1;
				uniqueMap.put(key, e);
			}
			assert(u.getClass() == e.getClass());
			return u;
		}
		return e;
	}
	private static ParsingExpression checkUnique(ParsingExpression e, boolean unique) {
		if(unique) {
			e = e.intern();
		}
		return e;
	}
	public static ParsingExpression dupUnary(ParsingUnary u, ParsingExpression e) {
		if(u.inner != e) {
			if(u instanceof ParsingNot) {
				return newNot(e);
			}
			if(u instanceof ParsingOption) {
				return newOption(e);
			}
			if(u instanceof ParsingRepetition) {
				return newRepetition(e);
			}
			if(u instanceof ParsingAnd) {
				return newAnd(e);
			}
			if(u instanceof ParsingConnector) {
				return newConnector(e, ((ParsingConnector) u).index);
			}
			throw new RuntimeException("undefined unary: " + u.getClass().getSimpleName());
		}
		return u;
	}
}




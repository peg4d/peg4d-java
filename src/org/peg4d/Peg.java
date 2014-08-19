package org.peg4d;
import org.peg4d.MemoMap.ObjectMemo;

public abstract class Peg {
	public final static int CyclicRule       = 1;
	public final static int HasNonTerminal    = 1 << 1;
	public final static int HasString         = 1 << 2;
	public final static int HasCharacter      = 1 << 3;
	public final static int HasAny            = 1 << 4;
	public final static int HasRepetation     = 1 << 5;
	public final static int HasOptional       = 1 << 6;
	public final static int HasChoice         = 1 << 7;
	public final static int HasAnd            = 1 << 8;
	public final static int HasNot            = 1 << 9;
	
	public final static int HasNewObject      = 1 << 10;
	public final static int HasSetter         = 1 << 11;
	public final static int HasTagging        = 1 << 12;
	public final static int HasMessage        = 1 << 13;
	public final static int HasContext        = 1 << 14;
	public final static int HasReserved       = 1 << 15;
	public final static int hasReserved2       = 1 << 16;
	public final static int Mask = HasNonTerminal | HasString | HasCharacter | HasAny
	                             | HasRepetation | HasOptional | HasChoice | HasAnd | HasNot
	                             | HasNewObject | HasSetter | HasTagging | HasMessage 
	                             | HasReserved | hasReserved2 | HasContext;
	public final static int LeftObjectOperation    = 1 << 17;
	public final static int PossibleDifferentRight = 1 << 18;
	
	public final static int NoMemo            = 1 << 20;
	public final static int Debug             = 1 << 24;
	
	Grammar    base;
	int        flag       = 0;
	short      uniqueId   = 0;
	short      semanticId = 0;
	int        refc       = 0;
	
	ParsingSource source = null;
	int          sourcePosition = 0;
	
	protected Peg(Grammar base, int flag) {
		this.base = base;
		this.flag = flag;
		base.definedExpressionList.add(this);
		this.uniqueId = (short)base.definedExpressionList.size();
		this.semanticId = this.uniqueId;
	}
		
	//protected abstract Peg clone(Grammar base, PegTransformer tr);
	protected abstract void visit(PegVisitor probe);
	public Peg getExpression() {
		return this;
	}
	public abstract ParsingObject simpleMatch(ParsingObject left, ParserContext context);
	//public abstract int fastMatch(int left, MonadicParser context);

	boolean acceptC1(int ch) {
		return true;
	}

	public final boolean is(int uflag) {
		return ((this.flag & uflag) == uflag);
	}

	public void set(int uflag) {
		this.flag = this.flag | uflag;
	}

	protected void derived(Peg e) {
		this.flag |= (e.flag & Peg.Mask);
	}
	
	public String key() {
		return "#" + this.uniqueId;
	}
	
	public int size() {
		return 0;
	}
	public Peg get(int index) {
		return this;  // to avoid NullPointerException
	}
	
	public Peg get(int index, Peg def) {
		return def;
	}

	String nameAt(int index) {
		return null;
	}

	private final static Formatter DefaultFormatter = new Formatter();
	@Override public String toString() {
		UStringBuilder sb = new UStringBuilder();
		DefaultFormatter.format(this, sb);
		return sb.toString();
	}

	public final String format(String name, Formatter fmt) {
		UStringBuilder sb = new UStringBuilder();
		fmt.formatRule(name, this, sb);
		return sb.toString();
	}

	public final String format(String name) {
		return this.format(name, new Formatter());
	}

	protected final void report(String type, String msg) {
		if(Main.StatLevel == 0) {
			if(this.source != null) {
				System.out.println(this.source.formatErrorMessage(type, this.sourcePosition-1, msg));
			}
			else {
				System.out.println(type + ": " + msg + "\n\t" + this);
			}
		}
	}
	
	protected void warning(String msg) {
		if(Main.VerbosePeg && Main.StatLevel == 0) {
			Main._PrintLine("PEG warning: " + msg);
		}
	}
	
	public final boolean hasObjectOperation() {
		return this.is(Peg.HasNewObject) || this.is(Peg.HasSetter) || this.is(Peg.HasTagging) || this.is(Peg.HasMessage);
	}

}

abstract class PegTerm extends Peg {
	public PegTerm (Grammar base, int flag) {
		super(base, flag);
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final Peg get(int index) {
		return this;  // just avoid NullPointerException
	}
}

class PegNonTerminal extends PegTerm {
	String symbol;
	Peg    jumpExpression = null;
	int    length = -1;
	
	PegNonTerminal(Grammar base, int flag, String ruleName) {
		super(base, flag | Peg.HasNonTerminal | Peg.NoMemo);
		this.symbol = ruleName;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNonTerminal(this);
	}
	@Override boolean acceptC1(int ch) {
		if(this.jumpExpression != null) {
			return this.jumpExpression.acceptC1(ch);
		}
		return true;
	}
	final Peg getNext() {
		if(this.jumpExpression == null) {
			return this.base.getExpression(this.symbol);
		}
		return this.jumpExpression;
	}
	public boolean isForeignNonTerminal() {
		return this.jumpExpression.base != this.base;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		return this.jumpExpression.simpleMatch(left, context);
	}
}

class PegString extends PegTerm {
	String text;
	byte[] textByte;
	public PegString(Grammar base, int flag, String text) {
		super(base, Peg.HasString | Peg.NoMemo | flag);
		this.text = text;
		textByte = UCharset.toUtf8(text);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitString(this);
	}
	@Override boolean acceptC1(int ch) {
		if(this.text.length() == 0) {
			return true;
		}
		return UCharset.getFirstChar(this.textByte) == ch;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(context.match(this.textByte)) {
			return left;
		}
		return context.foundFailure(this);
	}
}

class PegString1 extends PegString {
	int symbol1;
	public PegString1(Grammar base, int flag, String token) {
		super(base, flag, token);
		this.symbol1 = this.textByte[0] & 0xff;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		if(context.charAt(pos) == this.symbol1) {
			context.consume(1);
			return left;
		}
		return context.foundFailure(this);
	}
}

class PegString2 extends PegString {
	int symbol1;
	int symbol2;
	public PegString2(Grammar base, int flag, String token) {
		super(base, flag, token);
		this.symbol1 = this.textByte[0] & 0xff;
		this.symbol2 = this.textByte[1] & 0xff;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		if(context.charAt(pos) == this.symbol1 && context.charAt(pos+1) == this.symbol2) {
			context.consume(2);
			return left;
		}
		return context.foundFailure(this);
	}
}

class PegAny extends PegTerm {
	public PegAny(Grammar base, int flag) {
		super(base, Peg.HasAny | Peg.NoMemo | flag);
	}
	@Override boolean acceptC1(int ch) {
		return true;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitAny(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(context.hasChar()) {
			context.consume(1);
			return left;
		}
		return context.foundFailure(this);
	}
}

class PegNotAny extends PegTerm {
	PegNot not;
	Peg exclude;
	Peg orig;
	public PegNotAny(Grammar base, int flag, PegNot e, Peg orig) {
		super(base, flag | Peg.NoMemo);
		this.not = e;
		this.exclude = e.inner;
		this.orig = orig;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNotAny(this);
	}
	@Override boolean acceptC1(int ch) {
		return this.not.acceptC1(ch) && this.orig.acceptC1(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		ParsingObject right = this.exclude.simpleMatch(left, context);
		if(right.isFailure()) {
			assert(pos == context.getPosition());
			if(context.hasChar()) {
				context.consume(1);
				return left;
			}
		}
		else {
			context.rollback(pos);
		}
		return context.foundFailure(this);
	}
}

class PegCharacter extends PegTerm {
	UCharset charset;
	public PegCharacter(Grammar base, int flag, UCharset charset) {
		super(base, Peg.HasCharacter | Peg.NoMemo | flag);
		this.charset = charset;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitCharacter(this);
	}
	@Override boolean acceptC1(int ch) {
		return this.charset.match(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		int ch = context.getChar();
		if(!this.charset.match(ch)) {
			return context.foundFailure(this);
		}
		context.consume(1);
		return left;
	}
}

//class PegUtf8Range extends PegTerm {
//	char fromChar;
//	char toChar;
//	public PegUtf8Range(Grammar base, int flag, char fromChar, char toChar) {
//		super(base, Peg.HasCharacter | Peg.NoMemo | flag);
//		this.fromChar = fromChar;
//		this.toChar   = toChar;
//	}
//	@Override
//	protected void visit(PegVisitor probe) {
//		probe.visitUtf8Range(this);
//	}
//	@Override boolean acceptC1(int ch) {
//		return this.charset.match(ch);
//	}
//	@Override
//	public Pego simpleMatch(Pego left, ParserContext context) {
//		int ch = context.getChar();
//		if(!this.charset.match(ch)) {
//			return context.foundFailure(this);
//		}
//		context.consume(1);
//		return left;
//	}
//}

abstract class PegUnary extends Peg {
	Peg inner;
	public PegUnary(Grammar base, int flag, Peg e) {
		super(base, flag);
		this.inner = e;
		this.derived(e);
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final Peg get(int index) {
		return this.inner;
	}
}

class PegOptional extends PegUnary {
	public PegOptional(Grammar base, int flag, Peg e) {
		super(base, flag | Peg.HasOptional | Peg.NoMemo, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitOptional(this);
	}
	@Override boolean acceptC1(int ch) {
		return true;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		ParsingObject right = this.inner.simpleMatch(left, context);
		if(right.isFailure()) {
			assert(pos == context.getPosition());
//			context.setPosition(pos);
			return left;
		}
		return right;
	}
}

class PegOptionalString extends PegOptional {
	byte[] textByte;
	public PegOptionalString(Grammar base, int flag, PegString e) {
		super(base, flag | Peg.NoMemo, e);
		this.textByte = e.textByte;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		context.match(this.textByte);
		return left;
	}
}

class PegOptionalString1 extends PegOptional {
	int symbol1;
	public PegOptionalString1(Grammar base, int flag, PegString1 e) {
		super(base, flag | Peg.NoMemo, e);
		this.symbol1 = e.symbol1;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		context.match(this.symbol1);
		return left;
	}
}

class PegOptionalCharacter extends PegOptional {
	UCharset charset;
	public PegOptionalCharacter(Grammar base, int flag, PegCharacter e) {
		super(base, flag | Peg.NoMemo, e);
		this.charset = e.charset;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		context.match(this.charset);
		return left;
	}
}


class PegRepeat extends PegUnary {
	public int atleast = 0; 
	protected PegRepeat(Grammar base, int flag, Peg e, int atLeast) {
		super(base, flag | Peg.HasRepetation, e);
		this.atleast = atLeast;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitRepeat(this);
	}
	@Override boolean acceptC1(int ch) {
		if(this.atleast > 0) {
			return this.inner.acceptC1(ch);
		}
		return true;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long ppos = -1;
		long pos = context.getPosition();
		int count = 0;
		while(ppos < pos) {
			ParsingObject right = this.inner.simpleMatch(left, context);
			if(right.isFailure()) {
				break;
			}
			left = right;
			ppos = pos;
			pos = context.getPosition();
			count = count + 1;
		}
		if(count < this.atleast) {
			return context.foundFailure(this);
		}
		return left;
	}
}

class PegOneMoreCharacter extends PegRepeat {
	UCharset charset;
	public PegOneMoreCharacter(Grammar base, int flag, PegCharacter e) {
		super(base, flag, e, 1);
		charset = e.charset;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		int ch = context.charAt(pos);
		if(!this.charset.match(ch)) {
			return context.foundFailure(this);
		}
		pos++;
		for(;context.hasChar();pos++) {
			ch = context.charAt(pos);
			if(!this.charset.match(ch)) {
				break;
			}
		}
		context.setPosition(pos);
		return left;
	}
}

class PegZeroMoreCharacter extends PegRepeat {
	UCharset charset;
	public PegZeroMoreCharacter(Grammar base, int flag, PegCharacter e) {
		super(base, flag, e, 0);
		this.charset = e.charset;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		for(;context.hasChar();pos++) {
			int ch = context.charAt(pos);
			if(!this.charset.match(ch)) {
				break;
			}
		}
		context.setPosition(pos);
		return left;
	}
}

class PegAnd extends PegUnary {
	PegAnd(Grammar base, int flag, Peg e) {
		super(base, flag | Peg.HasAnd, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitAnd(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		ParsingObject right = this.inner.simpleMatch(left, context);
		context.rollback(pos);
		return right;
	}
	@Override
	boolean acceptC1(int ch) {
		return this.inner.acceptC1(ch);
	}
}

class PegNot extends PegUnary {
	PegNot(Grammar base, int flag, Peg e) {
		super(base, Peg.HasNot | flag, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNot(this);
	}
	@Override
	boolean acceptC1(int ch) {
		return !this.inner.acceptC1(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		ParsingObject right = this.inner.simpleMatch(left, context);
		if(right.isFailure()) {
			return left;
		}
		context.rollback(pos);
		return context.foundFailure(this);
	}
}

class PegNotString extends PegNot {
	byte[] textBuffer;
	public PegNotString(Grammar peg, int flag, PegString e) {
		super(peg, flag | Peg.NoMemo, e);
		this.textBuffer = e.textByte;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		if(context.match(this.textBuffer)) {
			//context.setPosition(pos);
			return context.foundFailure(this);
		}
		return left;
	}
}

class PegNotString1 extends PegNotString {
	int symbol;
	public PegNotString1(Grammar peg, int flag, PegString1 e) {
		super(peg, flag, e);
		this.symbol = e.symbol1;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(this.symbol == context.getChar()) {
			return context.foundFailure(this);
		}
		return left;
	}
}	

class PegNotString2 extends PegNotString {
	int symbol1;
	int symbol2;
	public PegNotString2(Grammar peg, int flag, PegString2 e) {
		super(peg, flag, e);
		this.symbol1 = e.symbol1;
		this.symbol2 = e.symbol2;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		if(this.symbol1 == context.charAt(pos) && this.symbol2 == context.charAt(pos+1)) {
			return context.foundFailure(this);
		}
//		if(this.nextAny) {
//			return this.matchNextAny(left, context);
//		}
		return left;
	}
}


class PegNotCharacter extends PegNot {
	UCharset charset;
	public PegNotCharacter(Grammar base, int flag, PegCharacter e) {
		super(base, flag | Peg.NoMemo, e);
		this.charset = e.charset;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		if(context.match(this.charset)) {
			context.setPosition(pos);
			return context.foundFailure(this);
		}
		return left;
	}
}

abstract class PegList extends Peg {
	UList<Peg> list;
	String[] nonTerminaNames = null;
	int length = 0;
	PegList(Grammar base, int flag, UList<Peg> list) {
		super(base, flag);
		this.list = list;
		this.indexName();
	}
	void indexName() {
		for(int i = 0; i < this.size(); i++) {
			Peg e = this.get(i);
			if(e instanceof PegNonTerminal) {
				if(this.nonTerminaNames == null) {
					this.nonTerminaNames = new String[this.size()];
				}
				this.nonTerminaNames[i] = ((PegNonTerminal) e).symbol;
			}
		}
	}
	@Override
	String nameAt(int index) {
		if(this.nonTerminaNames != null) {
			return this.nonTerminaNames[index];
		}
		return null;
	}
	@Override
	public final int size() {
		return this.list.size();
	}
	@Override
	public final Peg get(int index) {
		return this.list.ArrayValues[index];
	}
	public final void set(int index, Peg e) {
		this.list.ArrayValues[index] = e;
	}
	@Override
	public final Peg get(int index, Peg def) {
		if(index < this.size()) {
			return this.list.ArrayValues[index];
		}
		return def;
	}
//	public void add(Peg e) {
//		this.list.add(e);
//	}

	private boolean isOptional(Peg e) {
		if(e instanceof PegOptional) {
			return true;
		}
		if(e instanceof PegRepeat && ((PegRepeat) e).atleast == 0) {
			return true;
		}
		return false;
	}

	private boolean isUnconsumed(Peg e) {
		if(e instanceof PegNot && e instanceof PegAnd) {
			return true;
		}
		if(e instanceof PegString && ((PegString)e).textByte.length == 0) {
			return true;
		}
		if(e instanceof PegIndent) {
			return true;
		}
		return false;
	}
		
	@Override
	boolean acceptC1(int ch) {
		for(int start = 0; start < this.size(); start++) {
			Peg e = this.get(start);
			if(e instanceof PegTagging || e instanceof PegMessage) {
				continue;
			}
			if(this.isOptional(e)) {
				if(((PegUnary)e).inner.acceptC1(ch)) {
					return true;
				}
				continue;  // unconsumed
			}
			if(this.isUnconsumed(e)) {
				if(!e.acceptC1(ch)) {
					return false;
				}
				continue;
			}
			return e.acceptC1(ch);
		}
		return true;
	}

	public final Peg trim() {
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
		Peg e = this.list.ArrayValues[i];
		this.list.ArrayValues[i] = this.list.ArrayValues[j];
		this.list.ArrayValues[j] = e;
	}
}

class PegSequence extends PegList {
	PegSequence(Grammar base, int flag, UList<Peg> l) {
		super(base, flag, l);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitSequence(this);
	}	
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			ParsingObject right = this.get(i).simpleMatch(left, context);
			if(right.isFailure()) {
				context.rollbackObjectStack(mark);
				context.rollback(pos);
				return right;
			}
			left = right;
		}
		return left;
	}
}

class PegChoice extends PegList {
//	PegChoice(Grammar base, int flag, int initsize) {
//		super(base, flag | Peg.HasChoice, initsize);
//	}	
	PegChoice(Grammar base, int flag, UList<Peg> list) {
		super(base, flag | Peg.HasChoice, list);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitChoice(this);
	}
	
	@Override
	boolean acceptC1(int ch) {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).acceptC1(ch)) {
				return true;
			}
		}
		return false;
	}
			
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		for(int i = 0; i < this.size(); i++) {
			ParsingObject right = this.get(i).simpleMatch(left, context);
			if(!right.isFailure()) {
				return right;
			}
		}
		return context.foundFailure(this);
	}
}


class PegSelectiveChoice extends PegChoice {
	PegSelectiveChoice(Grammar base, int flag, UList<Peg> list) {
		super(base, flag, list);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		int ch = context.getChar();
		if(this.caseOf == null) {
			tryPrediction();
		}
		return caseOf[ch].simpleMatch(left, context);
	}

	Peg[] caseOf = null;

	void tryPrediction() {
		if(this.caseOf == null) {
			this.caseOf = new Peg[UCharset.MAX];
			Peg failed = new PegAlwaysFailure(this);
			for(int ch = 0; ch < UCharset.MAX; ch++) {
				this.caseOf[ch] = selectC1(ch, failed);
			}
			this.base.PredictionOptimization += 1;
		}
	}
	
	private Peg selectC1(int ch, Peg failed) {
		Peg e = null;
		UList<Peg> l = null; // new UList<Peg>(new Peg[2]);
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).acceptC1(ch)) {
				if(e == null) {
					e = this.get(i);
				}
				else {
					if(l == null) {
						l = new UList<Peg>(new Peg[2]);
						l.add(e);
					}
					l.add(get(i));
				}
			}
		}
		if(l != null) {
			e = new PegChoice(e.base, 0, l);
			e.base.UnpredicatedChoiceL1 += 1;
		}
		else {
			if(e == null) {
				e = failed;
			}
			e.base.PredicatedChoiceL1 +=1;
		}
		return e;
	}
}

class PegWordChoice extends PegChoice {
	UCharset charset = null;
	UList<byte[]> wordList = null;
	PegWordChoice(Grammar base, int flag, UList<Peg> list) {
		super(base, flag | Peg.HasChoice, list);
		this.wordList = new UList<byte[]>(new byte[list.size()][]);
		for(int i = 0; i < list.size(); i++) {
			Peg se = list.ArrayValues[i];
			if(se instanceof PegString1) {
				if(charset == null) {
					charset = new UCharset("");
				}
				charset.append(((PegString1)se).symbol1);
			}
			if(se instanceof PegCharacter) {
				if(charset == null) {
					charset = new UCharset("");
				}
				charset.append(((PegCharacter)se).charset);
			}
			if(se instanceof PegString) {
				wordList.add(((PegString)se).textByte);
			}
		}
	}
	
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(this.charset != null) {
			if(context.match(this.charset)) {
				return left;
			}
		}
		for(int i = 0; i < this.wordList.size(); i++) {
			if(context.match(this.wordList.ArrayValues[i])) {
				return left;
			}
		}
		return context.foundFailure(this);
	}
	
}

class PegAlwaysFailure extends PegString {
	public PegAlwaysFailure(Peg orig) {
		super(orig.base, 0, "\0");
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		return context.foundFailure(this);
	}
}

class PegSetter extends PegUnary {
	public int index;
	public PegSetter(Grammar base, int flag, Peg e, int index) {
		super(base, flag | Peg.HasSetter | Peg.NoMemo, e);
		this.index = index;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitSetter(this);
	}
	@Override
	boolean acceptC1(int ch) {
		return this.inner.acceptC1(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = left.getSourcePosition();
//		if(this.inner instanceof PegNonTerminal) {
//			System.out.println("label=" + this.inner);
//		}
		ParsingObject node = this.inner.simpleMatch(left, context);
		if(node.isFailure() || left == node) {
			return node;
		}
		if(context.isRecognitionOnly()) {
			left.setSourcePosition(pos);
		}
		else {
			context.logSetter(left, this.index, node);
		}
		return left;
	}
}

class PegTagging extends PegTerm {
	String symbol;
	public PegTagging(Grammar base, int flag, String tagName) {
		super(base, Peg.HasTagging | Peg.NoMemo | flag);
		this.symbol = tagName;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitTagging(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		left.setTag(this.symbol);
		return left;
	}
}

class PegMessage extends PegTerm {
	String symbol;
	public PegMessage(Grammar base, int flag, String message) {
		super(base, flag | Peg.NoMemo | Peg.HasMessage);
		this.symbol = message;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitMessage(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		left.setMessage(this.symbol);
		return left;
	}
}

class PegConstructor extends PegList {
	boolean leftJoin = false;
	String tagName;
	int prefetchIndex = 0;
	public PegConstructor(Grammar base, int flag, boolean leftJoin, String tagName, UList<Peg> list) {
		super(base, flag | Peg.HasNewObject, list);
		this.leftJoin = leftJoin;
		this.tagName = tagName == null ? "#new" : tagName;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNewObject(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		ParsingObject leftNode = left;
		long startIndex = context.getPosition();
		for(int i = 0; i < this.prefetchIndex; i++) {
			ParsingObject right = this.get(i).simpleMatch(left, context);
			if(right.isFailure()) {
				context.rollback(startIndex);
				return right;
			}
//			if(left != right) {
//				System.out.println("DEBUG: @" + i + " < " + this.prefetchIndex + " in " + this);
//				System.out.println("LEFT: " + left);
//				System.out.println("RIGHT: " + right);
//				System.out.println("FLAGS: " + this.get(i).hasObjectOperation());
//			}
			assert(left == right);
		}
		int mark = context.markObjectStack();
		ParsingObject newnode = context.newPegObject1(this.tagName, startIndex, this);
		if(this.leftJoin) {
			context.logSetter(newnode, -1, leftNode);
		}
		for(int i = this.prefetchIndex; i < this.size(); i++) {
			ParsingObject node = this.get(i).simpleMatch(newnode, context);
			if(node.isFailure()) {
				context.rollbackObjectStack(mark);
				context.rollback(startIndex);
				return node;
			}
			//			if(node != newnode) {
			//				e.warning("dropping @" + newnode.name + " " + node);
			//			}
		}
		context.popNewObject(newnode, startIndex, mark);
		if(context.stat != null) {
			context.stat.countObjectCreation();
		}
		return newnode;
	}
	public void lazyMatch(ParsingObject newnode, ParserContext context, long pos) {
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			ParsingObject node = this.get(i).simpleMatch(newnode, context);
			if(node.isFailure()) {
				break;  // this not happens
			}
		}
		context.popNewObject(newnode, pos, mark);
	}
}

class PegExport extends PegUnary {
	public PegExport(Grammar base, int flag, Peg e) {
		super(base, flag | Peg.NoMemo, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitExport(this);
	}
	@Override
	boolean acceptC1(int ch) {
		return this.inner.acceptC1(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		return context.matchExport(left, this);
	}
}

class PegIndent extends PegTerm {
	PegIndent(Grammar base, int flag) {
		super(base, flag | Peg.HasContext);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitIndent(this);
	}
	@Override
	boolean acceptC1(int ch) {
		return (ch == '\t' || ch == ' ');
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		String indent = left.getSource().getIndentText(left.getSourcePosition());
		if(context.match(indent.getBytes())) {  // very slow
			return left;
		}
		return context.foundFailure(this);
	}
}

class PegIndex extends PegTerm {
	int index;
	PegIndex(Grammar base, int flag, int index) {
		super(base, flag | Peg.HasContext);
		this.index = index;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitIndex(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
//		String indent = left.getSource().getIndentText(left.getSourcePosition());
//		if(context.match(indent.getBytes())) {  // very slow
//			return left;
//		}
		return context.foundFailure(this);
	}
}

abstract class PegOperation extends Peg {
	Peg inner;
	protected PegOperation(Peg inner) {
		super(inner.base, inner.flag);
		this.inner = inner;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitOperation(this);
	}
	@Override
	boolean acceptC1(int ch) {
		return this.inner.acceptC1(ch);
	}
	@Override
	public Peg getExpression() {
		return this.inner;
	}
}

class PegMemo extends PegOperation {
	Peg parent = null;
	boolean enableMemo = true;
	int memoHit = 0;
	int memoMiss = 0;

	protected PegMemo(Peg inner) {
		super(inner);
		this.semanticId = inner.semanticId;
	}

	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(!this.enableMemo) {
			return this.inner.simpleMatch(left, context);
		}
		long pos = context.getPosition();
		ObjectMemo m = context.memoMap.getMemo(this, pos);
		if(m != null) {
			this.memoHit += 1;
			context.setPosition(pos + m.consumed);
			if(m.generated == null) {
				return left;
			}
			return m.generated;
		}
		ParsingObject right = this.inner.simpleMatch(left, context);
		int length = (int)(context.getPosition() - pos);
//		if(length > 0) {
			if(right == left) {
				context.memoMap.setMemo(pos, this, null, length);
			}
			else {
				context.memoMap.setMemo(pos, this, right, length);
			}
//		}
		this.memoMiss += 1;
		this.tryTracing();
		return right;
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
		this.base.DisabledMemo += 1;
		int factor = this.base.EnabledMemo / 10;
		if(factor != 0 && this.base.DisabledMemo % factor == 0) {
			this.base.memoRemover.removeDisabled();
		}
	}

	void show() {
		if(Main.VerboseMode) {
			double f = (double)this.memoHit / this.memoMiss;
			System.out.println(this.inner.getClass().getSimpleName() + " #h/m=" + this.memoHit + "," + this.memoMiss + ", f=" + f + " " + this.inner);
		}
	}
	public ParsingObject simpleMatch1(ParsingObject left, ParserContext context) {
		if(!this.enableMemo) {
			return this.inner.simpleMatch(left, context);
		}
		long pos = context.getPosition();
		ObjectMemo m = context.memoMap.getMemo(this, pos);
		if(m != null) {
			this.memoHit += 1;
			assert(m.keypeg == this);
			if(m.generated == null) {
				return context.refoundFailure(this.inner, pos+m.consumed);
			}
//			if(m.consumed > 0) {
//				//System.out.println("HIT : " + this.semanticId + ":" + pos + "," + m.consumed+ " :" + m.generated);
//				context.setPosition(pos + m.consumed);
//				return m.generated;
//			}
			return m.generated;
		}
		ParsingObject result = this.inner.simpleMatch(left, context);
		if(result.isFailure()) {
			context.memoMap.setMemo(pos, this, null, /*(int)(result.getSourcePosition() - pos*/0);
		}
		else {
			int length = (int)(context.getPosition() - pos);
//			if(length > 0) {
				context.memoMap.setMemo(pos, this, result, length);
				//System.out.println("MEMO: " + this.semanticId + ":" + pos + "," + length+ " :&" + result.id);
//			}
		}
		this.memoMiss += 1;
		this.tryTracing();
		return result;
	}
}

class PegMonad extends PegOperation {
	protected PegMonad(Peg inner) {
		super(inner);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		int mark = context.markObjectStack();
		left = this.inner.simpleMatch(left, context);
		context.rollbackObjectStack(mark);
		return left;
	}
}

class PegCommit extends PegOperation {
	protected PegCommit(Peg inner) {
		super(inner);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		int mark = context.markObjectStack();
		left = this.inner.simpleMatch(left, context);
		if(left.isFailure()) {
			context.rollbackObjectStack(mark);
		}
		return left;
	}
}



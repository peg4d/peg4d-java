package org.peg4d;
import org.peg4d.MemoMap.ObjectMemo;

public abstract class PExpression {
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
	public final static int Debug             = 1 << 24;
	
	Grammar    base;
	int        flag       = 0;
	short      uniqueId   = 0;
	short      semanticId = 0;
		
	protected PExpression(Grammar base, int flag) {
		this.base = base;
		this.flag = flag;
		this.uniqueId = base.composer.issue(this);
		this.semanticId = this.uniqueId;
	}
		
	protected abstract void visit(PegVisitor probe);
	public PExpression getExpression() {
		return this;
	}
	public abstract ParsingObject simpleMatch(ParsingObject left, ParserContext context);

	boolean checkFirstCharcter(int ch) {
		return true;
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
	
	public String key() {
		return "#" + this.uniqueId;
	}
	
	public int size() {
		return 0;
	}
	public PExpression get(int index) {
		return this;  // to avoid NullPointerException
	}
	
	public PExpression get(int index, PExpression def) {
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
	protected void warning(String msg) {
		if(Main.VerbosePeg && Main.StatLevel == 0) {
			Main._PrintLine("PEG warning: " + msg);
		}
	}
	public final boolean hasObjectOperation() {
		return this.is(PExpression.HasConstructor) || this.is(PExpression.HasConnector) || this.is(PExpression.HasTagging) || this.is(PExpression.HasMessage);
	}
}

abstract class PTerm extends PExpression {
	PTerm (Grammar base, int flag) {
		super(base, flag);
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

class PNonTerminal extends PTerm {
	String symbol;
	PExpression    jumpExpression = null;
	
	PNonTerminal(Grammar base, int flag, String ruleName) {
		super(base, flag | PExpression.HasNonTerminal | PExpression.NoMemo);
		this.symbol = ruleName;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNonTerminal(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
		if(this.jumpExpression != null) {
			return this.jumpExpression.checkFirstCharcter(ch);
		}
		return true;
	}
	final PExpression getNext() {
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

class PLazyNonTerminal extends PTerm {
	String symbol;
	PLazyNonTerminal(Grammar base, int flag, String ruleName) {
		super(base, flag| PExpression.HasLazyNonTerminal | PExpression.NoMemo);
		this.symbol = ruleName;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitLazyNonTerminal(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
		return true;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		PExpression next = context.peg.getExpression(this.symbol);
		if(next != null) {
			return next.simpleMatch(left, context);
		}
		return left;
	}
}

class PString extends PTerm {
	String text;
	byte[] utf8;
	public PString(Grammar base, int flag, String text) {
		super(base, PExpression.HasString | PExpression.NoMemo | flag);
		this.text = text;
		utf8 = UCharset.toUtf8(text);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitString(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
		if(this.text.length() == 0) {
			return true;
		}
		return UCharset.getFirstChar(this.utf8) == ch;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(context.match(this.utf8)) {
			return left;
		}
		return context.foundFailure(this);
	}
}

class PString1 extends PString {
	int symbol1;
	public PString1(Grammar base, int flag, String token) {
		super(base, flag, token);
		this.symbol1 = this.utf8[0] & 0xff;
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

class PAny extends PTerm {
	public PAny(Grammar base, int flag) {
		super(base, PExpression.HasAny | PExpression.NoMemo | flag);
	}
	@Override boolean checkFirstCharcter(int ch) {
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

class PUnicodeAny extends PAny {
	public PUnicodeAny(Grammar base, int flag) {
		super(base, flag);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		int len = UCharset.lengthOfUtf8(context.getChar());
		if(context.hasChar(len)) {
			context.consume(len);
			return left;
		}
		return context.foundFailure(this);
	}
}

class PSkip extends PAny {
	int skip;
	public PSkip(Grammar base, int flag, int skip) {
		super(base, flag);
		this.skip = skip;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		if(context.hasChar(skip)) {
			context.consume(skip);
			return left;
		}
		return context.foundFailure(this);
	}
}

class PNotAny extends PTerm {
	PNot not;
	PExpression exclude;
	PExpression orig;
	public PNotAny(Grammar base, int flag, PNot e, PExpression orig) {
		super(base, flag | PExpression.NoMemo);
		this.not = e;
		this.exclude = e.inner;
		this.orig = orig;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNotAny(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
		return this.not.checkFirstCharcter(ch) && this.orig.checkFirstCharcter(ch);
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

class PCharacter extends PTerm {
	UCharset charset;
	public PCharacter(Grammar base, int flag, UCharset charset) {
		super(base, PExpression.HasCharacter | PExpression.NoMemo | flag);
		this.charset = charset;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitCharacter(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
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

//class PegUtf8Range extends PegCharacter {
//	int fromChar;
//	int toChar;
//	public PegUtf8Range(Grammar base, int flag, char fromChar, char toChar) {
//		super(base, flag, new UCharset(fromChar, toChar));
//		this.fromChar = UCharset.getFirstChar("" +fromChar);
//		this.toChar   = UCharset.getFirstChar("" +toChar);
//	}
//	@Override boolean acceptC1(int ch) {
//		return (this.fromChar <= ch && ch <= this.toChar);
//	}
//	@Override
//	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
//		int len = UCharset.lengthOfUtf8(context.getChar());
//		int uchar = context.getUChar();
//		if(this.fromChar <= uchar && uchar <= this.toChar ) {
//			context.consume(len);
//			return left;
//		}
//		return context.foundFailure(this);
//	}
//}

abstract class PUnary extends PExpression {
	PExpression inner;
	public PUnary(Grammar base, int flag, PExpression e) {
		super(base, flag);
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

class POptional extends PUnary {
	public POptional(Grammar base, int flag, PExpression e) {
		super(base, flag | PExpression.HasOptional | PExpression.NoMemo, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitOptional(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
		return true;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
//		long pos = context.getPosition();
		long f = context.rememberFailure();
		ParsingObject right = this.inner.simpleMatch(left, context);
		if(right.isFailure()) {
//			assert(pos == context.getPosition());
			context.forgetFailure(f);
			return left;
		}
		return right;
	}
}

class POptionalString extends POptional {
	byte[] utf8;
	public POptionalString(Grammar base, int flag, PString e) {
		super(base, flag | PExpression.NoMemo, e);
		this.utf8 = e.utf8;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		context.match(this.utf8);
		return left;
	}
}

class POptionalString1 extends POptional {
	int symbol1;
	public POptionalString1(Grammar base, int flag, PString1 e) {
		super(base, flag | PExpression.NoMemo, e);
		this.symbol1 = e.symbol1;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		context.match(this.symbol1);
		return left;
	}
}

class POptionalCharacter extends POptional {
	UCharset charset;
	public POptionalCharacter(Grammar base, int flag, PCharacter e) {
		super(base, flag | PExpression.NoMemo, e);
		this.charset = e.charset;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		context.match(this.charset);
		return left;
	}
}

class PRepetition extends PUnary {
	public int atleast = 0; 
	protected PRepetition(Grammar base, int flag, PExpression e, int atLeast) {
		super(base, flag | PExpression.HasRepetition, e);
		this.atleast = atLeast;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitRepetition(this);
	}
	@Override boolean checkFirstCharcter(int ch) {
		if(this.atleast > 0) {
			return this.inner.checkFirstCharcter(ch);
		}
		return true;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long ppos = -1;
		long pos = context.getPosition();
		long f = context.rememberFailure();
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
		context.forgetFailure(f);
		return left;
	}
}

class POneMoreCharacter extends PRepetition {
	UCharset charset;
	public POneMoreCharacter(Grammar base, int flag, PCharacter e) {
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

class PZeroMoreCharacter extends PRepetition {
	UCharset charset;
	public PZeroMoreCharacter(Grammar base, int flag, PCharacter e) {
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

class PAnd extends PUnary {
	PAnd(Grammar base, int flag, PExpression e) {
		super(base, flag | PExpression.HasAnd, e);
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
	boolean checkFirstCharcter(int ch) {
		return this.inner.checkFirstCharcter(ch);
	}
}

class PNot extends PUnary {
	PNot(Grammar base, int flag, PExpression e) {
		super(base, PExpression.HasNot | flag, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitNot(this);
	}
	@Override
	boolean checkFirstCharcter(int ch) {
		return !this.inner.checkFirstCharcter(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingObject right = this.inner.simpleMatch(left, context);
		if(right.isFailure()) {
			context.forgetFailure(f);
			return left;
		}
		context.rollback(pos);
		return context.foundFailure(this);
	}
}

class PNotString extends PNot {
	byte[] utf8;
	PNotString(Grammar peg, int flag, PString e) {
		super(peg, flag | PExpression.NoMemo, e);
		this.utf8 = e.utf8;
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = context.getPosition();
		if(context.match(this.utf8)) {
			//context.setPosition(pos);
			return context.foundFailure(this);
		}
		return left;
	}
}

class PNotString1 extends PNotString {
	int symbol;
	PNotString1(Grammar peg, int flag, PString1 e) {
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

class PNotCharacter extends PNot {
	UCharset charset;
	PNotCharacter(Grammar base, int flag, PCharacter e) {
		super(base, flag | PExpression.NoMemo, e);
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

abstract class PList extends PExpression {
	UList<PExpression> list;
	String[] nonTerminaNames = null;
	int length = 0;
	PList(Grammar base, int flag, UList<PExpression> list) {
		super(base, flag);
		this.list = list;
		this.indexName();
	}
	void indexName() {
		for(int i = 0; i < this.size(); i++) {
			PExpression e = this.get(i);
			if(e instanceof PNonTerminal) {
				if(this.nonTerminaNames == null) {
					this.nonTerminaNames = new String[this.size()];
				}
				this.nonTerminaNames[i] = ((PNonTerminal) e).symbol;
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
	public final PExpression get(int index) {
		return this.list.ArrayValues[index];
	}
	public final void set(int index, PExpression e) {
		this.list.ArrayValues[index] = e;
	}
	@Override
	public final PExpression get(int index, PExpression def) {
		if(index < this.size()) {
			return this.list.ArrayValues[index];
		}
		return def;
	}
	private boolean isOptional(PExpression e) {
		if(e instanceof POptional) {
			return true;
		}
		if(e instanceof PRepetition && ((PRepetition) e).atleast == 0) {
			return true;
		}
		return false;
	}

	private boolean isUnconsumed(PExpression e) {
		if(e instanceof PNot && e instanceof PAnd) {
			return true;
		}
		if(e instanceof PString && ((PString)e).utf8.length == 0) {
			return true;
		}
		if(e instanceof PIndent) {
			return true;
		}
		return false;
	}
		
	@Override
	boolean checkFirstCharcter(int ch) {
		for(int start = 0; start < this.size(); start++) {
			PExpression e = this.get(start);
			if(e instanceof PTagging || e instanceof PMessage) {
				continue;
			}
			if(this.isOptional(e)) {
				if(((PUnary)e).inner.checkFirstCharcter(ch)) {
					return true;
				}
				continue;  // unconsumed
			}
			if(this.isUnconsumed(e)) {
				if(!e.checkFirstCharcter(ch)) {
					return false;
				}
				continue;
			}
			return e.checkFirstCharcter(ch);
		}
		return true;
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
	PSequence(Grammar base, int flag, UList<PExpression> l) {
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

class PChoice extends PList {
	PChoice(Grammar base, int flag, UList<PExpression> list) {
		super(base, flag | PExpression.HasChoice, list);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitChoice(this);
	}
	@Override
	boolean checkFirstCharcter(int ch) {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).checkFirstCharcter(ch)) {
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


class PMappedChoice extends PChoice {
	PExpression[] caseOf = null;
	PMappedChoice(Grammar base, int flag, UList<PExpression> list) {
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
	void tryPrediction() {
		if(this.caseOf == null) {
			this.caseOf = new PExpression[UCharset.MAX];
			PExpression failed = new PAlwaysFailure(this);
			for(int ch = 0; ch < UCharset.MAX; ch++) {
				this.caseOf[ch] = selectC1(ch, failed);
			}
			this.base.PredictionOptimization += 1;
		}
	}
	private PExpression selectC1(int ch, PExpression failed) {
		PExpression e = null;
		UList<PExpression> l = null; // new UList<Peg>(new Peg[2]);
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).checkFirstCharcter(ch)) {
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
			e = new PChoice(e.base, 0, l);
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

class PegWordChoice extends PChoice {
	UCharset charset = null;
	UList<byte[]> wordList = null;
	PegWordChoice(Grammar base, int flag, UList<PExpression> list) {
		super(base, flag | PExpression.HasChoice, list);
		this.wordList = new UList<byte[]>(new byte[list.size()][]);
		for(int i = 0; i < list.size(); i++) {
			PExpression se = list.ArrayValues[i];
			if(se instanceof PString1) {
				if(charset == null) {
					charset = new UCharset("");
				}
				charset.append(((PString1)se).symbol1);
			}
			if(se instanceof PCharacter) {
				if(charset == null) {
					charset = new UCharset("");
				}
				charset.append(((PCharacter)se).charset);
			}
			if(se instanceof PString) {
				wordList.add(((PString)se).utf8);
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

class PAlwaysFailure extends PString {
	public PAlwaysFailure(PExpression orig) {
		super(orig.base, 0, "\0");
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		return context.foundFailure(this);
	}
}

class PConnector extends PUnary {
	public int index;
	public PConnector(Grammar base, int flag, PExpression e, int index) {
		super(base, flag | PExpression.HasConnector | PExpression.NoMemo, e);
		this.index = index;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitConnector(this);
	}
	@Override
	boolean checkFirstCharcter(int ch) {
		return this.inner.checkFirstCharcter(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long pos = left.getSourcePosition();
		ParsingObject node = this.inner.simpleMatch(left, context);
		if(node.isFailure() || left == node) {
			return node;
		}
		if(context.isRecognitionMode()) {
			left.setSourcePosition(pos);
		}
		else {
			context.pushConnection(left, this.index, node);
		}
		return left;
	}
}

class PTagging extends PTerm {
	String symbol;
	public PTagging(Grammar base, int flag, String tagName) {
		super(base, PExpression.HasTagging | PExpression.NoMemo | flag);
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

class PMessage extends PTerm {
	String symbol;
	public PMessage(Grammar base, int flag, String message) {
		super(base, flag | PExpression.NoMemo | PExpression.HasMessage);
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

class PConstructor extends PList {
	boolean leftJoin = false;
	String tagName;
	int prefetchIndex = 0;
	public PConstructor(Grammar base, int flag, boolean leftJoin, String tagName, UList<PExpression> list) {
		super(base, flag | PExpression.HasConstructor, list);
		this.leftJoin = leftJoin;
		this.tagName = tagName == null ? "#new" : tagName;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitConstructor(this);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		long startIndex = context.getPosition();
		if(context.isRecognitionMode()) {
			ParsingObject newnode = context.newParsingObject(this.tagName, startIndex, this);
			for(int i = 0; i < this.size(); i++) {
				ParsingObject node = this.get(i).simpleMatch(newnode, context);
				if(node.isFailure()) {
					context.rollback(startIndex);
					return node;
				}
			}
			return newnode;
		}
		else {
			ParsingObject leftNode = left;
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
			ParsingObject newnode = context.newParsingObject(this.tagName, startIndex, this);
			if(this.leftJoin) {
				context.pushConnection(newnode, -1, leftNode);
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
			context.popConnection(newnode, startIndex, mark);
			if(context.stat != null) {
				context.stat.countObjectCreation();
			}
			return newnode;
		}
	}
		
	public void lazyMatch(ParsingObject newnode, ParserContext context, long pos) {
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			ParsingObject node = this.get(i).simpleMatch(newnode, context);
			if(node.isFailure()) {
				break;  // this not happens
			}
		}
		context.popConnection(newnode, pos, mark);
	}
}

class PIndent extends PTerm {
	PIndent(Grammar base, int flag) {
		super(base, flag | PExpression.HasContext);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitIndent(this);
	}
	@Override
	boolean checkFirstCharcter(int ch) {
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

class PExport extends PUnary {
	public PExport(Grammar base, int flag, PExpression e) {
		super(base, flag | PExpression.NoMemo, e);
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitExport(this);
	}
	@Override
	boolean checkFirstCharcter(int ch) {
		return this.inner.checkFirstCharcter(ch);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		return context.matchExport(left, this);
	}
}

abstract class POperator extends PExpression {
	PExpression inner;
	protected POperator(PExpression inner) {
		super(inner.base, inner.flag);
		this.inner = inner;
	}
	@Override
	protected void visit(PegVisitor probe) {
		probe.visitOperation(this);
	}
	@Override
	boolean checkFirstCharcter(int ch) {
		return this.inner.checkFirstCharcter(ch);
	}
	@Override
	public PExpression getExpression() {
		return this.inner;
	}
}

class PMemo extends POperator {
	PExpression parent = null;
	boolean enableMemo = true;
	int memoHit = 0;
	int memoMiss = 0;

	protected PMemo(PExpression inner) {
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

class PMatch extends POperator {
	protected PMatch(PExpression inner) {
		super(inner);
	}
	@Override
	public ParsingObject simpleMatch(ParsingObject left, ParserContext context) {
		boolean oldMode = context.setRecognitionMode(true);
		ParsingObject right = this.inner.simpleMatch(left, context);
		context.setRecognitionMode(oldMode);
		if(!right.isFailure()) {
			return left;
		}
		return right;
	}
}

class PCommit extends POperator {
	protected PCommit(PExpression inner) {
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



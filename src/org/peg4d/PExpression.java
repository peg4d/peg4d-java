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
		this.uniqueId = base.factory.issue(this);
		this.semanticId = this.uniqueId;
	}
		
	protected abstract void visit(ParsingVisitor probe);
	public PExpression getExpression() {
		return this;
	}
	public abstract void vmMatch(ParsingContext context);
	public abstract void simpleMatch(ParsingStream context);

	boolean checkFirstByte(int ch) {
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
	protected void warning(String msg) {
		if(Main.VerbosePeg && Main.StatLevel == 0) {
			Main._PrintLine("PEG warning: " + msg);
		}
	}
	public final boolean hasObjectOperation() {
		return this.is(PExpression.HasConstructor) 
				|| this.is(PExpression.HasConnector) 
				|| this.is(PExpression.HasTagging) 
				|| this.is(PExpression.HasMessage);
	}
}

class PNonTerminal extends PExpression {
	String symbol;
	PExpression    resolvedExpression = null;
	PNonTerminal(Grammar base, int flag, String ruleName) {
		super(base, flag | PExpression.HasNonTerminal | PExpression.NoMemo);
		this.symbol = ruleName;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitNonTerminal(this);
	}
	@Override boolean checkFirstByte(int ch) {
		if(this.resolvedExpression != null) {
			return this.resolvedExpression.checkFirstByte(ch);
		}
		return true;
	}
	final PExpression getNext() {
		if(this.resolvedExpression == null) {
			return this.base.getExpression(this.symbol);
		}
		return this.resolvedExpression;
	}
	public boolean isForeignNonTerminal() {
		return this.resolvedExpression.base != this.base;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		this.resolvedExpression.vmMatch(context);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		this.resolvedExpression.simpleMatch(context);
		//System.out.println("pos=" + context.pos + " called " + this.symbol + " isFailure: " + context.isFailure() + " " + this.resolvedExpression);
	}
}

abstract class PTerminal extends PExpression {
	PTerminal (Grammar base, int flag) {
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

class PString extends PTerminal {
	String text;
	byte[] utf8;
	public PString(Grammar base, int flag, String text) {
		super(base, PExpression.HasString | PExpression.NoMemo | flag);
		this.text = text;
		if(text != null) {
			utf8 = ParsingCharset.toUtf8(text);
		}
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitString(this);
	}
	@Override boolean checkFirstByte(int ch) {
		if(this.text.length() == 0) {
			return true;
		}
		return ParsingCharset.getFirstChar(this.utf8) == ch;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchText(this.utf8);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchText(this.utf8);
	}
}

class PByteChar extends PString {
	int byteChar;
	PByteChar(Grammar base, int flag, String token) {
		super(base, flag, token);
		this.byteChar = this.utf8[0] & 0xff;
	}
	PByteChar(Grammar base, int flag, int ch, String text) {
		super(base, flag, null);
		this.utf8 = new byte[1];
		this.utf8[0] = (byte)ch;
		this.byteChar = ch & 0xff;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchByteChar(this.byteChar);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchByteChar(this.byteChar);
	}
}

class PAny extends PTerminal {
	PAny(Grammar base, int flag) {
		super(base, PExpression.HasAny | PExpression.NoMemo | flag);
	}
	@Override boolean checkFirstByte(int ch) {
		return true;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitAny(this);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchAnyChar();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		if(context.hasByteChar()) {
			context.consume(1);
			return;
		}
		context.foundFailure(this);
	}
}

class PCharacter extends PTerminal {
	ParsingCharset charset;
	PCharacter(Grammar base, int flag, ParsingCharset charset) {
		super(base, PExpression.HasCharacter | PExpression.NoMemo | flag);
		this.charset = charset;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitCharacter(this);
	}
	@Override boolean checkFirstByte(int ch) {
		return this.charset.hasByte(ch);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchCharset(this.charset);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchCharset(this.charset);
	}
}


//class PNotAny extends PTerm {
//	PNot not;
//	PExpression exclude;
//	PExpression orig;
//	public PNotAny(Grammar base, int flag, PNot e, PExpression orig) {
//		super(base, flag | PExpression.NoMemo);
//		this.not = e;
//		this.exclude = e.inner;
//		this.orig = orig;
//	}
//	@Override
//	protected void visit(ParsingVisitor probe) {
//		probe.visitNotAny(this);
//	}
//	@Override boolean checkFirstByte(int ch) {
//		return this.not.checkFirstByte(ch) && this.orig.checkFirstByte(ch);
//	}
//	@Override
//	public ParsingObject simpleMatch(ParsingObject left, ParsingContext2 context) {
//		long pos = context.getPosition();
//		ParsingObject right = this.exclude.simpleMatch(left, context);
//		if(context.isFailure()) {
//			assert(pos == context.getPosition());
//			if(context.hasByteChar()) {
//				context.consume(1);
//				return left;
//			}
//		}
//		else {
//			context.rollback(pos);
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
	protected void visit(ParsingVisitor probe) {
		probe.visitOptional(this);
	}
	@Override boolean checkFirstByte(int ch) {
		return true;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opRememberFailurePosition();
		context.opStoreObject();
		this.inner.vmMatch(context);
		context.opRestoreObjectIfFailure();
		context.opForgetFailurePosition();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
		this.inner.simpleMatch(context);
		if(context.isFailure()) {
			context.forgetFailure(f);
			context.left = left;
		}
	}
}

class POptionalString extends POptional {
	byte[] utf8;
	POptionalString(Grammar base, int flag, PString e) {
		super(base, flag | PExpression.NoMemo, e);
		this.utf8 = e.utf8;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchOptionalText(this.utf8);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchOptionalText(this.utf8);
	}
}

class POptionalByteChar extends POptional {
	int byteChar;
	POptionalByteChar(Grammar base, int flag, PByteChar e) {
		super(base, flag | PExpression.NoMemo, e);
		this.byteChar = e.byteChar;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchOptionalByteChar(this.byteChar);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchOptionalByteChar(this.byteChar);
	}
}

class POptionalCharacter extends POptional {
	ParsingCharset charset;
	POptionalCharacter(Grammar base, int flag, PCharacter e) {
		super(base, flag | PExpression.NoMemo, e);
		this.charset = e.charset;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchOptionalCharset(this.charset);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchOptionalCharset(this.charset);
	}
}

class PRepetition extends PUnary {
	public int atleast = 0; 
	protected PRepetition(Grammar base, int flag, PExpression e, int atLeast) {
		super(base, flag | PExpression.HasRepetition, e);
		this.atleast = atLeast;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitRepetition(this);
	}
	@Override boolean checkFirstByte(int ch) {
		if(this.atleast > 0) {
			return this.inner.checkFirstByte(ch);
		}
		return true;
	}
	
	@Override
	public void vmMatch(ParsingContext context) {
		if(this.atleast == 1) {
			this.inner.vmMatch(context);
			if(context.isFailure()) {
				return;
			}
		}
		long ppos = -1;
		long pos = context.getPosition();
		context.opRememberFailurePosition();
		context.opStoreObject();
		while(ppos < pos) {
			context.opRefreshStoredObject();
			this.inner.vmMatch(context);
			if(context.isFailure()) {
				context.opRestoreObject();
				break;
			}
			ppos = pos;
			pos = context.getPosition();
		}
		context.opForgetFailurePosition();
	}

	@Override
	public void simpleMatch(ParsingStream context) {
		long ppos = -1;
		long pos = context.getPosition();
		long f = context.rememberFailure();
		int count = 0;
		while(ppos < pos) {
			ParsingObject left = context.left;
			this.inner.simpleMatch(context);
			if(context.isFailure()) {
				context.left = left;
				break;
			}
			ppos = pos;
			pos = context.getPosition();
			count = count + 1;
		}
		if(count < this.atleast) {
			context.foundFailure(this);
		}
		else {
			context.forgetFailure(f);
		}
	}
}

//class POneMoreCharacter extends PRepetition {
//	ParsingCharset charset;
//	public POneMoreCharacter(Grammar base, int flag, PCharacter e) {
//		super(base, flag, e, 1);
//		charset = e.charset;
//	}
//	@Override
//	public ParsingObject simpleMatch(ParsingObject left, ParsingContext2 context) {
//		long pos = context.getPosition();
//		int consumed = this.charset.consume(context.source, pos);
//		if(consumed == 0) {
//			return context.foundFailure(this);
//		}
//		pos += consumed;
//		do {
//			consumed = this.charset.consume(context.source, pos);
//			pos += consumed;
//		}
//		while(consumed > 0);
//		context.setPosition(pos);
//		return left;
//	}
//}

class PZeroMoreCharacter extends PRepetition {
	ParsingCharset charset;
	public PZeroMoreCharacter(Grammar base, int flag, PCharacter e) {
		super(base, flag, e, 0);
		this.charset = e.charset;
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		long pos = context.getPosition();
		int consumed = 0;
		do {
			consumed = this.charset.consume(context.source, pos);
			pos += consumed;
		}
		while(consumed > 0);
		context.setPosition(pos);
	}
}

class PAnd extends PUnary {
	PAnd(Grammar base, int flag, PExpression e) {
		super(base, flag | PExpression.HasAnd, e);
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitAnd(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		return this.inner.checkFirstByte(ch);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opRememberPosition();
		this.inner.vmMatch(context);
		context.opBacktrackPosition();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		long pos = context.getPosition();
		this.inner.simpleMatch(context);
		context.rollback(pos);
	}
}

class PNot extends PUnary {
	PNot(Grammar base, int flag, PExpression e) {
		super(base, PExpression.HasNot | flag, e);
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitNot(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		return !this.inner.checkFirstByte(ch);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opRememberPosition();
		context.opStoreObject();
		context.opRememberFailurePosition();
		this.inner.vmMatch(context);
		context.opForgetFailurePosition();
		context.opRestoreNegativeObject();
		context.opBacktrackPosition();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		long pos = context.getPosition();
		long f   = context.rememberFailure();
		ParsingObject left = context.left;
		this.inner.simpleMatch(context);
		if(context.isFailure()) {
			context.forgetFailure(f);
			context.left = left;
		}
		else {
			context.foundFailure(this);
		}
		context.rollback(pos);
	}
}

class PNotString extends PNot {
	byte[] utf8;
	PNotString(Grammar peg, int flag, PString e) {
		super(peg, flag | PExpression.NoMemo, e);
		this.utf8 = e.utf8;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchTextNot(utf8);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchTextNot(utf8);
	}
}

class PNotByteChar extends PNotString {
	int byteChar;
	PNotByteChar(Grammar peg, int flag, PByteChar e) {
		super(peg, flag, e);
		this.byteChar = e.byteChar;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchByteCharNot(this.byteChar);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchByteCharNot(this.byteChar);
	}
}	

class PNotCharacter extends PNot {
	ParsingCharset charset;
	PNotCharacter(Grammar base, int flag, PCharacter e) {
		super(base, flag | PExpression.NoMemo, e);
		this.charset = e.charset;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opMatchCharsetNot(this.charset);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opMatchCharsetNot(this.charset);
	}
}

abstract class PList extends PExpression {
	UList<PExpression> list;
	int length = 0;
	PList(Grammar base, int flag, UList<PExpression> list) {
		super(base, flag);
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
	public final PExpression get(int index, PExpression def) {
		if(index < this.size()) {
			return this.list.ArrayValues[index];
		}
		return def;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opRememberSequencePosition();
		for(int i = 0; i < this.size(); i++) {
			this.get(i).vmMatch(context);
			if(context.isFailure()) {
				context.opBackTrackSequencePosition();
				return;
			}
		}
		context.opCommitSequencePosition();
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
	boolean checkFirstByte(int ch) {
		for(int start = 0; start < this.size(); start++) {
			PExpression e = this.get(start);
			if(e instanceof PTagging || e instanceof PMessage) {
				continue;
			}
			if(this.isOptional(e)) {
				if(((PUnary)e).inner.checkFirstByte(ch)) {
					return true;
				}
				continue;  // unconsumed
			}
			if(this.isUnconsumed(e)) {
				if(!e.checkFirstByte(ch)) {
					return false;
				}
				continue;
			}
			return e.checkFirstByte(ch);
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
	protected void visit(ParsingVisitor probe) {
		probe.visitSequence(this);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		long pos = context.getPosition();
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			this.get(i).simpleMatch(context);
			//System.out.println("Attmpt Sequence " + this.get(i) + " isFailure: " + context.isFailure());
			if(context.isFailure()) {
				context.abortLinkLog(mark);
				context.rollback(pos);
				break;
			}
		}
	}
}

class PChoice extends PList {
	PChoice(Grammar base, int flag, UList<PExpression> list) {
		super(base, flag | PExpression.HasChoice, list);
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitChoice(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).checkFirstByte(ch)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opRememberFailurePosition();
		context.opStoreObject();
		for(int i = 0; i < this.size(); i++) {
			this.get(i).vmMatch(context);
			if(context.isFailure()) {
				context.opRestoreObject();
				context.opStoreObject();
			}
			else {
				context.opDropStoredObject();
				context.opForgetFailurePosition();
				return;
			}
		}
		context.opDropStoredObject();
		context.opUpdateFailurePosition();
	}

	@Override
	public void simpleMatch(ParsingStream context) {
		long f = context.rememberFailure();
		ParsingObject left = context.left;
		for(int i = 0; i < this.size(); i++) {
			context.left = left;
			this.get(i).simpleMatch(context);
			if(!context.isFailure()) {
				context.forgetFailure(f);
				return;
			}
		}
		assert(context.isFailure());
	}
}

class PMappedChoice extends PChoice {
	PExpression[] caseOf = null;
	PMappedChoice(Grammar base, int flag, UList<PExpression> list) {
		super(base, flag, list);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		int ch = context.getByteChar();
		if(this.caseOf == null) {
			tryPrediction();
		}
		caseOf[ch].simpleMatch(context);
	}
	void tryPrediction() {
		if(this.caseOf == null) {
			this.caseOf = new PExpression[ParsingCharset.MAX];
			PExpression failed = new PAlwaysFailure(this);
			for(int ch = 0; ch < ParsingCharset.MAX; ch++) {
				this.caseOf[ch] = selectC1(ch, failed);
			}
			this.base.PredictionOptimization += 1;
		}
	}
	private PExpression selectC1(int ch, PExpression failed) {
		PExpression e = null;
		UList<PExpression> l = null; // new UList<Peg>(new Peg[2]);
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i).checkFirstByte(ch)) {
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

class PAlwaysFailure extends PString {
	public PAlwaysFailure(PExpression orig) {
		super(orig.base, 0, "\0");
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opFailure();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.foundFailure(this);
	}
}

class PConnector extends PUnary {
	public int index;
	public PConnector(Grammar base, int flag, PExpression e, int index) {
		super(base, flag | PExpression.HasConnector | PExpression.NoMemo, e);
		this.index = index;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitConnector(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		return this.inner.checkFirstByte(ch);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opStoreObject();
		this.inner.vmMatch(context);
		context.opConnectObject(this.index);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		ParsingObject left = context.left;
		assert(left != null);
		long pos = left.getSourcePosition();
		//System.out.println("== DEBUG connecting .. " + this.inner);
		this.inner.simpleMatch(context);
		//System.out.println("== DEBUG same? " + (context.left == left) + " by " + this.inner);
		if(context.isFailure() || context.left == left) {
			return;
		}
		if(context.canTransCapture()) {
			context.logLink(left, this.index, context.left);
		}
		else {
			left.setSourcePosition(pos);
		}
		context.left = left;
	}
}

class PTagging extends PTerminal {
	ParsingTag tag;
	PTagging(Grammar base, int flag, ParsingTag tag) {
		super(base, PExpression.HasTagging | PExpression.NoMemo | flag);
		this.tag = tag;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitTagging(this);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opTagging(this.tag);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		if(!context.isRecognitionMode()) {
			context.left.setTag(this.tag.tagging());
		}
	}
}

class PMessage extends PTerminal {
	String symbol;
	PMessage(Grammar base, int flag, String message) {
		super(base, flag | PExpression.NoMemo | PExpression.HasMessage);
		this.symbol = message;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitMessage(this);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opValue(this.symbol);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		if(!context.isRecognitionMode()) {
			context.left.setValue(this.symbol);
		}
	}
}

class PConstructor extends PList {
	boolean leftJoin = false;
	String tagName;
	int prefetchIndex = 0;
	PConstructor(Grammar base, int flag, boolean leftJoin, String tagName, UList<PExpression> list) {
		super(base, flag | PExpression.HasConstructor, list);
		this.leftJoin = leftJoin;
		this.tagName = tagName == null ? "#new" : tagName;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitConstructor(this);
	}
	
	@Override
	public void vmMatch(ParsingContext context) {
		if(this.leftJoin) {
			context.opLeftJoinObject(this);
		}
		else {
			context.opNewObject(this);
		}
		super.vmMatch(context);
		context.opCommitObject();
	}

	@Override
	public void simpleMatch(ParsingStream context) {
		long startIndex = context.getPosition();
		ParsingObject left = context.left;
		if(context.isRecognitionMode()) {
			ParsingObject newone = context.newParsingObject(this.tagName, startIndex, this);
			context.left = newone;
			for(int i = 0; i < this.size(); i++) {
				this.get(i).simpleMatch(context);
				if(context.isFailure()) {
					context.rollback(startIndex);
					return;
				}
			}
			context.left = newone;
			return;
		}
		else {
			for(int i = 0; i < this.prefetchIndex; i++) {
				this.get(i).simpleMatch(context);
				if(context.isFailure()) {
					context.rollback(startIndex);
					return;
				}
			}
			int mark = context.markObjectStack();
			ParsingObject newnode = context.newParsingObject(this.tagName, startIndex, this);
			context.left = newnode;
			if(this.leftJoin) {
				context.logLink(newnode, -1, left);
			}
			for(int i = this.prefetchIndex; i < this.size(); i++) {
				this.get(i).simpleMatch(context);
				if(context.isFailure()) {
					context.abortLinkLog(mark);
					context.rollback(startIndex);
					return;
				}
			}
			context.commitLinkLog(newnode, startIndex, mark);
			if(context.stat != null) {
				context.stat.countObjectCreation();
			}
			context.left = newnode;
			return;
		}
	}
		
//	public void lazyMatch(ParsingObject newnode, ParsingStream context, long pos) {
//		int mark = context.markObjectStack();
//		for(int i = 0; i < this.size(); i++) {
//			ParsingObject node = this.get(i).simpleMatch(context);
//			if(context.isFailure()) {
//				break;  // this not happens
//			}
//		}
//		context.commitLinkLog(newnode, pos, mark);
//	}
}

class PIndent extends PTerminal {
	PIndent(Grammar base, int flag) {
		super(base, flag | PExpression.HasContext);
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitIndent(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		return (ch == '\t' || ch == ' ');
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opIndent();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		context.opIndent();
	}
}

class PExport extends PUnary {
	public PExport(Grammar base, int flag, PExpression e) {
		super(base, flag | PExpression.NoMemo, e);
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitExport(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		return this.inner.checkFirstByte(ch);
	}
	@Override
	public void vmMatch(ParsingContext context) {
	
	}
	@Override
	public void simpleMatch(ParsingStream context) {

	}
}

abstract class POperator extends PExpression {
	PExpression inner;
	protected POperator(PExpression inner) {
		super(inner.base, inner.flag);
		this.inner = inner;
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitOperation(this);
	}
	@Override
	boolean checkFirstByte(int ch) {
		return this.inner.checkFirstByte(ch);
	}
	@Override
	public PExpression getExpression() {
		return this.inner;
	}
}

class PMemo extends POperator {
	PExpression parent = null;
	boolean enableMemo = false;
	int memoHit = 0;
	int memoMiss = 0;

	protected PMemo(PExpression inner) {
		super(inner);
		this.semanticId = inner.semanticId;
	}

	@Override
	public void vmMatch(ParsingContext context) {
		this.inner.vmMatch(context);
	}

	@Override
	public void simpleMatch(ParsingStream context) {
		if(!this.enableMemo) {
			this.inner.simpleMatch(context);
			return;
		}
		long pos = context.getPosition();
		ParsingObject left = context.left;
		ObjectMemo m = context.memoMap.getMemo(this, pos);
		if(m != null) {
			this.memoHit += 1;
			context.setPosition(pos + m.consumed);
			if(m.generated == null) {
				context.left = left;
				return;
			}
			context.left = m.generated;
			return;
		}
		this.inner.simpleMatch(context);
		int length = (int)(context.getPosition() - pos);
//		if(length > 0) {
			if(context.left == left) {
				context.memoMap.setMemo(pos, this, null, length);
			}
			else {
				context.memoMap.setMemo(pos, this, context.left, length);
			}
//		}
		this.memoMiss += 1;
		this.tryTracing();
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
}

class PMatch extends POperator {
	protected PMatch(PExpression inner) {
		super(inner);
	}
	@Override
	public void vmMatch(ParsingContext context) {
		context.opDisableTransCapture();
		this.inner.vmMatch(context);
		context.opEnableTransCapture();
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		boolean oldMode = context.setRecognitionMode(true);
		ParsingObject left = context.left;
		this.inner.simpleMatch(context);
		context.setRecognitionMode(oldMode);
		if(!context.isFailure()) {
			context.left = left;
		}
	}
}

class PDeprecated extends POperator {
	String message;
	protected PDeprecated(String message, PExpression inner) {
		super(inner);
		this.message = message;
	}
	@Override
	public void vmMatch(ParsingContext context) {
		this.inner.vmMatch(context);
		context.opDeprecated(this.message);
	}
	@Override
	public void simpleMatch(ParsingStream context) {
		this.inner.vmMatch(context);
		context.opDeprecated(this.message);
	}
	@Override
	protected void visit(ParsingVisitor probe) {
		probe.visitDeprecated(this);
	}
}

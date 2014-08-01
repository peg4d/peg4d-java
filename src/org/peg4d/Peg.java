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
	
	ParserSource source = null;
	int       sourcePosition = 0;
	
	protected Peg(Grammar base, int flag) {
		this.base = base;
		this.flag = flag;
		base.pegList.add(this);
		this.uniqueId = (short)base.pegList.size();
		this.semanticId = this.uniqueId;
	}
		
	protected abstract Peg clone(Grammar base, PegTransformer tr);
	protected abstract void visit(PegProbe probe);
	public abstract Pego simpleMatch(Pego left, ParserContext context);
	public abstract int fastMatch(int left, MonadicParser context);
	public abstract Object getPrediction(boolean enforceCharset);
	public final UCharset getCharsetPrediction() {
		return (UCharset)this.getPrediction(true);
	}
	
	public final boolean accept(int ch) {
		UCharset p = this.getCharsetPrediction();
		if(p == null) {
			return true;
		}
		return p.match(ch);
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

class PegNoTransformer extends PegTransformer {
	@Override
	public Peg transform(Grammar base, Peg e) {
		return e;
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
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return null;
	}
}

class PegNonTerminal extends PegTerm {
	String symbol;
	Peg    nextRule = null;
	int length = -1;  // to be set by Verifier
	
	PegNonTerminal(Grammar base, int flag, String ruleName) {
		super(base, flag | Peg.HasNonTerminal | Peg.NoMemo);
		this.symbol = ruleName;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegNonTerminal(base, this.flag, this.symbol);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitNonTerminal(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		if(this.nextRule != null) {
			return this.nextRule.getPrediction(enforceCharset);
		}
		return null;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return this.nextRule.simpleMatch(left, context);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		Peg next = context.getRule(this.symbol);
		return next.fastMatch(left, context);
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
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegString(base, this.flag, this.text);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitString(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		if(this.text.length() == 0) {
			return null;
		}
		if(enforceCharset) {
			UCharset u = new UCharset("");
			u.append(UCharset.getFirstChar(this.text));
			return u;
		}
		return this.text;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		if(context.match(this.textByte)) {
			return left;
		}
		return context.foundFailure(this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		if(context.match(this.textByte)) {
			return left;
		}
		return context.foundFailure2(this);
	}
}

class PegString1 extends PegString {
	int symbol1;
	public PegString1(Grammar base, int flag, String token) {
		super(base, flag, token);
		this.symbol1 = token.charAt(0);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
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
		this.symbol1 = token.charAt(0);
		this.symbol2 = token.charAt(1);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
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
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegAny(base, this.flag);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitAny(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return null;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		if(context.hasChar()) {
			context.consume(1);
			return left;
		}
		return context.foundFailure(this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		if(context.hasChar()) {
			context.consume(1);
			return left;
		}
		return context.foundFailure2(this);
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
	protected Peg clone(Grammar base, PegTransformer tr) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitNotAny(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return null;
	}

	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
		Pego right = this.exclude.simpleMatch(left, context);
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
	@Override
	public int fastMatch(int left, MonadicParser context) {
		// TODO Auto-generated method stub
		return 0;
	}
}


class PegCharacter extends PegTerm {
	UCharset charset;
//	public PegCharacter(Grammar base, int flag, String token) {
//		super(base, Peg.HasCharacter | Peg.NoMemo | flag);
//		this.charset = new UCharset(token);
//	}
	public PegCharacter(Grammar base, int flag, UCharset charset) {
		super(base, Peg.HasCharacter | Peg.NoMemo | flag);
		this.charset = charset;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegCharacter(base, this.flag, this.charset);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitCharacter(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return this.charset;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		int ch = context.getChar();
		if(!this.charset.match(ch)) {
			return context.foundFailure(this);
		}
		context.consume(1);
		return left;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		int ch = context.getChar();
		if(!this.charset.match(ch)) {
			return context.foundFailure2(this);
		}
		context.consume(1);
		return left;
	}
}

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
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegOptional(base, this.flag, this.inner.clone(base, tr));
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitOptional(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return null;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
		Pego right = this.inner.simpleMatch(left, context);
		if(right.isFailure()) {
			assert(pos == context.getPosition());
//			context.setPosition(pos);
			return left;
		}
		return right;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		long pos = context.getPosition();
		int markerId = context.markObjectStack();
		int right = this.inner.fastMatch(left, context);
		if(PEGUtils.isFailure(right)) {
			context.rollbackObjectStack(markerId);
			context.rollback(pos);
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
	public Pego simpleMatch(Pego left, ParserContext context) {
		context.match(this.textByte);
		return left;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
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
	public Pego simpleMatch(Pego left, ParserContext context) {
		context.match(this.symbol1);
		return left;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
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
	public Pego simpleMatch(Pego left, ParserContext context) {
		context.match(this.charset);
		return left;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
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
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegRepeat(base, this.flag, this.inner.clone(base, tr), this.atleast);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitRepeat(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		if(this.atleast > 0) {
			return this.inner.getPrediction(enforceCharset);
		}
		return null;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long ppos = -1;
		long pos = context.getPosition();
		int count = 0;
		while(ppos < pos) {
			Pego right = this.inner.simpleMatch(left, context);
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
	@Override
	public int fastMatch(int left1, MonadicParser context) {
		int left = left1;
		int count = 0;
		int markerId = context.markObjectStack();
		while(context.hasChar()) {
			long pos = context.getPosition();
			markerId = context.markObjectStack();
			int right = this.inner.fastMatch(left, context);
			if(PEGUtils.isFailure(right)) {
				assert(pos == context.getPosition());
				if(count < this.atleast) {
					context.rollbackObjectStack(markerId);
					return right;
				}
				break;
			}
			left = right;
			//System.out.println("startPostion=" + startPosition + ", current=" + context.getPosition() + ", count = " + count);
			if(!(pos < context.getPosition())) {
				if(count < this.atleast) {
					return context.foundFailure2(this);
				}
				break;
			}
			count = count + 1;
		}
		context.rollbackObjectStack(markerId); //FIXME
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
	public Pego simpleMatch(Pego left, ParserContext context) {
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
	public Pego simpleMatch(Pego left, ParserContext context) {
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
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegAnd(base, this.flag, this.inner.clone(base, tr));
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitAnd(this);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
//		int markerId = this.markObjectStack();
		Pego right = this.inner.simpleMatch(left, context);
//		this.rollbackObjectStack(markerId);
		context.rollback(pos);
		return right;
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return this.inner.getPrediction(enforceCharset);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		int markerId = context.markObjectStack();
		long pos = context.getPosition();
		int right = this.inner.fastMatch(left, context);
		context.rollback(pos);
		context.rollbackObjectStack(markerId);
		if(PEGUtils.isFailure(right)) {
			return right;
		}
		return left;
	}
}

class PegNot extends PegUnary {
	PegNot(Grammar base, int flag, Peg e) {
		super(base, Peg.HasNot | flag, e);
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegNot(base, this.flag, this.inner.clone(base, tr));
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitNot(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return null;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
		Pego right = this.inner.simpleMatch(left, context);
		if(right.isFailure()) {
			return left;
		}
		context.rollback(pos);
		return context.foundFailure(this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		long pos = context.getPosition();
		int right = this.inner.fastMatch(left, context);
		if(PEGUtils.isFailure(right)) {
			return left;
		}
		context.rollback(pos);
		return context.foundFailure2(this);
	}
}

class PegNotString extends PegNot {
	byte[] textBuffer;
	public PegNotString(Grammar peg, int flag, PegString e) {
		super(peg, flag | Peg.NoMemo, e);
		this.textBuffer = e.textByte;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
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
	public Pego simpleMatch(Pego left, ParserContext context) {
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
	public Pego simpleMatch(Pego left, ParserContext context) {
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
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
		if(context.match(this.charset)) {
			context.setPosition(pos);
			return context.foundFailure(this);
		}
//		if(this.nextAny) {
//			return this.matchNextAny(left, context);
//		}
		return left;
	}
//	@Override
//	public int fastMatch(int left, MonadicParser context) {
//		long pos = context.getPosition();
//		if(context.match(this.charset)) {
//			context.setPosition(pos);
//			return context.foundFailure2(this);
//		}
//		if(this.nextAny) {
//			return this.fastMatchNextAny(left, context);
//		}
//		return left;
//	}
}


abstract class PegList extends Peg {
	protected UList<Peg> list;
	int length = 0;
	PegList(Grammar base, int flag, UList<Peg> list) {
		super(base, flag);
		this.list = list;
	}
	PegList(Grammar base, int flag, int initSize) {
		this(base, flag, new UList<Peg>(new Peg[initSize]));
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
	public void add(Peg e) {
		this.list.add(e);
	}
	
	@Override
	public Object getPrediction(boolean enforceCharset) {
		int start = 0;
		UCharset optionalCharset = null;
		while(start < this.size()) {
			Peg e = this.get(start);
			if(e instanceof PegTagging || e instanceof PegMessage || e instanceof PegNot) {
				start += 1;
				continue;
			}
			if(e instanceof PegOptional) {
				optionalCharset = p(optionalCharset, ((PegOptional) e).inner);
				if(optionalCharset == null) {
					return null;
				}
				start += 1;
				continue;
			}
			if(e instanceof PegRepeat && ((PegRepeat) e).atleast == 0) {
				optionalCharset = p(optionalCharset, ((PegRepeat) e).inner);
				if(optionalCharset == null) {
					return null;
				}
				start += 1;
				continue;
			}
			break;
		}
		if(start < this.size()) {
			if(optionalCharset != null) {
				UCharset u = this.get(start).getCharsetPrediction();
				if(u == null) {
					return null;
				}
				optionalCharset.append(u);
				//System.out.println("optional: " + optionalCharset + " by " + this);
				return optionalCharset;
			}
			return this.get(start).getPrediction(enforceCharset);
		}
		return null;
	}

	private UCharset p(UCharset optionalCharset, Peg inner) {
		UCharset u = inner.getCharsetPrediction();
		if(u == null) {
			return null;
		}
		if(optionalCharset == null) {
			return u;
		}
		optionalCharset.append(u);
		return optionalCharset;
	}
	
//	public boolean isSame(UList<Peg> flatList) {
//		if(this.size() == flatList.size()) {
//			for(int i = 0; i < this.size(); i++) {
//				if(this.get(i) != flatList.ArrayValues[i]) {
//					return false;
//				}
//			}
//			return true;
//		}
//		return false;
//	}

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
	PegSequence(Grammar base, int flag, int initSize) {
		super(base, flag, initSize);
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			PegList l = new PegSequence(base, this.flag, this.size());
			for(int i = 0; i < this.size(); i++) {
				Peg e = this.get(i).clone(base, tr);
				l.list.add(e);
				this.derived(e);
			}
			return l;
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitSequence(this);
	}	
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
		int mark = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			Pego right = this.get(i).simpleMatch(left, context);
			if(right.isFailure()) {
				context.rollbackObjectStack(mark);
				context.rollback(pos);
				return right;
			}
			left = right;
		}
		return left;
	}
	
	@Override
	public int fastMatch(int left, MonadicParser context) {
		long pos = context.getPosition();
		int markerId = context.markObjectStack();
		for(int i = 0; i < this.size(); i++) {
			int right = this.get(i).fastMatch(left, context);
			if(PEGUtils.isFailure(right)) {
				context.rollbackObjectStack(markerId);
				context.rollback(pos);
				return right;
			}
//			if(left != right) {
//				System.out.println("SEQ SWITCH i= " +i + ", " + context.S(left) +"=>" +context.S(right) + " by " + this.get(i));
//			}
			left = right;
		}
		return left;
	}

}

class PegChoice extends PegList {
	PegChoice(Grammar base, int flag, int initSize) {
		super(base, flag | Peg.HasChoice, initSize);
	}
	PegChoice(Grammar base, int flag, UList<Peg> list) {
		super(base, flag | Peg.HasChoice, list);
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			PegList l = new PegChoice(base, this.flag, this.size());
			for(int i = 0; i < this.size(); i++) {
				Peg e = this.get(i).clone(base, tr);
				l.list.add(e);
				this.derived(e);
			}
			return l;
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitChoice(this);
	}

	protected UCharset predicted = null;
	protected String[] predictedText = null;
	protected int strongPredictedChoice = 0;
	protected int unpredictedChoice = 0;
	protected int prefixSize = 0; 
	
	@Override
	public Object getPrediction(boolean enforceCharset) {
		if(predictedText == null) {
			predictedText = new String[this.size()];
			this.uset = new UCharset[this.size()];
			this.predicted = new UCharset("");
			this.prefixSize = Integer.MAX_VALUE;
			this.strongPredictedChoice = 0;
			this.unpredictedChoice = 0;
			for(int i = 0; i < this.size(); i++) {
				Object p = this.get(i).getPrediction(enforceCharset);
				if(p == null) {
					this.unpredictedChoice += 1;
					this.base.UnpredicatedChoice += 1;
					//System.out.println("unpredicted: " + this.get(i) + " by " + this.get(i).getClass().getSimpleName());
//					if(this.get(i) instanceof PegNonTerminal) {
//						System.out.println(" => " + ((PegNonTerminal)this.get(i)).nextRule);
//					}
					continue;
				}
				this.base.PredicatedChoice += 1;
				if(p instanceof UCharset) {
					this.predicted.append((UCharset)p);
					this.uset[i] = (UCharset)p;
					this.prefixSize = 1;
					continue;
				}
//				if(!(p instanceof String)) {
//					System.out.println("error: " + p.getClass() + ", " + this.get(i).getClass());
//				}
				String text = (String)p;
				this.predictedText[i] = text;
				this.uset[i] = this.get(i).getCharsetPrediction();
				this.predicted.append(this.uset[i]);
				if(text.length() < this.prefixSize) {
					this.prefixSize = text.length();
				}
				this.strongPredictedChoice += 1;
				this.base.StrongPredicatedChoice += 1;
			}
			if(this.unpredictedChoice > 0) {
				this.predicted = null;
			}
		}
		return this.predicted;
	}
			
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		if(this.caseOf != null) {
//			int ch = context.getChar();
//			long pos = context.getPosition();
			Pego right = this.predictedMatch(left, context);
//			context.rollback(pos);
//			Pego right2 = this.simpleMatchImpl(left, context);
//			if(!right.equals2(right2)) {
//				System.out.println("ch=" + (char)ch + " in " + this);
//			}
			return right;
		}
		for(int i = 0; i < this.size(); i++) {
			Pego right = this.get(i).simpleMatch(left, context);
			if(!right.isFailure()) {
				return right;
			}
		}
		return context.foundFailure(this);
	}

	private Pego simpleMatchImpl(Pego left, ParserContext context) {
		for(int i = 0; i < this.size(); i++) {
			Pego right = this.get(i).simpleMatch(left, context);
			if(!right.isFailure()) {
				return right;
			}
		}
		return context.foundFailure(this);
	}

	Peg[] caseOf = null;
	Peg   alwaysFailure = null;
	UCharset uset[] = null;
	boolean foundUnicode = false;

	void tryPrediction2(int maxUnpredicatedChoice) {
		if(this.unpredictedChoice <= maxUnpredicatedChoice) {
			this.caseOf = new Peg[128];
			this.alwaysFailure = new PegAlwaysFailure(this);
			this.base.PredictionOptimization += 1;
			if(this.unpredictedChoice == 0) {
				for(int i = 0; i < this.size(); i++) {
					UCharset u = this.uset[i];
					for(int ch = 0; ch < 128; ch ++) {
						if(u.hasChar((char)ch)) {
							this.caseOf[ch] = this.base.mergeChoice(this.caseOf[ch], this.get(i));
						}
						if(u.hasUnicode()) {
							this.foundUnicode = true;
						}
					}
				}
				for(int i = 0; i < this.caseOf.length; i++) {
					if(this.caseOf[i] == null) {
						this.caseOf[i] = this.alwaysFailure;
					}
				}
			}
		}
	}
	
	private Pego predictedMatch(Pego left, ParserContext context) {
		int ch = context.getChar();
		if(ch < 128) {
			if(caseOf[ch] == null) {
				caseOf[ch] = performSelection(ch);
			}
			return caseOf[ch].simpleMatch(left, context);
		}
		else {
			if(this.foundUnicode) {
				return this.simpleMatchImpl(left, context);
			}
		}
		return context.foundFailure(this);
	}
		

	boolean accept(int i, int ch) {
		if(uset[i] == null) {
			return true;
		}
		return this.uset[i].match(ch);
	}

	private Peg performSelection(int ch) {
		Peg e = null;
		for(int i = 0; i < this.size(); i++) {
			if(this.accept(i, ch)) {
				e = this.base.mergeChoice(e, this.get(i));
			}
		}
		if(e == null) {
			e = this.alwaysFailure;
		}
		//System.out.println("selected: '" + (char)ch + "': " + e.size() + " / " + this.size());
		return e;
	}

	@Override
	public int fastMatch(int left, MonadicParser context) {
		int node = left;
		long pos = context.getPosition();
		for(int i = 0; i < this.size(); i++) {
			int markerId = context.markObjectStack();
			node = this.get(i).fastMatch(left, context);
			if(!PEGUtils.isFailure(node)) {
				break;
			}
			context.rollbackObjectStack(markerId);
			context.setPosition(pos);
		}
		return node;
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
	public Pego simpleMatch(Pego left, ParserContext context) {
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

//class PegMappedChoice extends PegChoice {
//	private HashMap<String, Peg> map;
//	PegMappedChoice(PegChoice choice) {
//		super(choice.base, choice.flag, choice.list);
//		this.getPrediction();
//		this.map = new HashMap<String, Peg>(this.size() * 4 + 1);
//		for(int i = 0; i < this.size(); i++) {
//			Peg sub = this.get(i);
//			
//			String key = sub.getPrediction().toString().substring(0, this.prefixSize);
//			Peg defined = map.get(key);
//			if(defined != null) {
//				sub = defined.appendAsChoice(sub);
//			}
//			map.put(key, sub);
//		}
//	}
//
//	@Override
//	public Pego simpleMatch(Pego left, ParserContext context) {
////		char ch = context.getChar();
////		if(this.predicted.match(ch)) {
//			long pos = context.getPosition();
//			String token = context.substring(pos, pos + this.prefixSize);
//			Peg e = this.map.get(token);
//			if(e != null) {
//				Pego node2 = e.simpleMatch(left, context);
////				if(node2.isFailure()) {
////					context.setPosition(pos);
////				}
//				return node2;
//			}
////		}
//		return context.foundFailure(this);
//	}
//	
//	@Override
//	public int fastMatch(int left, MonadicParser context) {
//		long pos = context.getPosition();
//		String token = context.substring(pos, pos + this.prefixSize);
//		Peg e = this.map.get(token);
//		if(e != null) {
//			int node2 = e.fastMatch(left, context);
//			if(PEGUtils.isFailure(node2)) {
//				context.setPosition(pos);
//			}
//			return node2;
//		}
//		//System.out.println("failure: " + pos + " token='"+token+"' in " + this.map.keys());
//		return context.foundFailure2(this);
//	}
//}

class PegAlwaysFailure extends PegString {
	public PegAlwaysFailure(Peg orig) {
		super(orig.base, 0, "\0");
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.foundFailure(this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.foundFailure2(this);
	}
}

class PegMappedCharacterChoice extends PegChoice {
	Peg[] caseOf;
	boolean foundUnicode = false;
	Peg   alwaysFailure;
	UList<String> keyList;
	
	PegMappedCharacterChoice(PegChoice choice) {
		super(choice.base, choice.flag, choice.list);
		this.getPrediction(false/*enforceCharset*/);
		this.keyList = new UList<String>(new String[16]);
		this.caseOf = new Peg[128];
	}

	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		int ch = context.getChar();
		if(ch < 128) {
			if(caseOf[ch] != null) {
				return caseOf[ch].simpleMatch(left, context);
			}
		}
		else {
			if(this.foundUnicode) {
				return super.simpleMatch(left, context);
			}
		}
		return context.foundFailure(this);
	}
	
	@Override
	public int fastMatch(int left, MonadicParser context) {
		int ch = context.getChar();
		if(ch < 128) {
			if(caseOf[ch] != null) {
				return caseOf[ch].fastMatch(left, context);
			}
		}
		else {
			if(this.foundUnicode) {
				return super.fastMatch(left, context);
			}
		}
		return context.foundFailure2(this);
	}
}

class PegSelectiveChoice extends PegChoice {
	Peg[] caseOf;
	Peg   alwaysFailure;
	UCharset uset[];
	PegSelectiveChoice(PegChoice c) {
		super(c.base, c.flag, c.list);
		this.caseOf = new Peg[128];
		this.alwaysFailure = new PegAlwaysFailure(c);
		uset = new UCharset[this.size()];
		for(int i = 0; i < uset.length; i++) {
			uset[i] = this.get(i).getCharsetPrediction();
		}
	}
			
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		int ch = context.getChar();
		if(ch < 128) {
			if(caseOf[ch] == null) {
				caseOf[ch] = performSelection(ch);
			}
			//System.out.println("selecting ch = " + (char)ch);
			return caseOf[ch].simpleMatch(left, context);
		}
		return super.simpleMatch(left, context);
	}

	@Override
	boolean accept(int i, int ch) {
		if(uset[i] == null) {
			return true;
		}
		return this.uset[i].match(ch);
	}
	
	private Peg performSelection(int ch) {
		Peg e = null;
		for(int i = 0; i < this.size(); i++) {
			if(this.accept(i, ch)) {
				e = this.base.mergeChoice(e, this.get(i));
			}
		}
		if(e == null) {
			e = this.alwaysFailure;
		}
		System.out.println("selected: '" + (char)ch + "': " + e.size() + " / " + this.size());
		return e;
	}

	@Override
	public int fastMatch(int left, MonadicParser context) {
		int ch = context.getChar();
		if(ch < 128) {
			if(caseOf[ch] == null) {
				caseOf[ch] = performSelection(ch);
			}
			return caseOf[ch].fastMatch(left, context);
		}
		return super.fastMatch(left, context);
	}
	
}


class PegSetter extends PegUnary {
	public int index;
	public PegSetter(Grammar base, int flag, Peg e, int index) {
		super(base, flag | Peg.HasSetter | Peg.NoMemo, e);
		this.index = index;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegSetter(base, this.flag, this.inner.clone(base, tr), this.index);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitSetter(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return this.inner.getPrediction(enforceCharset);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchSetter(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchSetter(left, this);
	}
}

class PegTagging extends PegTerm {
	String symbol;
	public PegTagging(Grammar base, int flag, String tagName) {
		super(base, Peg.HasTagging | Peg.NoMemo | flag);
		this.symbol = tagName;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegTagging(base, this.flag, this.symbol);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitTagging(this);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		left.setTag(this.symbol);
		return left;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		context.log.lazyTagging(left, this);
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
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegMessage(base, this.flag, this.symbol);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitMessage(this);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchMessage(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		context.log.lazyMessaging(left, this);
		return left;
	}
}

class PegNewObject extends PegList {
	boolean leftJoin = false;
	String nodeName = "noname";
	int prefetchIndex = 0;
	public PegNewObject(Grammar base, int flag, int initSize, boolean leftJoin) {
		super(base, flag | Peg.HasNewObject, initSize);
		this.leftJoin = leftJoin;
	}
	public PegNewObject(Grammar base, int flag, boolean leftJoin, Peg e) {
		super(base, flag | Peg.HasNewObject, e.size());
		this.leftJoin = leftJoin;
		if(e instanceof PegSequence) {
			for(int i = 0; i < e.size(); i++) {
				this.add(e.get(i));
			}
		}
		else {
			this.add(e);
		}
	}

	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			PegList l = new PegNewObject(base, this.flag, this.size(), this.leftJoin);
			for(int i = 0; i < this.size(); i++) {
				l.list.add(this.get(i).clone(base, tr));
			}
			return l;
		}
		return ne;
	}

	@Override
	protected void visit(PegProbe probe) {
		probe.visitNewObject(this);
	}

	@Override
	public void add(Peg e) {
		if(e instanceof PegSequence) {
			for(int i =0; i < e.size(); i++) {
				this.list.add(e.get(i));
			}
		}
		else {
			this.list.add(e);
		}
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		Pego leftNode = left;
		long startIndex = context.getPosition();
		for(int i = 0; i < this.prefetchIndex; i++) {
			Pego right = this.get(i).simpleMatch(left, context);
			if(right.isFailure()) {
				context.rollback(startIndex);
				return right;
			}
			if(left != right) {
				System.out.println("DEBUG: @" + i + " < " + this.prefetchIndex + " in " + this);
				System.out.println("LEFT: " + left);
				System.out.println("RIGHT: " + right);
				System.out.println("FLAGS: " + this.get(i).hasObjectOperation());
			}
			assert(left == right);
		}
		int mark = context.markObjectStack();
		Pego newnode = context.newPegObject(this.nodeName, this, startIndex);
		if(this.leftJoin) {
			context.pushSetter(newnode, -1, leftNode);
		}
		for(int i = this.prefetchIndex; i < this.size(); i++) {
			Pego node = this.get(i).simpleMatch(newnode, context);
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
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchNewObject(left, this);
	}
}

class PegExport extends PegUnary {
	public PegExport(Grammar base, int flag, Peg e) {
		super(base, flag | Peg.NoMemo, e);
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			return new PegExport(base, this.flag, this.inner.clone(base, tr));
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitExport(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return this.inner.getPrediction(enforceCharset);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchExport(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchExport(left, this);
	}
}

class PegIndent extends PegTerm {
	PegIndent(Grammar base, int flag) {
		super(base, flag | Peg.HasContext);
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegIndent(base, this.flag);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitIndent(this);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchIndent(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchIndent(left, this);
	}
}

class PegIndex extends PegTerm {
	int index;
	PegIndex(Grammar base, int flag, int index) {
		super(base, flag | Peg.HasContext);
		this.index = index;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		Peg ne = tr.transform(base, this);
		if(ne == null) {
			ne = new PegIndex(base, this.flag, this.index);
		}
		return ne;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitIndex(this);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchIndex(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchIndex(left, this);
	}
}



abstract class PegOperation extends Peg {
	Peg inner;
	protected PegOperation(Peg inner) {
		super(inner.base, inner.flag);
		this.inner = inner;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		return this;
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitOperation(this);
	}
	@Override
	public Object getPrediction(boolean enforceCharset) {
		return this.inner.getPrediction(enforceCharset);
	}
	@Override
	public abstract Pego simpleMatch(Pego left, ParserContext context);
	@Override
	public abstract int fastMatch(int left, MonadicParser context);
}

class PegMemo extends PegOperation {
	Peg parent = null;
	boolean enableMemo = true;
	int memoHit = 0;
	int memoMiss = 0;

	protected PegMemo(Peg inner) {
		super(inner);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		if(!this.enableMemo) {
			return this.inner.simpleMatch(left, context);
		}
		long pos = context.getPosition();
		ObjectMemo m = context.memoMap.getMemo(this, pos);
		if(m != null) {
			this.memoHit += 1;
			if(m.generated == null) {
				return context.refoundFailure(this.inner, pos+m.consumed);
			}
			context.setPosition(pos + m.consumed);
			return m.generated;
		}
		if(context.stat != null) {
			context.stat.countRepeatCall(this, pos);
		}
		Pego result = this.inner.simpleMatch(left, context);
		if(result.isFailure()) {
			context.memoMap.setMemo(pos, this, null, (int)(result.getSourcePosition() - pos));
		}
		else {
			context.memoMap.setMemo(pos, this, result, (int)(context.getPosition() - pos));
		}
		this.memoMiss += 1;
		if(Main.TracingMemo) {
			if(this.memoMiss == 32) {
				if(this.memoHit < 4) {
					return disabledMemo(result);
				}
			}
			if(this.memoMiss % 64 == 0) {
				if(this.memoMiss / this.memoHit > 4) {
					return disabledMemo(result);
				}
			}
		}
		return result;
	}
	
	private Pego disabledMemo(Pego left) {
		this.enableMemo = false;
		this.base.DisabledMemo += 1;
		int factor = this.base.EnabledMemo / 20;
		if(this.base.DisabledMemo % factor == 0) {
			//System.out.println("disabled: ");
			this.base.memoRemover.removeDisabled();
		}
		return left;
	}
	
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return this.inner.fastMatch(left, context);
	}
}

class PegMonad extends PegOperation {
	protected PegMonad(Peg inner) {
		super(inner);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		int mark = context.markObjectStack();
		left = this.inner.simpleMatch(left, context);
		context.rollbackObjectStack(mark);
		return left;
	}
	
	@Override
	public int fastMatch(int left, MonadicParser context) {
		int mark = context.markObjectStack();
		left = this.inner.fastMatch(left, context);
		context.rollbackObjectStack(mark);
		return left;
	}
}

class PegCommit extends PegOperation {
	protected PegCommit(Peg inner) {
		super(inner);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		int mark = context.markObjectStack();
		left = this.inner.simpleMatch(left, context);
		if(left.isFailure()) {
			context.rollbackObjectStack(mark);
		}
		return left;
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		int mark = context.markObjectStack();
		left = this.inner.fastMatch(left, context);
		if(PEGUtils.isFailure(left)) {
			context.rollbackObjectStack(mark);
		}
		return left;
	}
}



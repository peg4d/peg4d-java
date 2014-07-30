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
	public final static int hasReserved       = 1 << 16;
	public final static int Mask = HasNonTerminal | HasString | HasCharacter | HasAny
	                             | HasRepetation | HasOptional | HasChoice | HasAnd | HasNot
	                             | HasNewObject | HasSetter | HasTagging | HasMessage 
	                             | HasReserved | hasReserved | HasContext;
	public final static int LeftObjectOperation    = 1 << 17;
	public final static int PossibleDifferentRight = 1 << 18;
	
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
	protected abstract void stringfy(UStringBuilder sb, PegFormatter fmt);
	protected abstract void visit(PegProbe probe);
	protected abstract void makeList(String startRule, Grammar parser, UList<String> list, UMap<String> set);
	protected abstract void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited);
	public abstract Pego simpleMatch(Pego left, ParserContext context);
	public abstract int fastMatch(int left, MonadicParser context);
	
	public Object getPrediction() {
		return null;
	}

	public final boolean isPredictablyAcceptable(int ch) {
		Object predicted = this.getPrediction();
		if(predicted != null) {
			if(predicted instanceof String) {
				String p = (String)predicted;
				if(p.charAt(0) == ch) {
					return true;
				}
				return false;
			}
			if(predicted instanceof UCharset) {
				UCharset p = (UCharset)predicted;
				if(p.match(ch)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public final boolean is(int uflag) {
		return ((this.flag & uflag) == uflag);
	}

	public final void set(int uflag) {
		this.flag = this.flag | uflag;
	}

	protected final void derived(Peg e) {
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

	@Override public String toString() {
		UStringBuilder sb = new UStringBuilder();
		this.stringfy(sb, new PegFormatter());
//		if(this.ruleName != null) {
//			sb.append(" defined in ");
//			sb.append(this.ruleName);
//		}
		return sb.toString();
	}

	public final String format(String name, PegFormatter fmt) {
		UStringBuilder sb = new UStringBuilder();
		fmt.formatRule(sb, name, this);
		return sb.toString();
	}

	public final String format(String name) {
		return this.format(name, new PegFormatter());
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
		return this.is(Peg.HasNewObject) || this.is(Peg.HasSetter);
	}
	
	public final static void addAsChoice(UMap<Peg> map, String key, Peg e) {
		Peg defined = map.get(key);
		if(defined != null) {
			defined = defined.appendAsChoice(e);
			map.put(key, defined);
		}
		else {
			map.put(key, e);
		}
	}

	public final static Peg appendAsChoice(Peg e, Peg e1) {
		if(e == null) {
			return e1;
		}
		if(e1 == null) {
			return e;
		}
		if(e instanceof PegChoice) {
			((PegChoice) e).extend(e1);
			return e;
		}
		else {
			PegChoice choice = new PegChoice(e.base, 0, 2);
			choice.extend(e);
			choice.extend(e1);
			return choice;
		}
	}
	
	public final PegChoice appendAsChoice(Peg e) {
		if(this instanceof PegChoice) {
			((PegChoice)this).extend(e);
			return ((PegChoice)this);
		}
		else {
			PegChoice choice = new PegChoice(this.base, 0, 2);
			choice.add(this);
			choice.extend(e);
			return choice;
		}
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
	protected void makeList(String startRule, Grammar rules, UList<String> list, UMap<String> set) {
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
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
	Object predicted = null;
	Peg nextRule = null;
	
	PegNonTerminal(Grammar base, int flag, String ruleName) {
		super(base, flag | Peg.HasNonTerminal);
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatNonTerminal(sb, this);
	}
	@Override
	protected void visit(PegProbe probe) {
		probe.visitNonTerminal(this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		Peg next = this.base.getRule(this.symbol);
		if( next == null && Main.StatLevel == 0) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.sourcePosition, "undefined label: " + this.symbol));
			this.base.foundError = true;
			return;
		}
		next.refc = this.refc;
		if(ruleName.equals(this.symbol)) {
			nonTerminal.set(CyclicRule);
		}
		else if(visited != null && !visited.hasKey(this.symbol)) {
			visited.put(this.symbol, this.symbol);
			this.verify2(ruleName, nonTerminal, this.symbol, visited);
		}
		this.derived(next);
		nonTerminal.derived(this);
	}
	@Override
	public Object getPrediction() {
		return this.predicted;
	}
	@Override
	protected void makeList(String startRule, Grammar parser, UList<String> list, UMap<String> set) {
		if(!set.hasKey(this.symbol)) {
			Peg next = parser.getRule(this.symbol);
			list.add(this.symbol);
			set.put(this.symbol, this.symbol);
			next.makeList(startRule, parser, list, set);
		}
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchNonTerminal(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		Peg next = context.getRule(this.symbol);
		return next.fastMatch(left, context);
	}
}

class PegString extends PegTerm {
	String text;
	public PegString(Grammar base, int flag, String text) {
		super(base, Peg.HasString | flag);
		this.text = text;
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatString(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
	}
	@Override
	public Object getPrediction() {
		if(this.text.length() > 0) {
			return this.text;
		}
		return null;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		if(context.match(this.text)) {
			return left;
		}
		return context.foundFailure(this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		if(context.match(this.text)) {
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
		super(base, Peg.HasAny | flag);
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatAny(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
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

class PegCharacter extends PegTerm {
	UCharset charset;
	public PegCharacter(Grammar base, int flag, String token) {
		super(base, Peg.HasCharacter | flag);
		this.charset = new UCharset(token);
	}
	public PegCharacter(Grammar base, int flag, UCharset charset) {
		super(base, Peg.HasCharacter | flag);
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatCharacter(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		this.set(Peg.HasCharacter);
		nonTerminal.derived(this);
	}
	@Override
	public Object getPrediction() {
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
	@Override
	protected void makeList(String startRule, Grammar parser, UList<String> list, UMap<String> set) {
		this.inner.makeList(startRule, parser, list, set);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		this.inner.verify2(ruleName, nonTerminal, visitingName, visited);
		this.derived(this.inner);
	}
}

class PegOptional extends PegUnary {
	public PegOptional(Grammar base, int flag, Peg e) {
		super(base, flag | Peg.HasOptional, e);
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatOptional(sb, this);
	}

	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatRepeat(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
	}
	@Override
	public Object getPrediction() {
		if(this.atleast > 0) {
			this.inner.getPrediction();
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatAnd(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		this.set(Peg.HasAnd);
		nonTerminal.derived(this);
		if(visited == null) { /* in the second phase */
			if(this.inner.is(Peg.HasNewObject) || this.inner.is(Peg.HasSetter)) {
				this.report("warning", "ignored object operation");
			}
		}
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchAnd(left, this);
	}
	@Override
	public Object getPrediction() {
		return this.inner.getPrediction();
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatNot(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
		if(visited == null) { /* in the second phase */
			if(this.inner.hasObjectOperation()) {
				this.report("warning", "ignored object operation");
			}
		}
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
	String symbol;
	public PegNotString(Grammar peg, int flag, PegString e) {
		super(peg, flag, e);
		this.symbol = e.text;
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		long pos = context.getPosition();
		if(context.match(this.symbol)) {
			//context.setPosition(pos);
			return context.foundFailure(this);
		}
//		if(this.nextAny) {
//			return this.matchNextAny(left, context);
//		}
		return left;
	}
}

class PegNotString1 extends PegNot {
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
//		if(this.nextAny) {
//			return this.matchNextAny(left, context);
//		}
		return left;
	}
}	

class PegNotString2 extends PegNot {
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
		super(base, flag, e);
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
	public boolean isSame(UList<Peg> flatList) {
		if(this.size() == flatList.size()) {
			for(int i = 0; i < this.size(); i++) {
				if(this.get(i) != flatList.ArrayValues[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

//	public final void swap(int i, int j) {
//		Peg e = this.list.ArrayValues[i];
//		this.list.ArrayValues[i] = this.list.ArrayValues[j];
//		this.list.ArrayValues[j] = e;
//	}
	@Override
	protected void makeList(String startRule, Grammar parser, UList<String> list, UMap<String> set) {
		for(int i = 0; i < this.size(); i++) {
			this.get(i).makeList(startRule, parser, list, set);
		}
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		for(int i = 0; i < this.size(); i++) {
			Peg e  = this.get(i);
			e.verify2(ruleName, nonTerminal, visitingName, visited);
			this.derived(e);
		}
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatSequence(sb, this);
	}

	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
	}
	
	@Override
	public Object getPrediction() {
		return this.get(0).getPrediction();
	}
	
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchSequence(left, this);
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

	public void extend(Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				this.add(e.get(i));
			}
		}
		else if(e != null) {
			this.list.add(e);
		}
	}
	
	@Override protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatChoice(sb, this);
	}
	
	protected UCharset predicted = null;
	protected int predictableChoice = -1;
	protected int prefixSize = 0; 
	
	@Override
	public Object getPrediction() {
		if(this.predictableChoice == -1) {
			UCharset u = new UCharset("");
			this.predictableChoice = 0;
			this.predicted = null;
			this.prefixSize = Integer.MAX_VALUE;
			for(int i = 0; i < this.size(); i++) {
				Object p = this.get(i).getPrediction();
				if(p instanceof UCharset) {
					u.append((UCharset)p);
					this.predictableChoice += 1;
					this.prefixSize = 1;
				}
				if(p instanceof String) {
					String text = (String)p;
					if(text.length() > 0) {
						u.append(text.charAt(0));
						this.predictableChoice += 1;
						if(text.length() < this.prefixSize) {
							this.prefixSize = text.length();
						}
					}
				}
			}
			if(this.predictableChoice == this.size()) {
				this.predicted = u;
			}
		}
		return this.predicted;
	}
	
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchChoice(left, this);
	}
	
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
		if(visited == null) {  // in the second phase
			this.predictableChoice = -1;
			this.predicted = null; // reset
			this.getPrediction();
		}
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

class PegSetter extends PegUnary {
	public int index;
	public PegSetter(Grammar base, int flag, Peg e, int index) {
		super(base, flag | Peg.HasSetter, e);
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
	@Override protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatSetter(sb, this);
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchSetter(left, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
		if(visited == null) { /* in the second phase */
			if(!this.inner.is(Peg.HasNewObject)) {
				this.report("warning", "no object is generated");
			}
		}
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchSetter(left, this);
	}
}

class PegTagging extends PegTerm {
	String symbol;
	public PegTagging(Grammar base, int flag, String tagName) {
		super(base, Peg.HasTagging | flag);
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
	protected final void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatTagging(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		//rules.addObjectLabel(this.symbol);
		nonTerminal.derived(this);
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
		super(base, flag | Peg.HasMessage);
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
	protected final void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatMessage(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
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
	int predictionIndex = 0;
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

	@Override protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatNewObject(sb, this);
	}

	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
	}
	@Override
	public Object getPrediction() {
		return this.get(0).getPrediction();
	}
	@Override
	public Pego simpleMatch(Pego left, ParserContext context) {
		return context.matchNewObject(left, this);
	}
	@Override
	public int fastMatch(int left, MonadicParser context) {
		return context.matchNewObject(left, this);
	}
}

class PegExport extends PegUnary {
	public PegExport(Grammar base, int flag, Peg e) {
		super(base, flag, e);
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

	@Override protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatExport(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
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

	@Override protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatIndent(sb, this);
	}

	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
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

	@Override protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		fmt.formatIndex(sb, this);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		super.verify2(ruleName, nonTerminal, visitingName, visited);
		nonTerminal.derived(this);
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
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		this.inner.stringfy(sb, fmt);
	}
	@Override
	protected void visit(PegProbe probe) {
		this.inner.visit(probe);
	}
	@Override
	protected void makeList(String startRule, Grammar parser, UList<String> list, UMap<String> set) {
		this.inner.makeList(startRule, parser, list, set);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		this.inner.verify2(ruleName, nonTerminal, visitingName, visited);
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
		if(this.memoMiss == 32) {
			if(this.memoHit < 4) {
				this.enableMemo = false;
			}
		}
		if(this.memoMiss % 64 == 0) {
			if(this.memoMiss / this.memoHit > 4) {
				this.enableMemo = false;
			}
		}
		return result;
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



abstract class PegOptimized extends Peg {
	Peg orig;
	PegOptimized (Peg orig) {
		super(orig.base, orig.flag);
		this.orig = orig;
	}
	@Override
	protected Peg clone(Grammar base, PegTransformer tr) {
		return this;
	}
	@Override
	protected void stringfy(UStringBuilder sb, PegFormatter fmt) {
		this.orig.stringfy(sb, fmt);
	}
	@Override
	protected void verify2(String ruleName, Peg nonTerminal, String visitingName, UMap<String> visited) {
		this.orig.verify2(ruleName, nonTerminal, visitingName, visited);
		this.derived(this.orig);
	}
	@Override
	protected void makeList(String startRule, Grammar parser, UList<String> list, UMap<String> set) {
	}
	@Override
	protected void visit(PegProbe probe) {
	}
}


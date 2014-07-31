package org.peg4d;

public class Grammar {
	public final static PEG4dGrammar PEG4d = new PEG4dGrammar();

	String              name;
	UList<Peg>          pegList;
	UMap<Peg>           pegMap;
	UMap<String>        objectLabelMap = null;
	public boolean      foundError = false;
	public int          optimizationLevel;

	int LexicalOptimization       = 0;
	int InliningCount             = 0;
	int InterTerminalOptimization = 0;
	int PredictionOptimization    = 0;

	MemoRemover memoRemover = null;
		
	public Grammar() {
		this.pegMap  = new UMap<Peg>();
		this.pegList = new UList<Peg>(new Peg[128]);
		this.optimizationLevel = Main.OptimizationLevel;
	}

	public String getName() {
		return this.name;
	}

	public final boolean loadGrammarFile(String fileName) {
		PEG4dGrammar peg4d = Grammar.PEG4d;
		ParserContext p = peg4d.newParserContext(Main.loadSource(fileName));
		this.name = fileName;
		p.setRecognitionOnly(false);
		while(p.hasNode()) {
			Pego pego = p.parseNode("TopLevel");
			if(pego.isFailure()) {
				Main._Exit(1, "FAILED: " + pego);
				break;
			}
			if(!PEG4dGrammar.parse(this, p, pego)) {
				break;
			}
		}
		ObjectRemover objectRemover = null;
		if(Main.RecognitionOnlyMode) {
			objectRemover = new ObjectRemover();
		}
		this.verify(objectRemover);
		return this.foundError;
	}
	
	public final static Grammar load(String fileName) {
		Grammar peg = new Grammar();
		peg.loadGrammarFile(fileName);
		return peg;
	}
	
	public final Peg getPeg(long oid) {
		int index = (short)oid;
		return this.pegList.ArrayValues[index-1];
	}

	public final boolean hasRule(String ruleName) {
		return this.pegMap.get(ruleName, null) != null;
	}

	public final Peg getRule(String ruleName) {
		return this.pegMap.get(ruleName, null);
	}
	
	public final void setRule(String ruleName, Peg e) {
		this.pegMap.put(ruleName, e);
	}

	public final UList<Peg> getRuleList() {
		UList<String> nameList = this.pegMap.keys();
		UList<Peg> pegList = new UList<Peg>(new Peg[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			pegList.add(this.getRule(ruleName));
		}
		return pegList;
	}

	public final void verify(ObjectRemover objectRemover) {
		//this.objectLabelMap = new UMap<String>();
		this.foundError = false;
		UList<String> nameList = this.pegMap.keys();
		NonTerminalChecker nc = new NonTerminalChecker();
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			Peg nonTerminal = this.pegMap.get(ruleName, null);
			if(objectRemover != null) {
				//System.out.println("B: " + ruleName + "= " + nonTerminal);
				nonTerminal = objectRemover.removeObjectOperation(nonTerminal);
				//System.out.println("A: " + ruleName + "= " + nonTerminal);
				this.pegMap.put(ruleName, nonTerminal);
			}
			nc.verify(ruleName, nonTerminal);
		}

		new Inliner(this).performInlining();
		new Optimizer(this).optimize();
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		this.memoRemover = new MemoRemover(this);
	}

	private int MultiReference = 0;
	private int Reference = 0;
	int StrongPredicatedChoice = 0;
	int PredicatedChoice = 0;
	int UnpredicatedChoice = 0;


	void updateStat(Stat stat) {
		stat.setCount("PegReference",   this.Reference);
		stat.setCount("MultiReference", this.MultiReference);
		stat.setRatio("Complexity", this.MultiReference, this.Reference);
		stat.setCount("StrongPredicatedChoice",   this.StrongPredicatedChoice);
		stat.setCount("PredicatedChoice",   this.PredicatedChoice);
		stat.setCount("UnpredicatedChoice", this.UnpredicatedChoice);
		stat.setRatio("Predictablity", this.PredicatedChoice, this.PredicatedChoice + this.UnpredicatedChoice);

		stat.setCount("ActivatedMemo", this.EnabledMemo);
		stat.setCount("DisabledMemo", this.DisabledMemo);
		stat.setCount("RemovedMemo", this.memoRemover.RemovedCount);
		stat.setCount("LexicalOptimization", this.LexicalOptimization);
		stat.setCount("InterTerminalOptimization", this.InterTerminalOptimization);
		stat.setCount("PredictionOptimization", this.PredictionOptimization);
		
		
//		for(int i = 0; i < this.pegList.size(); i++) {
//			Peg e = this.pegList.ArrayValues[i];
//			if(e instanceof PegMemo) {
//				PegMemo me = (PegMemo)e;
//				if(me.enableMemo && me.memoMiss > 32) {
//					double f = (double)me.memoHit / me.memoMiss;
//					System.out.println("#h/m=" + me.memoHit + "," + me.memoMiss + ", f=" + f + " c=" + e.refc + " " + e);
//				}
//			}
//		}
	}

	public ParserContext newParserContext(ParserSource source) {
		ParserContext p = new TracingPackratParser(this, source);
		if(Main.RecognitionOnlyMode) {
			p.setRecognitionOnly(true);
		}
		return p;
	}

	public final UList<String> makeList(String startPoint) {
		return new ListMaker().make(this, startPoint);
	}

	public final void show(String startPoint) {
		this.show(startPoint, new Formatter());
	}

	public final void show(String startPoint, Formatter fmt) {
		UStringBuilder sb = new UStringBuilder();
		UList<String> list = makeList(startPoint);
		for(int i = 0; i < list.size(); i++) {
			String name = list.ArrayValues[i];
			Peg e = this.getRule(name);
			fmt.formatRule(name, e, sb);
		}
		System.out.println(sb.toString());
	}
	
	// factory
	
	UMap<Peg> semMap = new UMap<Peg>();

	int EnabledMemo  = 0;
	int DisabledMemo = 0;


	private static String prefixNonTerminal = "L\b";
	private static String prefixString = "t\b";
	private static String prefixCharacter = "c\b";
	private static String prefixNot = "!\b";
	private static String prefixAnd = "&\b";
	private static String prefixOneMore = "+\b";
	private static String prefixZeroMore = "*\b";
	private static String prefixOptional = "?\b";
	private static String prefixSequence = " \b";
	private static String prefixChoice = "/\b";
	
	private static String prefixSetter  = "S\b";
	private static String prefixTagging = "T\b";
	private static String prefixMessage = "M\b";
	
	private Peg getsem(String t) {
		Peg e = semMap.get(t);
		if(e != null) {
			this.MultiReference += 1;
			e.refc += 1;
		}
		this.Reference += 1;
		return e;
	}

	private Peg putsem(String t, Peg e) {
		if(Main.MemoFactor > 0 && !e.is(Peg.NoMemo)) {
			this.EnabledMemo += 1;
			e = new PegMemo(e);
		}
		semMap.put(t, e);
		return e;
	}
	
	public final Peg newMemoTodo(Peg e) {
		if(e instanceof PegMemo) {
			return e;
		}
		return new PegMemo(e);
	}

	public final Peg newNonTerminal(String text) {
		Peg e = getsem(prefixNonTerminal + text);
		if(e == null) {
			e = new PegNonTerminal(this, 0, text);
			e = putsem(prefixNonTerminal + text, e);
		}
		return e;
	}

	public final Peg newString(String text) {
		Peg e = getsem(prefixString + text);
		if(e == null) {
			e = putsem(prefixString + text, this.newStringImpl(text));
		}
		return e;
	}

	private Peg newStringImpl(String text) {
		if(this.optimizationLevel > 0) {
			if(text.length() == 1) {
				this.LexicalOptimization += 1;
				return new PegString1(this, 0, text);				
			}				
			if(text.length() == 2) {
				this.LexicalOptimization += 1;
				return new PegString2(this, 0, text);				
			}				
		}
		return new PegString(this, 0, text);	
	}
	
	public final Peg newAny() {
		Peg e = getsem("");
		if(e == null) {
			e = new PegAny(this, 0);
			e = putsem("", e);
		}
		return e;
	}
	
	public final Peg newCharacter(String text) {
		UCharset u = new UCharset(text);
		String key = prefixCharacter + u.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegCharacter(this, 0, u);
			e = putsem(key, e);
		}
		return e;
	}

	public final Peg newOptional(Peg p) {
		String key = prefixOptional + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newOptionalImpl(p));
		}
		return e;
	}

	private Peg newOptionalImpl(Peg p) {
		if(p instanceof PegString1) {
			this.LexicalOptimization += 1;
			return new PegOptionalString1(this, 0, (PegString1)p);
		}
		if(p instanceof PegString) {
			this.LexicalOptimization += 1;
			return new PegOptionalString(this, 0, (PegString)p);
		}
		if(p instanceof PegCharacter) {
			this.LexicalOptimization += 1;
			return new PegOptionalCharacter(this, 0, (PegCharacter)p);
		}
		return new PegOptional(this, 0, newCommit(p));
	}
	
	private Peg newCommit(Peg p) {
		if(!p.is(Peg.HasNewObject) && !p.is(Peg.HasNonTerminal) && !p.is(Peg.HasSetter)) {
			return p;
		}
		return new PegCommit(p);
	}

	private Peg newMonad(Peg p) {
		if(!p.is(Peg.HasNewObject) && !p.is(Peg.HasNonTerminal) && !p.is(Peg.HasSetter)) {
			return p;
		}
		return new PegMonad(p);
	}
	
	public final Peg newOneMore(Peg p) {
		String key = prefixOneMore + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newOneMoreImpl(p));
		}
		return e;
	}

	private Peg newOneMoreImpl(Peg p) {
		if(this.optimizationLevel > 0) {
			if(p instanceof PegCharacter) {
				this.LexicalOptimization += 1;
				return new PegOneMoreCharacter(this, 0, (PegCharacter)p);
			}
		}
		return new PegRepeat(this, 0, newCommit(p), 1);
	}
	
	public final Peg newZeroMore(Peg p) {
		String key = prefixZeroMore + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newZeroMoreImpl(p));
		}
		return e;
	}

	private Peg newZeroMoreImpl(Peg p) {
		if(p instanceof PegCharacter) {
			this.LexicalOptimization += 1;
			return new PegZeroMoreCharacter(this, 0, (PegCharacter)p);
		}
		return new PegRepeat(this, 0, newCommit(p), 0);
	}

	public final Peg newAnd(Peg p) {
		String key = prefixAnd + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newAndImpl(p));
		}
		return e;
	}
	
	private Peg newAndImpl(Peg p) {
		if(p instanceof PegOperation) {
			p = ((PegOperation)p).inner;
		}
		return new PegAnd(this, 0, newMonad(p));
	}

	public final Peg newNot(Peg p) {
		String key = prefixNot + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, this.newNotImpl(p));
		}
		return e;
	}
	
	private Peg newNotImpl(Peg p) {
		if(this.optimizationLevel > 0) {
			if(p instanceof PegString) {
				this.LexicalOptimization += 1;
				if(p instanceof PegString1) {
					return new PegNotString1(this, 0, (PegString1)p);
				}
				if(p instanceof PegString2) {
					return new PegNotString2(this, 0, (PegString2)p);
				}
				return new PegNotString(this, 0, (PegString)p);
			}
			if(p instanceof PegCharacter) {
				this.LexicalOptimization += 1;
				return new PegNotCharacter(this, 0, (PegCharacter)p);
			}
		}
		if(p instanceof PegOperation) {
			p = ((PegOperation)p).inner;
		}
		return new PegNot(this, 0, newMonad(p));
	}
	
	public Peg newChoice(UList<Peg> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		String key = prefixChoice;
		boolean isAllText = true;
		for(int i = 0; i < l.size(); i++) {
			Peg se = l.ArrayValues[i];
			key += se.key();
			if(!(se instanceof PegString) && !(se instanceof PegString)) {
				isAllText = false;
			}
		}
		Peg e = getsem(key);
		if(e == null) {
			e = putsem(key, newChoiceImpl(l, isAllText));
		}
		return e;
	}

	private Peg newChoiceImpl(UList<Peg> l, boolean isAllText) {
		if(this.optimizationLevel > 0) {
			if(isAllText) {
				return new PegWordChoice(this, 0, l);
			}
		}
		return new PegChoice(this, 0, l);
	}
	
	public Peg mergeChoice(Peg e, Peg e2) {
		if(e == null) return e2;
		if(e2 == null) return e;
		UList<Peg> l = new UList<Peg>(new Peg[e.size()+e2.size()]);
		addChoice(l, e);
		addChoice(l, e2);
		return new PegChoice(this, 0, l);
	}

	
	public Peg newSequence(UList<Peg> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		if(l.size() == 0) {
			return this.newString("");
		}
		String key = prefixSequence;
		for(int i = 0; i < l.size(); i++) {
			key += l.ArrayValues[i].key();
		}
		Peg e = getsem(key);
		if(e == null) {
			e = newSequenceImpl(l);
			e = putsem(key, e);
		}
		return e;
	}

	private Peg newSequenceImpl(UList<Peg> l) {
		Peg orig = new PegSequence(this, 0, l);
		if(this.optimizationLevel > 1) {
			int nsize = l.size()-1;
			if(nsize > 0 && l.ArrayValues[nsize] instanceof PegAny) {
				boolean allNot = true;
				for(int i = 0; i < nsize; i++) {
					if(!(l.ArrayValues[nsize] instanceof PegNot)) {
						allNot = false;
						break;
					}
				}
				if(allNot) {
					return newNotAnyImpl(orig, l, nsize);
				}
			}
		}
		return orig;
	}

	private Peg newNotAnyImpl(Peg orig, UList<Peg> l, int nsize) {
		if(nsize == 1) {
			return new PegNotAny(this, 0, (PegNot)l.ArrayValues[0], orig);
		}
		return orig;
	}
	
	public final Peg newSetter(Peg p, int index) {
		String key = prefixSetter + index + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegSetter(this, 0, p, index);
			e = putsem(key, e);
		}
		return e;
	}

	public final Peg newTagging(String tag) {
		String key = prefixTagging + tag;
		Peg e = getsem(key);
		if(e == null) {
			e = new PegTagging(this, 0, tag);
			e = putsem(key, e);
		}
		return e;
	}

	public final Peg newMessage(String msg) {
		String key = prefixMessage + msg;
		Peg e = getsem(key);
		if(e == null) {
			e = new PegMessage(this, 0, msg);
			e = putsem(key, e);
		}
		return e;
	}
		
	public void addChoice(UList<Peg> l, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				addChoice(l, e.get(i));
			}
			return;
		}
		if(this.optimizationLevel > 0) {
			if(l.size() > 0 && (e instanceof PegString1 || e instanceof PegCharacter)) {
				Peg prev = l.ArrayValues[l.size()-1];
				if(prev instanceof PegString1) {
					UCharset charset = new UCharset("");
					charset.append((char)((PegString1) prev).symbol1);
					PegCharacter c = new PegCharacter(e.base, 0, charset);
					l.ArrayValues[l.size()-1] = c;
					prev = c;
				}
				if(prev instanceof PegCharacter) {
					UCharset charset = ((PegCharacter) prev).charset;
					if(e instanceof PegCharacter) {
						charset.append(((PegCharacter) e).charset);
					}
					else {
						charset.append((char)((PegString1) e).symbol1);
					}
					return;
				}
			}
		}
		l.add(e);
	}

	public void addSequence(UList<Peg> l, Peg e) {
		if(e instanceof PegSequence) {
			for(int i = 0; i < e.size(); i++) {
				this.addSequence(l, e.get(i));
			}
		}
		else {
			l.add(e);
		}
	}

}

class PEG4dGrammar extends Grammar {
	static boolean parse(Grammar loadingGrammar, ParserContext context, Pego pego) {
		//System.out.println("DEBUG? parsed: " + node);		
		if(pego.is("#rule")) {
			String ruleName = pego.textAt(0, "");
			Peg e = toParsingExpression(loadingGrammar, ruleName, pego.get(1));
			loadingGrammar.setRule(ruleName, e);
			//System.out.println("#rule** " + node + "\n@@@@ => " + e);
			return true;
		}
		if(pego.is("#import")) {
			String ruleName = pego.textAt(0, "");
			String fileName = context.source.checkFileName(pego.textAt(1, ""));
			importRuleFromFile(loadingGrammar, ruleName, fileName);
			return true;
		}
		if(pego.is("#error")) {
			int c = pego.getSource().charAt(pego.getSourcePosition());
			System.out.println(pego.formatSourceMessage("error", "syntax error: ascii=" + c));
			return false;
		}
		System.out.println("Unknown peg node: " + pego);
		return false;
	}
	
	private static void importRuleFromFile(Grammar loadingGrammar, String label, String fileName) {
		if(Main.VerbosePeg) {
			System.out.println("importing " + fileName);
		}
		Grammar p = new Grammar();
		p.loadGrammarFile(fileName);
		UList<String> list = p.makeList(label);
		String prefix = "";
		int loc = label.indexOf(":");
		if(loc > 0) {
			prefix = label.substring(0, loc+1);
			label = label.substring(loc+1);
			loadingGrammar.pegMap.put(label, new PegNonTerminal(loadingGrammar, 0,  prefix+label));
		}
		for(int i = 0; i < list.size(); i++) {
			String l = list.ArrayValues[i];
			Peg e = p.getRule(l);
			loadingGrammar.pegMap.put(prefix + l, e.clone(loadingGrammar, new PegNoTransformer()));
		}
	}

	private static Peg toParsingExpression(Grammar loadingGrammar, String ruleName, Pego node) {
		Peg e = toParsingExpressionImpl(loadingGrammar, ruleName, node);
		e.source = node.getSource();
		e.sourcePosition = (int)node.getSourcePosition();
		//System.out.println("seq: " + e.getClass() + ", size="+e.size());
		return e;
	}	
	
	private static Peg toParsingExpressionImpl(Grammar loadingGrammar, String ruleName, Pego pego) {
		if(pego.is("#PegNonTerminal")) {
			String nonTerminalSymbol = pego.getText();
			if(ruleName.equals(nonTerminalSymbol)) {
				Peg e = loadingGrammar.getRule(ruleName);
				if(e != null) {
					// self-redefinition
					return e;  // FIXME
				}
			}
			if(nonTerminalSymbol.equals("indent") && !loadingGrammar.hasRule("indent")) {
				loadingGrammar.setRule("indent", new PegIndent(loadingGrammar, 0));
			}
			return new PegNonTerminal(loadingGrammar, 0, nonTerminalSymbol);
		}
		if(pego.is("#PegString")) {
			return loadingGrammar.newString(UCharset._UnquoteString(pego.getText()));
		}
		if(pego.is("#PegCharacter")) {
			return loadingGrammar.newCharacter(pego.getText());
		}
		if(pego.is("#PegAny")) {
			return loadingGrammar.newAny();
		}
		if(pego.is("#PegChoice")) {
			UList<Peg> l = new UList<Peg>(new Peg[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				Peg e = toParsingExpression(loadingGrammar, ruleName, pego.get(i));
				loadingGrammar.addChoice(l, e);
			}
			return loadingGrammar.newChoice(l);
		}
		if(pego.is("#PegSequence")) {
			UList<Peg> l = new UList<Peg>(new Peg[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				Peg e = toParsingExpression(loadingGrammar, ruleName, pego.get(i));
				loadingGrammar.addSequence(l, e);
			}
			return loadingGrammar.newSequence(l);
		}
		if(pego.is("#PegTagging")) {
			return loadingGrammar.newTagging(pego.getText());
		}
		if(pego.is("#PegMessage")) {
			return loadingGrammar.newMessage(pego.getText());
		}

		if(pego.is("#PegNot")) {
			return loadingGrammar.newNot(toParsingExpression(loadingGrammar, ruleName, pego.get(0)));
		}
		if(pego.is("#PegAnd")) {
			return loadingGrammar.newAnd(toParsingExpression(loadingGrammar, ruleName, pego.get(0)));
		}
		if(pego.is("#PegOneMore")) {
			return loadingGrammar.newOneMore(toParsingExpression(loadingGrammar, ruleName, pego.get(0)));
		}
		if(pego.is("#PegZeroMore")) {
			return loadingGrammar.newZeroMore(toParsingExpression(loadingGrammar, ruleName, pego.get(0)));
		}
		if(pego.is("#PegOptional")) {
			return loadingGrammar.newOptional(toParsingExpression(loadingGrammar, ruleName, pego.get(0)));
		}
		if(pego.is("##PegNewObjectJoin")) {
			Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
			return new PegNewObject(loadingGrammar, 0, true, seq);
		}
		if(pego.is("#PegNewObject")) {
			Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
			return new PegNewObject(loadingGrammar, 0, false, seq);
		}
		if(pego.is("#PegExport")) {
			Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
			PegNewObject o = new PegNewObject(loadingGrammar, 0, false, seq);
			return new PegExport(loadingGrammar, 0, o);
		}
		if(pego.is("#PegSetter")) {
			int index = -1;
			String indexString = pego.getText();
			if(indexString.length() > 0) {
				index = UCharset.parseInt(indexString, -1);
			}
			return loadingGrammar.newSetter(toParsingExpression(loadingGrammar, ruleName, pego.get(0)), index);
		}
//		if(node.is("#pipe")) {
//			return new PegPipe(node.getText());
//		}
//		if(node.is("#catch")) {
//			return new PegCatch(null, toPeg(node.get(0)));
//		}
		Main._Exit(1, "undefined peg: " + pego);
		return null;
	}

	PEG4dGrammar() {
		super();
		this.optimizationLevel = 2;
		this.loadPEG4dGrammar();
		this.name = "PEG4d";
	}
	
	@Override
	public ParserContext newParserContext(ParserSource source) {
		return new TracingPackratParser(this, source);  // best parser
	}

	// Definiton of Bun's Peg	
	private final Peg s(String token) {
		return new PegString(this, 0, token);
	}
	private final Peg c(String charSet) {
		return new PegCharacter(this, 0, new UCharset(charSet));
	}
	public Peg n(String ruleName) {
		return new PegNonTerminal(this, 0, ruleName);
	}
	private final Peg opt(Peg e) {
		return new PegOptional(this, 0, e);
	}
	private final Peg zero(Peg e) {
		return new PegRepeat(this, 0, e, 0);
	}
	private final Peg zero(Peg ... elist) {
		return new PegRepeat(this, 0, seq(elist), 0);
	}
	private final Peg one(Peg e) {
		return new PegRepeat(this, 0, e, 1);
	}
	private final Peg one(Peg ... elist) {
		return new PegRepeat(this, 0, seq(elist), 1);
	}
	private final Peg seq(Peg ... elist) {
		PegSequence l = new PegSequence(this, 0, elist.length);
		for(Peg e : elist) {
			l.list.add(e);
		}
		return l;
	}
	private final Peg choice(Peg ... elist) {
		PegChoice l = new PegChoice(this, 0, elist.length);
		for(Peg e : elist) {
			l.add(e);
		}
		return l;
	}
	private Peg not(Peg e) {
		return new PegNot(this, 0, e);
	}
	private Peg L(String label) {
		return new PegTagging(this, 0, label);
	}
	private Peg O(Peg ... elist) {
		PegNewObject l = new PegNewObject(this, 0, elist.length, false);
		for(Peg e : elist) {
			l.list.add(e);
		}
		return l;
	}
	private Peg LO(Peg ... elist) {
		PegNewObject l = new PegNewObject(this, 0, elist.length, true);
		for(Peg e : elist) {
			l.list.add(e);
		}
		return l;
	}
	private Peg set(Peg e) {
		return new PegSetter(this, 0, e, -1);
	}

	public Grammar loadPEG4dGrammar() {
		Peg Any = new PegAny(this, 0);
		Peg NewLine = c("\\r\\n");
//		Comment
//		  = '/*' (!'*/' .)* '*/'
//		  / '//' (![\r\n] .)* [\r\n]
//		  ;
		Peg Comment = choice(
			seq(s("/*"), zero(not(s("*/")), Any), s("*/")),
			seq(s("//"), zero(not(NewLine), Any), NewLine)	
		);
//		_ = 
//		  ([ \t\r\n]+ / Comment )* 
//		  ;
		this.setRule("_", zero(choice(one(c(" \\t\\n\\r")), Comment)));
		
//		RuleName
//		  = << [A-Za-z_] [A-Za-z0-9_]* #PegNonTerminal >>
//		  ;
		this.setRule("RuleName", O(c("A-Za-z_"), zero(c("A-Za-z0-9_")), L("#PegNonTerminal")));
////	String
////	  = "'" << (!"'" .)*  #PegString >> "'"
////	  / '"' <<  (!'"' .)* #PegString >> '"'
////	  ;
		Peg _String = choice(
			seq(s("'"), O(zero(not(s("'")), Any), L("#PegString")), s("'")),
			seq(s("\""), O(zero(not(s("\"")), Any), L("#PegString")), s("\"")),
			seq(s("`"), O(zero(not(s("`")), Any), L("#PegMessage")), s("`"))
		);	
//	Character 
//	  = "[" <<  (!']' .)* #PegCharacter >> "]"
//	  ;
		Peg _Character = seq(s("["), O(zero(not(s("]")), Any), L("#PegCharacter")), s("]"));
//	Any
//	  = << '.' #PegAny >>
//	  ;
		Peg _Any = O(s("."), L("#PegAny"));
//	ObjectLabel 
//	  = << '#' [A-z0-9_.]+ #PegTagging>>
//	  ;
		Peg _Tagging = O(s("#"), 
			choice(
					seq(s("<"), opt(n("_")), set(n("Expr")), opt(n("_")), L("#PegCaptureTagging"), s(">")), 
					seq(one(c("A-Za-z0-9_.")), L("#PegTagging"))
			)
		);
//	Index
//	  = << [0-9] #PegIndex >>
//	  ;
		Peg _Index = O(c("0-9"), L("#PegIndex"));
//		Index
//		  = << [0-9] #PegIndex >>
//		Peg _Pipe = seq(s("|>"), opt(n("_")), O(c("A-Za-z_"), zero(c("A-Za-z0-9_")), L("#pipe")));
		Peg _Export = O(s("<|"), opt(n("_")), set(n("Expr")), opt(n("_")), L("#PegExport"), s("|>"));
//	Setter
//	  = '@' <<@ [0-9]? #PegSetter>>
//	  ;
		setRule("Setter", seq(choice(s("^"), s("@")), LO(opt(c("0-9")), L("#PegSetter"))));
//		SetterTerm
//		  = '(' Expr ')' Setter?
//		  / '<<' << ('@' [ \t\n] ##PegNewObjectJoin / '' #PegNewObject) _? Expr@ >> _? '>>' Setter?
//		  / RuleName Setter?
//		  ;
		Peg _SetterTerm = choice(
			seq(s("("), opt(n("_")), n("Expr"), opt(n("_")), s(")"), opt(n("Setter"))),
			seq(O(choice(s("<<"), s("<{"), s("8<")), choice(seq(choice(s("^"), s("@")), c(" \\t\\n\\r"), L("##PegNewObjectJoin")), seq(s(""), L("#PegNewObject"))), 
					opt(n("_")), set(n("Expr")), opt(n("_")), choice(s(">>"), s("}>"), s(">8"))), opt(n("Setter"))),
			seq(n("RuleName"), opt(n("Setter")))
		);
//	Term
//	  = String 
//	  / Character
//	  / Any
//	  / ObjectLabel
//	  / Index
//	  / SetterTerm
//	  ;
		setRule("Term", choice(
			_String, _Character, _Any, _Tagging, _Index, _Export, _SetterTerm
		));
//
//	SuffixTerm
//	  = Term <<@ ('*' #PegZeroMore / '+' #PegOneMore / '?' #PegOptional) >>?
//	  ;
		this.setRule("SuffixTerm", seq(n("Term"), opt(LO(choice(seq(s("*"), L("#PegZeroMore")), seq(s("+"), L("#PegOneMore")), seq(s("?"), L("#PegOptional")))))));
//	Predicated
//	  = << ('&' #PegAnd / '!' #PegNot) SuffixTerm@ >> / SuffixTerm 
//	  ;
		this.setRule("Predicate",  choice(
			O(choice(seq(s("&"), L("#PegAnd")),seq(s("!"), L("#PegNot"))), set(n("SuffixTerm"))), 
			n("SuffixTerm")
		));
//  Catch
//    = << 'catch' Expr@ >>
//    ;
//		Peg Catch = O(s("catch"), n("_"), L("#catch"), set(n("Expr")));
//	Sequence 
//	  = Predicated <<@ (_ Predicated@)+ #seq >>?
//	  ;
		setRule("Sequence", seq(n("Predicate"), opt(LO(L("#PegSequence"), one(n("_"), set(n("Predicate")))))));
//	Choice
//	  = Sequence <<@ _? ('/' _? Sequence@)+ #PegChoice >>?
//	  ;
		Peg _Choice = seq(n("Sequence"), opt(LO( L("#PegChoice"), one(opt(n("_")), s("/"), opt(n("_")), set(n("Sequence"))))));
//	Expr
//	  = Choice
//	  ;
		this.setRule("Expr", _Choice);
//	Rule
//	  = << RuleName@ _? '=' _? Expr@ #rule>>
//	  ;
		this.setRule("Rule", O(L("#rule"), set(n("RuleName")), opt(n("_")), s("="), opt(n("_")), set(n("Expr"))));
//	Import
//    = << 'import' _ RuleName@ from String@ #import>>
//		  ;
		this.setRule("Import", O(s("import"), L("#import"), n("_"), set(n("RuleName")), n("_"), s("from"), n("_"), set(_String)));
//	TopLevel   
//	  =  Rule _? ';'
//	  ;
//		this.setRule("TopLevel", seq(n("Rule"), opt(n("_")), s(";"), opt(n("_"))));
		this.setRule("TopLevel", seq(
			opt(n("_")), choice(n("Rule"), n("Import")), opt(n("_")), s(";"), opt(n("_"))
		));
		this.verify(null);
		return this;
	}

}



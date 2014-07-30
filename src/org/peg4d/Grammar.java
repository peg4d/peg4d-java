package org.peg4d;


public class Grammar {
	public final static PEG4dGrammar PEG4d = new PEG4dGrammar();

	String              name;
	UList<Peg>          pegList;
	UMap<Peg>           pegMap;
	UMap<Peg>           optimizedPegMap;
	UMap<String>        objectLabelMap = null;
	public boolean      foundError = false;
	public int          optimizationLevel;
	
//	int statOptimizedPeg = 0;
//	int statInlineCount = 0;
//	int statChoice = 0;
//
//	int statUnpredictableChoice = 0;
//	int statPredictableChoice = 0;
	
	public Grammar() {
		this.pegMap  = new UMap<Peg>();
		this.pegList = new UList<Peg>(new Peg[128]);
		//this.pegMap.put("indent", new PegIndent(this, 0));  // default rule
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
		this.check();
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
//		Peg checked = this.checkPegRule(ruleName, e);
//		if(checked != null) {
//			this.pegMap.put(ruleName, checked);
//		}
	}

//	private Peg checkPegRule(String name, Peg e) {
//		if(e instanceof PegChoice) {
//			UList<Peg> newlist = new UList<Peg>(new Peg[e.size()]);
//			for(int i = 0; i < e.size(); i++) {
//				newlist.add(this.checkPegRule(name, e.get(i)));
//			}
//			if(newlist.size() == 1) {
//				return newlist.ArrayValues[0];
//			}
//			((PegChoice)e).list = newlist;
//			return e;
//		}
//		if(e instanceof PegNonTerminal) {  // self reference
//			if(name.equals(((PegNonTerminal) e).symbol)) {
//				Peg defined = this.pegMap.get(name, null);
//				if(defined == null) {
//					e.warning("undefined self reference: " + name);
//				}
////				System.out.println("name " + name + ", " + ((PegLabel) e).symbol + " " + defined);
//				return defined;
//			}
//		}
//		return e;
//	}
		
	public final void check() {
		this.objectLabelMap = new UMap<String>();
		this.foundError = false;
		UList<String> list = this.pegMap.keys();
		UMap<String> visited = new UMap<String>();
		for(int i = 0; i < list.size(); i++) {
			String ruleName = list.ArrayValues[i];
			Peg nonTerminal = this.pegMap.get(ruleName, null);
			nonTerminal.verify2(ruleName, nonTerminal, ruleName, visited);
			visited.clear();
			if(Main.VerbosePeg && Main.StatLevel == 0) {
				if(nonTerminal.is(Peg.HasNewObject)) {
					ruleName = "object " + ruleName; 
				}
				if(!nonTerminal.is(Peg.HasNewObject) && !nonTerminal.is(Peg.HasSetter)) {
					ruleName = "text " + ruleName; 
				}
				if(nonTerminal.is(Peg.CyclicRule)) {
					ruleName += "*"; 
				}
				System.out.println(nonTerminal.format(ruleName));
			}
		}
		/* to complete the verification of cyclic rules */
		PegNormaizer norm = new PegNormaizer();
		for(int i = 0; i < list.size(); i++) {
			String ruleName = list.ArrayValues[i];
			Peg nonTerminal = this.pegMap.get(ruleName, null);
			nonTerminal.verify2(ruleName, nonTerminal, ruleName, null);
			norm.setRuleName(ruleName);
			Peg ne = nonTerminal.clone(this, norm);
			if(ne != nonTerminal) {
				this.pegMap.put(ruleName, ne);
			}
		}
		if(this.foundError) {
			Main._Exit(1, "peg error found");
		}
		this.optimizedPegMap = optimize();
	}
	
	PegOptimizer optimizer = null;
	
	public UMap<Peg> optimize() {
		UMap<Peg> pegCache = new UMap<Peg>();
		UList<String> list = this.pegMap.keys();
		this.optimizer = new PegOptimizer(this, pegCache);
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = this.pegMap.get(key, null);
			Peg ne = pegCache.get(key);
			if(ne == null) {
				ne = e.clone(this, optimizer);
				pegCache.put(key, ne);
			}
			this.pegList.add(ne);
		}
		return pegCache;
	}
	
	void updateStat(Stat stat) {
		if(this.optimizer != null) {
			this.optimizer.updateStat(stat);
		}
	}

	class PegNormaizer extends PegTransformer {
		private String ruleName;
		void setRuleName(String ruleName) {
			this.ruleName = ruleName;
		}
		@Override
		public Peg transform(Grammar base, Peg e) {
			if(e instanceof PegChoice) {
				return this.flattenChoice((PegChoice)e);
			}
			if(e instanceof PegList) {
				for(int i = 0; i < e.size(); i++) {
					((PegList) e).list.ArrayValues[i] = e.get(i).clone(base, this);
				}
				return e;
			}
			if(e instanceof PegSetter) {
				return this.flattenSetter((PegSetter)e);
			}
			if(e instanceof PegUnary) {
				((PegUnary) e).inner = ((PegUnary) e).inner.clone(base, this);
			}
			return e;
		}
		private Peg flattenChoice(PegChoice e) {
			boolean hasChoice = false;
			for(int i = 0; i < e.size(); i++) {
				if(e.get(i) instanceof PegChoice) {
					hasChoice = true;
					break;
				}
			}
			if(hasChoice) {
				UList<Peg> l = new UList<Peg>(new Peg[e.size()*2]);
				flattenChoiceImpl(e, l);
				e.list = l;
			}
			return e;
		}
		private void flattenChoiceImpl(PegChoice e, UList<Peg> l) {
			for(int i = 0; i < e.size(); i++) {
				Peg sub = e.get(i);
				if(sub instanceof PegChoice) {
					this.flattenChoiceImpl((PegChoice)sub, l);
				}
				else {
					l.add(sub);
				}
			}
		}
		private Peg flattenSetter(PegSetter e) {
			if(!e.inner.is(Peg.HasNewObject)) {
				return e.inner;
			}
			return e;
		}
	}
	
	public void addObjectLabel(String objectLabel) {
		this.objectLabelMap.put(objectLabel, objectLabel);
	}

	public ParserContext newParserContext(ParserSource source) {
		ParserContext p = null;
		String t = Main.ParserType;
		if(t.equalsIgnoreCase("--parser:packrat") || t.equals("--packrat")) {
			p = new PackratParser(this, source);
		}
		if(t.equalsIgnoreCase("--parser:simple")) {
			p = new RecursiveDecentParser(this, source);
		}
		if(t.equalsIgnoreCase("--monadic")) {
			p = new MonadicParser(this, source);
		}
		if(p == null) {
			p = new PEG4dParser(this, source);  // best parser
		}
		if(Main.RecognitionOnlyMode) {
			p.setRecognitionOnly(true);
		}
		return p;
	}

	public final UList<String> makeList(String startPoint) {
		UList<String> list = new UList<String>(new String[100]);
		UMap<String> set = new UMap<String>();
		Peg e = this.getRule(startPoint);
		if(e != null) {
			list.add(startPoint);
			set.put(startPoint, startPoint);
			e.makeList(startPoint, this, list, set);
		}
		return list;
	}

	public final void show(String startPoint) {
		this.show(startPoint, new PegFormatter());
	}

	public final void show(String startPoint, PegFormatter fmt) {
		UStringBuilder sb = new UStringBuilder();
		UList<String> list = makeList(startPoint);
		for(int i = 0; i < list.size(); i++) {
			String name = list.ArrayValues[i];
			Peg e = this.getRule(name);
			fmt.formatRule(sb, name, e);
		}
		System.out.println(sb.toString());
	}
	
	// factory
	
	UMap<Peg> semMap = new UMap<Peg>();
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
			e.refc += 1;
		}
		return e;
	}

	private void putsem(String t, Peg e) {
		semMap.put(t, e);
	}

	public final Peg newNonTerminal(String text) {
		Peg e = getsem(prefixNonTerminal + text);
		if(e == null) {
			e = new PegNonTerminal(this, 0, text);
			putsem(prefixNonTerminal + text, e);
		}
		return e;
	}

	public final Peg newString(String text) {
		Peg e = getsem(prefixString + text);
		if(e == null) {
			e = new PegString(this, 0, text);
			putsem(prefixString + text, e);
		}
		return e;
	}

	public final Peg newAny() {
		Peg e = getsem("");
		if(e == null) {
			e = new PegAny(this, 0);
			putsem("", e);
		}
		return e;
	}
	
	public final Peg newCharacter(String text) {
		UCharset u = new UCharset(text);
		String key = prefixCharacter + u.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegCharacter(this, 0, u);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newOptional(Peg p) {
		String key = prefixOptional + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegOptional(this, 0, p);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newOneMore(Peg p) {
		String key = prefixOneMore + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegRepeat(this, 0, p, 1);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newZeroMore(Peg p) {
		String key = prefixZeroMore + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegRepeat(this, 0, p, 0);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newAnd(Peg p) {
		String key = prefixAnd + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegAnd(this, 0, p);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newNot(Peg p) {
		String key = prefixNot + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegNot(this, 0, p);
			putsem(key, e);
		}
		return e;
	}
	public Peg newChoice(UList<Peg> l) {
		if(l.size() == 1) {
			return l.ArrayValues[0];
		}
		String key = prefixChoice;
		for(int i = 0; i < l.size(); i++) {
			key += l.ArrayValues[i].key();
		}
		Peg e = getsem(key);
		if(e == null) {
			e = new PegChoice(this, 0, l);
			putsem(key, e);
		}
		return e;
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
			e = new PegSequence(this, 0, l);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newSetter(Peg p, int index) {
		String key = prefixSetter + index + p.key();
		Peg e = getsem(key);
		if(e == null) {
			e = new PegSetter(this, 0, p, index);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newTagging(String tag) {
		String key = prefixTagging + tag;
		Peg e = getsem(key);
		if(e == null) {
			e = new PegTagging(this, 0, tag);
			putsem(key, e);
		}
		return e;
	}

	public final Peg newMessage(String msg) {
		String key = prefixMessage + msg;
		Peg e = getsem(key);
		if(e == null) {
			e = new PegMessage(this, 0, msg);
			putsem(key, e);
		}
		return e;
	}

	public static void addChoice(UList<Peg> l, Peg e) {
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				l.add(e.get(i));
			}
		}
		else if(e != null) {
			l.add(e);
		}
	}

	public static void addSequence(UList<Peg> l, Peg e) {
		if(e instanceof PegSequence) {
			for(int i = 0; i < e.size(); i++) {
				l.add(e.get(i));
			}
		}
		else if(e != null) {
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
			char c = pego.getSource().charAt(pego.getSourcePosition());
			System.out.println(pego.formatSourceMessage("error", "syntax error: ascii=" + (int)c));
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
				Grammar.addChoice(l, e);
			}
			return loadingGrammar.newChoice(l);
		}
		if(pego.is("#PegSequence")) {
			UList<Peg> l = new UList<Peg>(new Peg[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				Peg e = toParsingExpression(loadingGrammar, ruleName, pego.get(i));
				Grammar.addSequence(l, e);
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
		return new PEG4dParser(this, source);  // best parser
	}

	// Definiton of Bun's Peg	
	private final Peg s(String token) {
		return new PegString(this, 0, token);
	}
	private final Peg c(String charSet) {
		return new PegCharacter(this, 0, charSet);
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
		this.check();
		//this.show("TopLevel");
		return this;
	}

}



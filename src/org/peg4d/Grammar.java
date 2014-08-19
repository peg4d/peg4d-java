package org.peg4d;

import java.io.File;

public class Grammar {
	public final static PEG4dGrammar PEG4d = new PEG4dGrammar();

	GrammarComposer     db;
	UMap<PegRule>       nsRuleMap;
	String              name;
	UMap<PegRule>       ruleMap;

	UList<Peg>          definedExpressionList;
	UList<PegRule>      exportedRuleList;
	UMap<String>        objectLabelMap = null;
	public boolean      foundError = false;
	int memoFactor      ;
	int optimizationLevel;

	int LexicalOptimization       = 0;
	int InliningCount             = 0;
	int InterTerminalOptimization = 0;
	int PredictionOptimization    = 0;

	MemoRemover memoRemover = null;

	class PegRule {
		String ruleName;
		Peg expr;
		public PegRule(String ruleName, Peg e) {
			this.ruleName = ruleName;
			this.expr = e;
		}
	}
	
	public Grammar(GrammarComposer db) {
		this.db = db;
		this.nsRuleMap = null;
		this.ruleMap  = new UMap<PegRule>();
		this.definedExpressionList = new UList<Peg>(new Peg[128]);
		this.optimizationLevel = Main.OptimizationLevel;
		this.memoFactor = Main.MemoFactor;
		if(this.memoFactor < 0) {
			this.memoFactor = - this.memoFactor;
		}
	}

	public String getName() {
		return this.name;
	}

	public final boolean loadGrammarFile(String fileName) {
		PEG4dGrammar peg4d = Grammar.PEG4d;
		ParserContext p = peg4d.newParserContext(Main.loadSource(peg4d, fileName));
		this.name = fileName;
		if(fileName.indexOf('/') > 0) {
			this.name = fileName.substring(fileName.lastIndexOf('/')+1);
		}
		p.setRecognitionOnly(false);
		while(p.hasNode()) {
			ParsingObject pego = p.parseNode("TopLevel");
			if(pego.isFailure()) {
				Main._Exit(1, "FAILED: " + pego);
				break;
			}
			if(!PEG4dGrammar.performExpressionConstruction(this, p, pego)) {
				break;
			}
		}
//		ObjectRemover objectRemover = null;
//		if(Main.RecognitionOnlyMode) {
//			objectRemover = new ObjectRemover();
//		}
		this.verify(/*objectRemover*/);
		return this.foundError;
	}
	
	public final static Grammar load(GrammarComposer db, String fileName) {
		Grammar peg = new Grammar(db);
		peg.loadGrammarFile(fileName);
		return peg;
	}
	
	public final Peg getDefinedExpression(long oid) {
		int index = (short)oid;
		return this.definedExpressionList.ArrayValues[index-1];
	}

	public final static char NameSpaceSeparator = ':';

	public final void importGramamr(String ns, String filePath) {
		Grammar peg = this.db.getGrammar(filePath);
		if(peg != null) {
			UList<PegRule> ruleList = peg.getExportRuleList();
			//System.out.println("filePath: " + filePath + " peg=" + ruleList);
			if(this.nsRuleMap == null && ruleList.size() > 0) {
				this.nsRuleMap = new UMap<PegRule>();
			}
			for(int i = 0; i < ruleList.size(); i++) {
				PegRule rule = ruleList.ArrayValues[i];
				String key = ns + ':' + rule.ruleName;
				if(this.nsRuleMap.hasKey(key)) {
					Main.printVerbose("duplicated: ", key + " ");
				}
				Main.printVerbose("importing: ", key + " ");
				this.nsRuleMap.put(key, rule);
			}
		}
	}
	
	public final boolean hasRule(String ruleName) {
		if(this.nsRuleMap != null) {
			int loc = ruleName.indexOf(NameSpaceSeparator);
			if(loc != -1) {
				return this.nsRuleMap.get(ruleName) != null;
			}
		}
		return this.ruleMap.get(ruleName) != null;
	}

	public final PegRule getRule(String ruleName) {
		if(this.nsRuleMap != null) {
			int loc = ruleName.indexOf(NameSpaceSeparator);
			if(loc != -1) {
				return this.nsRuleMap.get(ruleName);
			}
		}
		return this.ruleMap.get(ruleName);
	}

	public final Peg getExpression(String ruleName) {
		PegRule rule = this.getRule(ruleName);
		if(rule != null) {
			return rule.expr;
		}
		return null;
	}

	public final void setRule(String ruleName, Peg e) {
		this.ruleMap.put(ruleName, new PegRule(ruleName, e));
	}

	public final void setRule(String ruleName, PegRule rule) {
		this.ruleMap.put(ruleName, rule);
	}
	
	public final UList<PegRule> getRuleList() {
		UList<String> nameList = this.ruleMap.keys();
		UList<PegRule> pegList = new UList<PegRule>(new PegRule[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			pegList.add(this.getRule(ruleName));
		}
		return pegList;
	}

	public final UList<Peg> getExpressionList() {
		UList<String> nameList = this.ruleMap.keys();
		UList<Peg> pegList = new UList<Peg>(new Peg[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			pegList.add(this.getRule(ruleName).expr);
		}
		return pegList;
	}

	public final void verify() {
		//this.objectLabelMap = new UMap<String>();
		this.foundError = false;
		this.exportedRuleList = null;
		UList<String> nameList = this.ruleMap.keys();
		NonTerminalChecker nc = new NonTerminalChecker();
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			PegRule rule = this.getRule(ruleName);
			if(Main.PackratStyleMemo && !(rule.expr instanceof PegMemo)) {
				rule.expr = new PegMemo(rule.expr);
				this.EnabledMemo += 1;
			}
			nc.verify(ruleName, rule.expr);
		}
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		this.getExportRuleList();
		ObjectRemover objectRemover = new ObjectRemover();
//		for(int i = 0; i < nameList.size(); i++) {
//			String ruleName = nameList.ArrayValues[i];
//			PegRule rule = this.getRule(ruleName);
//			if(rule.expr.hasObjectOperation()) {
//				String name = ruleName + "'";
//				Peg e = objectRemover.removeObjectOperation(rule.expr);
//				this.setRule(name, e);
//				System.out.println(name + " = " + e);
//			}
//		}

		new Inliner(this).performInlining();
		new Optimizer(this).optimize();
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		this.memoRemover = new MemoRemover(this);
	}
	

	final UList<PegRule> getExportRuleList() {
		if(this.exportedRuleList == null) {
			UList<PegRule> l = new UList<PegRule>(new PegRule[4]);
			appendExportRuleList(l, "EXPORT");
			appendExportRuleList(l, "Export");
			appendExportRuleList(l, "export");
			this.exportedRuleList = l;
		}
		return this.exportedRuleList;
	}

	private void appendExportRuleList(UList<PegRule> l, String name) {
		Peg e = this.getExpression(name);
		if(e != null) {
			e = e.getExpression();
			if(e instanceof PegChoice) {
				for(int i = 0; i < e.size(); i++) {
					Peg se = e.get(i);
					if(se instanceof PegNonTerminal) {
						PegRule rule = new PegRule(((PegNonTerminal) se).symbol, ((PegNonTerminal) se).getNext());
						l.add(rule);
						Main.printVerbose("export", rule.ruleName);
					}
				}
			}
		}
	}

	int DefinedPegSize = 0;
	private int MultiReference = 0;
	private int Reference = 0;
	int StrongPredicatedChoice = 0;
	int PredicatedChoice = 0;
	int UnpredicatedChoice = 0;
	int PredicatedChoiceL1 = 0;
	int UnpredicatedChoiceL1 = 0;

	void updateStat(Stat stat) {
		stat.setText("Peg", this.getName());
		stat.setCount("PegSize", this.DefinedPegSize);
		stat.setCount("PegReference",   this.Reference);
		stat.setCount("MultiReference", this.MultiReference);
		stat.setRatio("Complexity", this.MultiReference, this.Reference);
		stat.setCount("StrongPredicatedChoice",   this.StrongPredicatedChoice);
		stat.setCount("PredicatedChoice",   this.PredicatedChoice);
		stat.setCount("UnpredicatedChoice", this.UnpredicatedChoice);
		stat.setRatio("Predictablity", this.PredicatedChoice, this.PredicatedChoice + this.UnpredicatedChoice);
		stat.setRatio("L1Predictability", this.PredicatedChoiceL1, this.PredicatedChoiceL1 + this.UnpredicatedChoiceL1);

		stat.setCount("ActivatedMemo", this.EnabledMemo);
		stat.setCount("DisabledMemo", this.DisabledMemo);
		stat.setCount("RemovedMemo", this.memoRemover.RemovedCount);
		stat.setCount("LexicalOptimization", this.LexicalOptimization);
		stat.setCount("InterTerminalOptimization", this.InterTerminalOptimization);
		stat.setCount("PredictionOptimization", this.PredictionOptimization);
		
		for(int i = 0; i < this.definedExpressionList.size(); i++) {
			Peg e = this.definedExpressionList.ArrayValues[i];
			if(e instanceof PegMemo) {
				PegMemo me = (PegMemo)e;
				if(me.enableMemo && me.memoMiss > 32) {
					me.show();
				}
			}
		}
	}

	public ParserContext newParserContext(ParsingSource source) {
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
			Peg e = this.getExpression(name);
			fmt.formatRule(name, e, sb);
		}
		System.out.println(sb.toString());
	}
	
	// factory
	
	UMap<Peg> semMap = new UMap<Peg>();

	int EnabledMemo  = 0;
	int DisabledMemo = 0;


	private static String prefixNonTerminal = "a\b";
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
		if(this.memoFactor > 0 && Main.AllExpressionMemo && !e.is(Peg.NoMemo)) {
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
		if(this.optimizationLevel > 2) {
			return new PegSelectiveChoice(this, 0, l);
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

	public Peg newConstructor(String tagName, Peg p) {
		Peg e = new PegConstructor(this, 0, false, "#"+tagName, toSequenceList(p));
		if(this.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
			e = new PegMemo(e);
		}
		return e;
	}

	public Peg newJoinConstructor(String tagName, Peg p) {
		Peg e = new PegConstructor(this, 0, true, "#"+tagName, toSequenceList(p));
		if(this.memoFactor != 0 && (Main.AllExpressionMemo || Main.ObjectFocusedMemo)) {
			e = new PegMemo(e);
		}
		return e;
	}
	
	public UList<Peg> toSequenceList(Peg e) {
		if(e instanceof PegSequence) {
			return ((PegSequence) e).list;
		}
		UList<Peg> l = new UList<Peg>(new Peg[1]);
		l.add(e);
		return l;
	}
	
	private Peg newNotAnyImpl(Peg orig, UList<Peg> l, int nsize) {
		if(nsize == 1) {
			return new PegNotAny(this, 0, (PegNot)l.ArrayValues[0], orig);
		}
		return orig;
	}
	
	public final Peg newConnector(Peg p, int index) {
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
	static boolean performExpressionConstruction(Grammar loadingGrammar, ParserContext context, ParsingObject pego) {
		//System.out.println("DEBUG? parsed: " + pego);		
		if(pego.is("#PegRule")) {
			if(pego.size() > 2) {
				System.out.println("DEBUG? parsed: " + pego);		
			}
			String ruleName = pego.textAt(0, "");
			Peg e = toParsingExpression(loadingGrammar, ruleName, pego.get(1));
			loadingGrammar.setRule(ruleName, e);
			return true;
		}
		if(pego.is("#PegImport")) {
			String filePath = searchPegFilePath(context, pego.textAt(0, ""));
			String ns = pego.textAt(1, "");
			loadingGrammar.importGramamr(ns, filePath);
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
	
	private static String searchPegFilePath(ParserContext context, String filePath) {
		String f = context.source.getFilePath(filePath);
		if(new File(f).exists()) {
			return f;
		}
		if(new File(filePath).exists()) {
			return filePath;
		}
		return "lib/"+filePath;
	}
	
	private static Peg toParsingExpression(Grammar loadingGrammar, String ruleName, ParsingObject node) {
		Peg e = toParsingExpressionImpl(loadingGrammar, ruleName, node);
		e.source = node.getSource();
		e.sourcePosition = (int)node.getSourcePosition();
		//System.out.println("seq: " + e.getClass() + ", size="+e.size());
		loadingGrammar.DefinedPegSize += 1;
		return e;
	}	
	
	private static Peg toParsingExpressionImpl(Grammar loadingGrammar, String ruleName, ParsingObject pego) {
		if(pego.is("#PegNonTerminal")) {
			String nonTerminalSymbol = pego.getText();
			if(ruleName.equals(nonTerminalSymbol)) {
				Peg e = loadingGrammar.getExpression(ruleName);
				if(e != null) {
					// self-redefinition
					return e;  // FIXME
				}
			}
			if(nonTerminalSymbol.equals("indent") && !loadingGrammar.hasRule("indent")) {
				loadingGrammar.setRule("indent", new PegIndent(loadingGrammar, 0));
			}
			if(nonTerminalSymbol.equals("_") && !loadingGrammar.hasRule("_")) {
				loadingGrammar.setRule("_", Grammar.PEG4d.getExpression("_"));
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
			return loadingGrammar.newJoinConstructor(ruleName, seq);
		}
		if(pego.is("#PegNewObject")) {
			Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
			return loadingGrammar.newConstructor(ruleName, seq);
		}
		if(pego.is("#PegConnector")) {
			int index = -1;
			if(pego.size() == 2) {
				index = UCharset.parseInt(pego.textAt(1, ""), -1);
			}
			return loadingGrammar.newConnector(toParsingExpression(loadingGrammar, ruleName, pego.get(0)), index);
		}
//		if(pego.is("#PegExport")) {
//		Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
//		Peg o = loadingGrammar.newConstructor(ruleName, seq);
//		return new PegExport(loadingGrammar, 0, o);
//	}
//	if(pego.is("#PegSetter")) {
//		int index = -1;
//		String indexString = pego.getText();
//		if(indexString.length() > 0) {
//			index = UCharset.parseInt(indexString, -1);
//		}
//		return loadingGrammar.newConnector(toParsingExpression(loadingGrammar, ruleName, pego.get(0)), index);
//	}
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
		super(new GrammarComposer());
		this.optimizationLevel = 0;
		this.loadPEG4dGrammar();
		this.name = "PEG4d";
		this.db.setGrammar("peg", this);
	}
	
	@Override
	public ParserContext newParserContext(ParsingSource source) {
		return new TracingPackratParser(this, source, 0);  // best parser
	}

	// Definiton of PEG4d 	
	private final Peg t(String token) {
		return new PegString(this, 0, token);
	}
	private final Peg c(String charSet) {
		return new PegCharacter(this, 0, new UCharset(charSet));
	}
	private final Peg n(String ruleName) {
		return new PegNonTerminal(this, 0, ruleName);
	}
	private final Peg Optional(Peg e) {
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
		UList<Peg> l = new UList<Peg>(new Peg[8]);
		for(Peg e : elist) {
			this.addSequence(l, e);
		}
		return new PegSequence(this, 0, l);
	}
	private final Peg Choice(Peg ... elist) {
		UList<Peg> l = new UList<Peg>(new Peg[8]);
		for(Peg e : elist) {
			this.addChoice(l, e);
		}
		return new PegChoice(this, 0, l);
	}
	private final Peg Not(Peg e) {
		return new PegNot(this, 0, e);
	}
	private final Peg Tag(String tag) {
		return newTagging(tag);
	}
	private final Peg Constructor(Peg ... elist) {
		UList<Peg> l = new UList<Peg>(new Peg[8]);
		for(Peg e : elist) {
			this.addSequence(l, e);
		}
		return new PegConstructor(this, 0, false, null, l);
	}
	private Peg LeftJoin(Peg ... elist) {
		UList<Peg> l = new UList<Peg>(new Peg[8]);
		for(Peg e : elist) {
			this.addSequence(l, e);
		}
		return new PegConstructor(this, 0, true, null, l);
	}
	private Peg set(Peg e) {
		return new PegSetter(this, 0, e, -1);
	}
	private Peg set(int index, Peg e) {
		return new PegSetter(this, 0, e, index);
	}

	public Grammar loadPEG4dGrammar() {
		Peg Any = newAny();
		Peg _NEWLINE = c("\\r\\n");
		Peg _WS = c(" \\t\\r\\n");
		Peg _NUMBER = one(c("0-9"));
		this.setRule("COMMENT", 
			Choice(
				seq(t("/*"), zero(Not(t("*/")), Any), t("*/")),
				seq(t("//"), zero(Not(_NEWLINE), Any), _NEWLINE)
			)
		);
		this.setRule("_", zero(Choice(one(_WS), n("COMMENT"))));
		Peg Spacing = Optional(n("_"));
		this.setRule("RuleName", Constructor(c("A-Za-z_"), zero(c("A-Za-z0-9_")), Tag("#name")));
		this.setRule("LibName",  Constructor(c("A-Za-z_"), zero(c("A-Za-z0-9_.")), Tag("#name")));

		this.setRule("Number", Constructor(_NUMBER, Tag("#Integer")));
		
		this.setRule("NonTerminalName", 
			Constructor(
				c("A-Za-z_"), 
				zero(c("A-Za-z0-9_:")), 
				Tag("#PegNonTerminal")
			)
		);

		this.setRule("NonTerminal", 
			seq(
				n("NonTerminalName"),
				Optional(
					LeftJoin(
						n("Param"),
						Tag("#PegNonTerminal")				
					)
				)
			)
		);
		
//		Peg StringContent = zero(Choice(t("\\\\'"), seq(Not(t("'")), Any)));
//		Peg StringContent2 = zero(Choice(t("\\\\\""), seq(Not(t("\"")), Any)));
		Peg StringContent = zero(Not(t("'")), Any);
		Peg StringContent2 = zero(Not(t("\"")), Any);
		this.setRule("String", 
			Choice(
				seq(t("'"), Constructor(StringContent, Tag("#PegString")), t("'")),
				seq(t("\""), Constructor(StringContent2, Tag("#PegString")), t("\""))
			)
		);
		Peg _Message = seq(t("`"), Constructor(zero(Not(t("`")), Any), Tag("#PegMessage")), t("`"));
		Peg CharacterContent = zero(Not(t("]")), Any);
		Peg _Character = seq(t("["), Constructor(CharacterContent, Tag("#PegCharacter")), t("]"));
		Peg _Any = Constructor(t("."), Tag("#PegAny"));
		Peg _Tagging = Constructor(t("#"), seq(one(c("A-Za-z0-9_.")), Tag("#PegTagging")));

		Peg ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		Peg Connector  = Choice(t("@"), t("^"));
		Peg ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));
//		setRule("Setter", seq(Connector, LeftJoin(Optional(Number), Tag("#PegSetter"))));

		this.setRule("Constructor", Constructor(
			ConstructorBegin, 
			Choice(
				seq(Connector, _WS, Tag("##PegNewObjectJoin")), 
				Tag("#PegNewObject")
			), 
			Spacing, 
			set(n("Expr")), 
			Spacing,
			ConstructorEnd
		));
		Peg _AssertFunc = Constructor(
			t("<assert"), 
			_WS,
			n("Expr"),
			Spacing,
			t(">"),
			Tag("#assert")
		);
		Peg _ChoiceFunc = Constructor(
			t("<choice"), _WS,
			n("Expr"), Spacing, t(">"),
			Tag("#choice")
		);
		Peg _RangeFunc = Constructor(
			t("<range"), _WS,
			n("String"), _WS,
			n("String"), Spacing, t(">"),
			Tag("#range")
		);
		setRule("Term", 
			Choice(
				n("String"), _Character, _Any, _Message, _Tagging, 
				seq(t("("), Spacing, n("Expr"), Spacing, t(")")),
				n("Constructor"), n("NonTerminal"), 
				_AssertFunc, _ChoiceFunc, _RangeFunc
			)
		);
		this.setRule("SuffixTerm", seq(
			n("Term"), 
			Optional(
				LeftJoin(
					Choice(
						seq(t("*"), Tag("#PegZeroMore")), 
						seq(t("+"), Tag("#PegOneMore")), 
						seq(t("?"), Tag("#PegOptional")),
						seq(Connector, Optional(set(1, n("Number"))), Tag("#PegConnector"))
					)
				)
			)
		));
		
		this.setRule("Predicate", Choice(
				Constructor(
						Choice(
								seq(t("&"), Tag("#PegAnd")),
								seq(t("!"), Tag("#PegNot")),
								seq(t("@["), Spacing, set(1, n("Number")), Spacing, t("]"), Tag("#PegConnector")),							
								seq(t("@"), Tag("#PegConnector"))
						), 
						set(0, n("SuffixTerm"))
				), 
				n("SuffixTerm")
		));

		this.setRule("Sequence", seq(
				n("Predicate"), 
				Optional(
					LeftJoin(
						one(
							Spacing, 
							set(n("Predicate"))
						),
						Tag("#PegSequence") 
					)
				)
		));
		this.setRule("Expr", 
			seq(
				n("Sequence"), 
				Optional(
					LeftJoin(
						one(
							Spacing, t("/"), Spacing, 
							set(n("Sequence"))
						),
						Tag("#PegChoice") 
					)
				)
			)
		);
		this.setRule("Param",
			seq(
				t("["),
				Constructor(
					set(n("RuleName")),
					zero(
						Spacing,
						set(n("RuleName"))
					),
					Tag("#PegParam") 
				),
				t("]")
			)
		);
		this.setRule("Annotation",
			seq(
				t("["),
				Constructor(
					set(n("RuleName")),
					t(": "),
					set(
						Constructor(
							zero(Not(t("]")), Any),
							Tag("#value") 
						)
					),
					Tag("#PegAnnotation") 
				),
				t("]")
			)
		);
		this.setRule("Annotations",
				Constructor(
					one(set(n("Annotation"))),
					Tag("#Annotations") 
				)
		);
		this.setRule("Rule", 
			Constructor(
				set(0, n("RuleName")), Spacing, 
				Optional(seq(set(2, n("Param")), Spacing)),
				Optional(seq(set(3, n("Annotations")), Spacing)),
				t("="), Spacing, 
				set(1, n("Expr")),
				Tag("#PegRule") 
			)
		);
		this.setRule("Import", Constructor(
			t("import"), 
			Tag("#PegImport"), 
			_WS, 
			Choice(set(n("String")), n("LibName")), 
			_WS, 
			t("as"), 
			_WS, 
			set(n("RuleName"))
		));
		this.setRule("TopLevel", seq(
			Spacing, 
			Choice(
				n("Rule"), 
				n("Import"),
				n("Expr")
			), 
			Spacing, 
			Optional(t(";")), 
			Spacing 
		));
		this.setRule("File", seq(
			Spacing, 
			Choice(
				n("Rule"), 
				n("Import")
			), 
			Spacing, 
			t(";"), 
			Spacing 
		));
		this.verify(/*null*/);
		return this;
	}

}



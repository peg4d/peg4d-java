package org.peg4d;

import java.io.File;

public class Grammar {
	public final static PEG4dGrammar PEG4d = new PEG4dGrammar();
	GrammarComposer     composer;
	UMap<PegRule>       nsRuleMap;
	String              name;
	UMap<PegRule>       ruleMap;

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
	int EnabledMemo  = 0;
	int DisabledMemo = 0;
		
	public Grammar(GrammarComposer db) {
		this.composer = db;
		this.nsRuleMap = null;
		this.ruleMap  = new UMap<PegRule>();
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
	
	public final PExpression getDefinedExpression(long oid) {
		return this.getDefinedExpression(oid);
	}

	public final static char NameSpaceSeparator = ':';

	public final void importGramamr(String ns, String filePath) {
		Grammar peg = this.composer.getGrammar(filePath);
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

	public final PExpression getExpression(String ruleName) {
		PegRule rule = this.getRule(ruleName);
		if(rule != null) {
			return rule.expr;
		}
		return null;
	}

	public final void setRule(String ruleName, PExpression e) {
		this.ruleMap.put(ruleName, new PegRule(null, 0, ruleName, e));
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

	public final UList<PExpression> getExpressionList() {
		UList<String> nameList = this.ruleMap.keys();
		UList<PExpression> pegList = new UList<PExpression>(new PExpression[nameList.size()]);
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
			nc.verify(rule);
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
		PExpression e = this.getExpression(name);
		if(e != null) {
			e = e.getExpression();
			if(e instanceof PChoice) {
				for(int i = 0; i < e.size(); i++) {
					PExpression se = e.get(i);
					if(se instanceof PNonTerminal) {
						PegRule rule = this.getRule(((PNonTerminal) se).symbol);
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

//		stat.setCount("ActivatedMemo", this.EnabledMemo);
//		stat.setCount("DisabledMemo", this.DisabledMemo);
		stat.setCount("RemovedMemo", this.memoRemover.RemovedCount);
		stat.setCount("LexicalOptimization", this.LexicalOptimization);
		stat.setCount("InterTerminalOptimization", this.InterTerminalOptimization);
		stat.setCount("PredictionOptimization", this.PredictionOptimization);
		
//		for(int i = 0; i < this.definedExpressionList.size(); i++) {
//			Peg e = this.definedExpressionList.ArrayValues[i];
//			if(e instanceof PegMemo) {
//				PegMemo me = (PegMemo)e;
//				if(me.enableMemo && me.memoMiss > 32) {
//					me.show();
//				}
//			}
//		}
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
			PExpression e = this.getExpression(name);
			fmt.formatRule(name, e, sb);
		}
		System.out.println(sb.toString());
	}
	
	
	final PExpression newNonTerminal(String text) {
		return this.composer.newNonTerminal(this, text);
	}
	final PExpression newString(String text) {
		return this.composer.newString(this, text);
	}
	final PExpression newAny() {
		return this.composer.newAny(this);
	}	
	final PExpression newCharacter(String text) {
		return this.composer.newCharacter(this, text);
	}
	final PExpression newOptional(PExpression p) {
		return this.composer.newOptional(this, p);
	}
	final PExpression newOneMore(PExpression p) {
		return this.composer.newOneMore(this, p);
	}
	final PExpression newZeroMore(PExpression p) {
		return this.composer.newZeroMore(this, p);
	}
	final PExpression newAnd(PExpression p) {
		return this.composer.newAnd(this, p);
	}
	final PExpression newNot(PExpression p) {
		return this.composer.newNot(this, p);
	}
	final PExpression newChoice(UList<PExpression> l) {
		return this.composer.newChoice(this, l);
	}
	final PExpression newSequence(UList<PExpression> l) {
		return this.composer.newSequence(this, l);
	}
	final PExpression newConstructor(String tagName, PExpression p) {
		return this.composer.newConstructor(this, tagName, p);
	}
	final PExpression newJoinConstructor(String tagName, PExpression p) {
		return this.composer.newJoinConstructor(this, tagName, p);
	}
	final PExpression newConnector(PExpression p, int index) {
		return this.composer.newConnector(this, p, index);
	}
	final PExpression newTagging(String tag) {
		return this.composer.newTagging(this, tag);
	}
	final PExpression newMessage(String msg) {
		return this.composer.newMessage(this, msg);
	}
	final void addChoice(UList<PExpression> l, PExpression e) {
		this.composer.addChoice(this, l, e);
	}
	final void addSequence(UList<PExpression> l, PExpression e) {
		this.composer.addSequence(l, e);
	}
}

class PegRule {
	ParsingSource source;
	long     pos;
	String ruleName;
	PExpression expr;
	int checked = 0;
	int length = 0;
	boolean objectType;
	public PegRule(ParsingSource source, long pos, String ruleName, PExpression e) {
		this.source = source;
		this.pos = pos;
		this.ruleName = ruleName;
		this.expr = e;
		this.objectType = false;
	}
	public void reportError(String msg) {
		if(this.source != null) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.pos, msg));
		}
		else {
			System.out.println("ERROR: " + msg);
		}
	}
	public void reportWarning(String msg) {
		if(this.source != null) {
			Main._PrintLine(this.source.formatErrorMessage("warning", this.pos, msg));
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
			PExpression e = toParsingExpression(loadingGrammar, ruleName, pego.get(1));
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
	
	private static PExpression toParsingExpression(Grammar loadingGrammar, String ruleName, ParsingObject node) {
		PExpression e = toParsingExpressionImpl(loadingGrammar, ruleName, node);
//		e.source = node.getSource();
//		e.sourcePosition = (int)node.getSourcePosition();
		//System.out.println("seq: " + e.getClass() + ", size="+e.size());
		loadingGrammar.DefinedPegSize += 1;
		return e;
	}	
	
	private static PExpression toParsingExpressionImpl(Grammar loading, String ruleName, ParsingObject pego) {
		if(pego.is("#PegNonTerminal")) {
			String nonTerminalSymbol = pego.getText();
			if(ruleName.equals(nonTerminalSymbol)) {
				PExpression e = loading.getExpression(ruleName);
				if(e != null) {
					// self-redefinition
					return e;  // FIXME
				}
			}
			if(nonTerminalSymbol.equals("indent") && !loading.hasRule("indent")) {
				loading.setRule("indent", new PIndent(loading, 0));
			}
			if(nonTerminalSymbol.equals("_") && !loading.hasRule("_")) {
				loading.setRule("_", Grammar.PEG4d.getExpression("_"));
			}
			return new PNonTerminal(loading, 0, nonTerminalSymbol);
		}
		if(pego.is("#PegString")) {
			return loading.newString(UCharset._UnquoteString(pego.getText()));
		}
		if(pego.is("#PegCharacter")) {
			return loading.newCharacter(pego.getText());
		}
		if(pego.is("#PegAny")) {
			return loading.newAny();
		}
		if(pego.is("#PegChoice")) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, pego.get(i));
				loading.addChoice(l, e);
			}
			return loading.newChoice(l);
		}
		if(pego.is("#PegSequence")) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, pego.get(i));
				loading.addSequence(l, e);
			}
			return loading.newSequence(l);
		}
		if(pego.is("#PegTagging")) {
			return loading.newTagging(pego.getText());
		}
		if(pego.is("#PegMessage")) {
			return loading.newMessage(pego.getText());
		}
		if(pego.is("#PegNot")) {
			return loading.newNot(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is("#PegAnd")) {
			return loading.newAnd(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is("#PegOneMore")) {
			return loading.newOneMore(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is("#PegZeroMore")) {
			return loading.newZeroMore(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is("#PegOptional")) {
			return loading.newOptional(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is("##PegNewObjectJoin")) {
			PExpression seq = toParsingExpression(loading, ruleName, pego.get(0));
			return loading.newJoinConstructor(ruleName, seq);
		}
		if(pego.is("#PegNewObject")) {
			PExpression seq = toParsingExpression(loading, ruleName, pego.get(0));
			return loading.newConstructor(ruleName, seq);
		}
		if(pego.is("#PegConnector")) {
			int index = -1;
			if(pego.size() == 2) {
				index = UCharset.parseInt(pego.textAt(1, ""), -1);
			}
			return loading.newConnector(toParsingExpression(loading, ruleName, pego.get(0)), index);
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
		this.composer.setGrammar("peg", this);
	}
	
	@Override
	public ParserContext newParserContext(ParsingSource source) {
		return new TracingPackratParser(this, source, 0);  // best parser
	}

	// Definiton of PEG4d 	
	private final PExpression t(String token) {
		return new PString(this, 0, token);
	}
	private final PExpression c(String charSet) {
		return new PCharacter(this, 0, new UCharset(charSet));
	}
	private final PExpression n(String ruleName) {
		return new PNonTerminal(this, 0, ruleName);
	}
	private final PExpression Optional(PExpression e) {
		return new POptional(this, 0, e);
	}
	private final PExpression zero(PExpression e) {
		return new PRepetition(this, 0, e, 0);
	}
	private final PExpression zero(PExpression ... elist) {
		return new PRepetition(this, 0, seq(elist), 0);
	}
	private final PExpression one(PExpression e) {
		return new PRepetition(this, 0, e, 1);
	}
	private final PExpression one(PExpression ... elist) {
		return new PRepetition(this, 0, seq(elist), 1);
	}
	private final PExpression seq(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			this.addSequence(l, e);
		}
		return new PSequence(this, 0, l);
	}
	private final PExpression Choice(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			this.addChoice(l, e);
		}
		return new PChoice(this, 0, l);
	}
	private final PExpression Not(PExpression e) {
		return new PNot(this, 0, e);
	}
	private final PExpression Tag(String tag) {
		return newTagging(tag);
	}
	private final PExpression Constructor(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			this.addSequence(l, e);
		}
		return new PConstructor(this, 0, false, null, l);
	}
	private PExpression LeftJoin(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			this.addSequence(l, e);
		}
		return new PConstructor(this, 0, true, null, l);
	}
	private PExpression set(PExpression e) {
		return new PConnector(this, 0, e, -1);
	}
	private PExpression set(int index, PExpression e) {
		return new PConnector(this, 0, e, index);
	}

	public Grammar loadPEG4dGrammar() {
		PExpression Any = newAny();
		PExpression _NEWLINE = c("\\r\\n");
		PExpression _WS = c(" \\t\\r\\n");
		PExpression _NUMBER = one(c("0-9"));
		this.setRule("COMMENT", 
			Choice(
				seq(t("/*"), zero(Not(t("*/")), Any), t("*/")),
				seq(t("//"), zero(Not(_NEWLINE), Any), _NEWLINE)
			)
		);
		this.setRule("_", zero(Choice(one(_WS), n("COMMENT"))));
		PExpression Spacing = Optional(n("_"));
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
		
		PExpression StringContent  = zero(Choice(
			t("\\'"), t("\\\\"), 
			seq(Not(t("'")), Any)
		));
		PExpression StringContent2 = zero(Choice(
				t("\\\""), t("\\\\"),
				seq(Not(t("\"")), Any)
		));
//		Peg StringContent = zero(Not(t("'")), Any);
//		Peg StringContent2 = zero(Not(t("\"")), Any);
		this.setRule("String", 
			Choice(
				seq(t("'"), Constructor(StringContent, Tag("#PegString")), t("'")),
				seq(t("\""), Constructor(StringContent2, Tag("#PegString")), t("\""))
			)
		);
		PExpression _Message = seq(t("`"), Constructor(zero(Not(t("`")), Any), Tag("#PegMessage")), t("`"));
		PExpression CharacterContent = zero(Not(t("]")), Any);
		PExpression _Character = seq(t("["), Constructor(CharacterContent, Tag("#PegCharacter")), t("]"));
		PExpression _Any = Constructor(t("."), Tag("#PegAny"));
		PExpression _Tagging = Constructor(t("#"), seq(one(c("A-Za-z0-9_.")), Tag("#PegTagging")));

		PExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		PExpression Connector  = Choice(t("@"), t("^"));
		PExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));
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
		PExpression _AssertFunc = Constructor(
			t("<assert"), 
			_WS,
			n("Expr"),
			Spacing,
			t(">"),
			Tag("#assert")
		);
		PExpression _ChoiceFunc = Constructor(
			t("<choice"), _WS,
			n("Expr"), Spacing, t(">"),
			Tag("#choice")
		);
		PExpression _RangeFunc = Constructor(
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



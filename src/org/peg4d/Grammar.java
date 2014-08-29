package org.peg4d;

import java.io.File;

import org.peg4d.model.ParsingModel;

public class Grammar {
	public final static PEG4dGrammar PEG4d = new PEG4dGrammar();
	
	GrammarFactory      factory;
	String              name;
	UMap<PegRule>       ruleMap;

	UList<PegRule>      exportedRuleList;
	UMap<String>        objectLabelMap = null;
	public boolean      foundError = false;

	MemoRemover memoRemover = null;

	int memoFactor;
	int optimizationLevel;
	int LexicalOptimization       = 0;
	int InliningCount             = 0;
	int InterTerminalOptimization = 0;
	int PredictionOptimization    = 0;

	int EnabledMemo  = 0;
	int DisabledMemo = 0;
		
	Grammar(GrammarFactory factory, String name) {
		this.name = name;
		this.factory = factory == null ? PEG4d.factory : factory;
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

	final boolean loadGrammarFile(String fileName) {
		PEG4dGrammar peg4d = Grammar.PEG4d;
		ParsingContext p = peg4d.newParserContext(Main.loadSource(peg4d, fileName));
		this.name = fileName;
		if(fileName.indexOf('/') > 0) {
			this.name = fileName.substring(fileName.lastIndexOf('/')+1);
		}
		p.setRecognitionMode(false);
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
		this.verify(/*objectRemover*/);
		return this.foundError;
	}
	
	private ParsingModel model = new ParsingModel();
	public final ParsingTag getModelTag(String tagName) {
		return model.get(tagName);
	}
	
	public final PExpression getDefinedExpression(long oid) {
		return this.getDefinedExpression(oid);
	}

	public final static char NameSpaceSeparator = ':';

	public final void importGrammar(String filePath) {
		this.importGrammar("", filePath);
	}
	
	final void importGrammar(String ns, String filePath) {
		Grammar peg = this.factory.getGrammar(filePath);
		if(peg != null) {
			UList<PegRule> ruleList = peg.getExportRuleList();
			//System.out.println("filePath: " + filePath + " peg=" + ruleList);
			for(int i = 0; i < ruleList.size(); i++) {
				PegRule rule = ruleList.ArrayValues[i];
				String key = rule.ruleName;
				if(ns != null && ns.length() > 0) {
					key = ns + ':' + rule.ruleName;
				}
				if(this.ruleMap.hasKey(key)) {
					Main.printVerbose("duplicated: ", key + " ");
				}
				Main.printVerbose("importing: ", rule);
				this.ruleMap.put(key, rule);
			}
		}
	}


	
	
	public final boolean hasRule(String ruleName) {
//		if(this.nsRuleMap != null) {
//			int loc = ruleName.indexOf(NameSpaceSeparator);
//			if(loc != -1) {
//				return this.nsRuleMap.get(ruleName) != null;
//			}
//		}
		return this.ruleMap.get(ruleName) != null;
	}

	public final PegRule getRule(String ruleName) {
//		if(this.nsRuleMap != null) {
//			int loc = ruleName.indexOf(NameSpaceSeparator);
//			if(loc != -1) {
//				return this.nsRuleMap.get(ruleName);
//			}
//		}
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
//		ObjectRemover objectRemover = new ObjectRemover();
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
		ParsingContext c = this.newParserContext();
		for(int i = 0; i < nameList.size(); i++) {
			PegRule rule = this.getRule(nameList.ArrayValues[i]);
			if(rule.getGrammar() == this) {
				rule.testExample(c);
			}
		}
	}

	final UList<PegRule> getExportRuleList() {
		if(this.exportedRuleList == null) {
			UList<PegRule> l = new UList<PegRule>(new PegRule[4]);
			PExpression e = this.getExpression("export");
			if(e != null) {
				appendExportRuleList(l, e.getExpression());
			}
			this.exportedRuleList = l;
		}
		return this.exportedRuleList;
	}

	private void appendExportRuleList(UList<PegRule> l, PExpression e) {
		if(e instanceof PNonTerminal) {
			PegRule rule = this.getRule(((PNonTerminal) e).symbol);
			l.add(rule);
			Main.printVerbose("export", rule.ruleName);
		}
		if(e instanceof PChoice) {
			for(int i = 0; i < e.size(); i++) {
				appendExportRuleList(l, e.get(i).getExpression());
			}
		}
	}

	int DefinedExpressionSize = 0;
	private int MultiReference = 0;
	private int Reference = 0;
	int StrongPredicatedChoice = 0;
	int PredicatedChoice = 0;
	int UnpredicatedChoice = 0;
	int PredicatedChoiceL1 = 0;
	int UnpredicatedChoiceL1 = 0;

	void updateStat(Stat stat) {
		stat.setText("Peg", this.getName());
		stat.setCount("PegSize", this.DefinedExpressionSize);
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

	public ParsingContext newParserContext() {
		return new TracingPackratParser(this, new StringSource(this, ""), 0);
	}

	public ParsingContext newParserContext(ParsingSource source) {
		ParsingContext p = new TracingPackratParser(this, source);
		if(Main.RecognitionOnlyMode) {
			p.setRecognitionMode(true);
		}
		return p;
	}

	public final UList<String> makeList(String startPoint) {
		return new ListMaker().make(this, startPoint);
	}

	public final void show(String startPoint) {
		this.show(startPoint, new GrammarFormatter());
	}

	public final void show(String startPoint, GrammarFormatter fmt) {
		StringBuilder sb = new StringBuilder();
		fmt.formatHeader(sb);
		UList<String> list = makeList(startPoint);
		for(int i = 0; i < list.size(); i++) {
			String name = list.ArrayValues[i];
			PExpression e = this.getExpression(name);
			fmt.formatRule(name, e, sb);
		}
		fmt.formatFooter(sb);
		System.out.println(sb.toString());
	}
	
	
	final PExpression newNonTerminal(String text) {
		return this.factory.newNonTerminal(this, text);
	}
	final PExpression newString(String text) {
		return this.factory.newString(this, text);
	}
	final PExpression newAny(String t) {
		return this.factory.newAny(this, t);
	}	
	public PExpression newByte(int ch, String t) {
		return this.factory.newByte(this, ch, t);
	}
	final PExpression newCharacter(String text) {
		return this.factory.newCharacter(this, text);
	}
	final PExpression newOptional(PExpression p) {
		return this.factory.newOptional(this, p);
	}
	final PExpression newOneMore(PExpression p) {
		return this.factory.newOneMore(this, p);
	}
	final PExpression newZeroMore(PExpression p) {
		return this.factory.newZeroMore(this, p);
	}
	final PExpression newAnd(PExpression p) {
		return this.factory.newAnd(this, p);
	}
	final PExpression newNot(PExpression p) {
		return this.factory.newNot(this, p);
	}
	final PExpression newChoice(UList<PExpression> l) {
		return this.factory.newChoice(this, l);
	}
	final PExpression newSequence(UList<PExpression> l) {
		return this.factory.newSequence(this, l);
	}
	final PExpression newConstructor(String tagName, PExpression p) {
		return this.factory.newConstructor(this, tagName, p);
	}
	final PExpression newJoinConstructor(String tagName, PExpression p) {
		return this.factory.newJoinConstructor(this, tagName, p);
	}
	final PExpression newConnector(PExpression p, int index) {
		return this.factory.newConnector(this, p, index);
	}
	final PExpression newTagging(String tag) {
		return this.factory.newTagging(this, tag);
	}
	final PExpression newMessage(String msg) {
		return this.factory.newMessage(this, msg);
	}
	final PExpression newMatch(PExpression e) {
		return this.factory.newMatch(this, e);
	}

	final void addChoice(UList<PExpression> l, PExpression e) {
		this.factory.addChoice(this, l, e);
	}
	final void addSequence(UList<PExpression> l, PExpression e) {
		this.factory.addSequence(l, e);
	}

}

class PegRule {
	ParsingSource source;
	long     pos;
	
	String ruleName;
	PExpression expr;
	int length = 0;
	boolean objectType;

	public PegRule(ParsingSource source, long pos, String ruleName, PExpression e) {
		this.source = source;
		this.pos = pos;
		this.ruleName = ruleName;
		this.expr = e;
		this.objectType = false;
	}
	@Override
	public String toString() {
		return this.ruleName + "=" + this.expr;
	}
	
	Grammar getGrammar() {
		return this.expr.base;
	}
	
	void reportError(String msg) {
		if(this.source != null) {
			Main._PrintLine(this.source.formatErrorMessage("error", this.pos, msg));
		}
		else {
			System.out.println("ERROR: " + msg);
		}
	}
	void reportWarning(String msg) {
		if(this.source != null) {
			Main._PrintLine(this.source.formatErrorMessage("warning", this.pos, msg));
		}
	}

	class PegRuleAnnotation {
		String key;
		String value;
		PegRuleAnnotation next;
		PegRuleAnnotation(String key, String value, PegRuleAnnotation next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
	}

	PegRuleAnnotation annotation;
	public void addAnotation(String key, String value) {
		this.annotation = new PegRuleAnnotation(key,value, this.annotation);
	}
	
	public void testExample(ParsingContext context) {
		PegRuleAnnotation a = this.annotation;
		while(a != null) {
			if(a.key.equals("ex") || a.key.equals("eg") || a.key.equals("example")) {
				System.out.println("Testing " + this.ruleName + " " + a.value);
				ParsingSource s = new StringSource(this.getGrammar(), "string", 1, a.value);
				context.resetSource(s);
				ParsingObject p = context.match(this.ruleName);
				if(p.isFailure() || context.hasUnconsumedCharacter()) {
					System.out.println("FAILED: " + this.ruleName + " " + a.value);
				}
			}
			a = a.next;
		}
	}
	
	
}

class PEG4dGrammar extends Grammar {
	static final int PRule        = ParsingTag.tagId("#PRule");
	static final int PImport      = ParsingTag.tagId("#PImport");
	static final int PAnnotation  = ParsingTag.tagId("#PAnnotation");
	static final int PString      = ParsingTag.tagId("#PString");
	static final int PByte        = ParsingTag.tagId("#PByte");
	static final int PCharacter   = ParsingTag.tagId("#PCharacter");
	static final int PAny         = ParsingTag.tagId("#PAny");
	static final int PNonTerminal = ParsingTag.tagId("#PNonTerminal");
	static final int PAnd         = ParsingTag.tagId("#PAnd");
	static final int PNot         = ParsingTag.tagId("#PNot");
	static final int POptional    = ParsingTag.tagId("#POptional");
	static final int POneMore     = ParsingTag.tagId("#POneMore");
	static final int PZeroMore    = ParsingTag.tagId("#PZeroMore");
	static final int PTimes       = ParsingTag.tagId("#PTimes");
	static final int PMatch       = ParsingTag.tagId("#PMatch");
	static final int PSequence    = ParsingTag.tagId("#PSequence");
	static final int PChoice      = ParsingTag.tagId("#PChoice");
	static final int PConstructor = ParsingTag.tagId("#PConstructor");
	static final int PConnector   = ParsingTag.tagId("#PConnector");
	static final int PLeftJoin    = ParsingTag.tagId("#PLeftJoin");
	static final int PTagging     = ParsingTag.tagId("#PTagging");
	static final int PMessage     = ParsingTag.tagId("#PMessage");
	static final int CommonError  = ParsingTag.tagId("#error");
	
	static boolean performExpressionConstruction(Grammar loading, ParsingContext context, ParsingObject pego) {
		//System.out.println("DEBUG? parsed: " + pego);		
		if(pego.is(PEG4dGrammar.PRule)) {
			if(pego.size() > 3) {
				System.out.println("DEBUG? parsed: " + pego);		
			}
			String ruleName = pego.textAt(0, "");
			PExpression e = toParsingExpression(loading, ruleName, pego.get(1));
			PegRule rule = new PegRule(pego.getSource(), pego.getSourcePosition(), ruleName, e);
			loading.setRule(ruleName, rule);
			if(pego.size() >= 3) {
				readAnnotations(rule, pego.get(2));
			}
			return true;
		}
		if(pego.is(PEG4dGrammar.PImport)) {
			String filePath = searchPegFilePath(context, pego.textAt(0, ""));
			String ns = pego.textAt(1, "");
			loading.importGrammar(ns, filePath);
			return true;
		}
		if(pego.is(PEG4dGrammar.CommonError)) {
			int c = pego.getSource().byteAt(pego.getSourcePosition());
			System.out.println(pego.formatSourceMessage("error", "syntax error: ascii=" + c));
			return false;
		}
		System.out.println("Unknown peg node: " + pego);
		return false;
	}
	
	private static void readAnnotations(PegRule rule, ParsingObject pego) {
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			if(p.is(PEG4dGrammar.PAnnotation)) {
				String key = p.textAt(0, "");
				String value = p.textAt(1, "");
				rule.addAnotation(key, value);
			}
		}
		
	}

	private static String searchPegFilePath(ParsingContext context, String filePath) {
		String f = context.source.getFilePath(filePath);
		if(new File(f).exists()) {
			return f;
		}
		if(new File(filePath).exists()) {
			return filePath;
		}
		return "lib/"+filePath;
	}
	
	private static PExpression toParsingExpression(Grammar loading, String ruleName, ParsingObject node) {
		PExpression e = toParsingExpressionImpl(loading, ruleName, node);
		loading.DefinedExpressionSize += 1;
		return e;
	}	
	
	private static PExpression toParsingExpressionImpl(Grammar loading, String ruleName, ParsingObject pego) {
		if(pego.is(PEG4dGrammar.PNonTerminal)) {
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
			if(nonTerminalSymbol.equals("_") && !loading.hasRule("_")) {      // space
				loading.setRule("_", Grammar.PEG4d.getExpression("_"));
			}
			if(nonTerminalSymbol.equals("COMMENT") && !loading.hasRule("COMMENT")) { // comment
				loading.setRule("COMMENT", Grammar.PEG4d.getRule("COMMENT"));
			}
			if(nonTerminalSymbol.equals("W") && !loading.hasRule("W")) { // W
				loading.setRule("W", Grammar.PEG4d.getRule("W"));
			}
			return new PNonTerminal(loading, 0, nonTerminalSymbol);
		}
		if(pego.is(PEG4dGrammar.PString)) {
			return loading.newString(ParsingCharset.unquoteString(pego.getText()));
		}
		if(pego.is(PEG4dGrammar.PCharacter)) {
			return loading.newCharacter(pego.getText());
		}
		if(pego.is(PEG4dGrammar.PByte)) {
			String t = pego.getText();
			int ch = ParsingCharset.parseHex2(t);
			return loading.newByte(ch, t);
		}
		if(pego.is(PEG4dGrammar.PAny)) {
			return loading.newAny(pego.getText());
		}
		if(pego.is(PEG4dGrammar.PChoice)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, pego.get(i));
				loading.addChoice(l, e);
			}
			return loading.newChoice(l);
		}
		if(pego.is(PEG4dGrammar.PSequence)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[pego.size()]);
			for(int i = 0; i < pego.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, pego.get(i));
				loading.addSequence(l, e);
			}
			return loading.newSequence(l);
		}
		if(pego.is(PEG4dGrammar.PNot)) {
			return loading.newNot(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is(PEG4dGrammar.PAnd)) {
			return loading.newAnd(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is(PEG4dGrammar.POneMore)) {
			return loading.newOneMore(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is(PEG4dGrammar.PZeroMore)) {
			return loading.newZeroMore(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is(PEG4dGrammar.POptional)) {
			return loading.newOptional(toParsingExpression(loading, ruleName, pego.get(0)));
		}
		if(pego.is(PEG4dGrammar.PTimes)) {
			int n = ParsingCharset.parseInt(pego.textAt(0, ""), 1);
			PExpression e = toParsingExpression(loading, ruleName, pego.get(0));
			UList<PExpression> l = new UList<PExpression>(new PExpression[n]);
			for(int i = 0; i < n; i++) {
				loading.addSequence(l, e);
			}
			return loading.newSequence(l);
		}
		if(pego.is(PEG4dGrammar.PTagging)) {
			return loading.newTagging(pego.getText());
		}
		if(pego.is(PEG4dGrammar.PMessage)) {
			return loading.newMessage(pego.getText());
		}
		if(pego.is(PEG4dGrammar.PLeftJoin)) {
			PExpression seq = toParsingExpression(loading, ruleName, pego.get(0));
			return loading.newJoinConstructor(ruleName, seq);
		}
		if(pego.is(PEG4dGrammar.PConstructor)) {
			PExpression seq = toParsingExpression(loading, ruleName, pego.get(0));
			return loading.newConstructor(ruleName, seq);
		}
		if(pego.is(PEG4dGrammar.PConnector)) {
			int index = -1;
			if(pego.size() == 2) {
				index = ParsingCharset.parseInt(pego.textAt(1, ""), -1);
			}
			return loading.newConnector(toParsingExpression(loading, ruleName, pego.get(0)), index);
		}
		if(pego.is(PEG4dGrammar.PMatch)) {
			return loading.newMatch(toParsingExpression(loading, ruleName, pego.get(0)));
		}
//		if(pego.is("#PExport")) {
//		Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
//		Peg o = loadingGrammar.newConstructor(ruleName, seq);
//		return new PegExport(loadingGrammar, 0, o);
//	}
//	if(pego.is("#PSetter")) {
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
		super(new GrammarFactory(), "PEG4d");
		this.optimizationLevel = 0;
		this.loadPEG4dGrammar();
		this.factory.setGrammar("p4d", this);
	}
	
	@Override
	public ParsingContext newParserContext(ParsingSource source) {
		return new TracingPackratParser(this, source, 0);  // best parser
	}

	// Definiton of PEG4d 	
	private final PExpression t(String token) {
		return new PString(this, 0, token);
	}
	private final PExpression c(String text) {
		return new PCharacter(this, 0, ParsingCharset.newParsingCharset(text));
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
		PExpression Any = newAny(".");
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
		this.setRule("idchar", c("A-Za-z0-9_."));
		this.setRule("digit", c("0-9"));
		this.setRule("hexdigit", c("0-9A-Fa-f"));
		PExpression Spacing = Optional(n("_"));
		this.setRule("RuleName", Constructor(c("A-Za-z_"), zero(c("A-Za-z0-9_")), Tag("#name")));
		this.setRule("LibName",  Constructor(c("A-Za-z_"), zero(c("A-Za-z0-9_.")), Tag("#name")));
		this.setRule("Number", Constructor(_NUMBER, Tag("#Integer")));
		
		this.setRule("NonTerminalName", 
			Constructor(
				c("A-Za-z_"), 
				zero(c("A-Za-z0-9_:")), 
				Tag("#PNonTerminal")
			)
		);
		this.setRule("NonTerminal", 
			seq(
				n("NonTerminalName"),
				Optional(
					LeftJoin(
						n("Param"),
						Tag("#PNonTerminal")				
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
				seq(t("'"), Constructor(StringContent, Tag("#PString")), t("'")),
				seq(t("\""), Constructor(StringContent2, Tag("#PString")), t("\""))
			)
		);
		PExpression _Message = seq(t("`"), Constructor(zero(Not(t("`")), Any), Tag("#PMessage")), t("`"));
		PExpression CharacterContent = zero(Not(t("]")), Any);
		PExpression _Character = seq(t("["), Constructor(CharacterContent, Tag("#PCharacter")), t("]"));
		PExpression _Any = Constructor(t("."), Optional(t(".")), Tag("#PAny"));
		PExpression _Tagging = Constructor(t("#"), seq(one(c("A-Za-z0-9_.")), Tag("#PTagging")));
		PExpression _Byte = Constructor(t("0x"), c("0-9A-Fa-f"), c("0-9A-Fa-f"), Tag("#PByte"));
		PExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		PExpression Connector  = Choice(t("@"), t("^"));
		PExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));
//		setRule("Setter", seq(Connector, LeftJoin(Optional(Number), Tag("#PSetter"))));

		this.setRule("Constructor", Constructor(
			ConstructorBegin, 
			Choice(
				seq(Connector, _WS, Tag("#PLeftJoin")), 
				Tag("#PConstructor")
			), 
			Spacing, 
			set(n("Expr")), 
			Spacing,
			ConstructorEnd
		));
		PExpression _LazyFunc = Constructor(
			t("<lazy"), 
			_WS,
			set(n("NonTerminal")),
			Spacing,
			t(">"),
			Tag("#PLazyNonTerminal")
		);
		PExpression _MatchFunc = Constructor(
			t("<match"), _WS,
			set(n("Expr")), Spacing, t(">"),
			Tag("#PMatch")
		);
		PExpression _TimesFunc = Constructor(
			t("<times"), _WS,
			set(n("Number")), _WS, 
			set(n("Expr")), Spacing, t(">"),
			Tag("#PTimes")
		);
		setRule("Term", 
			Choice(
				n("String"), _Character, _Any, _Message, _Tagging, _Byte, 
				seq(t("("), Spacing, n("Expr"), Spacing, t(")")),
				n("Constructor"), n("NonTerminal"), 
				_LazyFunc, _MatchFunc, _TimesFunc
			)
		);
		this.setRule("SuffixTerm", seq(
			n("Term"), 
			Optional(
				LeftJoin(
					Choice(
						seq(t("*"), Tag("#PZeroMore")), 
						seq(t("+"), Tag("#POneMore")), 
						seq(t("?"), Tag("#POptional")),
						seq(Connector, Optional(set(1, n("Number"))), Tag("#PConnector"))
					)
				)
			)
		));
		
		this.setRule("Predicate", Choice(
				Constructor(
						Choice(
								seq(t("&"), Tag("#PAnd")),
								seq(t("!"), Tag("#PNot")),
								seq(t("@["), Spacing, set(1, n("Number")), Spacing, t("]"), Tag("#PConnector")),							
								seq(t("@"), Tag("#PConnector"))
						), 
						set(0, n("SuffixTerm"))
				), 
				n("SuffixTerm")
		));
		PExpression _notRule = Not(Choice(
				n("Rule"), 
				n("Import")
		));
		this.setRule("Sequence", seq(
				n("Predicate"), 
				Optional(
					LeftJoin(
						one(
							Spacing, 
							_notRule,
							set(n("Predicate"))
						),
						Tag("#PSequence") 
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
						Tag("#PChoice") 
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
					Tag("#PParam") 
				),
				t("]")
			)
		);
		this.setRule("DOC", seq(
			zero(Not(t("]")), Not(t("[")), Any),
			Optional(seq(t("["), n("DOC"), t("]"), n("DOC") ))
		));
		
		this.setRule("Annotation",
			seq(
				t("["),
				Constructor(
					set(n("RuleName")),
					t(":"), 
					Spacing, 
					set(
						Constructor(
							n("DOC"),
							Tag("#value") 
						)
					),
					Tag("#PAnnotation") 
				),
				t("]"),
				Spacing
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
				Optional(seq(set(3, n("Param")), Spacing)),
				Optional(seq(set(2, n("Annotations")), Spacing)),
				t("="), Spacing, 
				set(1, n("Expr")),
				Tag("#PRule") 
			)
		);
		this.setRule("Import", Constructor(
			t("import"), 
			Tag("#PImport"), 
			_WS, 
			Choice(set(n("String")), n("LibName")), 
			Optional(
				seq(_WS, t("as"), _WS, set(n("RuleName")))
			)
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
			Optional(seq(t(";"), Spacing)) 
		));
		this.verify(/*null*/);
		return this;
	}

}



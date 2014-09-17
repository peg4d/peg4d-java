package org.peg4d;

import java.io.File;

import org.peg4d.model.ParsingModel;

public class Grammar {
	
	GrammarFactory      factory;
	String              name;
	UList<PegRule>      ruleList;
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
		this.factory = factory == null ? GrammarFactory.Grammar.factory : factory;
		this.ruleMap  = new UMap<PegRule>();
		this.ruleList  = new UList<PegRule>(new PegRule[16]);
		this.optimizationLevel = Main.OptimizationLevel;
		this.memoFactor = Main.MemoFactor;
		if(this.memoFactor < 0) {
			this.memoFactor = - this.memoFactor;
		}
	}

	public String getName() {
		return this.name;
	}
	
	public String uniqueRuleName(String ruleName) {
		return this.name + ":" + ruleName;
	}

	private UMap<PNonTerminal> nonTerminalMap = new UMap<PNonTerminal>();
	
	final PNonTerminal newNonTerminal(String text) {
		PNonTerminal e = nonTerminalMap.get(text);
		if(e == null) {
			e = new PNonTerminal(this, 0, text);
			e.uniqueId = PExpression.newExpressionId();
			nonTerminalMap.put(text, e);
		}
		return e;
	}
	
	void checkNonTerminal() {
		UList<String> l = nonTerminalMap.keys();
		for(int i = 0; i < l.size(); i++) {
			PNonTerminal e = nonTerminalMap.get(l.ArrayValues[i]);
			if(e.resolvedExpression == null) {
				e.resolvedExpression = this.getExpression(e.symbol);
				if(e.resolvedExpression == null) {
					System.out.println("undefined label: " + e.symbol);
					e.resolvedExpression = new ParsingIfFlag(0, e.symbol);
					PegRule rule = new PegRule(e.base, e.symbol, null, e.resolvedExpression);
					this.setRule(e.symbol, rule);
				}
			}
		}
	}

	final boolean loadGrammarFile(String fileName) {
		PEG4dGrammar peg4d = GrammarFactory.Grammar;
		ParsingSource s = Main.loadSource(peg4d, fileName);
		ParsingContext context = new ParsingContext(s); //peg4d.newParserContext();
		this.name = fileName;
		if(fileName.indexOf('/') > 0) {
			this.name = fileName.substring(fileName.lastIndexOf('/')+1);
		}
		context.setRecognitionMode(false);
		PEG4d builder = new PEG4d(this);
		while(context.hasByteChar()) {
			ParsingObject po = context.parse(peg4d, "Chunk");
			if(context.isFailure()) {
				String msg = context.source.formatPositionLine("error", context.fpos, context.getErrorMessage());
				Main._Exit(1, msg);
				return false;
			}
			if(!builder.parse(po)) {
				return false;
			}
//			if(!PEG4dGrammar.performExpressionConstruction1(this, po)) {
//				return false;
//			}
		}
		this.verify(/*objectRemover*/);
		return this.foundError;
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
		return this.ruleMap.get(ruleName) != null;
	}

	public final PegRule getRule(String ruleName) {
		return this.ruleMap.get(ruleName);
	}

	public final PExpression getExpression(String ruleName) {
		PegRule rule = this.getRule(ruleName);
		if(rule != null) {
			return rule.expr;
		}
		return null;
	}

//	public final void setRule(String ruleName, PExpression e) {
//		this.ruleMap.put(ruleName, new PegRule(this, null, 0, ruleName, e));
//	}

	public final void setRule(String ruleName, PegRule rule) {
		if(!this.hasRule(ruleName)) {
			this.ruleList.add(rule);
		}
		else {
			for(int i = 0; i  < this.ruleList.size(); i++) {
				if(ruleName.equals(this.ruleList.ArrayValues[i].ruleName)) {
					this.ruleList.ArrayValues[i] = rule;
					break;
				}
			}
		}
		this.ruleMap.put(ruleName, rule);
	}
	
	public final UList<PegRule> getRuleList() {
		return this.ruleList;
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
		this.foundError = false;
		this.checkNonTerminal();
		this.exportedRuleList = null;
		this.getExportRuleList();
////		ObjectRemover objectRemover = new ObjectRemover();
		UList<String> nameList = this.ruleMap.keys();
//		UMap<FlowGraph> buffer = new UMap<FlowGraph>();
//		UList<PExpression> stack = new UList<PExpression>(new PExpression[64]);
//		for(int i = 0; i < nameList.size(); i++) {
//			String ruleName = nameList.ArrayValues[i];
//			PegRule rule = this.getRule(ruleName);
//			String name = ruleName;
//			System.out.println("DEBUG: " + name + " = " + rule.expr);
//			FlowGraph g = FlowGraph.makeFlowGraph(rule.getUniqueName(), rule.expr, buffer);
//			stack.clear(0);
//			rule.length = g.checkLeftRecursion(rule, rule.getUniqueName(), stack, 0);
//			stack.clear(0);
//			rule.objectType = g.checkObjectType(rule, stack);
//			g.dump();
//		}

		new Inliner(this).performInlining();
		new Optimizer(this).optimize();
		
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		this.memoRemover = new MemoRemover(this);
		ParsingContext context = new ParsingContext(null);
		for(int i = 0; i < nameList.size(); i++) {
			PegRule rule = this.getRule(nameList.ArrayValues[i]);
			if(rule.getGrammar() == this) {
				rule.testExample1(this, context);
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

	void updateStat(ParsingStat stat) {
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

	public final void formatAll(GrammarFormatter fmt) {
		StringBuilder sb = new StringBuilder();
		fmt.formatHeader(sb);
		UList<PegRule> list = this.getRuleList();
		for(int i = 0; i < list.size(); i++) {
			PegRule r = list.ArrayValues[i];
			fmt.formatRule(r.ruleName, r.expr, sb);
		}
		fmt.formatFooter(sb);
		System.out.println(sb.toString());
	}
	
	public final void simpleFormatAll(SimpleGrammarFormatter fmt) {
		StringBuilder sb = new StringBuilder();
		fmt.formatHeader(sb);
		UList<PegRule> list = this.getRuleList();
		for(int i = 0; i < list.size(); i++) {
			PegRule r = list.ArrayValues[i];
			fmt.nonTerminalMap.put(r.ruleName, fmt.opList.size());
			fmt.formatRule(r.ruleName, r.expr, sb);
		}
		fmt.formatFooter(sb);
		System.out.println(sb.toString());
	}
	
	
	private ParsingModel model = new ParsingModel();
	public final ParsingTag newTag(String tagName) {
		return model.get(tagName);
	}

	public final ParsingTag newStartTag() {
		return model.get("Text");
	}
}

class PegRule {
	Grammar  peg;
	String ruleName;

	ParsingObject po;
	
	PExpression expr;
	int length = 0;
	boolean objectType;

	PegRule(Grammar peg, String ruleName, ParsingObject po, PExpression e) {
		this.peg = peg;
		this.po = po;
		this.ruleName = ruleName;
		this.expr = e;
		this.objectType = false;
	}
	public String getUniqueName() {
		return this.peg.uniqueRuleName(ruleName);
	}
	@Override
	public String toString() {
		return this.ruleName + "=" + this.expr;
	}
	
	Grammar getGrammar() {
		return this.peg;
	}
	
	void reportError(String msg) {
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage("error", msg + " in:\n" + this));
		}
		else {
			System.out.println("ERROR: " + msg + " in:\n" + this);
		}
	}
	void reportWarning(String msg) {
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage("warning", msg + " in:\n" + this));
		}
		else {
			System.out.println("WARNING: " + msg + " in:\n" + this);
		}
	}

	class PegRuleAnnotation {
		String key;
		ParsingObject value;
		PegRuleAnnotation next;
		PegRuleAnnotation(String key, ParsingObject value, PegRuleAnnotation next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
	}

	PegRuleAnnotation annotation;
	public void addAnotation(String key, ParsingObject value) {
		this.annotation = new PegRuleAnnotation(key,value, this.annotation);
	}
	
	public final void testExample1(Grammar peg, ParsingContext context) {
		PegRuleAnnotation a = this.annotation;
		while(a != null) {
			boolean isExample = a.key.equals("example");
			boolean isBadExample = a.key.equals("bad-example");
			if(isExample || isBadExample) {
				boolean ok = true;
				ParsingSource s = ParsingObjectUtils.newStringSource(a.value);
				context.resetSource(s, 0);
				context.parse(peg, this.ruleName);
//				System.out.println("@@ " + context.isFailure() + " " + context.hasByteChar() + " " + isExample + " " + isBadExample);
				if(context.isFailure() || context.hasByteChar()) {
					if(isExample) ok = false;
				}
				else {
					if(isBadExample) ok = false;
				}
				String msg = ( ok ? "[PASS]" : "[FAIL]" ) + " " + this.ruleName + " " + a.value.getText();
				if(Main.TestMode && !ok) {	
					Main._Exit(1, "[FAIL] tested " + a.value.getText() + " by " + peg.getRule(this.ruleName));
				}
				Main.printVerbose("Testing", msg);
			}
			a = a.next;
		}
	}
}

class PEG4dGrammar extends Grammar {
		
	static boolean performExpressionConstruction(Grammar loading, ParsingObject po) {
		//System.out.println("DEBUG? parsed: " + po);
		if(po.is(PEG4d.Rule)) {
			if(po.size() > 3) {
				System.out.println("DEBUG? parsed: " + po);		
			}
			String ruleName = po.textAt(0, "");
			if(po.get(0).is(ParsingTag.String)) {
				ruleName = quote(ruleName);
			}
			PegRule rule = loading.getRule(ruleName);
			PExpression e = toParsingExpression(loading, ruleName, po.get(1));
			if(rule != null) {
				if(rule.po != null) {
					if(rule.peg == loading) {
						rule.reportWarning("duplicated rule name: " + ruleName);
					}
					rule = null;
				}
			}
			if(rule == null) {
				rule = new PegRule(loading, ruleName, po.get(0), e);
			}
			else {
				rule.peg = loading;
				rule.po = po.get(0);
				rule.expr = e;
			}
			loading.setRule(ruleName, rule);
			if(po.size() >= 3) {
				readAnnotations(rule, po.get(2));
			}
			return true;
		}
		if(po.is(PEG4d.Import)) {
			String filePath = searchPegFilePath(po.getSource(), po.textAt(0, ""));
			String ns = po.textAt(1, "");
			loading.importGrammar(ns, filePath);
			return true;
		}
		if(po.is(ParsingTag.CommonError)) {
			int c = po.getSource().byteAt(po.getSourcePosition());
			System.out.println(po.formatSourceMessage("error", "syntax error: ascii=" + c));
			return false;
		}
		System.out.println(po.formatSourceMessage("error", "PEG rule is required: " + po));
		return false;
	}
	
	private static void readAnnotations(PegRule rule, ParsingObject pego) {
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			if(p.is(PEG4d.Annotation)) {
				rule.addAnotation(p.textAt(0, ""), p.get(1));
			}
		}
	}

	private static String searchPegFilePath(ParsingSource s, String filePath) {
		String f = s.getFilePath(filePath);
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
	
	private static String quote(String t) {
		return "\"" + t + "\"";
	}
	
	private static PExpression toParsingExpressionImpl(Grammar loading, String ruleName, ParsingObject po) {
		if(po.is(PEG4d.NonTerminal)) {
			String symbol = po.getText();
			if(ruleName.equals(symbol)) {
				PExpression e = loading.getExpression(ruleName);
				if(e != null) {
					// self-redefinition
					return e;  // FIXME
				}
			}
			if(symbol.length() > 0 && !symbol.endsWith("_") && !loading.hasRule(symbol) && GrammarFactory.Grammar.hasRule(symbol)) { // comment
				Main.printVerbose("implicit importing", symbol);
				loading.setRule(symbol, GrammarFactory.Grammar.getRule(symbol));
			}
			return loading.newNonTerminal(symbol);
		}
		if(po.is(ParsingTag.String)) {
			String t = quote(po.getText());
			if(loading.hasRule(t)) {
				Main.printVerbose("direct inlining", t);
				return loading.getExpression(t);
			}
			return PExpression.newString(ParsingCharset.unquoteString(po.getText()));
		}
		if(po.is(PEG4d.CharacterSequence)) {
			return PExpression.newString(ParsingCharset.unquoteString(po.getText()));
		}
		if(po.is(PEG4d.Character)) {
			ParsingCharset u = null;
			if(po.size() > 0) {
				for(int i = 0; i < po.size(); i++) {
					ParsingObject o = po.get(i);
					if(o.is(ParsingTag.List)) {
						u = ParsingCharset.addText(u, o.textAt(0, ""), o.textAt(1, ""));
					}
					if(o.is(PEG4d.Character)) {
						u = ParsingCharset.addText(u, o.getText(), o.getText());
					}
					//System.out.println("u=" + u + " by " + o);
				}
			}
			return PExpression.newCharacter(u);
		}
		if(po.is(PEG4d.Byte)) {
			String t = po.getText();
			if(t.startsWith("U+")) {
				int c = ParsingCharset.hex(t.charAt(2));
				c = (c * 16) + ParsingCharset.hex(t.charAt(3));
				c = (c * 16) + ParsingCharset.hex(t.charAt(4));
				c = (c * 16) + ParsingCharset.hex(t.charAt(5));
				if(c < 128) {
					return PExpression.newByteChar(c);					
				}
				String t2 = java.lang.String.valueOf((char)c);
				return PExpression.newString(t2);
			}
			int c = ParsingCharset.hex(t.charAt(t.length()-2)) * 16 + ParsingCharset.hex(t.charAt(t.length()-1)); 
			return PExpression.newByteChar(c);
		}
		if(po.is(PEG4d.Any)) {
			return PExpression.newAny(po.getText());
		}
		if(po.is(PEG4d.Choice)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
			for(int i = 0; i < po.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, po.get(i));
				PExpression.addChoice(l, e);
			}
			return PExpression.newChoice(l);
		}
		if(po.is(PEG4d.Sequence)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
			for(int i = 0; i < po.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, po.get(i));
				PExpression.addSequence(l, e);
			}
			return PExpression.newSequence(l);
		}
		if(po.is(PEG4d.Not)) {
			return PExpression.newNot(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.And)) {
			return PExpression.newAnd(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.OneMoreRepetition)) {
			return PExpression.newOneMore(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.Repetition)) {
			if(po.size() == 2) {
				int ntimes = ParsingCharset.parseInt(po.textAt(1, ""), -1);
				return PExpression.newTimes(ntimes, toParsingExpression(loading, ruleName, po.get(0)));
			}
			return PExpression.newRepetition(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.Optional)) {
			return PExpression.newOptional(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.Tagging)) {
			return PExpression.newTagging(loading.newTag(po.getText()));
		}
		if(po.is(PEG4d.Value)) {
			return PExpression.newValue(po.getText());
		}
		if(po.is(PEG4d.LeftJoin)) {
			PExpression seq = toParsingExpression(loading, ruleName, po.get(0));
			return PExpression.newJoinConstructor(seq);
		}
		if(po.is(PEG4d.Constructor)) {
			PExpression seq = toParsingExpression(loading, ruleName, po.get(0));
			return PExpression.newConstructor(seq);
		}
		if(po.is(PEG4d.Connector)) {
			int index = -1;
			if(po.size() == 2) {
				index = ParsingCharset.parseInt(po.textAt(1, ""), -1);
			}
			return PExpression.newConnector(toParsingExpression(loading, ruleName, po.get(0)), index);
		}
		if(po.is(PEG4d.Match)) {
			return PExpression.newMatch(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.With)) {
			return PExpression.newEnableFlag(po.textAt(0, ""), toParsingExpression(loading, ruleName, po.get(1)));
		}
		if(po.is(PEG4d.Without)) {
			return PExpression.newDisableFlag(po.textAt(0, ""), toParsingExpression(loading, ruleName, po.get(1)));
		}
		if(po.is(PEG4d.If)) {
			return PExpression.newFlag(po.textAt(0, ""));
		}
		if(po.is(PEG4d.Indent)) {
			if(po.size() == 0) {
				return PExpression.newIndent(null);
			}
			return PExpression.newIndent(toParsingExpression(loading, ruleName, po.get(0)));
		}
		
		if(po.is(PEG4d.Debug)) {
			return PExpression.newDebug(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4d.Fail)) {
			return PExpression.newFail(ParsingCharset.unquoteString(po.textAt(0, "")));
		}
		if(po.is(PEG4d.Catch)) {
			return PExpression.newCatch();
		}
//		if(po.is(PEG4dGrammar.ParsingApply)) {
//			return PExpression.newApply(toParsingExpression(loading, ruleName, po.get(0)));
//		}
//		if(po.is(PEG4dGrammar.ParsingStringfy)) {
//			return PExpression.newStringfy();
//		}
//		if(pego.is("PExport")) {
//		Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
//		Peg o = loadingGrammar.newConstructor(ruleName, seq);
//		return new PegExport(loadingGrammar, 0, o);
//	}
//	if(pego.is("PSetter")) {
//		int index = -1;
//		String indexString = pego.getText();
//		if(indexString.length() > 0) {
//			index = UCharset.parseInt(indexString, -1);
//		}
//		return loadingGrammar.newConnector(toParsingExpression(loadingGrammar, ruleName, pego.get(0)), index);
//	}
//		if(node.is("pipe")) {
//			return new PegPipe(node.getText());
//		}
//		if(node.is("catch")) {
//			return new PegCatch(null, toPeg(node.get(0)));
//		}
		Main._Exit(1, "undefined peg: " + po);
		return null;
	}
	
	PEG4dGrammar() {
		super(new GrammarFactory(), "PEG4d");
		this.optimizationLevel = 0;
		this.loadPEG4dGrammar();
		this.factory.setGrammar("p4d", this);
	}
	
	// Definiton of PEG4d 	
	private final PExpression t(String token) {
		return PExpression.newString(token);
	}
	private final PExpression c(String text) {
		return PExpression.newCharacter(ParsingCharset.newParsingCharset(text));
	}
	private final PExpression P(String ruleName) {
		return newNonTerminal(ruleName);
	}
	private final PExpression Optional(PExpression e) {
		return PExpression.newOptional(e);
	}
	private final PExpression ZeroMore(PExpression e) {
		return PExpression.newRepetition(e);
	}
	private final PExpression ZeroMore(PExpression ... elist) {
		return PExpression.newRepetition(Sequence(elist));
	}
	private final PExpression OneMore(PExpression e) {
		return PExpression.newOneMore(e);
	}
	private final PExpression OneMore(PExpression ... elist) {
		return PExpression.newOneMore(Sequence(elist));
	}
	private final PExpression Sequence(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			PExpression.addSequence(l, e);
		}
		return PExpression.newSequence(l);
	}
	private final PExpression Choice(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			PExpression.addChoice(l, e);
		}
		return PExpression.newChoice(l);
	}
	private final PExpression Not(PExpression e) {
		return PExpression.newNot(e);
	}
	private final PExpression Tag(int tagId) {
		return PExpression.newTagging(newTag(ParsingTag.tagName(tagId)));
	}
	private final PExpression Constructor(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			PExpression.addSequence(l, e);
		}
		return PExpression.newConstructor(PExpression.newSequence(l));
	}
	private PExpression LeftJoin(PExpression ... elist) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[8]);
		for(PExpression e : elist) {
			PExpression.addSequence(l, e);
		}
		return PExpression.newJoinConstructor(PExpression.newSequence(l));
	}
	private PExpression Link(PExpression e) {
		return PExpression.newConnector(e, -1);
	}
	
	private PExpression Link(int index, PExpression e) {
		return PExpression.newConnector(e, index);
	}
	
	public Grammar loadPEG4dGrammar() {
		PExpression Any = PExpression.newAny(".");
		PExpression _NEWLINE = c("\\r\\n");
		PExpression _S = Choice(c(" \\t\\r\\n"), t("\u3000"));
		PExpression _DIGIT = c("0-9");
		PExpression _HEX = c("0-9A-Fa-f");
		
		PExpression _LETTER = c("A-Za-z_");
		PExpression _W = c("A-Za-z0-9_");
		
		this.setRule("DIGIT", _DIGIT);
		this.setRule("S", _S);
		this.setRule("LETTER", _LETTER);		
		this.setRule("W", _W);
		this.setRule("NAME", Sequence(_LETTER, ZeroMore(_W)));
		this.setRule("HEX", c("0-9A-Fa-f"));

		PExpression _INTEGER = OneMore(_DIGIT);
		this.setRule("COMMENT", 
			Choice(
				Sequence(t("/*"), ZeroMore(Not(t("*/")), Any), t("*/")),
				Sequence(t("//"), ZeroMore(Not(_NEWLINE), Any), _NEWLINE)
			)
		);
		this.setRule("_", ZeroMore(Choice(OneMore(_S), P("COMMENT"))));
		PExpression Spacing = P("_");
		
		this.setRule("Name",       Constructor(_LETTER, ZeroMore(_W), Tag(ParsingTag.Name)));
		this.setRule("DotName",    Constructor(_LETTER, ZeroMore(c("A-Za-z0-9_.")), Tag(ParsingTag.Name)));
		this.setRule("HyphenName_", Constructor(_LETTER, ZeroMore(Choice(_W, t("-"))), Tag(ParsingTag.Name)));
		this.setRule("Integer",    Constructor(_INTEGER, Tag(ParsingTag.Integer)));
		
		this.setRule("NonTerminal_", 
			Constructor(
				_LETTER, 
				ZeroMore(c("A-Za-z0-9_:")), 
				Tag(PEG4d.NonTerminal)
			)
		);
		PExpression StringContent  = ZeroMore(Choice(
			t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any)
		));
		PExpression StringContent2 = ZeroMore(Choice(
			t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any)
		));
		this.setRule("String", 
			Sequence(t("\""), Constructor(StringContent2, Tag(ParsingTag.String)), t("\""))
		);
		this.setRule("SingleQuotedString", 
			Sequence(t("'"),  Constructor(StringContent, Tag(PEG4d.CharacterSequence)), t("'"))
		);
		PExpression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), Any)
		));
		PExpression _Message = Sequence(t("`"), Constructor(ValueContent, Tag(PEG4d.Value)), t("`"));
		PExpression _Char2 = Choice( 
			Sequence(t("\\u"), _HEX, _HEX, _HEX, _HEX),
			Sequence(t("\\x"), _HEX, _HEX),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), Any)
		);
		PExpression _CharChunk = Sequence(
			Constructor (_Char2, Tag(PEG4d.Character)), 
			Optional(
				LeftJoin(t("-"), Link(Constructor(_Char2, Tag(PEG4d.Character))), Tag(ParsingTag.List))
			)
		);
		this.setRule("Charcter_", Sequence(t("["), Constructor(ZeroMore(Link(_CharChunk)), Tag(PEG4d.Character)), t("]")));

		PExpression _Any = Constructor(t("."), Tag(PEG4d.Any));
		PExpression _Tagging = Sequence(t("#"), Constructor(OneMore(c("A-Za-z0-9_."), Tag(PEG4d.Tagging))));
		PExpression _Byte = Constructor(t("0x"), _HEX, _HEX, Tag(PEG4d.Byte));
		PExpression _Unicode = Constructor(t("U+"), _HEX, _HEX, _HEX, _HEX, Tag(PEG4d.Byte));
		PExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		PExpression Connector  = Choice(t("@"), t("^"));
		PExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));

		this.setRule("Constructor_", Constructor(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, _S, Tag(PEG4d.LeftJoin)), 
				Tag(PEG4d.Constructor)
			), 
			Spacing, 
			Link(P("Expr_")), 
			Spacing,
			ConstructorEnd
		));
		PExpression _Pipe = Optional(t("|"));
		setRule("Func_", 
			Sequence(t("<"), Constructor(
				Choice(
					Sequence(t("debug"),   _S, Link(P("Expr_")), Tag(PEG4d.Debug)),
					Sequence(t("memo"),   _S, Link(P("Expr_")), Spacing, t(">"), Tag(PEG4d.Memo)),
					Sequence(t("match"),   _S, Link(P("Expr_")), Spacing, t(">"), Tag(PEG4d.Match)),
					Sequence(t("fail"),   _S, Link(P("SingleQuotedString")), Spacing, t(">"), Tag(PEG4d.Fail)),
					Sequence(t("catch"), Tag(PEG4d.Catch)),
					Sequence(t("if"), _S, Optional(t("!")), Link(P("Name")), Tag(PEG4d.If)),
					Sequence(t("with"),  _S, Link(P("Name")), _S, Link(P("Expr_")), Tag(PEG4d.With)),
					Sequence(t("without"), _S, Link(P("Name")), _S, Link(P("Expr_")), Tag(PEG4d.Without)),
					Sequence(t("indent"), Optional(Sequence(_S, Link(P("Expr_")))), Tag(PEG4d.Indent)),
					Sequence(t("choice"), Tag(PEG4d.Choice)),
					Sequence(_Pipe, t("append-choice"), Tag(PEG4d.Choice)),
					Sequence(_Pipe, t("stringfy"), Tag(PEG4d.Stringfy)),
					Sequence(_Pipe, t("apply"), _S, Link(P("Expr_")), Tag(PEG4d.Apply))
				)
			), Spacing, t(">"))
		);
		
		setRule("Term_", 
			Choice(
				P("SingleQuotedString"), P("Charcter_"), P("Func_"),  
				_Any, _Message, _Tagging, _Byte, _Unicode,
				Sequence(t("("), Spacing, P("Expr_"), Spacing, t(")")),
				P("Constructor_"), P("String"), P("NonTerminal_") 
			)
		);
		this.setRule("SuffixTerm_", Sequence(
			P("Term_"), 
			Optional(
				LeftJoin(
					Choice(
						Sequence(t("*"), Optional(Link(1, P("Integer"))), Tag(PEG4d.Repetition)), 
						Sequence(t("+"), Tag(PEG4d.OneMoreRepetition)), 
						Sequence(t("?"), Tag(PEG4d.Optional)),
						Sequence(Connector, Optional(Link(1, P("Integer"))), Tag(PEG4d.Connector))
					)
				)
			)
		));
		
		this.setRule("Predicate_", Choice(
				Constructor(
						Choice(
								Sequence(t("&"), Tag(PEG4d.And)),
								Sequence(t("!"), Tag(PEG4d.Not)),
								Sequence(t("@["), Spacing, Link(1, P("Integer")), Spacing, t("]"), Tag(PEG4d.Connector)),							
								Sequence(t("@"), Tag(PEG4d.Connector))
						), 
						Link(0, P("SuffixTerm_"))
				), 
				P("SuffixTerm_")
		));
		PExpression _NotRule = Not(Choice(
				P("Rule_"), 
				P("Import_")
		));
		this.setRule("Sequence_", Sequence(
				P("Predicate_"), 
				Optional(
					LeftJoin(
						OneMore(
							Spacing, 
							_NotRule,
							Link(P("Predicate_"))
						),
						Tag(PEG4d.Sequence) 
					)
				)
		));
		this.setRule("Expr_", 
			Sequence(
				P("Sequence_"), 
				Optional(
					LeftJoin(
						OneMore(
							Spacing, t("/"), Spacing, 
							Link(P("Sequence_"))
						),
						Tag(PEG4d.Choice) 
					)
				)
			)
		);
		this.setRule("Param_",
			Sequence(
				t("["),
				Constructor(
					Link(P("Name")),
					ZeroMore(
						Spacing,
						Link(P("Name"))
					),
					Tag(ParsingTag.List) 
				),
				t("]")
			)
		);
		this.setRule("DOC_", Sequence(
			ZeroMore(Not(t("]")), Not(t("[")), Any),
			Optional(Sequence(t("["), P("DOC_"), t("]"), P("DOC_") ))
		));
		
		this.setRule("Annotation_",
			Sequence(
				t("["),
				Constructor(
					Link(P("HyphenName_")),
					t(":"), 
					Spacing, 
					Link(
						Constructor(
							P("DOC_"),
							Tag(ParsingTag.Text) 
						)
					),
					Tag(PEG4d.Annotation) 
				),
				t("]"),
				Spacing
			)
		);
		this.setRule("Annotations_",
				Constructor(
					OneMore(Link(P("Annotation_"))),
					Tag(ParsingTag.List) 
				)
		);
		this.setRule("Rule_", 
			Constructor(
				Link(0, Choice(P("Name"), P("String"))), Spacing, 
				Optional(Sequence(Link(3, P("Param_")), Spacing)),
				Optional(Sequence(Link(2, P("Annotations_")), Spacing)),
				t("="), Spacing, 
				Link(1, P("Expr_")),
				Tag(PEG4d.Rule) 
			)
		);
		this.setRule("Import_", Constructor(
			t("import"), 
			Tag(PEG4d.Import), 
			_S, 
			Choice(Link(P("SingleQuotedString")), P("DotName")), 
			Optional(
				Sequence(_S, t("as"), _S, Link(P("Name")))
			)
		));
		this.setRule("Chunk", Sequence(
			Spacing, 
			Choice(
				P("Rule_"), 
				P("Import_")
			), 
			Spacing, 
			Optional(Sequence(t(";"), Spacing))
		));
		this.setRule("File", Sequence(
			Spacing, 
			Choice(
				P("Rule_"), 
				P("Import_")
			), 
			Spacing, 
			Optional(Sequence(t(";"), Spacing)) 
		));
		this.verify(/*null*/);
		return this;
	}
	
	private void setRule(String ruleName, PExpression e) {
		PegRule rule = new PegRule(this, ruleName, null, null);
		rule.expr = e;
		this.setRule(ruleName, rule);
	}

}



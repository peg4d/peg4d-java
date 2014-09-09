package org.peg4d;

import java.io.File;

import org.peg4d.model.ParsingModel;

public class Grammar {
	public final static PEG4dGrammar PEG4d = new PEG4dGrammar();
	
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
		this.factory = factory == null ? PEG4d.factory : factory;
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

	final boolean loadGrammarFile(String fileName) {
		PEG4dGrammar peg4d = Grammar.PEG4d;
		ParsingSource s = Main.loadSource(peg4d, fileName);
		ParsingContext context = new ParsingContext(s); //peg4d.newParserContext();
		this.name = fileName;
		if(fileName.indexOf('/') > 0) {
			this.name = fileName.substring(fileName.lastIndexOf('/')+1);
		}
		context.setRecognitionMode(false);
		while(context.hasByteChar()) {
			ParsingObject po = context.parse(peg4d, "Chunk");
			if(context.isFailure()) {
				String msg = context.source.formatPositionLine("error", context.fpos, context.getErrorMessage());
				Main._Exit(1, msg);
				break;
			}
			if(!PEG4dGrammar.performExpressionConstruction1(this, po)) {
				return false;
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

	public final void setRule(String ruleName, PExpression e) {
		this.ruleMap.put(ruleName, new PegRule(null, 0, ruleName, e));
	}

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
//			String name = ruleName;
//			if(name.equals("PrimaryType")) {
//				System.out.println("DEBUG: " + name + " = " + rule.expr);
//			}
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
				rule.testExample(this, context);
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

//	public ParsingStream newParserContext() {
//		return new TracingPackratParser(this, new StringSource(this, ""), 0);
//	}
//
//	public ParsingStream newParserContext(ParsingSource source) {
//		ParsingStream p = new TracingPackratParser(this, source);
//		if(Main.RecognitionOnlyMode) {
//			p.setRecognitionMode(true);
//		}
//		return p;
//	}

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
	final PExpression newCharacter(ParsingCharset u) {
		return this.factory.newCharacter(this, u);
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

	public PExpression newDebug(PExpression e) {
		return new ParsingDebug(e);
	}

	public PExpression newFail(String message) {
		return new ParsingFail(this, 0, message);
	}

	public PExpression newCatch() {
		return new ParsingCatch(this, 0);
	}

	
	public PExpression newFlag(String flagName) {
		return new ParsingFlag(this, 0, flagName);
	}

	public PExpression newEnableFlag(String flagName, PExpression e) {
		return new ParsingEnableFlag(flagName, e);
	}

	public PExpression newDisableFlag(String flagName, PExpression e) {
		return new ParsingDisableFlag(flagName, e);
	}

	public PExpression newIndent(PExpression e) {
		if(e == null) {
			return new ParsingIndent(this, 0);
		}
		return new ParsingStackIndent(e);
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
			Main._PrintLine(this.source.formatPositionLine("error", this.pos, msg));
		}
		else {
			System.out.println("ERROR: " + msg);
		}
	}
	void reportWarning(String msg) {
		if(this.source != null) {
			Main._PrintLine(this.source.formatPositionLine("warning", this.pos, msg));
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
	
	public void testExample(Grammar peg, ParsingContext context) {
		PegRuleAnnotation a = this.annotation;
		while(a != null) {
			if(a.key.equals("example")) {
				String msg = this.ruleName + " " + a.value;
				ParsingSource s = new StringSource(this.getGrammar(), "string", 1, a.value);
				context.resetSource(s, 0);
				context.parse(peg, this.ruleName);
				if(context.isFailure() || context.hasByteChar()) {
					msg = "[FAILED] " + msg;
					if(Main.TestMode) {
						Main._Exit(1, msg);
					}
					System.out.println(msg);
				}
				Main.printVerbose("Testing", this.ruleName + " " + a.value);
			}
			if(a.key.equals("bad-example")) {
				String msg = this.ruleName + " " + a.value;
				ParsingSource s = new StringSource(this.getGrammar(), "string", 1, a.value);
				context.resetSource(s, 0);
				context.parse(peg, this.ruleName);
				if(!context.isFailure() && !context.hasByteChar()) {
					msg = "[FAILED] " + msg;
					if(Main.TestMode) {
						Main._Exit(1, msg);
					}
				}
				Main.printVerbose("Testing", msg);
			}
			a = a.next;
		}
	}
}

class PEG4dGrammar extends Grammar {
	
	static final int Name         = ParsingTag.tagId("Name");
	static final int List         = ParsingTag.tagId("List");
	static final int Integer      = ParsingTag.tagId("Integer");
	static final int String       = ParsingTag.tagId("String");
	static final int Text         = ParsingTag.tagId("Text");
	static final int CommonError  = ParsingTag.tagId("error");
	
	static final int ParsingRule        = ParsingTag.tagId("ParsingRule");
	static final int ParsingImport      = ParsingTag.tagId("ParsingImport");
	static final int ParsingAnnotation  = ParsingTag.tagId("ParsingAnnotation");
	static final int ParsingString      = ParsingTag.tagId("ParsingString");
	static final int ParsingByte        = ParsingTag.tagId("ParsingByte");
	static final int ParsingCharacter   = ParsingTag.tagId("ParsingCharacter");
	static final int ParsingAny         = ParsingTag.tagId("ParsingAny");
	static final int ParsingNonTerminal = ParsingTag.tagId("ParsingNonTerminal");
	static final int ParsingAnd         = ParsingTag.tagId("ParsingAnd");
	static final int ParsingNot         = ParsingTag.tagId("ParsingNot");
	static final int ParsingOptional    = ParsingTag.tagId("ParsingOptional");
	static final int ParsingOneMore     = ParsingTag.tagId("ParsingOneMore");
	static final int ParsingZeroMore    = ParsingTag.tagId("ParsingZeroMore");
	static final int PTimes       = ParsingTag.tagId("ParsingTimes");
	
	
	static final int ParsingSequence    = ParsingTag.tagId("ParsingSequence");
	static final int ParsingChoice      = ParsingTag.tagId("ParsingChoice");
	static final int ParsingConstructor = ParsingTag.tagId("ParsingConstructor");
	static final int ParsingConnector   = ParsingTag.tagId("ParsingConnector");
	static final int ParsingLeftJoin    = ParsingTag.tagId("ParsingLeftJoin");
	static final int ParsingTagging     = ParsingTag.tagId("ParsingTagging");
	static final int ParsingValue     = ParsingTag.tagId("ParsingValue");
	
	static final int ParsingMatch       = ParsingTag.tagId("ParsingMatch");
	static final int ParsingMemo        = ParsingTag.tagId("ParsingMatch");
	static final int ParsingDebug       = ParsingTag.tagId("ParsingDebug");
	static final int ParsingFail        = ParsingTag.tagId("ParsingFail");
	static final int ParsingCatch       = ParsingTag.tagId("ParsingCatch");

	static final int ParsingFlag        = ParsingTag.tagId("ParsingFlag");
	static final int ParsingEnableFlag  = ParsingTag.tagId("ParsingEnable");
	static final int ParsingDisableFlag = ParsingTag.tagId("ParsingDisable");
	static final int ParsingIndent      = ParsingTag.tagId("ParsingIndent");

	static final int ParsingApply       = ParsingTag.tagId("ParsingApply");
	static final int ParsingStringfy    = ParsingTag.tagId("ParsingStringfy");

	static boolean performExpressionConstruction1(Grammar loading, ParsingObject po) {
		//System.out.println("DEBUG? parsed: " + po);		
		if(po.is(PEG4dGrammar.ParsingRule)) {
			if(po.size() > 3) {
				System.out.println("DEBUG? parsed: " + po);		
			}
			String ruleName = po.textAt(0, "");
			if(po.get(0).is(PEG4dGrammar.String)) {
				ruleName = quote(ruleName);
			}
			PExpression e = toParsingExpression(loading, ruleName, po.get(1));
			PegRule rule = new PegRule(po.getSource(), po.getSourcePosition(), ruleName, e);
//			if(ruleName.equals("ParsingrimaryType")) {
//				System.out.println("DEBUG: " + po + "\n" + rule);
//			}
			loading.setRule(ruleName, rule);
			if(po.size() >= 3) {
				readAnnotations(rule, po.get(2));
			}
			return true;
		}
		if(po.is(PEG4dGrammar.ParsingImport)) {
			String filePath = searchPegFilePath(po.getSource(), po.textAt(0, ""));
			String ns = po.textAt(1, "");
			loading.importGrammar(ns, filePath);
			return true;
		}
		if(po.is(PEG4dGrammar.CommonError)) {
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
			if(p.is(PEG4dGrammar.ParsingAnnotation)) {
				String key = p.textAt(0, "");
				String value = p.textAt(1, "");
				rule.addAnotation(key, value);
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
		if(po.is(PEG4dGrammar.ParsingNonTerminal)) {
			String symbol = po.getText();
			if(ruleName.equals(symbol)) {
				PExpression e = loading.getExpression(ruleName);
				if(e != null) {
					// self-redefinition
					return e;  // FIXME
				}
			}
			if(symbol.length() > 0 && !symbol.endsWith("_") && !loading.hasRule(symbol) && Grammar.PEG4d.hasRule(symbol)) { // comment
				Main.printVerbose("implicit importing", symbol);
				loading.setRule(symbol, Grammar.PEG4d.getRule(symbol));
			}
			return new PNonTerminal(loading, 0, symbol);
		}
		if(po.is(PEG4dGrammar.String)) {
			String t = quote(po.getText());
			if(loading.hasRule(t)) {
				Main.printVerbose("direct inlining", t);
				return loading.getExpression(t);
			}
			return loading.newString(ParsingCharset.unquoteString(po.getText()));
		}
		if(po.is(PEG4dGrammar.ParsingString)) {
			return loading.newString(ParsingCharset.unquoteString(po.getText()));
		}
		if(po.is(PEG4dGrammar.ParsingCharacter)) {
			ParsingCharset u = null;
			if(po.size() > 0) {
				for(int i = 0; i < po.size(); i++) {
					ParsingObject o = po.get(i);
					if(o.is(PEG4dGrammar.List)) {
						u = ParsingCharset.addText(u, o.textAt(0, ""), o.textAt(1, ""));
					}
					if(o.is(PEG4dGrammar.ParsingCharacter)) {
						u = ParsingCharset.addText(u, o.getText(), o.getText());
					}
					//System.out.println("u=" + u + " by " + o);
				}
			}
			return loading.newCharacter(u);
		}
		if(po.is(PEG4dGrammar.ParsingByte)) {
			String t = po.getText();
			if(t.startsWith("U+")) {
				int c = ParsingCharset.hex(t.charAt(2));
				c = (c * 16) + ParsingCharset.hex(t.charAt(3));
				c = (c * 16) + ParsingCharset.hex(t.charAt(4));
				c = (c * 16) + ParsingCharset.hex(t.charAt(5));
				if(c < 128) {
					return loading.newByte(c, java.lang.String.valueOf((char)c));					
				}
				String t2 = java.lang.String.valueOf((char)c);
				return loading.newString(t2);
			}
			int c = ParsingCharset.hex(t.charAt(t.length()-2)) * 16 + ParsingCharset.hex(t.charAt(t.length()-1)); 
			return loading.newByte(c, t);
		}
		if(po.is(PEG4dGrammar.ParsingAny)) {
			return loading.newAny(po.getText());
		}
		if(po.is(PEG4dGrammar.ParsingChoice)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
			for(int i = 0; i < po.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, po.get(i));
				loading.addChoice(l, e);
			}
			return loading.newChoice(l);
		}
		if(po.is(PEG4dGrammar.ParsingSequence)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
			for(int i = 0; i < po.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, po.get(i));
				loading.addSequence(l, e);
			}
			return loading.newSequence(l);
		}
		if(po.is(PEG4dGrammar.ParsingNot)) {
			return loading.newNot(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingAnd)) {
			return loading.newAnd(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingOneMore)) {
			return loading.newOneMore(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingZeroMore)) {
			return loading.newZeroMore(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingOptional)) {
			return loading.newOptional(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.PTimes)) {
			int n = ParsingCharset.parseInt(po.textAt(0, ""), 1);
			PExpression e = toParsingExpression(loading, ruleName, po.get(0));
			UList<PExpression> l = new UList<PExpression>(new PExpression[n]);
			for(int i = 0; i < n; i++) {
				loading.addSequence(l, e);
			}
			return loading.newSequence(l);
		}
		if(po.is(PEG4dGrammar.ParsingTagging)) {
			return loading.newTagging(po.getText());
		}
		if(po.is(PEG4dGrammar.ParsingValue)) {
			return loading.newMessage(po.getText());
		}
		if(po.is(PEG4dGrammar.ParsingLeftJoin)) {
			PExpression seq = toParsingExpression(loading, ruleName, po.get(0));
			return loading.newJoinConstructor(ruleName, seq);
		}
		if(po.is(PEG4dGrammar.ParsingConstructor)) {
			PExpression seq = toParsingExpression(loading, ruleName, po.get(0));
			return loading.newConstructor(ruleName, seq);
		}
		if(po.is(PEG4dGrammar.ParsingConnector)) {
			int index = -1;
			if(po.size() == 2) {
				index = ParsingCharset.parseInt(po.textAt(1, ""), -1);
			}
			return loading.newConnector(toParsingExpression(loading, ruleName, po.get(0)), index);
		}
		if(po.is(PEG4dGrammar.ParsingMatch)) {
			return loading.newMatch(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingEnableFlag)) {
			return loading.newEnableFlag(po.textAt(0, ""), toParsingExpression(loading, ruleName, po.get(1)));
		}
		if(po.is(PEG4dGrammar.ParsingDisableFlag)) {
			return loading.newDisableFlag(po.textAt(0, ""), toParsingExpression(loading, ruleName, po.get(1)));
		}
		if(po.is(PEG4dGrammar.ParsingFlag)) {
			return loading.newFlag(po.getText());
		}
		if(po.is(PEG4dGrammar.ParsingIndent)) {
			if(po.size() == 0) {
				return loading.newIndent(null);
			}
			return loading.newIndent(toParsingExpression(loading, ruleName, po.get(0)));
		}
		
		if(po.is(PEG4dGrammar.ParsingDebug)) {
			return loading.newDebug(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingFail)) {
			return loading.newFail(ParsingCharset.unquoteString(po.textAt(0, "")));
		}
		if(po.is(PEG4dGrammar.ParsingCatch)) {
			return loading.newCatch();
		}
//		if(po.is(PEG4dGrammar.ParsingApply)) {
//			return loading.newApply(toParsingExpression(loading, ruleName, po.get(0)));
//		}
//		if(po.is(PEG4dGrammar.ParsingStringfy)) {
//			return loading.newStringfy();
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
	
//	@Override
//	public ParsingStream newParserContext(ParsingSource source) {
//		return new TracingPackratParser(this, source, 0);  // best parser
//	}

	// Definiton of PEG4d 	
	private final PExpression t(String token) {
		return this.newString(token);
	}
	private final PExpression c(String text) {
		return new PCharacter(this, 0, ParsingCharset.newParsingCharset(text));
	}
	private final PExpression P(String ruleName) {
		return new PNonTerminal(this, 0, ruleName);
	}
	private final PExpression Optional(PExpression e) {
		return new POptional(this, 0, e);
	}
	private final PExpression zero(PExpression e) {
		return new PRepetition(this, 0, e, 0);
	}
	private final PExpression zero(PExpression ... elist) {
		return new PRepetition(this, 0, Sequence(elist), 0);
	}
	private final PExpression one(PExpression e) {
		return new PRepetition(this, 0, e, 1);
	}
	private final PExpression one(PExpression ... elist) {
		return new PRepetition(this, 0, Sequence(elist), 1);
	}
	private final PExpression Sequence(PExpression ... elist) {
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
	private final PExpression Tag(int tagId) {
		return newTagging(ParsingTag.tagName(tagId));
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
	private PExpression Link(PExpression e) {
		return new PConnector(this, 0, e, -1);
	}
	
	private PExpression Link(int index, PExpression e) {
		return new PConnector(this, 0, e, index);
	}
	
	public Grammar loadPEG4dGrammar() {
		PExpression Any = newAny(".");
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
		this.setRule("NAME", Sequence(_LETTER, zero(_W)));
		this.setRule("HEX", c("0-9A-Fa-f"));

		PExpression _INTEGER = one(_DIGIT);
		this.setRule("COMMENT", 
			Choice(
				Sequence(t("/*"), zero(Not(t("*/")), Any), t("*/")),
				Sequence(t("//"), zero(Not(_NEWLINE), Any), _NEWLINE)
			)
		);
		this.setRule("_", zero(Choice(one(_S), P("COMMENT"))));
		PExpression Spacing = P("_");
		
		this.setRule("Name",       Constructor(_LETTER, zero(_W), Tag(Name)));
		this.setRule("DotName",    Constructor(_LETTER, zero(c("A-Za-z0-9_.")), Tag(Name)));
		this.setRule("HyphenName_", Constructor(_LETTER, zero(Choice(_W, t("-"))), Tag(Name)));
		this.setRule("Integer",    Constructor(_INTEGER, Tag(Integer)));
		
		this.setRule("NonTerminal_", 
			Constructor(
				_LETTER, 
				zero(c("A-Za-z0-9_:")), 
				Tag(ParsingNonTerminal)
			)
		);
		PExpression StringContent  = zero(Choice(
			t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any)
		));
		PExpression StringContent2 = zero(Choice(
			t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any)
		));
		this.setRule("String", 
			Sequence(t("\""), Constructor(StringContent2, Tag(String)), t("\""))
		);
		this.setRule("SingleQuotedString", 
			Sequence(t("'"),  Constructor(StringContent, Tag(ParsingString)), t("'"))
		);
		PExpression ValueContent = zero(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), Any)
		));
		PExpression _Message = Sequence(t("`"), Constructor(ValueContent, Tag(ParsingValue)), t("`"));
		PExpression _Char2 = Choice( 
			Sequence(t("\\u"), _HEX, _HEX, _HEX, _HEX),
			Sequence(t("\\x"), _HEX, _HEX),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), Any)
		);
		PExpression _CharChunk = Sequence(
			Constructor (_Char2, Tag(ParsingCharacter)), 
			Optional(
				LeftJoin(t("-"), Link(Constructor(_Char2, Tag(ParsingCharacter))), Tag(List))
			)
		);
		this.setRule("Charcter_", Sequence(t("["), Constructor(zero(Link(_CharChunk)), Tag(ParsingCharacter)), t("]")));

		PExpression _Any = Constructor(t("."), Tag(ParsingAny));
		PExpression _Tagging = Sequence(t("#"), Constructor(one(c("A-Za-z0-9_."), Tag(ParsingTagging))));
		PExpression _Byte = Constructor(t("0x"), _HEX, _HEX, Tag(ParsingByte));
		PExpression _Unicode = Constructor(t("U+"), _HEX, _HEX, _HEX, _HEX, Tag(ParsingByte));
		PExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		PExpression Connector  = Choice(t("@"), t("^"));
		PExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));

		this.setRule("Constructor_", Constructor(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, _S, Tag(ParsingLeftJoin)), 
				Tag(ParsingConstructor)
			), 
			Spacing, 
			Link(P("Expr_")), 
			Spacing,
			ConstructorEnd
		));
		setRule(
			"Flag_", 
			Sequence(t("."), 
				Constructor(one(_W), Tag(ParsingFlag)) 
			)
		);
		PExpression _Pipe = Optional(t("|"));
		setRule("Func_", 
			Sequence(t("<"), Constructor(
				Choice(
					Sequence(t("debug"),   _S, Link(P("Expr_")), Tag(ParsingDebug)),
					Sequence(t("memo"),   _S, Link(P("Expr_")), Spacing, t(">"), Tag(ParsingMemo)),
					Sequence(t("match"),   _S, Link(P("Expr_")), Spacing, t(">"), Tag(ParsingMatch)),
					Sequence(t("fail"),   _S, Link(P("SingleQuotedString")), Spacing, t(">"), Tag(ParsingFail)),
					Sequence(t("catch"), Tag(ParsingCatch)),
					Sequence(t("enable"),  _S, Link(P("Flag_")), _S, Link(P("Expr_")), Tag(ParsingEnableFlag)),
					Sequence(t("disable"), _S, Link(P("Flag_")), _S, Link(P("Expr_")), Tag(ParsingDisableFlag)),
					Sequence(t("indent"), Optional(Sequence(_S, Link(P("Expr_")))), Tag(ParsingIndent)),
					Sequence(t("choice"), Tag(ParsingChoice)),
					Sequence(_Pipe, t("append-choice"), Tag(ParsingChoice)),
					Sequence(_Pipe, t("stringfy"), Tag(ParsingStringfy)),
					Sequence(_Pipe, t("apply"), _S, Link(P("Expr_")), Tag(ParsingApply))
				)
			), Spacing, t(">"))
		);
		
		setRule("Term_", 
			Choice(
				P("SingleQuotedString"), P("Charcter_"), P("Func_"), P("Flag_"), 
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
						Sequence(t("*"), Tag(ParsingZeroMore)), 
						Sequence(t("+"), Tag(ParsingOneMore)), 
						Sequence(t("?"), Tag(ParsingOptional)),
						Sequence(Connector, Optional(Link(1, P("Integer"))), Tag(ParsingConnector))
					)
				)
			)
		));
		
		this.setRule("Predicate_", Choice(
				Constructor(
						Choice(
								Sequence(t("&"), Tag(ParsingAnd)),
								Sequence(t("!"), Tag(ParsingNot)),
								Sequence(t("@["), Spacing, Link(1, P("Integer")), Spacing, t("]"), Tag(ParsingConnector)),							
								Sequence(t("@"), Tag(ParsingConnector))
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
						one(
							Spacing, 
							_NotRule,
							Link(P("Predicate_"))
						),
						Tag(ParsingSequence) 
					)
				)
		));
		this.setRule("Expr_", 
			Sequence(
				P("Sequence_"), 
				Optional(
					LeftJoin(
						one(
							Spacing, t("/"), Spacing, 
							Link(P("Sequence_"))
						),
						Tag(ParsingChoice) 
					)
				)
			)
		);
		this.setRule("Param_",
			Sequence(
				t("["),
				Constructor(
					Link(P("Name")),
					zero(
						Spacing,
						Link(P("Name"))
					),
					Tag(List) 
				),
				t("]")
			)
		);
		this.setRule("DOC_", Sequence(
			zero(Not(t("]")), Not(t("[")), Any),
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
							Tag(Text) 
						)
					),
					Tag(ParsingAnnotation) 
				),
				t("]"),
				Spacing
			)
		);
		this.setRule("Annotations_",
				Constructor(
					one(Link(P("Annotation_"))),
					Tag(List) 
				)
		);
		this.setRule("Rule_", 
			Constructor(
				Link(0, Choice(P("Name"), P("String"))), Spacing, 
				Optional(Sequence(Link(3, P("Param_")), Spacing)),
				Optional(Sequence(Link(2, P("Annotations_")), Spacing)),
				t("="), Spacing, 
				Link(1, P("Expr_")),
				Tag(ParsingRule) 
			)
		);
		this.setRule("Import_", Constructor(
			t("import"), 
			Tag(ParsingImport), 
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

}



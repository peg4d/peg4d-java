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
		this.ruleMap.put(ruleName, new PegRule(this, null, 0, ruleName, e));
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
	
	private UMap<PExpression> nonTerminalMap = new UMap<PExpression>();
	
	final PExpression newNonTerminal(String text) {
		PExpression e = nonTerminalMap.get(text);
		if(e == null) {
			e = new PNonTerminal(this, 0, text);
			e.uniqueId = PExpression.newExpressionId();
			nonTerminalMap.put(text, e);
		}
		return e;
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
	ParsingSource source;
	long     pos;
	Grammar  peg;
	
	String ruleName;
	PExpression expr;
	int length = 0;
	boolean objectType;

	public PegRule(Grammar peg, ParsingSource source, long pos, String ruleName, PExpression e) {
		this.peg = peg;
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
		return this.peg;
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
	static final int ParsingMemo        = ParsingTag.tagId("ParsingMemo");
	static final int ParsingDebug       = ParsingTag.tagId("ParsingDebug");
	static final int ParsingFail        = ParsingTag.tagId("ParsingFail");
	static final int ParsingCatch       = ParsingTag.tagId("ParsingCatch");

	static final int ParsingIfFlag        = ParsingTag.tagId("ParsingIfFlag");
	static final int ParsingWithFlag      = ParsingTag.tagId("ParsingWithFlag");
	static final int ParsingWithoutFlag   = ParsingTag.tagId("ParsingWithoutFlag");
	static final int ParsingIndent        = ParsingTag.tagId("ParsingIndent");

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
			PegRule rule = new PegRule(loading, po.getSource(), po.getSourcePosition(), ruleName, e);
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
			return loading.newNonTerminal(symbol);
		}
		if(po.is(PEG4dGrammar.String)) {
			String t = quote(po.getText());
			if(loading.hasRule(t)) {
				Main.printVerbose("direct inlining", t);
				return loading.getExpression(t);
			}
			return PExpression.newString(ParsingCharset.unquoteString(po.getText()));
		}
		if(po.is(PEG4dGrammar.ParsingString)) {
			return PExpression.newString(ParsingCharset.unquoteString(po.getText()));
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
			return PExpression.newCharacter(u);
		}
		if(po.is(PEG4dGrammar.ParsingByte)) {
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
		if(po.is(PEG4dGrammar.ParsingAny)) {
			return PExpression.newAny(po.getText());
		}
		if(po.is(PEG4dGrammar.ParsingChoice)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
			for(int i = 0; i < po.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, po.get(i));
				PExpression.addChoice(l, e);
			}
			return PExpression.newChoice(l);
		}
		if(po.is(PEG4dGrammar.ParsingSequence)) {
			UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
			for(int i = 0; i < po.size(); i++) {
				PExpression e = toParsingExpression(loading, ruleName, po.get(i));
				PExpression.addSequence(l, e);
			}
			return PExpression.newSequence(l);
		}
		if(po.is(PEG4dGrammar.ParsingNot)) {
			return PExpression.newNot(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingAnd)) {
			return PExpression.newAnd(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingOneMore)) {
			return PExpression.newOneMore(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingZeroMore)) {
			if(po.size() == 2) {
				int ntimes = ParsingCharset.parseInt(po.textAt(1, ""), -1);
				return PExpression.newTimes(ntimes, toParsingExpression(loading, ruleName, po.get(0)));
			}
			return PExpression.newRepetition(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingOptional)) {
			return PExpression.newOptional(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.PTimes)) {
			int n = ParsingCharset.parseInt(po.textAt(0, ""), 1);
			PExpression e = toParsingExpression(loading, ruleName, po.get(0));
			UList<PExpression> l = new UList<PExpression>(new PExpression[n]);
			for(int i = 0; i < n; i++) {
				PExpression.addSequence(l, e);
			}
			return PExpression.newSequence(l);
		}
		if(po.is(PEG4dGrammar.ParsingTagging)) {
			return PExpression.newTagging(loading.newTag(po.getText()));
		}
		if(po.is(PEG4dGrammar.ParsingValue)) {
			return PExpression.newMessage(po.getText());
		}
		if(po.is(PEG4dGrammar.ParsingLeftJoin)) {
			PExpression seq = toParsingExpression(loading, ruleName, po.get(0));
			return PExpression.newJoinConstructor(seq);
		}
		if(po.is(PEG4dGrammar.ParsingConstructor)) {
			PExpression seq = toParsingExpression(loading, ruleName, po.get(0));
			return PExpression.newConstructor(seq);
		}
		if(po.is(PEG4dGrammar.ParsingConnector)) {
			int index = -1;
			if(po.size() == 2) {
				index = ParsingCharset.parseInt(po.textAt(1, ""), -1);
			}
			return PExpression.newConnector(toParsingExpression(loading, ruleName, po.get(0)), index);
		}
		if(po.is(PEG4dGrammar.ParsingMatch)) {
			return PExpression.newMatch(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingWithFlag)) {
			return PExpression.newEnableFlag(po.textAt(0, ""), toParsingExpression(loading, ruleName, po.get(1)));
		}
		if(po.is(PEG4dGrammar.ParsingWithoutFlag)) {
			return PExpression.newDisableFlag(po.textAt(0, ""), toParsingExpression(loading, ruleName, po.get(1)));
		}
		if(po.is(PEG4dGrammar.ParsingIfFlag)) {
			return PExpression.newFlag(po.textAt(0, ""));
		}
		if(po.is(PEG4dGrammar.ParsingIndent)) {
			if(po.size() == 0) {
				return PExpression.newIndent(null);
			}
			return PExpression.newIndent(toParsingExpression(loading, ruleName, po.get(0)));
		}
		
		if(po.is(PEG4dGrammar.ParsingDebug)) {
			return PExpression.newDebug(toParsingExpression(loading, ruleName, po.get(0)));
		}
		if(po.is(PEG4dGrammar.ParsingFail)) {
			return PExpression.newFail(ParsingCharset.unquoteString(po.textAt(0, "")));
		}
		if(po.is(PEG4dGrammar.ParsingCatch)) {
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
	
//	@Override
//	public ParsingStream newParserContext(ParsingSource source) {
//		return new TracingPackratParser(this, source, 0);  // best parser
//	}

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
		
		this.setRule("Name",       Constructor(_LETTER, ZeroMore(_W), Tag(Name)));
		this.setRule("DotName",    Constructor(_LETTER, ZeroMore(c("A-Za-z0-9_.")), Tag(Name)));
		this.setRule("HyphenName_", Constructor(_LETTER, ZeroMore(Choice(_W, t("-"))), Tag(Name)));
		this.setRule("Integer",    Constructor(_INTEGER, Tag(Integer)));
		
		this.setRule("NonTerminal_", 
			Constructor(
				_LETTER, 
				ZeroMore(c("A-Za-z0-9_:")), 
				Tag(ParsingNonTerminal)
			)
		);
		PExpression StringContent  = ZeroMore(Choice(
			t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any)
		));
		PExpression StringContent2 = ZeroMore(Choice(
			t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any)
		));
		this.setRule("String", 
			Sequence(t("\""), Constructor(StringContent2, Tag(String)), t("\""))
		);
		this.setRule("SingleQuotedString", 
			Sequence(t("'"),  Constructor(StringContent, Tag(ParsingString)), t("'"))
		);
		PExpression ValueContent = ZeroMore(Choice(
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
		this.setRule("Charcter_", Sequence(t("["), Constructor(ZeroMore(Link(_CharChunk)), Tag(ParsingCharacter)), t("]")));

		PExpression _Any = Constructor(t("."), Tag(ParsingAny));
		PExpression _Tagging = Sequence(t("#"), Constructor(OneMore(c("A-Za-z0-9_."), Tag(ParsingTagging))));
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
		PExpression _Pipe = Optional(t("|"));
		setRule("Func_", 
			Sequence(t("<"), Constructor(
				Choice(
					Sequence(t("debug"),   _S, Link(P("Expr_")), Tag(ParsingDebug)),
					Sequence(t("memo"),   _S, Link(P("Expr_")), Spacing, t(">"), Tag(ParsingMemo)),
					Sequence(t("match"),   _S, Link(P("Expr_")), Spacing, t(">"), Tag(ParsingMatch)),
					Sequence(t("fail"),   _S, Link(P("SingleQuotedString")), Spacing, t(">"), Tag(ParsingFail)),
					Sequence(t("catch"), Tag(ParsingCatch)),
					Sequence(t("if"), _S, Optional(t("!")), Link(P("Name")), Tag(ParsingIfFlag)),
					Sequence(t("with"),  _S, Link(P("Name")), _S, Link(P("Expr_")), Tag(ParsingWithFlag)),
					Sequence(t("without"), _S, Link(P("Name")), _S, Link(P("Expr_")), Tag(ParsingWithoutFlag)),
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
						Sequence(t("*"), Optional(Link(1, P("Integer"))), Tag(ParsingZeroMore)), 
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
						OneMore(
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
						OneMore(
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
					ZeroMore(
						Spacing,
						Link(P("Name"))
					),
					Tag(List) 
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
					OneMore(Link(P("Annotation_"))),
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



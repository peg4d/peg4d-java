package org.peg4d;

import org.peg4d.model.ParsingModel;

public class Grammar {
	
	GrammarFactory      factory;
	String              name;
	UList<String>       nameList;
	UMap<ParsingRule>    ruleMap;

	UList<ParsingRule>   exportedRuleList;
	UMap<String>         objectLabelMap = null;
	public boolean       foundError = false;

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
		this.ruleMap  = new UMap<ParsingRule>();
		this.nameList  = new UList<String>(new String[16]);
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
	
	final PNonTerminal newNonTerminal(String text) {
		return new PNonTerminal(this, 0, text);
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
		}
		this.verifyRules();
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
			UList<ParsingRule> ruleList = peg.getExportRuleList();
			//System.out.println("filePath: " + filePath + " peg=" + ruleList);
			for(int i = 0; i < ruleList.size(); i++) {
				ParsingRule rule = ruleList.ArrayValues[i];
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

	public final ParsingRule getRule(String ruleName) {
		return this.ruleMap.get(ruleName);
	}

	public final PExpression getExpression(String ruleName) {
		ParsingRule rule = this.getRule(ruleName);
		if(rule != null) {
			return rule.expr;
		}
		return null;
	}

//	public final void setRule(String ruleName, PExpression e) {
//		this.ruleMap.put(ruleName, new PegRule(this, null, 0, ruleName, e));
//	}

	public final void setRule(String ruleName, ParsingRule rule) {
		PExpression.makeFlow(rule.expr, null);
		if(!this.hasRule(ruleName)) {
			this.nameList.add(ruleName);
		}
		this.ruleMap.put(ruleName, rule);
	}
	
	public final UList<ParsingRule> getRuleList() {
		UList<ParsingRule> ruleList = new UList<ParsingRule>(new ParsingRule[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			ruleList.add(getRule(nameList.ArrayValues[i]));
		}
		return ruleList;
	}

	public final UList<PExpression> getExpressionList() {
		UList<PExpression> pegList = new UList<PExpression>(new PExpression[nameList.size()]);
		for(int i = 0; i < nameList.size(); i++) {
			String ruleName = nameList.ArrayValues[i];
			pegList.add(this.getRule(ruleName).expr);
		}
		return pegList;
	}

	public final void verifyRules() {
		this.foundError = false;
		this.exportedRuleList = null;
		this.getExportRuleList();
		UList<String> stack = new UList<String>(new String[64]);
		for(int i = 0; i < nameList.size(); i++) {
			ParsingRule rule = this.getRule(nameList.ArrayValues[i]);
			String uName = rule.getUniqueName();
			stack.clear(0);
			stack.add(uName);
			rule.length = PExpression.checkLeftRecursion(rule.expr, uName, stack, 0);
			if(rule.type == null) {
				rule.type = new ParsingType();
				rule.type = PExpression.typeCheck(rule.expr, stack, rule.type, null);
			}
			System.out.println("@Verify " + rule);
		}
		
		new Inliner(this).performInlining();
		new Optimizer(this).optimize();
		
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		this.memoRemover = new MemoRemover(this);
		ParsingContext context = new ParsingContext(null);
		for(int i = 0; i < nameList.size(); i++) {
			ParsingRule rule = this.getRule(nameList.ArrayValues[i]);
			if(rule.getGrammar() == this) {
				rule.testExample1(this, context);
			}
		}
	}

	final UList<ParsingRule> getExportRuleList() {
		if(this.exportedRuleList == null) {
			UList<ParsingRule> l = new UList<ParsingRule>(new ParsingRule[4]);
			PExpression e = this.getExpression("export");
			if(e != null) {
				appendExportRuleList(l, e.getExpression());
			}
			this.exportedRuleList = l;
		}
		return this.exportedRuleList;
	}

	private void appendExportRuleList(UList<ParsingRule> l, PExpression e) {
		if(e instanceof PNonTerminal) {
			ParsingRule rule = this.getRule(((PNonTerminal) e).symbol);
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
		UList<ParsingRule> list = this.getRuleList();
		for(int i = 0; i < list.size(); i++) {
			ParsingRule r = list.ArrayValues[i];
			fmt.formatRule(r.ruleName, r.expr, sb);
		}
		fmt.formatFooter(sb);
		System.out.println(sb.toString());
	}
	
	public final void simpleFormatAll(SimpleGrammarFormatter fmt) {
		StringBuilder sb = new StringBuilder();
		fmt.formatHeader(sb);
		UList<ParsingRule> list = this.getRuleList();
		for(int i = 0; i < list.size(); i++) {
			ParsingRule r = list.ArrayValues[i];
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

class ParsingRule {
	Grammar  peg;
	String ruleName;

	ParsingObject po;
	ParsingType type;
	PExpression expr;
	
	int length = 0;

	ParsingRule(Grammar peg, String ruleName, ParsingObject po, PExpression e) {
		this.peg = peg;
		this.po = po;
		this.ruleName = ruleName;
		this.expr = e;
	}
	
	final String getUniqueName() {
		return this.peg.uniqueRuleName(ruleName);
	}
	
	@Override
	public String toString() {
		return type + " " + this.ruleName + "[" + this.length + "]" + "=" + this.expr;
	}
	
	final void report(ReportLevel level, String msg) {
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
		}
		else {
			System.out.println("" + level.toString() + ": " + msg);
		}
	}
	
	Grammar getGrammar() {
		return this.peg;
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
	
	boolean isObjectType() {
		return this.type.isObjectType();
	}
}

class PEG4dGrammar extends Grammar {
	
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
//	private final PExpression OneMore(PExpression e) {
//		return PExpression.newOneMore(e);
//	}
//	private final PExpression OneMore(PExpression ... elist) {
//		return PExpression.newOneMore(Sequence(elist));
//	}
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

	private PExpression Any() {
		return PExpression.newAny(".");
	}

	public Grammar loadPEG4dGrammar() {
//		PExpression Any = PExpression.newAny(".");
		this.setRule("EOL", c("\\r\\n"));
		this.setRule("EOT", Not(Any()));
		this.setRule("DIGIT", c("0-9"));
		this.setRule("S", Choice(c(" \\t\\r\\n"), t("\u3000")));
		this.setRule("LETTER", c("A-Za-z_"));		
		this.setRule("W", c("A-Za-z0-9_"));
		this.setRule("NAME", Sequence(P("LETTER"), ZeroMore(P("W"))));
		this.setRule("HEX", c("0-9A-Fa-f"));

		this.setRule("INT", Sequence(P("DIGIT"), ZeroMore(P("DIGIT"))));
		this.setRule("COMMENT", 
			Choice(
				Sequence(t("/*"), ZeroMore(Not(t("*/")), Any()), t("*/")),
				Sequence(t("//"), ZeroMore(Not(P("EOL")), Any()), P("EOL"))
			)
		);
		this.setRule("_", ZeroMore(Choice(P("S"), P("COMMENT"))));
		
		this.setRule("Name",       Constructor(P("LETTER"), ZeroMore(P("W")), Tag(ParsingTag.Name)));
		this.setRule("DotName",    Constructor(P("LETTER"), ZeroMore(c("A-Za-z0-9_.")), Tag(ParsingTag.Name)));
		this.setRule("HyphenName_", Constructor(P("LETTER"), ZeroMore(Choice(P("W"), t("-"))), Tag(ParsingTag.Name)));
		this.setRule("Integer",    Constructor(P("INT"), Tag(ParsingTag.Integer)));
		
		this.setRule("NonTerminal_", 
			Constructor(
				P("LETTER"), 
				ZeroMore(c("A-Za-z0-9_:")), 
				Tag(PEG4d.NonTerminal)
			)
		);
		{
			PExpression StringContent  = ZeroMore(Choice(
				t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any())
			));
			PExpression StringContent2 = ZeroMore(Choice(
				t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any())
			));
			this.setRule("String", 
				Sequence(t("\""), Constructor(StringContent2, Tag(ParsingTag.String)), t("\""))
			);
			this.setRule("SingleQuotedString", 
				Sequence(t("'"),  Constructor(StringContent, Tag(PEG4d.CharacterSequence)), t("'"))
			);
		}
		PExpression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), Any())
		));
		PExpression _Message = Sequence(t("`"), Constructor(ValueContent, Tag(PEG4d.Value)), t("`"));
		this.setRule("CHARCONTET_", Choice( 
			Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")),
			Sequence(t("\\x"), P("HEX"), P("HEX")),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), Any())
		));
		PExpression _CharChunk = Sequence(
			Constructor (P("CHARCONTET_"), Tag(PEG4d.Character)), 
			Optional(
				LeftJoin(t("-"), Link(Constructor(P("CHARCONTET_"), Tag(PEG4d.Character))), Tag(ParsingTag.List))
			)
		);
		this.setRule("Charcter_", Sequence(t("["), Constructor(ZeroMore(Link(_CharChunk)), Tag(PEG4d.Character)), t("]")));

		PExpression _Any = Constructor(t("."), Tag(PEG4d.Any));
		PExpression _Tagging = Sequence(t("#"), Constructor(c("A-Za-z0-9"), ZeroMore(c("A-Za-z0-9_.")), Tag(PEG4d.Tagging)));
		PExpression _Byte = Constructor(t("0x"), P("HEX"), P("HEX"), Tag(PEG4d.Byte));
		PExpression _Unicode = Constructor(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag(PEG4d.Byte));
		PExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		PExpression Connector  = Choice(t("@"), t("^"));
		PExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));

		this.setRule("Constructor_", Constructor(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, P("S"), Tag(PEG4d.LeftJoin)), 
				Tag(PEG4d.Constructor)
			), 
			P("_"), 
			Link(P("Expr_")), 
			P("_"),
			ConstructorEnd
		));
		setRule("PIPE_", Optional(t("|")));
		setRule("Func_", 
			Sequence(t("<"), Constructor(
				Choice(
					Sequence(t("debug"),   P("S"), Link(P("Expr_")), Tag(PEG4d.Debug)),
					Sequence(t("memo"),   P("S"), Link(P("Expr_")), P("_"), t(">"), Tag(PEG4d.Memo)),
					Sequence(t("match"),   P("S"), Link(P("Expr_")), P("_"), t(">"), Tag(PEG4d.Match)),
					Sequence(t("fail"),   P("S"), Link(P("SingleQuotedString")), P("_"), t(">"), Tag(PEG4d.Fail)),
					Sequence(t("catch"), Tag(PEG4d.Catch)),
					Sequence(t("if"), P("S"), Optional(t("!")), Link(P("Name")), Tag(PEG4d.If)),
					Sequence(t("with"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr_")), Tag(PEG4d.With)),
					Sequence(t("without"), P("S"), Link(P("Name")), P("S"), Link(P("Expr_")), Tag(PEG4d.Without)),
					Sequence(t("indent"), Optional(Sequence(P("S"), Link(P("Expr_")))), Tag(PEG4d.Indent)),
					Sequence(t("choice"), Tag(PEG4d.Choice)),
					Sequence(P("PIPE_"), t("append-choice"), Tag(PEG4d.Choice)),
					Sequence(P("PIPE_"), t("stringfy"), Tag(PEG4d.Stringfy)),
					Sequence(P("PIPE_"), t("apply"), P("S"), Link(P("Expr_")), Tag(PEG4d.Apply))
				)
			), P("_"), t(">"))
		);
		
		setRule("Term_", 
			Choice(
				P("SingleQuotedString"), P("Charcter_"), P("Func_"),  
				_Any, _Message, _Tagging, _Byte, _Unicode,
				Sequence(t("("), P("_"), P("Expr_"), P("_"), t(")")),
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
								Sequence(t("@["), P("_"), Link(1, P("Integer")), P("_"), t("]"), Tag(PEG4d.Connector)),							
								Sequence(t("@"), Tag(PEG4d.Connector))
						), 
						Link(0, P("SuffixTerm_"))
				), 
				P("SuffixTerm_")
		));
		this.setRule("NotRule_", Not(Choice(P("Rule_"), P("Import_"))));
		this.setRule("Sequence_", Sequence(
				P("Predicate_"), 
				Optional(
					LeftJoin(
						P("_"), 
						P("NotRule_"),
						Link(P("Predicate_")),
						ZeroMore(
							P("_"), 
							P("NotRule_"),
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
						P("_"), t("/"), P("_"), 
						Link(P("Sequence_")), 
						ZeroMore(
							P("_"), t("/"), P("_"), 
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
						P("_"),
						Link(P("Name"))
					),
					Tag(ParsingTag.List) 
				),
				t("]")
			)
		);
		this.setRule("DOC_", Sequence(
			ZeroMore(Not(t("]")), Not(t("[")), Any()),
			Optional(Sequence(t("["), P("DOC_"), t("]"), P("DOC_") ))
		));
		
		this.setRule("Annotation_",
			Sequence(
				t("["),
				Constructor(
					Link(P("HyphenName_")),
					t(":"), 
					P("_"), 
					Link(
						Constructor(
							P("DOC_"),
							Tag(ParsingTag.Text) 
						)
					),
					Tag(PEG4d.Annotation) 
				),
				t("]"),
				P("_")
			)
		);
		this.setRule("Annotations_",
				Constructor(
					Link(P("Annotation_")),
					ZeroMore(Link(P("Annotation_"))),
					Tag(ParsingTag.List) 
				)
		);
		this.setRule("Rule_", 
			Constructor(
				Link(0, Choice(P("Name"), P("String"))), P("_"), 
				Optional(Sequence(Link(3, P("Param_")), P("_"))),
				Optional(Sequence(Link(2, P("Annotations_")), P("_"))),
				t("="), P("_"), 
				Link(1, P("Expr_")),
				Tag(PEG4d.Rule) 
			)
		);
		this.setRule("Import_", Constructor(
			t("import"), 
			Tag(PEG4d.Import), 
			P("S"), 
			Choice(Link(P("SingleQuotedString")), P("DotName")), 
			Optional(
				Sequence(P("S"), t("as"), P("S"), Link(P("Name")))
			)
		));
		this.setRule("Chunk", Sequence(
			P("_"), 
			Choice(
				P("Rule_"), 
				P("Import_")
			), 
			P("_"), 
			Optional(Sequence(t(";"), P("_")))
		));
		this.setRule("File", Sequence(
			P("_"), 
			Choice(
				P("Rule_"), 
				P("Import_")
			), 
			P("_"), 
			Optional(Sequence(t(";"), P("_"))) 
		));
		this.verifyRules(/*null*/);
		return this;
	}
	
	private void setRule(String ruleName, PExpression e) {
		ParsingRule rule = new ParsingRule(this, ruleName, null, null);
		rule.expr = e;
		this.setRule(ruleName, rule);
	}

}



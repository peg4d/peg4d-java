package org.peg4d;

import java.util.TreeMap;

import org.peg4d.expression.*;
import org.peg4d.model.ParsingModel;

public class Grammar {
	
	GrammarFactory      factory;
	String              name;
	UList<String>       nameList;
	UMap<ParsingRule>    ruleMap;

	UList<ParsingRule>   exportedRuleList;
	UMap<String>         objectLabelMap = null;
	public boolean       foundError = false;

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
	}

	public String getName() {
		return this.name;
	}
	
	public String uniqueRuleName(String ruleName) {
		return this.name + ":" + ruleName;
	}
	
	public final NonTerminal newNonTerminal(String symbol) {
		return new NonTerminal(this, symbol);
	}
	
	final boolean loadGrammarFile(String fileName) {
		PEG4dGrammar peg4d = GrammarFactory.Grammar;
		ParsingSource s = Main.loadSource(peg4d, fileName);
		ParsingContext context = new ParsingContext(s); //peg4d.newParserContext();
		this.name = fileName;
		if(fileName.indexOf('/') > 0) {
			this.name = fileName.substring(fileName.lastIndexOf('/')+1);
		}
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
		
	public final ParsingExpression getDefinedExpression(long oid) {
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
	
	public int getRuleSize() {
		return this.ruleMap.size();
	}

	public final boolean hasRule(String ruleName) {
		return this.ruleMap.get(ruleName) != null;
	}

	public final ParsingRule getRule(String ruleName) {
		return this.ruleMap.get(ruleName);
	}

	public final ParsingRule getLexicalRule(String ruleName) {
		ParsingRule r = this.getRule(ruleName);
		if(ParsingRule.isLexicalName(ruleName) || r == null) {
			return r;
		}
		String lexName = ParsingRule.toOptionName(r, true, null);
		makeOptionRule(r, lexName, true, null);
		return this.getRule(lexName);
	}

	public void makeOptionRule(ParsingRule r, String optName, boolean lexOnly, TreeMap<String, String> withoutMap) {
		ParsingRule r2 = this.getRule(optName);
		if(r2 == null) {
			r2 = new ParsingRule(this, optName, null, null);
			this.setRule(optName, r2);
			r2.type = lexOnly ? ParsingRule.LexicalRule : r.type;
			r2.baseName = r.baseName;  // important
			r2.minlen = r.minlen;
			r2.refc = r.refc;
			r2.expr = r.expr.normalizeImpl(lexOnly, withoutMap).uniquefy();
			Main.printVerbose("producing lexical rule", r2);
		}
	}
	
	public final ParsingExpression getExpression(String ruleName) {
		ParsingRule rule = this.getRule(ruleName);
		if(rule != null) {
			return rule.expr;
		}
		return null;
	}

//	public final void setRule(String ruleName, ParsingExpression e) {
//		this.ruleMap.put(ruleName, new PegRule(this, null, 0, ruleName, e));
//	}

	public final void setRule(String ruleName, ParsingRule rule) {
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

	public final UList<ParsingExpression> getExpressionList() {
		UList<ParsingExpression> pegList = new UList<ParsingExpression>(new ParsingExpression[nameList.size()]);
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
		UMap<String> flagMap = new UMap<String>();
		for(int i = 0; i < nameList.size(); i++) {
			ParsingRule rule = this.getRule(nameList.ArrayValues[i]);
			String uName = rule.getUniqueName();
			stack.clear(0);
			stack.add(uName);
			rule.minlen = ParsingExpression.checkLeftRecursion(rule.expr, uName, 0, 0, stack);
		}
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		for(int i = 0; i < nameList.size(); i++) {
			ParsingRule rule = this.getRule(nameList.ArrayValues[i]);
			ParsingExpression.typeCheck(rule, flagMap);
			rule.expr = rule.expr.uniquefy();
			//ParsingExpression.dumpId(rule.ruleName+ " ", rule.expr);
		}
		int size = nameList.size();
		TreeMap<String,String> withoutMap = new TreeMap<String,String>();
		for(int i = 0; i < size; i++) {
			ParsingRule rule = this.getRule(nameList.ArrayValues[i]);
			if(rule.getGrammar() == this) {
				ParsingExpression e = rule.expr.normalizeImpl(false, withoutMap).uniquefy();
//				if(rule.expr.uniqueId != e.uniqueId) {
//					System.out.println("RULE; " + rule.ruleName);
//					System.out.println("\tBEFORE; " + rule.expr);
//					System.out.println("\tAFTER ; " + e);
//					ParsingExpression.dumpId("", rule.expr);
//					ParsingExpression.dumpId("", e);
//				}
				rule.expr = e;
			}
		}

		Optimizer2.enableOptimizer();
		for(int i = 0; i < nameList.size(); i++) {
			ParsingRule rule = this.getRule(nameList.ArrayValues[i]);
			Optimizer2.optimize(rule.expr);
		
		}
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
			ParsingExpression e = this.getExpression("export");
			if(e != null) {
				appendExportRuleList(l, e.getExpression());
			}
			this.exportedRuleList = l;
		}
		return this.exportedRuleList;
	}

	private void appendExportRuleList(UList<ParsingRule> l, ParsingExpression e) {
		if(e instanceof NonTerminal) {
			ParsingRule rule = this.getRule(((NonTerminal) e).ruleName);
			l.add(rule);
			Main.printVerbose("export", rule.ruleName);
		}
		if(e instanceof ParsingChoice) {
			for(int i = 0; i < e.size(); i++) {
				appendExportRuleList(l, e.get(i).getExpression());
			}
		}
	}

	public final void show(String startPoint) {
		this.show(startPoint, new GrammarFormatter());
	}

	public final void show(String startPoint, GrammarFormatter fmt) {
		ParsingRule rule = this.getRule(startPoint);
		UList<ParsingRule> l = rule.subRule();
		StringBuilder sb = new StringBuilder();
		fmt.formatHeader(sb);
		for(ParsingRule r : l) {
			fmt.formatRule(r.ruleName, r.expr, sb);
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
	
//	public final void simpleFormatAll(SimpleGrammarFormatter fmt) {
//		StringBuilder sb = new StringBuilder();
//		fmt.formatHeader(sb);
//		UList<ParsingRule> list = this.getRuleList();
//		for(int i = 0; i < list.size(); i++) {
//			ParsingRule r = list.ArrayValues[i];
//			fmt.nonTerminalMap.put(r.ruleName, fmt.opList.size());
//			fmt.formatRule(r.ruleName, r.expr, sb);
//		}
//		fmt.formatFooter(sb);
//		System.out.println(sb.toString());
//	}
	
	private ParsingModel model = new ParsingModel();
	public final ParsingTag newTag(String tagName) {
		return model.get(tagName);
	}

	public final ParsingTag newStartTag() {
		return model.get("Text");
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
	private final ParsingExpression t(String token) {
		return ParsingExpression.newString(token);
	}
	private final ParsingExpression c(String text) {
		return ParsingExpression.newCharacter(text);
	}
	private final ParsingExpression P(String ruleName) {
		return newNonTerminal(ruleName);
	}
	private final ParsingExpression Optional(ParsingExpression e) {
		return ParsingExpression.newOption(e);
	}
	private final ParsingExpression ZeroMore(ParsingExpression e) {
		return ParsingExpression.newRepetition(e);
	}
	private final ParsingExpression ZeroMore(ParsingExpression ... elist) {
		return ParsingExpression.newRepetition(Sequence(elist));
	}
	private final ParsingExpression Sequence(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}
	private final ParsingExpression Choice(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addChoice(l, e);
		}
		return ParsingExpression.newChoice(l);
	}
	private final ParsingExpression Not(ParsingExpression e) {
		return ParsingExpression.newNot(e);
	}
	private final ParsingExpression Tag(int tagId) {
		return ParsingExpression.newTagging(newTag(ParsingTag.tagName(tagId)));
	}
	private final ParsingExpression Constructor(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newConstructor(ParsingExpression.newSequence(l));
	}
	private ParsingExpression LeftJoin(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newJoinConstructor(ParsingExpression.newSequence(l));
	}
	private ParsingExpression Link(ParsingExpression e) {
		return ParsingExpression.newConnector(e, -1);
	}
	
	private ParsingExpression Link(int index, ParsingExpression e) {
		return ParsingExpression.newConnector(e, index);
	}

	private ParsingExpression Any() {
		return ParsingExpression.newAny(".");
	}

	public Grammar loadPEG4dGrammar() {
//		ParsingExpression Any = ParsingExpression.newAny(".");
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
			ParsingExpression StringContent  = ZeroMore(Choice(
				t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any())
			));
			ParsingExpression StringContent2 = ZeroMore(Choice(
				t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any())
			));
			this.setRule("String", 
				Sequence(t("\""), Constructor(StringContent2, Tag(ParsingTag.String)), t("\""))
			);
			this.setRule("SingleQuotedString", 
				Sequence(t("'"),  Constructor(StringContent, Tag(PEG4d.CharacterSequence)), t("'"))
			);
		}
		ParsingExpression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), Any())
		));
		ParsingExpression _Message = Sequence(t("`"), Constructor(ValueContent, Tag(PEG4d.Value)), t("`"));
		this.setRule("CHARCONTET_", Choice( 
			Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")),
			Sequence(t("\\x"), P("HEX"), P("HEX")),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), Any())
		));
		ParsingExpression _CharChunk = Sequence(
			Constructor (P("CHARCONTET_"), Tag(PEG4d.Character)), 
			Optional(
				LeftJoin(t("-"), Link(Constructor(P("CHARCONTET_"), Tag(PEG4d.Character))), Tag(ParsingTag.List))
			)
		);
		this.setRule("Charcter_", Sequence(t("["), Constructor(ZeroMore(Link(_CharChunk)), Tag(PEG4d.Character)), t("]")));

		ParsingExpression _Any = Constructor(t("."), Tag(PEG4d.Any));
		ParsingExpression _Tagging = Sequence(t("#"), Constructor(c("A-Za-z0-9"), ZeroMore(c("A-Za-z0-9_.")), Tag(PEG4d.Tagging)));
		ParsingExpression _Byte = Constructor(t("0x"), P("HEX"), P("HEX"), Tag(PEG4d.Byte));
		ParsingExpression _Unicode = Constructor(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag(PEG4d.Byte));
		ParsingExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		ParsingExpression Connector  = Choice(t("@"), t("^"));
		ParsingExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));

		this.setRule("Constructor_", Constructor(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, P("S"), Tag(PEG4d.LeftJoin)), 
				Tag(PEG4d.Constructor)
			), 
			P("_"), 
			Optional(Sequence(Link(P("Expr_")), P("_"))),
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
					Sequence(t("block"), Optional(Sequence(P("S"), Link(P("Expr_")))), Tag(PEG4d.Block)),
					Sequence(t("indent"), Tag(PEG4d.Indent)),
//					Sequence(t("choice"), Tag(PEG4d.Choice)),
					Sequence(t("isa"), P("S"), Link(P("Name")), Tag(PEG4d.Isa)),
					Sequence(t("name"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr_")), Tag(PEG4d.Name)),
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
		this.setRule("NOTRULE_", Not(Choice(P("Rule_"), P("Import_"))));
		this.setRule("Sequence_", Sequence(
				P("Predicate_"), 
				Optional(
					LeftJoin(
						P("_"), 
						P("NOTRULE_"),
						Link(P("Predicate_")),
						ZeroMore(
							P("_"), 
							P("NOTRULE_"),
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
			Link(Choice(P("SingleQuotedString"), P("DotName"))), 
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
	
	private void setRule(String ruleName, ParsingExpression e) {
		ParsingRule rule = new ParsingRule(this, ruleName, null, null);
		rule.expr = e;
		this.setRule(ruleName, rule);
	}

}



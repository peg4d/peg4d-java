package org.peg4d;

import java.util.TreeMap;

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
	
	final NonTerminal newNonTerminal(String symbol) {
		return new NonTerminal(this, symbol);
	}
	
	final boolean loadGrammarFile(String fileName) {
		Grammar peg4d = GrammarFactory.Grammar;
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
		
	private ParsingModel model = new ParsingModel();
	public final ParsingTag newTag(String tagName) {
		return model.get(tagName);
	}

	public final ParsingTag newStartTag() {
		return model.get("Text");
	}

}




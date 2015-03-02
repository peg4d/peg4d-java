package org.peg4d;

import java.util.TreeMap;

import nez.expr.NodeTransition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.Optimizer;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingDef;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.model.ParsingModel;
import org.peg4d.pegcode.GrammarGenerator;
import org.peg4d.pegcode.PEG4dFormatter;

public class Grammar {
	GrammarFactory       factory;
	String               name;
	public UList<String> definedNameList;
	UMap<ParsingRule>    ruleMap;

	UList<ParsingRule>   exportedRuleList;
	UMap<String>         objectLabelMap = null;
	public boolean       foundError = false;

	int optimizationLevel;
		
	Grammar(GrammarFactory factory, String name) {
		this.name = name;
		this.factory = factory == null ? GrammarFactory.Grammar.factory : factory;
		this.ruleMap  = new UMap<ParsingRule>();
		this.definedNameList  = new UList<String>(new String[16]);
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
	
	final boolean loadGrammarFile(String fileName, NezLogger stats) {
		Grammar peg4d = GrammarFactory.Grammar;
		ParsingSource s = ParsingSource.loadSource(fileName);
		ParsingContext context = new ParsingContext(s); //peg4d.newParserContext();
		this.name = fileName;
		if(fileName.indexOf('/') > 0) {
			this.name = fileName.substring(fileName.lastIndexOf('/')+1);
		}
		PEG4d builder = new PEG4d(this);
		while(context.hasByteChar()) {
			ParsingObject po = context.parse2(peg4d, "Chunk", new ParsingObject(), null);
			if(context.isFailure()) {
				String msg = context.source.formatPositionLine("error", context.fpos, context.getErrorMessage());
				Main._Exit(1, msg);
				return false;
			}
			if(!builder.parse(po)) {
				return false;
			}
		}
		this.verifyRules(stats);
		return this.foundError;
	}
		
	public final static char NameSpaceSeparator = ':';
	
	final void importGrammar(UList<ParsingObject> nameList, String filePath) {
		Grammar peg = this.factory.getGrammar(filePath);
		if(peg != null) {
			for(ParsingObject po : nameList) {
				String key = po.getText();
				String ruleName = key;
				if(ruleName.indexOf(NameSpaceSeparator) > 0) {
					ruleName = ruleName.substring(ruleName.indexOf(NameSpaceSeparator)+1);
				}
				ParsingRule rule = peg.getRule(ruleName);
				if(rule == null) {
					Main._PrintLine(po.formatSourceMessage("warning", "undefined rule: " + ruleName));
					continue;
					//rule = newUndefinedRule(ruleName);
				}
				if(this.ruleMap.hasKey(key)) {
					Main.printVerbose("duplicated: ", key + " ");
				}
				this.ruleMap.put(key, rule);
			}
		}
	}
	
	public final ParsingRule newRule(String name, ParsingExpression e) {
		ParsingRule r = new ParsingRule(this, name, null, e);
		this.ruleMap.put(name, r);
		//System.out.println("creating rule: " + name);
		return r;
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

	public void makeOptionRule(ParsingRule r, String optName, boolean lexOnly, TreeMap<String, String> undefedFlags) {
		ParsingRule r2 = this.getRule(optName);
		if(r2 == null) {
			r2 = new ParsingRule(this, optName, null, null);
			this.setRule(optName, r2);
			r2.type = lexOnly ? ParsingRule.LexicalRule : r.type;
			r2.baseName = r.baseName;  // important
			r2.minlen = r.minlen;
			r2.refc = r.refc;
			r2.expr = r.expr.norm(lexOnly, undefedFlags).intern();
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
			this.definedNameList.add(ruleName);
		}
		this.ruleMap.put(ruleName, rule);
	}
	
	public final UList<ParsingRule> getRuleList() {
		UList<ParsingRule> ruleList = new UList<ParsingRule>(new ParsingRule[definedNameList.size()]);
		for(int i = 0; i < definedNameList.size(); i++) {
			ruleList.add(getRule(definedNameList.ArrayValues[i]));
		}
		return ruleList;
	}

	public final UList<ParsingExpression> getExpressionList() {
		UList<ParsingExpression> pegList = new UList<ParsingExpression>(new ParsingExpression[definedNameList.size()]);
		for(int i = 0; i < definedNameList.size(); i++) {
			String ruleName = definedNameList.ArrayValues[i];
			pegList.add(this.getRule(ruleName).expr);
		}
		return pegList;
	}

	public final void verifyRules(NezLogger stats) {
		this.foundError = false;
		this.exportedRuleList = null;
		this.getExportRuleList();
		if(stats != null) {
			stats.setText("PEG", this.getName());
			stats.setCount("PEG.NonTerminal", definedNameList.size());
		}
		UList<String> stack = new UList<String>(new String[64]);
		for(int i = 0; i < definedNameList.size(); i++) {
			ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
			stack.clear(0);
			rule.checkAlwaysConsumed(null, stack);
		}
		if(this.foundError) {
			Main._Exit(1, "PegError found");
		}
		if(stats != null) {
			stats.setCount("PEG.Rules", definedNameList.size());
		}
		// type check
		for(int i = 0; i < definedNameList.size(); i++) {
			ParsingRule r = this.getRule(definedNameList.ArrayValues[i]);
			r.checkNodeTransition(new NodeTransition());
		}
		for(int i = 0; i < definedNameList.size(); i++) {
			ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
			//ParsingExpression.typeCheck(rule, flagMap);
			rule.expr = rule.expr.intern();
			//ParsingExpression.dumpId(rule.ruleName+ " ", rule.expr);
		}
		int size = definedNameList.size();
		TreeMap<String,String> undefedFlags = new TreeMap<String,String>();
		for(int i = 0; i < size; i++) {
			ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
			if(rule.getGrammar() == this) {
				rule.expr = rule.expr.removeFlag(undefedFlags).intern();
//				ParsingExpression e = rule.expr.norm(false, undefedFlags).intern();
//				if(rule.expr.uniqueId != e.uniqueId) {
//					System.out.println("RULE; " + rule.ruleName);
//					System.out.println("\tBEFORE; " + rule.expr);
//					System.out.println("\tAFTER ; " + e);
//					ParsingExpression.dumpId("", rule.expr);
//					ParsingExpression.dumpId("", e);
//				}
//				rule.expr = e;
			}
		}
		if(stats != null) {
			stats.setCount("PEG.NormalizedRules", definedNameList.size());
			int count = 0;
			UMap<String> flagMap = new UMap<String>();
			for(int i = 0; i < definedNameList.size(); i++) {
				ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
				flagMap.clear();
				if(ParsingDef.checkContextSensitivity(rule.expr, flagMap)) {
					count+=1;
				}
			}
			stats.setCount("PEG.ContextSensitiveRules", count);
			stats.setRatio("PEG.ContextSensitivity", count, definedNameList.size());
		}
		try {
			new Optimizer().optimize(this, stats);
		}
		catch(Exception e) {
			System.out.println("Optimizer error: " + e);
		}
		ParsingContext context = new ParsingContext(null);
		for(int i = 0; i < definedNameList.size(); i++) {
			ParsingRule rule = this.getRule(definedNameList.ArrayValues[i]);
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
				appendExportRuleList(l, e);
			}
			this.exportedRuleList = l;
		}
		return this.exportedRuleList;
	}

	private void appendExportRuleList(UList<ParsingRule> l, ParsingExpression e) {
		if(e instanceof NonTerminal) {
			ParsingRule rule = this.getRule(((NonTerminal) e).ruleName);
			l.add(rule);
			Main.printVerbose("export", rule.localName);
		}
		if(e instanceof ParsingChoice) {
			for(int i = 0; i < e.size(); i++) {
				appendExportRuleList(l, e.get(i));
			}
		}
	}

	public final void show(String startPoint) {
		this.show(startPoint, new PEG4dFormatter());
	}

	public final void show(String startPoint, GrammarGenerator fmt) {
		ParsingRule rule = this.getRule(startPoint);
		UList<ParsingRule> l = rule.subRule();
		StringBuilder sb = new StringBuilder();
//		fmt.formatHeader(sb);
		for(ParsingRule r : l) {
			fmt.formatRule(r, sb);
		}
//		fmt.formatFooter(sb);
		System.out.println(sb.toString());
	}

	public final void formatAll(GrammarGenerator fmt) {
		StringBuilder sb = new StringBuilder();
		fmt.formatGrammar(this, sb);
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




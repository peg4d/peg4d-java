package org.peg4d;

import java.io.File;

import nez.util.ReportLevel;
import nez.util.UList;

import org.peg4d.expression.ParsingExpression;

public class PEG4d extends ParsingBuilder {
	
	static final int Any         = ParsingTag.tagId("Any");
	static final int Character   = ParsingTag.tagId("Character");
	static final int Byte        = ParsingTag.tagId("Byte");
	static final int CharacterSequence      = ParsingTag.tagId("CharacterSequence");

	static final int NonTerminal = ParsingTag.tagId("NonTerminal");
	static final int Choice      = ParsingTag.tagId("Choice");
	static final int Sequence    = ParsingTag.tagId("Sequence");
	static final int Repetition    = ParsingTag.tagId("Repetition");
	static final int OneMoreRepetition     = ParsingTag.tagId("OneMoreRepetition");
	static final int Optional    = ParsingTag.tagId("Optional");
	static final int Not         = ParsingTag.tagId("Not");
	static final int And         = ParsingTag.tagId("And");

	static final int Rule        = ParsingTag.tagId("Rule");
	static final int Import      = ParsingTag.tagId("Import");
	static final int Annotation  = ParsingTag.tagId("Annotation");

	static final int Constructor = ParsingTag.tagId("Constructor");
	static final int LeftJoin    = ParsingTag.tagId("LeftJoin");
	static final int Connector   = ParsingTag.tagId("Connector");
	static final int Value       = ParsingTag.tagId("Value");
	static final int Tagging     = ParsingTag.tagId("Tagging");

	static final int Match       = ParsingTag.tagId("Match");
	static final int Debug       = ParsingTag.tagId("Debug");
	static final int Memo        = ParsingTag.tagId("Memo");
	static final int Catch       = ParsingTag.tagId("Catch");
	static final int Fail        = ParsingTag.tagId("Fail");


	static final int Block       = ParsingTag.tagId("Block");
	public static final int Indent      = ParsingTag.tagId("Indent");
	static final int Without     = ParsingTag.tagId("Without");
	static final int With        = ParsingTag.tagId("With");
	static final int If          = ParsingTag.tagId("If");
	static final int Def         = ParsingTag.tagId("Def");
	static final int Is          = ParsingTag.tagId("Is");
	static final int Isa         = ParsingTag.tagId("Isa");

	static final int Stringfy    = ParsingTag.tagId("Stringfy");
	static final int Apply       = ParsingTag.tagId("Apply");

	static final int PowerSet     = ParsingTag.tagId("PowerSet");
	static final int Permutation     = ParsingTag.tagId("Permutation");
	static final int PermutationExpr = ParsingTag.tagId("PermutationExpr");
	
	static final int Scan = ParsingTag.tagId("Scan");
	static final int Repeat = ParsingTag.tagId("Repeat");

	Grammar peg;
	
	PEG4d(Grammar peg) {
		this.peg = peg;
	}
	
	boolean parse(ParsingObject po) {
		//System.out.println("DEBUG? parsed: " + po);
		if(po.is(PEG4d.Rule)) {
			if(po.size() > 3) {
				System.out.println("DEBUG? parsed: " + po);		
			}
			String ruleName = po.textAt(0, "");
			if(po.get(0).is(ParsingTag.String)) {
				ruleName = quote(ruleName);
			}
			ParsingRule rule = peg.getRule(ruleName);
			ParsingExpression e = toParsingExpression(po.get(1));
			if(rule != null) {
				if(rule.po != null) {
					if(rule.peg == peg) {
						rule.report(ReportLevel.warning, "duplicated rule name: " + ruleName);
					}
					rule = null;
				}
			}
			rule = new ParsingRule(peg, ruleName, po.get(0), e);
			peg.setRule(ruleName, rule);
			if(po.size() >= 3) {
				readAnnotations(rule, po.get(2));
			}
			return true;
		}
		if(po.is(PEG4d.Import)) {
			UList<ParsingObject> l = new UList<ParsingObject>(new ParsingObject[po.size()-1]);
			for(int i = 0; i < po.size()-1;i++) {
				l.add(po.get(i));
			}
			String filePath = searchPegFilePath(po.getSource(), po.textAt(po.size()-1, ""));
			peg.importGrammar(l, filePath);
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
	
	private void readAnnotations(ParsingRule rule, ParsingObject pego) {
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			if(p.is(PEG4d.Annotation)) {
				rule.addAnotation(p.textAt(0, ""), p.get(1));
			}
		}
	}

	private String searchPegFilePath(ParsingSource s, String filePath) {
		String f = s.getFilePath(filePath);
		if(new File(f).exists()) {
			return f;
		}
		if(new File(filePath).exists()) {
			return filePath;
		}
		return "lib/"+filePath;
	}

	
	ParsingExpression toParsingExpression(ParsingObject po) {
		ParsingExpression e = (ParsingExpression)this.build(po);
		e.po = po;
		return e;
	}

	public ParsingExpression toNonTerminal(ParsingObject po) {
		String symbol = po.getText();
//		if(ruleName.equals(symbol)) {
//			ParsingExpression e = peg.getExpression(ruleName);
//			if(e != null) {
//				// self-redefinition
//				return e;  // FIXME
//			}
//		}
		if(symbol.length() > 0 && !symbol.endsWith("_") && !peg.hasRule(symbol)
				&& GrammarFactory.Grammar.hasRule(symbol)) { // comment
			Main.printVerbose("implicit importing", symbol);
			peg.setRule(symbol, GrammarFactory.Grammar.getRule(symbol));
		}
		return peg.newNonTerminal(symbol);
	}

	private String quote(String t) {
		return "\"" + t + "\"";
	}

	public ParsingExpression toString(ParsingObject po) {
		String t = quote(po.getText());
		if(peg.hasRule(t)) {
			Main.printVerbose("direct inlining", t);
			return peg.getExpression(t);
		}
		return ParsingExpression.newString(Utils.unquoteString(po.getText()));
	}

	public ParsingExpression toCharacterSequence(ParsingObject po) {
		return ParsingExpression.newString(Utils.unquoteString(po.getText()));
	}

	public ParsingExpression toCharacter(ParsingObject po) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[2]);
		if(po.size() > 0) {
			for(int i = 0; i < po.size(); i++) {
				ParsingObject o = po.get(i);
				if(o.is(ParsingTag.List)) {  // range
					l.add(ParsingExpression.newCharset(o.textAt(0, ""), o.textAt(1, ""), po.getText()));
				}
				if(o.is(org.peg4d.PEG4d.Character)) {  // single
					l.add(ParsingExpression.newCharset(o.getText(), o.getText(), po.getText()));
				}
				//System.out.println("u=" + u + " by " + o);
			}
		}
		return ParsingExpression.newChoice(l);
	}

	public ParsingExpression toByte(ParsingObject po) {
		String t = po.getText();
		if(t.startsWith("U+")) {
			int c = ParsingCharset.hex(t.charAt(2));
			c = (c * 16) + ParsingCharset.hex(t.charAt(3));
			c = (c * 16) + ParsingCharset.hex(t.charAt(4));
			c = (c * 16) + ParsingCharset.hex(t.charAt(5));
			if(c < 128) {
				return ParsingExpression.newByte(c);					
			}
			String t2 = java.lang.String.valueOf((char)c);
			return ParsingExpression.newString(t2);
		}
		int c = ParsingCharset.hex(t.charAt(t.length()-2)) * 16 + ParsingCharset.hex(t.charAt(t.length()-1)); 
		return ParsingExpression.newByte(c);
	}

	public ParsingExpression toAny(ParsingObject po) {
		return ParsingExpression.newAny(po.getText());
	}

	public ParsingExpression toChoice(ParsingObject po) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[po.size()]);
		for(int i = 0; i < po.size(); i++) {
			ParsingExpression.addChoice(l, toParsingExpression(po.get(i)));
		}
		return ParsingExpression.newChoice(l);
	}

	public ParsingExpression toSequence(ParsingObject po) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[po.size()]);
		for(int i = 0; i < po.size(); i++) {
			ParsingExpression.addSequence(l, toParsingExpression(po.get(i)));
		}
		return ParsingExpression.newSequence(l);
	}

	public ParsingExpression toNot(ParsingObject po) {
		return ParsingExpression.newNot(toParsingExpression(po.get(0)));
	}

	public ParsingExpression toAnd(ParsingObject po) {
		return ParsingExpression.newAnd(toParsingExpression(po.get(0)));
	}

	public ParsingExpression toOptional(ParsingObject po) {
		return ParsingExpression.newOption(toParsingExpression(po.get(0)));
	}

	public ParsingExpression toOneMoreRepetition(ParsingObject po) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[2]);
		l.add(toParsingExpression(po.get(0)));
		l.add(ParsingExpression.newRepetition(toParsingExpression(po.get(0))));
		return ParsingExpression.newSequence(l);
	}

	public ParsingExpression toRepetition(ParsingObject po) {
		if(po.size() == 2) {
			int ntimes = Utils.parseInt(po.textAt(1, ""), -1);
			if(ntimes != 1) {
				UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[ntimes]);
				for(int i = 0; i < ntimes; i++) {
					ParsingExpression.addSequence(l, toParsingExpression(po.get(0)));
				}
				return ParsingExpression.newSequence(l);
			}
		}
		return ParsingExpression.newRepetition(toParsingExpression(po.get(0)));
	}

	// PEG4d TransCapturing

	public ParsingExpression toConstructor(ParsingObject po) {
		ParsingExpression seq = (po.size() == 0) ? ParsingExpression.newEmpty() : toParsingExpression(po.get(0));
		return ParsingExpression.newConstructor(seq);
	}

	public ParsingExpression toLeftJoin(ParsingObject po) {
		ParsingExpression seq = (po.size() == 0) ? ParsingExpression.newEmpty() : toParsingExpression(po.get(0));
		return ParsingExpression.newJoinConstructor(seq);
	}

	public ParsingExpression toConnector(ParsingObject po) {
		int index = -1;
		if(po.size() == 2) {
			index = Utils.parseInt(po.textAt(1, ""), -1);
		}
		return ParsingExpression.newConnector(toParsingExpression(po.get(0)), index);
	}

	public ParsingExpression toTagging(ParsingObject po) {
		return ParsingExpression.newTagging(peg.newTag(po.getText()));
	}

	public ParsingExpression toValue(ParsingObject po) {
		return ParsingExpression.newValue(po.getText());
	}

	//PEG4d Function
	
	public ParsingExpression toDebug(ParsingObject po) {
		return ParsingExpression.newDebug(toParsingExpression(po.get(0)));
	}

	public ParsingExpression toMatch(ParsingObject po) {
		return ParsingExpression.newMatch(toParsingExpression(po.get(0)));
	}

	public ParsingExpression toCatch(ParsingObject po) {
		return ParsingExpression.newCatch();
	}

	public ParsingExpression toFail(ParsingObject po) {
		return ParsingExpression.newFail(Utils.unquoteString(po.textAt(0, "")));
	}

	public ParsingExpression toWith(ParsingObject po) {
		return ParsingExpression.newWithFlag(po.textAt(0, ""), toParsingExpression(po.get(1)));
	}

	public ParsingExpression toWithout(ParsingObject po) {
		return ParsingExpression.newWithoutFlag(po.textAt(0, ""), toParsingExpression(po.get(1)));
	}

	public ParsingExpression toIf(ParsingObject po) {
		return ParsingExpression.newIf(po.textAt(0, ""));
	}

	public ParsingExpression toBlock(ParsingObject po) {
		return ParsingExpression.newBlock(toParsingExpression(po.get(0)));
	}

	public ParsingExpression toIndent(ParsingObject po) {
		return ParsingExpression.newIndent();
	}

	public ParsingExpression toName(ParsingObject po) {
		int tagId = ParsingTag.tagId(po.textAt(0, ""));
		return ParsingExpression.newDef(tagId, toParsingExpression(po.get(1)));
	}

	public ParsingExpression toDef(ParsingObject po) {
		int tagId = ParsingTag.tagId(po.textAt(0, ""));
		return ParsingExpression.newDef(tagId, toParsingExpression(po.get(1)));
	}

	public ParsingExpression toIs(ParsingObject po) {
		int tagId = ParsingTag.tagId(po.textAt(0, ""));
		return ParsingExpression.newIs(tagId);
	}

	public ParsingExpression toIsa(ParsingObject po) {
		int tagId = ParsingTag.tagId(po.textAt(0, ""));
		return ParsingExpression.newIsa(tagId);
	}
	
	private UList<ParsingExpression> createOrder(int ruleSize, ParsingObject seq) {
		UList<ParsingExpression> l2 = new UList<ParsingExpression>(new ParsingExpression[ruleSize]);
		UPermutation p = new UPermutation(seq.size());
		do {
			int[] a = p.next();
			UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[a.length]);
			for(int v: a) {
				l.add(toParsingExpression(seq.get(v)));
			}
			ParsingExpression.addChoice(l2, ParsingExpression.newSequence(l));
		} while(p.hasNext());
		return l2;
	}

	private UList<ParsingExpression> createPowerSet(int ruleSize, ParsingObject seq) {
		UList<ParsingExpression> ret = new UList<ParsingExpression>(new ParsingExpression[ruleSize]);
		//nCn
		UList<ParsingExpression> l1 = new UList<ParsingExpression>(new ParsingExpression[seq.size()]);
		for(int i = 0; i < seq.size(); i++) {
			l1.add(toParsingExpression(seq.get(i)));
		}
		ParsingExpression.addChoice(ret, ParsingExpression.newSequence(l1));
		//nC(n-1)
		for(int i = 0; i < seq.size(); i++) {
			UList<ParsingExpression> l2 = new UList<ParsingExpression>(new ParsingExpression[seq.size()-1]);
			for(int j = 0; j < seq.size(); j++) {
				if(i==j) {
					continue;
				}
				l2.add(toParsingExpression(seq.get(j)));
			}
			ParsingExpression.addChoice(ret, ParsingExpression.newSequence(l2));
		}
		//TODO create nC(n-2) ... nC1
		return ret;
	}

//	public ParsingExpression toPermutationExpr(ParsingObject po) {
//		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[po.size()]);
//		for(int i = 0; i < po.size(); i++) {
//			ParsingExpression.addSequence(l, toParsingExpression(po.get(i)));
//		}
//		return ParsingExpression.newPermutation(l);
//	}

	public ParsingExpression toPermutation(ParsingObject po) {
		ParsingObject seq = po.get(0);
		if(seq.getTag().tagId == Sequence) {
			int ruleChoiceSize = 1;
			for(int i = 2; i <= seq.size(); i++) {
				ruleChoiceSize *= i;
			}
			return ParsingExpression.newChoice(createOrder(ruleChoiceSize, seq));
		}
		// not Sequence
		return toParsingExpression(po.get(0));
	}

	public ParsingExpression toPowerSet(ParsingObject po) {
		ParsingObject seq = po.get(0);
		if(seq.getTag().tagId == Sequence) {
			int ruleChoiceSize = seq.size() + 1;
			return ParsingExpression.newChoice(createPowerSet(ruleChoiceSize, seq));
		}
		// not Sequence
		return toParsingExpression(po.get(0));
	}
	
	public ParsingExpression toScan(ParsingObject po) {
		return ParsingExpression.newScan(Integer.parseInt(po.get(0).getText()), toParsingExpression(po.get(1)), toParsingExpression(po.get(2)));
	}
	
	public ParsingExpression toRepeat(ParsingObject po) {
		return ParsingExpression.newRepeat(toParsingExpression(po.get(0)));
	}
}

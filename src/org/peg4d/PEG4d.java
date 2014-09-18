package org.peg4d;

import java.io.File;

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


	static final int Indent      = ParsingTag.tagId("Indent");
	static final int Without     = ParsingTag.tagId("Without");
	static final int With        = ParsingTag.tagId("With");
	static final int If          = ParsingTag.tagId("If");

	static final int Stringfy    = ParsingTag.tagId("Stringfy");
	static final int Apply       = ParsingTag.tagId("Apply");

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
			PExpression e = toPExpression(po.get(1));
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
			String filePath = searchPegFilePath(po.getSource(), po.textAt(0, ""));
			String ns = po.textAt(1, "");
			peg.importGrammar(ns, filePath);
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

	
	PExpression toPExpression(ParsingObject po) {
		PExpression e = (PExpression)this.build(po);
		if(e != null) {
			e.po = po;
		}
		return e;
	}

	public PExpression toNonTerminal(ParsingObject po) {
		String symbol = po.getText();
//		if(ruleName.equals(symbol)) {
//			PExpression e = peg.getExpression(ruleName);
//			if(e != null) {
//				// self-redefinition
//				return e;  // FIXME
//			}
//		}
		if(symbol.length() > 0 && !symbol.endsWith("_") && !peg.hasRule(symbol) && GrammarFactory.Grammar.hasRule(symbol)) { // comment
			Main.printVerbose("implicit importing", symbol);
			peg.setRule(symbol, GrammarFactory.Grammar.getRule(symbol));
		}
		return peg.newNonTerminal(symbol);
	}

	private String quote(String t) {
		return "\"" + t + "\"";
	}

	public PExpression toString(ParsingObject po) {
		String t = quote(po.getText());
		if(peg.hasRule(t)) {
			Main.printVerbose("direct inlining", t);
			return peg.getExpression(t);
		}
		return PExpression.newString(ParsingCharset.unquoteString(po.getText()));
	}

	public PExpression toCharacterSequence(ParsingObject po) {
		return PExpression.newString(ParsingCharset.unquoteString(po.getText()));
	}

	public PExpression toCharacter(ParsingObject po) {
		ParsingCharset u = null;
		if(po.size() > 0) {
			for(int i = 0; i < po.size(); i++) {
				ParsingObject o = po.get(i);
				if(o.is(ParsingTag.List)) {
					u = ParsingCharset.addText(u, o.textAt(0, ""), o.textAt(1, ""));
				}
				if(o.is(org.peg4d.PEG4d.Character)) {
					u = ParsingCharset.addText(u, o.getText(), o.getText());
				}
				//System.out.println("u=" + u + " by " + o);
			}
		}
		return PExpression.newCharacter(u);
	}

	public PExpression toByte(ParsingObject po) {
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

	public PExpression toAny(ParsingObject po) {
		return PExpression.newAny(po.getText());
	}

	public PExpression toChoice(ParsingObject po) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
		for(int i = 0; i < po.size(); i++) {
			PExpression.addChoice(l, toPExpression(po.get(i)));
		}
		return PExpression.newChoice(l);
	}

	public PExpression toSequence(ParsingObject po) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[po.size()]);
		for(int i = 0; i < po.size(); i++) {
			PExpression.addSequence(l, toPExpression(po.get(i)));
		}
		return PExpression.newSequence(l);
	}

	public PExpression toNot(ParsingObject po) {
		return PExpression.newNot(toPExpression(po.get(0)));
	}

	PExpression toAnd(ParsingObject po) {
		return PExpression.newAnd(toPExpression(po.get(0)));
	}

	public PExpression toOptional(ParsingObject po) {
		return PExpression.newOptional(toPExpression(po.get(0)));
	}

	public PExpression toOneMoreRepetition(ParsingObject po) {
		UList<PExpression> l = new UList<PExpression>(new PExpression[2]);
		l.add(toPExpression(po.get(0)));
		l.add(PExpression.newRepetition(toPExpression(po.get(0))));
		return PExpression.newSequence(l);
	}

	public PExpression toRepetition(ParsingObject po) {
		if(po.size() == 2) {
			int ntimes = ParsingCharset.parseInt(po.textAt(1, ""), -1);
			if(ntimes != 1) {
				UList<PExpression> l = new UList<PExpression>(new PExpression[ntimes]);
				for(int i = 0; i < ntimes; i++) {
					PExpression.addSequence(l, toPExpression(po.get(0)));
				}
				return PExpression.newSequence(l);
			}
		}
		return PExpression.newRepetition(toPExpression(po.get(0)));
	}

	// PEG4d TransCapturing

	public PExpression toConstructor(ParsingObject po) {
		PExpression seq = toPExpression(po.get(0));
		return PExpression.newConstructor(seq);
	}

	public PExpression toLeftJoin(ParsingObject po) {
		PExpression seq = toPExpression(po.get(0));
		return PExpression.newJoinConstructor(seq);
	}

	public PExpression toConnector(ParsingObject po) {
		int index = -1;
		if(po.size() == 2) {
			index = ParsingCharset.parseInt(po.textAt(1, ""), -1);
		}
		return PExpression.newConnector(toPExpression(po.get(0)), index);
	}

	public PExpression toTagging(ParsingObject po) {
		return PExpression.newTagging(peg.newTag(po.getText()));
	}

	public PExpression toValue(ParsingObject po) {
		return PExpression.newValue(po.getText());
	}

	//PEG4d Function
	
	public PExpression toMatch(ParsingObject po) {
		return PExpression.newMatch(toPExpression(po.get(0)));
	}

	public PExpression toWith(ParsingObject po) {
		return PExpression.newEnableFlag(po.textAt(0, ""), toPExpression(po.get(1)));
	}

	public PExpression toWithout(ParsingObject po) {
		return PExpression.newDisableFlag(po.textAt(0, ""), toPExpression(po.get(1)));
	}
//
//	private static public PExpression toParsingExpressionImpl(Grammar peg, String ruleName, ParsingObject po) {
//		if(po.is(org.peg4d.PEG4d.ParsingMatch)) {
//			return PExpression.newMatch(toPExpression(po.get(0)));
//		}
//		if(po.is(org.peg4d.PEG4d.ParsingWithFlag)) {
//		}
//		if(po.is(org.peg4d.PEG4d.ParsingWithoutFlag)) {
//			return PExpression.newDisableFlag(po.textAt(0, ""), toPExpression(po.get(1)));
//		}
//		if(po.is(org.peg4d.PEG4d.ParsingIfFlag)) {
//			return PExpression.newFlag(po.textAt(0, ""));
//		}
//		if(po.is(org.peg4d.PEG4d.ParsingIndent)) {
//			if(po.size() == 0) {
//				return PExpression.newIndent(null);
//			}
//			return PExpression.newIndent(toPExpression(po.get(0)));
//		}
//		
//		if(po.is(org.peg4d.PEG4d.ParsingDebug)) {
//			return PExpression.newDebug(toPExpression(po.get(0)));
//		}
//		if(po.is(org.peg4d.PEG4d.ParsingFail)) {
//			return PExpression.newFail(ParsingCharset.unquoteString(po.textAt(0, "")));
//		}
//		if(po.is(org.peg4d.PEG4d.ParsingCatch)) {
//			return PExpression.newCatch();
//		}
////		if(po.is(PEG4dGrammar.ParsingApply)) {
////			return PExpression.newApply(toParsingExpression(loading, ruleName, po.get(0)));
////		}
////		if(po.is(PEG4dGrammar.ParsingStringfy)) {
////			return PExpression.newStringfy();
////		}
////		if(pego.is("PExport")) {
////		Peg seq = toParsingExpression(loadingGrammar, ruleName, pego.get(0));
////		Peg o = loadingGrammar.newConstructor(ruleName, seq);
////		return new PegExport(loadingGrammar, 0, o);
////	}
////	if(pego.is("PSetter")) {
////		int index = -1;
////		String indexString = pego.getText();
////		if(indexString.length() > 0) {
////			index = UCharset.parseInt(indexString, -1);
////		}
////		return loadingGrammar.newConnector(toParsingExpression(loadingGrammar, ruleName, pego.get(0)), index);
////	}
////		if(node.is("pipe")) {
////			return new PegPipe(node.getText());
////		}
////		if(node.is("catch")) {
////			return new PegCatch(null, toPeg(node.get(0)));
////		}
//		Main._Exit(1, "undefined peg: " + po);
//		return null;
//	}


}

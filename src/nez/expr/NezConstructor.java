package nez.expr;

import nez.Grammar;
import nez.ast.AST;
import nez.ast.Converter;
import nez.ast.Tag;
import nez.util.ReportLevel;
import nez.util.StringUtils;
import nez.util.UList;

public class NezConstructor extends Converter {
		
	Grammar peg;
	
	NezConstructor(Grammar peg) {
		this.peg = peg;
	}
	
	boolean parse(AST ast) {
		//System.out.println("DEBUG? parsed: " + ast);
		if(ast.is(NezTag.Rule)) {
			if(ast.size() > 3) {
				System.out.println("DEBUG? parsed: " + ast);		
			}
			String ruleName = ast.textAt(0, "");
			if(ast.get(0).is(NezTag.String)) {
				ruleName = quote(ruleName);
			}
			Rule rule = peg.getRule(ruleName);
			Expression e = toExpression(ast.get(1));
			if(rule != null) {
				rule.report(ReportLevel.warning, "duplicated rule name: " + ruleName);
				rule = null;
			}
			rule = peg.defineRule(ast.get(0), ruleName, e);
			if(ast.size() >= 3) {
				readAnnotations(rule, ast.get(2));
			}
			return true;
		}
//		if(ast.is(NezTag.Import)) {
//			UList<AST> l = new UList<AST>(new AST[ast.size()-1]);
//			for(int i = 0; i < ast.size()-1;i++) {
//				l.add(ast.get(i));
//			}
//			String filePath = searchPegFilePath(ast.getSource(), ast.textAt(ast.size()-1, ""));
//			peg.imastrtGrammar(l, filePath);
//			return true;
//		}
//		if(ast.is(Tag.CommonError)) {
//			int c = ast.getSource().byteAt(ast.getSourcePosition());
//			System.out.println(ast.formatSourceMessage("error", "syntax error: ascii=" + c));
//			return false;
//		}
		System.out.println(ast.formatSourceMessage("error", "PEG rule is required: " + ast));
		return false;
	}
	
	private void readAnnotations(Rule rule, AST pego) {
		for(int i = 0; i < pego.size(); i++) {
			AST p = pego.get(i);
			if(p.is(NezTag.Annotation)) {
				rule.addAnotation(p.textAt(0, ""), p.get(1));
			}
		}
	}

//	private String searchPegFilePath(Source s, String filePath) {
//		String f = s.getFilePath(filePath);
//		if(new File(f).exists()) {
//			return f;
//		}
//		if(new File(filePath).exists()) {
//			return filePath;
//		}
//		return "lib/"+filePath;
//	}

	Expression toExpression(AST po) {
		return (Expression)this.build(po);
	}

	public Expression toNonTerminal(AST ast) {
		String symbol = ast.getText();
//		if(ruleName.equals(symbol)) {
//			Expression e = peg.getExpression(ruleName);
//			if(e != null) {
//				// self-redefinition
//				return e;  // FIXME
//			}
//		}
//		if(symbol.length() > 0 && !symbol.endsWith("_") && !peg.hasRule(symbol)
//				&& GrammarFactory.Grammar.hasRule(symbol)) { // comment
//			Main.printVerbose("implicit importing", symbol);
//			peg.setRule(symbol, GrammarFactory.Grammar.getRule(symbol));
//		}
		return Factory.newNonTerminal(ast, peg, symbol);
	}

	private String quote(String t) {
		return "\"" + t + "\"";
	}

	public Expression toString(AST ast) {
		String name = quote(ast.getText());
		Rule r = peg.getRule(name);
		if(r != null) {
			return r.getExpression();
		}
		return Factory.newString(ast, StringUtils.unquoteString(ast.getText()));
	}

	public Expression toCharacterSequence(AST ast) {
		return Factory.newString(ast, StringUtils.unquoteString(ast.getText()));
	}

	public Expression toCharacter(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		if(ast.size() > 0) {
			for(int i = 0; i < ast.size(); i++) {
				AST o = ast.get(i);
				if(o.is(NezTag.List)) {  // range
					l.add(Factory.newCharSet(ast, o.textAt(0, ""), o.textAt(1, "")));
				}
				if(o.is(NezTag.Character)) {  // single
					l.add(Factory.newCharSet(ast, o.getText(), o.getText()));
				}
				//System.out.println("u=" + u + " by " + o);
			}
		}
		return Factory.newChoice(ast, l);
	}

	public Expression toByte(AST ast) {
		String t = ast.getText();
		if(t.startsWith("U+")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			if(c < 128) {
				return Factory.newByteChar(ast, c);					
			}
			String t2 = java.lang.String.valueOf((char)c);
			return Factory.newString(ast, t2);
		}
		int c = StringUtils.hex(t.charAt(t.length()-2)) * 16 + StringUtils.hex(t.charAt(t.length()-1)); 
		return Factory.newByteChar(ast, c);
	}

	public Expression toAny(AST ast) {
		return Factory.newAnyChar(ast);
	}

	public Expression toChoice(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[ast.size()]);
		for(int i = 0; i < ast.size(); i++) {
			Factory.addChoice(l, toExpression(ast.get(i)));
		}
		return Factory.newChoice(ast, l);
	}

	public Expression toSequence(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[ast.size()]);
		for(int i = 0; i < ast.size(); i++) {
			Factory.addSequence(l, toExpression(ast.get(i)));
		}
		return Factory.newSequence(ast, l);
	}

	public Expression toNot(AST ast) {
		return Factory.newNot(ast, toExpression(ast.get(0)));
	}

	public Expression toAnd(AST ast) {
		return Factory.newAnd(ast, toExpression(ast.get(0)));
	}

	public Expression toOption(AST ast) {
		return Factory.newOption(ast, toExpression(ast.get(0)));
	}

	public Expression toOneMoreRepetition(AST ast) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		l.add(toExpression(ast.get(0)));
		l.add(Factory.newRepetition(ast, toExpression(ast.get(0))));
		return Factory.newSequence(ast, l);
	}

	public Expression toRepetition(AST ast) {
		if(ast.size() == 2) {
			int ntimes = StringUtils.parseInt(ast.textAt(1, ""), -1);
			if(ntimes != 1) {
				UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
				for(int i = 0; i < ntimes; i++) {
					Factory.addSequence(l, toExpression(ast.get(0)));
				}
				return Factory.newSequence(ast, l);
			}
		}
		return Factory.newRepetition(ast, toExpression(ast.get(0)));
	}

	// PEG4d TransCapturing

	public Expression toConstructor(AST ast) {
		Expression seq = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
		return Factory.newNew(ast, seq.toList());
	}

	public Expression toLeftJoin(AST ast) {
		Expression seq = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
		return Factory.newNewLeftLink(ast, seq.toList());
	}

	public Expression toConnector(AST ast) {
		int index = -1;
		if(ast.size() == 2) {
			index = StringUtils.parseInt(ast.textAt(1, ""), -1);
		}
		return Factory.newLink(ast, toExpression(ast.get(0)), index);
	}

	public Expression toTagging(AST ast) {
		return Factory.newTagging(ast, Tag.tag(ast.getText()));
	}

	public Expression toValue(AST ast) {
		return Factory.newReplace(ast, ast.getText());
	}

	//PEG4d Function
	
//	public Expression toDebug(AST ast) {
//		return Factory.newDebug(toExpression(ast.get(0)));
//	}

	public Expression toMatch(AST ast) {
		return Factory.newMatch(ast, toExpression(ast.get(0)));
	}

//	public Expression toCatch(AST ast) {
//		return Factory.newCatch();
//	}
//
//	public Expression toFail(AST ast) {
//		return Factory.newFail(Utils.unquoteString(ast.textAt(0, "")));
//	}

	public Expression toIf(AST ast) {
		return Factory.newIfFlag(ast, ast.textAt(0, ""));
	}

	public Expression toWith(AST ast) {
		return Factory.newWithFlag(ast, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toWithout(AST ast) {
		return Factory.newWithoutFlag(ast, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toBlock(AST ast) {
		return Factory.newBlock(ast, toExpression(ast.get(0)));
	}

	public Expression toDef(AST ast) {
		return Factory.newDefSymbol(ast, Tag.tag(ast.textAt(0, "")), toExpression(ast.get(1)));
	}

	public Expression toIs(AST ast) {
		return Factory.newIsSymbol(ast, Tag.tag(ast.textAt(0, "")));
	}

	public Expression toIsa(AST ast) {
		return Factory.newIsaSymbol(ast, Tag.tag(ast.textAt(0, "")));
	}

	public Expression toDefIndent(AST ast) {
		return Factory.newDefIndent(ast);
	}

	public Expression toIndent(AST ast) {
		return Factory.newIndent(ast);
	}

//	public Expression toScan(AST ast) {
//		return Factory.newScan(Integer.parseInt(ast.get(0).getText()), toExpression(ast.get(1)), toExpression(ast.get(2)));
//	}
//	
//	public Expression toRepeat(AST ast) {
//		return Factory.newRepeat(toExpression(ast.get(0)));
//	}
}

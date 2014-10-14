package org.peg4d;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.peg4d.expression.ParsingExpression;

public class ParsingGrammar extends Grammar {
	
	ParsingGrammar(GrammarFactory factory, String name) {
		super(factory, name);
		load();
	}
	
	void load() {
		Class<?> c = this.getClass();
		for(Method m : c.getDeclaredMethods()) {
			if(m.getReturnType() == ParsingExpression.class && m.getParameterCount() == 0) {
				String name = m.getName();
				//System.out.println("rule name: " + name);
				if(name.equals("SPACING")) {
					name = "_";
				}
				try {
					ParsingExpression e = (ParsingExpression)m.invoke(this);
					ParsingRule rule = new ParsingRule(this, name, null, e);
					this.setRule(name, rule);
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
			}
		}
		this.verifyRules();
	}

	protected final ParsingExpression P(String ruleName) {
		return newNonTerminal(ruleName);
	}

	protected final ParsingExpression t(String token) {
		return ParsingExpression.newString(token);
	}

	protected final ParsingExpression c(String text) {
		return ParsingExpression.newCharacter(text);
	}

	protected ParsingExpression Any() {
		return ParsingExpression.newAny(".");
	}
	
	protected final ParsingExpression Sequence(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newSequence(l);
	}

	protected final ParsingExpression Choice(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addChoice(l, e);
		}
		return ParsingExpression.newChoice(l);
	}
	
	protected final ParsingExpression Optional(ParsingExpression ... e) {
		return ParsingExpression.newOption(Sequence(e));
	}
	
	protected final ParsingExpression ZeroMore(ParsingExpression ... e) {
		return ParsingExpression.newRepetition(Sequence(e));
	}
	
	protected final ParsingExpression Not(ParsingExpression ... e) {
		return ParsingExpression.newNot(Sequence(e));
	}

	protected final ParsingExpression And(ParsingExpression ... e) {
		return ParsingExpression.newNot(Sequence(e));
	}

	protected final ParsingExpression Constructor(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newConstructor(ParsingExpression.newSequence(l));
	}
	protected ParsingExpression LeftJoin(ParsingExpression ... elist) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[8]);
		for(ParsingExpression e : elist) {
			ParsingExpression.addSequence(l, e);
		}
		return ParsingExpression.newJoinConstructor(ParsingExpression.newSequence(l));
	}
	
	protected ParsingExpression Link(ParsingExpression ... e) {
		return ParsingExpression.newConnector(Sequence(e), -1);
	}
	
	protected ParsingExpression Link(int index, ParsingExpression ... e) {
		return ParsingExpression.newConnector(Sequence(e), index);
	}

	protected final ParsingExpression Tag(int tagId) {
		return ParsingExpression.newTagging(newTag(ParsingTag.tagName(tagId)));
	}
}

class PEG4dGrammar2 extends ParsingGrammar {
	PEG4dGrammar2() {
		super(new GrammarFactory(), "PEG4d");
		this.optimizationLevel = 0;
		this.factory.setGrammar("p4d", this);
	}

	ParsingExpression EOL() {
		return c("\\r\\n");
	}

	ParsingExpression EOT() {
		return Not(Any());
	}

	ParsingExpression S() {
		return Choice(c(" \\t\\r\\n"), t("\u3000"));
	}

	ParsingExpression DIGIT() {
		return c("0-9");
	}

	ParsingExpression LETTER() {
		return c("A-Za-z_");
	}

	ParsingExpression HEX() {
		return c("0-9A-Fa-f");
	}

	ParsingExpression W() {
		return c("A-Za-z0-9_");
	}

	ParsingExpression INT() {
		return Sequence(P("DIGIT"), ZeroMore(P("DIGIT")));
	}
	
	ParsingExpression NAME() {
		return Sequence(P("LETTER"), ZeroMore(P("W")));
	}

	ParsingExpression COMMENT() {
		return Choice(
			Sequence(t("/*"), ZeroMore(Not(t("*/")), Any()), t("*/")),
			Sequence(t("//"), ZeroMore(Not(P("EOL")), Any()), P("EOL"))
		);
	}

	ParsingExpression SPACING() {
		return ZeroMore(Choice(P("S"), P("COMMENT")));
	}
	
	ParsingExpression Integer() {
		return Constructor(P("INT"), Tag(ParsingTag.Integer));
	}

	ParsingExpression Name() {
		return Constructor(P("LETTER"), ZeroMore(P("W")), Tag(ParsingTag.Name));
	}

	ParsingExpression DotName() {
		return Constructor(P("LETTER"), ZeroMore(c("A-Za-z0-9_.")), Tag(ParsingTag.Name));
	}

	ParsingExpression HyphenName() {
		return Constructor(P("LETTER"), ZeroMore(Choice(P("W"), t("-"))), Tag(ParsingTag.Name));
	}

	ParsingExpression String() {
		ParsingExpression StringContent  = ZeroMore(Choice(
			t("\\\""), t("\\\\"), Sequence(Not(t("\"")), Any())
		));
		return Sequence(t("\""), Constructor(StringContent, Tag(ParsingTag.String)), t("\""));
	}

	ParsingExpression SingleQuotedString() {
		ParsingExpression StringContent  = ZeroMore(Choice(
			t("\\'"), t("\\\\"), Sequence(Not(t("'")), Any())
		));
		return Sequence(t("'"),  Constructor(StringContent, Tag(PEG4d.CharacterSequence)), t("'"));
	}

	ParsingExpression ValueReplacement() {
		ParsingExpression ValueContent = ZeroMore(Choice(
			t("\\`"), t("\\\\"), Sequence(Not(t("`")), Any())
		));
		return Sequence(t("`"), Constructor(ValueContent, Tag(PEG4d.Value)), t("`"));
	}

	ParsingExpression NonTerminal() {
		return Constructor(
				P("LETTER"), 
				ZeroMore(c("A-Za-z0-9_:")), 
				Tag(PEG4d.NonTerminal)
		);
	}
	
	ParsingExpression CHAR() {
		return Choice( 
			Sequence(t("\\u"), P("HEX"), P("HEX"), P("HEX"), P("HEX")),
			Sequence(t("\\x"), P("HEX"), P("HEX")),
			t("\\n"), t("\\t"), t("\\\\"), t("\\r"), t("\\v"), t("\\f"), t("\\-"), t("\\]"), 
			Sequence(Not(t("]")), Any())
		);
	}

	ParsingExpression Charset() {
		ParsingExpression _CharChunk = Sequence(
			Constructor (P("CHAR"), Tag(PEG4d.Character)), 
			Optional(
				LeftJoin(t("-"), Link(Constructor(P("CHAR"), Tag(PEG4d.Character))), Tag(ParsingTag.List))
			)
		);
		return Sequence(t("["), Constructor(ZeroMore(Link(_CharChunk)), Tag(PEG4d.Character)), t("]"));
	}

	ParsingExpression Constructor() {
		ParsingExpression ConstructorBegin = Choice(t("{"), t("<{"), t("<<"), t("8<"));
		ParsingExpression Connector  = Choice(t("@"), t("^"));
		ParsingExpression ConstructorEnd   = Choice(t("}>"), t("}"), t(">>"), t(">8"));
		return Constructor(
			ConstructorBegin, 
			Choice(
				Sequence(Connector, P("S"), Tag(PEG4d.LeftJoin)), 
				Tag(PEG4d.Constructor)
			), 
			P("_"), 
			Optional(Sequence(Link(P("Expr")), P("_"))),
			ConstructorEnd
		);
	}
	
	ParsingExpression Func() {
		return Sequence(t("<"), Constructor(
		Choice(
			Sequence(t("debug"),   P("S"), Link(P("Expr")), Tag(PEG4d.Debug)),
			Sequence(t("memo"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(PEG4d.Memo)),
			Sequence(t("match"),   P("S"), Link(P("Expr")), P("_"), t(">"), Tag(PEG4d.Match)),
			Sequence(t("fail"),   P("S"), Link(P("SingleQuotedString")), P("_"), t(">"), Tag(PEG4d.Fail)),
			Sequence(t("catch"), Tag(PEG4d.Catch)),
			Sequence(t("if"), P("S"), Optional(t("!")), Link(P("Name")), Tag(PEG4d.If)),
			Sequence(t("with"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(PEG4d.With)),
			Sequence(t("without"), P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(PEG4d.Without)),
			Sequence(t("block"), Optional(Sequence(P("S"), Link(P("Expr")))), Tag(PEG4d.Block)),
			Sequence(t("indent"), Tag(PEG4d.Indent)),
				//						Sequence(t("choice"), Tag(PEG4d.Choice)),
			Sequence(t("isa"), P("S"), Link(P("Name")), Tag(PEG4d.Isa)),
			Sequence(t("name"),  P("S"), Link(P("Name")), P("S"), Link(P("Expr")), Tag(PEG4d.Name)),
			Sequence(Optional(t("|")), t("append-choice"), Tag(PEG4d.Choice)),
			Sequence(Optional(t("|")), t("stringfy"), Tag(PEG4d.Stringfy)),
			Sequence(Optional(t("|")), t("apply"), P("S"), Link(P("Expr")), Tag(PEG4d.Apply))
		)), P("_"), t(">")
		);
	}

	ParsingExpression Term() {
		ParsingExpression _Any = Constructor(t("."), Tag(PEG4d.Any));
		ParsingExpression _Tagging = Sequence(t("#"), Constructor(c("A-Za-z0-9"), ZeroMore(c("A-Za-z0-9_.")), Tag(PEG4d.Tagging)));
		ParsingExpression _Byte = Constructor(t("0x"), P("HEX"), P("HEX"), Tag(PEG4d.Byte));
		ParsingExpression _Unicode = Constructor(t("U+"), P("HEX"), P("HEX"), P("HEX"), P("HEX"), Tag(PEG4d.Byte));
		return Choice(
			P("SingleQuotedString"), P("Charset"), P("Func"),  
			_Any, P("ValueReplacement"), _Tagging, _Byte, _Unicode,
			Sequence(t("("), P("_"), P("Expr"), P("_"), t(")")),
			P("Constructor"), P("String"), P("NonTerminal") 
		);
	}
	
	ParsingExpression SuffixTerm() {
		ParsingExpression Connector  = Choice(t("@"), t("^"));
		return Sequence(
			P("Term"), 
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
		);
	}
	
	ParsingExpression Predicate() {
		return Choice(
			Constructor(
				Choice(
					Sequence(t("&"), Tag(PEG4d.And)),
					Sequence(t("!"), Tag(PEG4d.Not)),
					Sequence(t("@["), P("_"), Link(1, P("Integer")), P("_"), t("]"), Tag(PEG4d.Connector)),							
					Sequence(t("@"), Tag(PEG4d.Connector))
				), 
				Link(0, P("SuffixTerm"))
			), 
			P("SuffixTerm")
		);
	}

	ParsingExpression NOTRULE() {
		return Not(Choice(P("Rule"), P("Import")));
	}

	ParsingExpression Sequence() {
		return Sequence(
			P("Predicate"), 
			Optional(
				LeftJoin(
					P("_"), 
					P("NOTRULE"),
					Link(P("Predicate")),
					ZeroMore(
						P("_"), 
						P("NOTRULE"),
						Link(P("Predicate"))
					),
					Tag(PEG4d.Sequence) 
				)
			)
		);
	}

	ParsingExpression Expr() {
		return Sequence(
			P("Sequence"), 
			Optional(
				LeftJoin(
					P("_"), t("/"), P("_"), 
					Link(P("Sequence")), 
					ZeroMore(
						P("_"), t("/"), P("_"), 
						Link(P("Sequence"))
					),
					Tag(PEG4d.Choice) 
				)
			)
		);
	}
		
	ParsingExpression DOC() {
		return Sequence(
			ZeroMore(Not(t("]")), Not(t("[")), Any()),
			Optional(Sequence(t("["), P("DOC"), t("]"), P("DOC") ))
		);
	}

	ParsingExpression Annotation() {
		return Sequence(
			t("["),
			Constructor(
				Link(P("HyphenName")),
				Optional(
					t(":"),  P("_"), 
					Link(Constructor(P("DOC"), Tag(ParsingTag.Text))),
					Tag(PEG4d.Annotation)
				)
			),
			t("]"),
			P("_")
		);
	}

	ParsingExpression Annotations() {
		return Constructor(
			Link(P("Annotation")),
			ZeroMore(Link(P("Annotation"))),
			Tag(ParsingTag.List) 
		);	
	}
	
	ParsingExpression Rule() {
		return Constructor(
			Link(0, Choice(P("Name"), P("String"))), P("_"), 
//			Optional(Sequence(Link(3, P("Param_")), P("_"))),
			Optional(Sequence(Link(2, P("Annotations")), P("_"))),
			t("="), P("_"), 
			Link(1, P("Expr")),
			Tag(PEG4d.Rule) 
		);
	}
	
	ParsingExpression Import() {
//		return Constructor(
//			t("import"), 
//			P("S"), 
//			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
//			Optional(Sequence(P("S"), t("as"), P("S"), Link(P("Name")))),
//			Tag(PEG4d.Import)
//		);
		return Constructor(
			t("import"), P("S"), 
			Link(P("NonTerminal")),
			ZeroMore(P("_"), t(","), P("_"),  Link(P("NonTerminal"))), P("_"), 
			t("from"), P("S"), 
			Link(Choice(P("SingleQuotedString"), P("String"), P("DotName"))), 
		Tag(PEG4d.Import)
	);
	}
	
	ParsingExpression Chunk() {
		return Sequence(
			P("_"), 
			Choice(
				P("Rule"), 
				P("Import")
			), 
			P("_"), 
			Optional(Sequence(t(";"), P("_")))
		);
	}

	ParsingExpression File() {
		return Constructor(
			P("_"), 
			ZeroMore(Link(P("Chunk"))),
			Tag(ParsingTag.List)
		);
	}

}

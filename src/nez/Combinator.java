package nez;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.util.UList;

public class Combinator {
	Grammar grammar;
	protected Combinator(Grammar grammar) {
		this.grammar = grammar;
	}
	
	public Grammar load(GrammarChecker checker) {
		Class<?> c = this.getClass();
		for(Method m : c.getDeclaredMethods()) {
			if(m.getReturnType() == Expression.class && m.getParameterCount() == 0) {
				String name = m.getName();
				//System.out.println("rule name: " + name);
				if(name.equals("SPACING")) {
					name = "_";
				}
				try {
					Expression e = (Expression)m.invoke(this);
					grammar.newRule(name, e);
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
			}
		}
		checker.verify(grammar);
		return grammar;
	}

	private SourcePosition src() {
		return null;
	}
	
	protected final Expression P(String ruleName) {
		return Factory.newNonTerminal(src(), this.grammar, ruleName);
	}

	protected final Expression t(String token) {
		return Factory.newString(src(), token);
	}

	protected final Expression c(String text) {
		return Factory.newCharSet(src(), text);
	}

	protected Expression Any() {
		return Factory.newAnyChar(src());
	}
	
	protected final Expression Sequence(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			Factory.addSequence(l, e);
		}
		return Factory.newSequence(src(), l);
	}

	protected final Expression Choice(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			Factory.addChoice(l, e);
		}
		return Factory.newChoice(src(), l);
	}
	
	protected final Expression Optional(Expression ... e) {
		return Factory.newOption(src(), Sequence(e));
	}
	
	protected final Expression ZeroMore(Expression ... e) {
		return Factory.newRepetition(src(), Sequence(e));
	}
	
	protected final Expression Not(Expression ... e) {
		return Factory.newNot(src(), Sequence(e));
	}

	protected final Expression And(Expression ... e) {
		return Factory.newAnd(src(), Sequence(e));
	}

	protected final Expression Constructor(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			Factory.addSequence(l, e);
		}
		return Factory.newNew(src(), l);
	}
	
	protected Expression LeftJoin(Expression ... elist) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression e : elist) {
			Factory.addSequence(l, e);
		}
		return Factory.newNewLeftLink(src(), l);
	}
	
	protected Expression Link(Expression ... e) {
		return Factory.newLink(src(), Sequence(e), -1);
	}
	
	protected Expression Link(int index, Expression ... e) {
		return Factory.newLink(src(), Sequence(e), index);
	}

	protected final Expression Tag(Tag t) {
		return Factory.newTagging(src(), t);
	}

}

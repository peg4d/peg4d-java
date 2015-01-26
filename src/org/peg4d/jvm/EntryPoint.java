package org.peg4d.jvm;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.UList;
import org.peg4d.UMap;
import org.peg4d.expression.PEG4dTransition;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.pegcode.GrammarVisitor;

public class EntryPoint extends ParsingExpression {
	private final String methodName;
	private Class<?> parserClass;

	public EntryPoint(String ruleName) {
		this.methodName = ruleName;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		throw new RuntimeException("TODO");
	}
	@Override
	public int inferPEG4dTranstion(UMap<String> visited) {
		throw new RuntimeException("TODO");
	}
	@Override
	public ParsingExpression checkPEG4dTransition(PEG4dTransition c) {
		throw new RuntimeException("TODO");
	}

	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> undefedFlags) {
		return this;
	}

	@Override
	public void accept(GrammarVisitor visitor) {
	}

	@Override
	public short acceptByte(int ch) {
		return 0;
	}

	@Override
	public boolean simpleMatch(ParsingContext context) {
		try {
			java.lang.reflect.Method method = parserClass.getMethod(this.methodName, ParsingContext.class);
			Object status = method.invoke(null, context);
			return ((Boolean) status).booleanValue();
		}
		catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		}
		return false;
	}

	public void setParserClass(Class<?> parserClass) {
		this.parserClass = parserClass;
	}

	@Override
	public String getInterningKey() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public ParsingExpression removePEG4dOperator() {
		return this;
	}
	@Override
	public ParsingExpression removeParsingFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
}

package org.peg4d.jvm;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeMap;

import org.peg4d.ParsingContext;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.pegcode.GrammarVisitor;

public class EntryPoint extends ParsingExpression {
	private final String methodName;
	private Class<?> parserClass;

	public EntryPoint(String ruleName) {
		this.methodName = ruleName;
	}

	@Override
	public ParsingExpression norm(boolean lexOnly, TreeMap<String, String> withoutMap) {
		return this;
	}

	@Override
	public void visit(GrammarVisitor visitor) {
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
}

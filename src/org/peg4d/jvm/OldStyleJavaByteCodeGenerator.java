package org.peg4d.jvm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.peg4d.Grammar;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingRule;
import org.peg4d.ParsingSource;
import org.peg4d.ParsingTag;
import org.peg4d.ParsingUtils;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingApply;
import org.peg4d.expression.ParsingAssert;
import org.peg4d.expression.ParsingBlock;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingCatch;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingName;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;
import org.peg4d.jvm.ClassBuilder;
import org.peg4d.jvm.InvocationTarget;

import static org.peg4d.jvm.InvocationTarget.*;

import org.peg4d.jvm.Methods;
import org.peg4d.jvm.UserDefinedClassLoader;
import org.peg4d.jvm.ClassBuilder.MethodBuilder;
import org.peg4d.jvm.ClassBuilder.VarEntry;
import org.peg4d.pegcode.GrammarFormatter;

public class OldStyleJavaByteCodeGenerator extends GrammarFormatter implements Opcodes {
	private final static String packagePrefix = "org/peg4d/generated/";

	private static int nameSuffix = -1;

	private ClassBuilder cBuilder;

	/**
	 * current method builder
	 */
	private MethodBuilder mBuilder;

	/**
	 * represents argument (ParsingContext ctx)
	 */
	private VarEntry entry_context;

	// invocation target
	private InvocationTarget target_byteAt          = newVirtualTarget(ParsingSource.class, int.class, "byteAt", long.class);
	private InvocationTarget target_consume         = newVirtualTarget(ParsingContext.class, void.class, "consume", int.class);
	private InvocationTarget target_getPosition     = newVirtualTarget(ParsingContext.class, long.class, "getPosition");
	private InvocationTarget target_rollback        = newVirtualTarget(ParsingContext.class, void.class, "rollback", long.class);
	private InvocationTarget target_markLogStack = newVirtualTarget(ParsingContext.class, int.class, "markLogStack");
	private InvocationTarget target_abortLog        = newVirtualTarget(ParsingContext.class, void.class, "abortLog", int.class);
	private InvocationTarget target_rememberFailure = newVirtualTarget(ParsingContext.class, long.class, "rememberFailure");
	private InvocationTarget target_forgetFailure   = newVirtualTarget(ParsingContext.class, void.class, "forgetFailure", long.class);
	private InvocationTarget target_lazyLink = 
			newVirtualTarget(ParsingContext.class, void.class, "lazyLink", ParsingObject.class, int.class, ParsingObject.class);
	private InvocationTarget target_setFlag = 
			newVirtualTarget(ParsingContext.class, void.class, "setFlag", String.class, boolean.class);
	private InvocationTarget target_getFlag         = newVirtualTarget(ParsingContext.class, boolean.class, "getFlag", String.class);


	/**
	 * 
	 * @param ruleName
	 * @return
	 * if ruleName is java identifier, return it,
	 * if ruleName is not java identifier, return replaced rule name.
	 */
	private String checkAndReplaceRuleName(String ruleName) {
		StringBuilder sBuilder = new StringBuilder();
		final int size = ruleName.length();
		for(int i = 0; i < size; i++) {
			char ch = ruleName.charAt(i);
			if(i == 0 && !Character.isJavaIdentifierStart(ch)) {
				sBuilder.append("Rule_");
				sBuilder.append((int) ch);
				continue;
			}
			if(!Character.isJavaIdentifierPart(ch)) {
				sBuilder.append('_');
				sBuilder.append((int) ch);
			}
			else {
				sBuilder.append(ch);
			}
		}
		return sBuilder.toString();
	}

	@Override
	public String getDesc() {
		return "JVM ";
	}

	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			this.formatRule(r.ruleName, r.expr);
		}
		this.formatFooter();
	}

	@Override
	public void formatHeader() {
		this.cBuilder = new ClassBuilder(packagePrefix + "GeneratedParser" + ++nameSuffix, null, null, null);
	}

	@Override
	public void formatFooter() {
		this.cBuilder.visitEnd();	// finalize class builder
	}

	private void formatRule(String ruleName, ParsingExpression e) { // not use string builder
		String methodName = this.checkAndReplaceRuleName(ruleName);

		/**
		 * create new method builder. 
		 * ex. FILE ->  public static boolean FILE(ParsingContext ctx)
		 */
		this.mBuilder = this.cBuilder.newMethodBuilder(ACC_PUBLIC | ACC_STATIC, boolean.class, methodName, ParsingContext.class);

		// initialize
		this.mBuilder.enterScope(); // enter block scope
		this.entry_context = this.mBuilder.defineArgument(ParsingContext.class);	// represent first argument of generating method

		// generate method body
		e.visit(this);

		// finalize
		this.mBuilder.exitScope();
		this.mBuilder.returnValue(); // return stack top value (must be boolean type)
		this.mBuilder.endMethod();
	}

	/**
	 * finalize class builder and generate class form byte code
	 * @return
	 * generated parser class
	 */
	public Class<?> generateClass() {
		UserDefinedClassLoader loader = new UserDefinedClassLoader();
		return loader.definedAndLoadClass(this.cBuilder.getInternalName(), cBuilder.toByteArray());
	}

	// helper method.
	private void generateFailure() { // generate equivalent code to ParsingContext#failure
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// if cond
		this.getFieldOfContext("pos", long.class);
		this.getFieldOfContext("fpos", long.class);

		this.mBuilder.ifCmp(Type.LONG_TYPE, GeneratorAdapter.GT, thenLabel);

		// else
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.pushNull();
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.dup();
		this.mBuilder.getField(Type.getType(ParsingContext.class), "pos", Type.getType(long.class));
		this.mBuilder.putField(Type.getType(ParsingContext.class), "fpos", Type.getType(long.class));

		//merge
		this.mBuilder.mark(mergeLabel);
	}

	/**
	 * generate code of access ParsingContext field and put field value at stack top
	 * @param fieldName
	 * @param fieldClass
	 */
	private void getFieldOfContext(String fieldName, Class<?> fieldClass) {
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.getField(Type.getType(ParsingContext.class), fieldName, Type.getType(fieldClass));
	}

	private void newParsingTag(String tagName) {
		Type typeDesc = Type.getType(ParsingTag.class);
		this.mBuilder.newInstance(typeDesc);
		this.mBuilder.dup();
		this.mBuilder.push(tagName);	// push tag name
		this.mBuilder.invokeConstructor(typeDesc, Methods.constructor(String.class));
	}

	// visitor api
	@Override
	public void visitNonTerminal(NonTerminal e) {
		Method methodDesc = Methods.method(boolean.class, this.checkAndReplaceRuleName(e.ruleName), ParsingContext.class);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.invokeStatic(this.cBuilder.getTypeDesc(), methodDesc);
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
		this.mBuilder.push(true);
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		this.generateFailure();
		this.mBuilder.push(false);
	}

	@Override
	public void visitByte(ParsingByte e) {
		// generate if cond
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// generate byteAt

		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInvocationTarget(this.target_byteAt);

		// push byteChar
		this.mBuilder.push(e.byteChar);
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.NE, elseLabel);

		// generate if block
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.push((int) 1);
		this.mBuilder.callInvocationTarget(this.target_consume);
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		// generate else block
		this.mBuilder.mark(elseLabel);
		this.generateFailure();
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		this.mBuilder.enterScope();

		// generate byteAt
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInvocationTarget(this.target_byteAt);

		// generate variable
		VarEntry entry_ch = this.mBuilder.createNewVarAndStore(int.class);

		Label andRightLabel = this.mBuilder.newLabel();
		Label thenLabel = this.mBuilder.newLabel();
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// and left
		this.mBuilder.push(e.startByteChar);
		this.mBuilder.loadFromVar(entry_ch);
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.LE, andRightLabel);
		this.mBuilder.goTo(elseLabel);

		// and right
		this.mBuilder.mark(andRightLabel);
		this.mBuilder.loadFromVar(entry_ch);
		this.mBuilder.push(e.endByteChar);
		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.LE, thenLabel);
		this.mBuilder.goTo(elseLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.push(1);
		this.mBuilder.callInvocationTarget(this.target_consume);
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		// else 
		this.mBuilder.mark(elseLabel);
		this.generateFailure();
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitString(ParsingString e) {	//FIXME:
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);

		// init utf8 array
		final int size = e.utf8.length;
		this.mBuilder.push(size);
		this.mBuilder.newArray(Type.getType(byte.class));
		for(int i = 0; i < size; i++) {
			this.mBuilder.dup();
			this.mBuilder.push(i);
			this.mBuilder.push(e.utf8[i]);
			this.mBuilder.arrayStore(Type.getType(byte.class));
		}

		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.mBuilder.callInstanceMethod(ParsingSource.class, boolean.class, "match", long.class, byte[].class);
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, thenLabel);

		// else
		this.generateFailure();
		this.mBuilder.push(false);
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.push(size);
		this.mBuilder.callInvocationTarget(this.target_consume);
		this.mBuilder.push(true);

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visitAny(ParsingAny e) {
		this.getFieldOfContext("source", ParsingSource.class);
		this.getFieldOfContext("pos", long.class);
		this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "charAt", long.class);

		this.mBuilder.push(-1);

		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.mBuilder.ifCmp(Type.INT_TYPE, GeneratorAdapter.NE, thenLabel);

		// else 
		this.generateFailure();
		this.mBuilder.push(false);
		this.mBuilder.goTo(mergeLabel);

		// then
		{
			this.mBuilder.mark(thenLabel);
			this.mBuilder.enterScope();

			this.getFieldOfContext("source", ParsingSource.class);
			this.getFieldOfContext("pos", long.class);
			this.mBuilder.callInstanceMethod(ParsingSource.class, int.class, "charLength", long.class);
			VarEntry entry_len = this.mBuilder.createNewVarAndStore(int.class);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_len);
			this.mBuilder.callInvocationTarget(this.target_consume);
			this.mBuilder.push(true);

			this.mBuilder.exitScope();
		}

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		this.getFieldOfContext("left", ParsingObject.class);
		this.newParsingTag(e.tag.toString());
		this.mBuilder.callInstanceMethod(ParsingObject.class, void.class, "setTag", ParsingTag.class);
		this.mBuilder.push(true);
	}

	@Override
	public void visitValue(ParsingValue e) {
		this.getFieldOfContext("left", ParsingObject.class);

		this.mBuilder.push(e.value);
		this.mBuilder.callInstanceMethod(ParsingObject.class, void.class, "setValue", Object.class);
		this.mBuilder.push(true);
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

//	@Override
//	public void visitUnary(ParsingUnary e) {
//		throw new RuntimeException("unimplemented visit method: " + e.getClass());
//	}

	@Override
	public void visitNot(ParsingNot e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_rememberFailure);
		VarEntry entry_f = this.mBuilder.createNewVarAndStore(long.class);

		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		// if cond
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		e.inner.visit(this);
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, thenLabel);

		// else
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_f);
		this.mBuilder.callInvocationTarget(this.target_forgetFailure);

		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.loadFromVar(entry_left);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);
		this.generateFailure();
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		this.mBuilder.enterScope();

		// generate getPosition
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);

		// generate variable
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		e.inner.visit(this);
//		this.mBuilder.pop();

		// generate rollback
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);
		
//		// generate isFailure
//		this.mBuilder.loadFromVar(this.entry_context);
//		this.mBuilder.callInvocationTarget(this.target_isFailure);
//		this.mBuilder.not();

		this.mBuilder.exitScope();
	}

	@Override
	public void visitOptional(ParsingOption e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_rememberFailure);
		VarEntry entry_f = this.mBuilder.createNewVarAndStore(long.class);

		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		// if cond
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		e.inner.visit(this);
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, thenLabel);

		// else
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_left);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_f);
		this.mBuilder.callInvocationTarget(this.target_forgetFailure);

		// merge
		this.mBuilder.mark(mergeLabel);
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(true);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.push((long)-1);
		VarEntry entry_ppos = this.mBuilder.createNewVarAndStore(long.class);

		this.mBuilder.loadFromVar(entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		Label continueLabel = this.mBuilder.newLabel();
		Label breakLabel = this.mBuilder.newLabel();
		Label whileBlockLabel = this.mBuilder.newLabel();

		// while continue
		this.mBuilder.mark(continueLabel);
		// while cond
		this.mBuilder.loadFromVar(entry_ppos);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.ifCmp(Type.LONG_TYPE, GeneratorAdapter.LT, whileBlockLabel);
		this.mBuilder.goTo(breakLabel);

		// while then
		{
			this.mBuilder.mark(whileBlockLabel);
			this.mBuilder.enterScope();

			this.getFieldOfContext("left", ParsingObject.class);
			VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

			Label thenLabel = this.mBuilder.newLabel();
			Label mergeLabel = this.mBuilder.newLabel();

			e.inner.visit(this);
			this.mBuilder.push(true);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, thenLabel);
			this.mBuilder.goTo(mergeLabel);

			// then
			this.mBuilder.mark(thenLabel);
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

			this.mBuilder.pushNull();
			this.mBuilder.storeToVar(entry_left);
			this.mBuilder.goTo(breakLabel);

			// merge
			this.mBuilder.mark(mergeLabel);
			this.mBuilder.loadFromVar(entry_pos);
			this.mBuilder.storeToVar(entry_ppos);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.callInvocationTarget(this.target_getPosition);
			this.mBuilder.storeToVar(entry_pos);

			this.mBuilder.pushNull();
			this.mBuilder.storeToVar(entry_left);

			this.mBuilder.exitScope();
			this.mBuilder.goTo(continueLabel);
		}

		// break
		this.mBuilder.mark(breakLabel);
		this.mBuilder.push(true);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		this.mBuilder.enterScope();

		// variable
		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_markLogStack);
		VarEntry entry_mark = this.mBuilder.createNewVarAndStore(int.class);

		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		// if cond
		e.inner.visit(this);
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, thenLabel);

		// else
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_mark);
		this.mBuilder.callInvocationTarget(this.target_abortLog);
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(false);
		this.mBuilder.goTo(mergeLabel);

		// then
		{
			this.mBuilder.mark(thenLabel);

			Label thenLabel2 = this.mBuilder.newLabel();
			Label mergeLabel2 = this.mBuilder.newLabel();

			this.getFieldOfContext("left", ParsingObject.class);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.ifCmp(Type.getType(Object.class), GeneratorAdapter.NE, thenLabel2);
			this.mBuilder.goTo(mergeLabel2);

			// then2
			this.mBuilder.mark(thenLabel2);
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_mark);
			this.getFieldOfContext("left", ParsingObject.class);
			this.mBuilder.callInstanceMethod(ParsingContext.class, void.class, "commitLog", int.class, ParsingObject.class);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.push(e.index);
			this.getFieldOfContext("left", ParsingObject.class);
			this.mBuilder.callInvocationTarget(this.target_lazyLink);

			// merge2
			this.mBuilder.mark(mergeLabel2);
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
//			this.mBuilder.pushNull();
//			this.mBuilder.storeToVar(entry_left);
			this.mBuilder.push(true);
		}

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitExport(ParsingExport e) {	//TODO:
		this.mBuilder.push(true);
	}

//	@Override
//	public void visitList(ParsingList e) {
//		throw new RuntimeException("unimplemented visit method: " + e.getClass());
//	}

	@Override
	public void visitSequence(ParsingSequence e) {
		this.mBuilder.enterScope();

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		VarEntry entry_pos = this.mBuilder.createNewVarAndStore(long.class);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_markLogStack);
		VarEntry entry_mark = this.mBuilder.createNewVarAndStore(int.class);

		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
			this.mBuilder.push(true);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, thenLabel);
		}

		this.mBuilder.push(true);
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_mark);
		this.mBuilder.callInvocationTarget(this.target_abortLog);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_pos);
		this.mBuilder.callInvocationTarget(this.target_rollback);
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		this.mBuilder.enterScope();

		// generate context.rememberFailure and store to f
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_rememberFailure);
		VarEntry entry_f = this.mBuilder.createNewVarAndStore(long.class);

		// generate context.left and store to left
		this.getFieldOfContext("left", ParsingObject.class);
		VarEntry entry_left = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		for(int i = 0; i < e.size(); i++) {
			// store to context.left
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_left);
			this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));

			e.get(i).visit(this);
			this.mBuilder.push(true);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, thenLabel);
		}

//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(false);
		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_f);
		this.mBuilder.callInvocationTarget(this.target_forgetFailure);
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_left);
		this.mBuilder.push(true);

		// merge
		this.mBuilder.mark(mergeLabel);
		this.mBuilder.exitScope();
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		this.mBuilder.enterScope();

		// variable
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		VarEntry entry_startIndex = this.mBuilder.createNewVarAndStore(long.class);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_markLogStack);
		VarEntry entry_mark = this.mBuilder.createNewVarAndStore(int.class);

		// new parsingObject
		Type parsingObjectTypeDesc = Type.getType(ParsingObject.class);
		this.mBuilder.newInstance(parsingObjectTypeDesc);
		this.mBuilder.dup();
		this.newParsingTag("Text");
		this.getFieldOfContext("source", ParsingSource.class);
		
		// call objectId
		this.mBuilder.loadFromVar(entry_startIndex);
		this.mBuilder.push(e.uniqueId);
		this.mBuilder.callStaticMethod(ParsingUtils.class, 
				long.class, "objectId", long.class, short.class);

		this.mBuilder.invokeConstructor(parsingObjectTypeDesc, 
				Methods.constructor(ParsingTag.class, ParsingSource.class, long.class));
		VarEntry entry_newnode = this.mBuilder.createNewVarAndStore(ParsingObject.class);

		if(e.leftJoin) {
			this.mBuilder.loadFromVar(this.entry_context);
			this.getFieldOfContext("left", ParsingObject.class);
			this.mBuilder.callInstanceMethod(ParsingContext.class, void.class, "lazyJoin", ParsingObject.class);
	
			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.loadFromVar(entry_newnode);
			this.mBuilder.push(0);
			this.getFieldOfContext("left", ParsingObject.class);
			this.mBuilder.callInvocationTarget(this.target_lazyLink);
		}

		// put to context.left
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_newnode);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));


		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		for(int i = 0; i < e.size(); i++) {	// only support prefetchIndex = 0
			e.get(i).visit(this);
			this.mBuilder.push(true);
			this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, thenLabel);
		}
		// else
		this.mBuilder.loadFromVar(entry_newnode);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.callInvocationTarget(this.target_getPosition);
		this.mBuilder.loadFromVar(entry_startIndex);
		this.mBuilder.math(GeneratorAdapter.SUB, Type.LONG_TYPE);
		this.mBuilder.cast(Type.LONG_TYPE, Type.INT_TYPE);

		this.mBuilder.callInstanceMethod(ParsingObject.class, void.class, "setLength", int.class);

		// put to context.left
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_newnode);
		this.mBuilder.putField(Type.getType(ParsingContext.class), "left", Type.getType(ParsingObject.class));
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_newnode);
		this.mBuilder.push(true);

		this.mBuilder.goTo(mergeLabel);

		// then
		this.mBuilder.mark(thenLabel);
		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_mark);
		this.mBuilder.callInvocationTarget(this.target_abortLog);

		this.mBuilder.loadFromVar(this.entry_context);
		this.mBuilder.loadFromVar(entry_startIndex);
		this.mBuilder.callInvocationTarget(this.target_rollback);
//		this.mBuilder.pushNull();
//		this.mBuilder.storeToVar(entry_newnode);
		this.mBuilder.push(false);

		// merge
		this.mBuilder.mark(mergeLabel);

		this.mBuilder.exitScope();
	}

//	@Override
//	public void visitParsingFunction(ParsingFunction parsingFunction) {
//		if(parsingFunction instanceof ParsingIf) {
//			this.visitParsingIfFlag((ParsingIf) parsingFunction);
//		}
//		else {
//			throw new RuntimeException("unimplemented visit method: " + parsingFunction.getClass());
//		}
//	}

//	@Override
//	public void visitParsingOperation(ParsingOperation e) {
//		if(e instanceof ParsingWithFlag) {
//			this.visitWithFlag((ParsingWithFlag) e);
//		} else if(e instanceof ParsingWithoutFlag) {
//			this.visitWithoutFlag((ParsingWithoutFlag) e);
//		} else {
//			throw new RuntimeException("unimplemented visit method: " + e.getClass());
//		}
//	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		final String flagName = e.getParameters().substring(1);
		if(ParsingIf.OldFlag) {
			this.mBuilder.enterScope();

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.push(flagName);
			this.mBuilder.callInvocationTarget(this.target_getFlag);
			VarEntry entry_currentFlag = this.mBuilder.createNewVarAndStore(boolean.class);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.push(flagName);
			this.mBuilder.push(true);
			this.mBuilder.callInvocationTarget(this.target_setFlag);

			e.inner.visit(this);
			VarEntry entry_res = this.mBuilder.createNewVarAndStore(boolean.class);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.push(flagName);
			this.mBuilder.loadFromVar(entry_currentFlag);
			this.mBuilder.callInvocationTarget(this.target_setFlag);

			this.mBuilder.loadFromVar(entry_res);

			this.mBuilder.exitScope();
			return;
		}
		e.inner.visit(this);
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		final String flagName = e.getParameters().substring(1);
		if(ParsingIf.OldFlag) {
			this.mBuilder.enterScope();

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.push(flagName);
			this.mBuilder.callInvocationTarget(this.target_getFlag);
			VarEntry entry_currentFlag = this.mBuilder.createNewVarAndStore(boolean.class);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.push(flagName);
			this.mBuilder.push(false);
			this.mBuilder.callInvocationTarget(this.target_setFlag);

			e.inner.visit(this);
			VarEntry entry_res = this.mBuilder.createNewVarAndStore(boolean.class);

			this.mBuilder.loadFromVar(this.entry_context);
			this.mBuilder.push(flagName);
			this.mBuilder.loadFromVar(entry_currentFlag);
			this.mBuilder.callInvocationTarget(this.target_setFlag);

			this.mBuilder.loadFromVar(entry_res);

			this.mBuilder.exitScope();
			return;
		}
		e.inner.visit(this);
	}

	public void visitParsingIfFlag(ParsingIf e) {
		if(ParsingIf.OldFlag) {
			this.mBuilder.enterScope();

			//TODO:

			this.mBuilder.exitScope();
			return;
		}
		this.mBuilder.push(true);
	}

	@Override
	public void visitRule(ParsingRule e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitName(ParsingName e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitApply(ParsingApply e) {
		// TODO Auto-generated method stub
		
	}
}

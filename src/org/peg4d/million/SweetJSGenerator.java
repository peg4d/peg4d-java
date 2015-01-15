package org.peg4d.million;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.HTML.Tag;
import javax.xml.soap.Node;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingTag;

public class SweetJSGenerator extends SourceGenerator {
	private static boolean UseExtend;

	public SweetJSGenerator() {
		SweetJSGenerator.UseExtend = false;
	}

/*
	public void visitThrow(ParsingObject node) {
		this.currentBuilder.append("throw ");
		this.visit(node.get(0));
	}
	*/
	
	protected int getOperatorPrecedence(int tagId){
		if(tagId == MillionTag.TAG_INTEGER) return 0;
		if(tagId == MillionTag.TAG_BINARY_INTEGER) return 0;
		if(tagId == MillionTag.TAG_OCTAL_INTEGER) return 0;
		if(tagId == MillionTag.TAG_HEX_INTEGER) return 0;
		if(tagId == MillionTag.TAG_LONG) return 0;
		if(tagId == MillionTag.TAG_BINARY_LONG) return 0;
		if(tagId == MillionTag.TAG_OCTAL_LONG) return 0;
		if(tagId == MillionTag.TAG_HEX_LONG) return 0;
		if(tagId == MillionTag.TAG_FLOAT) return 0;
		if(tagId == MillionTag.TAG_HEX_FLOAT) return 0;
		if(tagId == MillionTag.TAG_DOUBLE) return 0;
		if(tagId == MillionTag.TAG_HEX_DOUBLE) return 0;
		if(tagId == MillionTag.TAG_STRING) return 0;
		if(tagId == MillionTag.TAG_REGULAR_EXP) return 0;
		if(tagId == MillionTag.TAG_NULL) return 0;
		if(tagId == MillionTag.TAG_TRUE) return 0;
		if(tagId == MillionTag.TAG_FALSE) return 0;
		if(tagId == MillionTag.TAG_THIS) return 0;
		if(tagId == MillionTag.TAG_SUPER) return 0;
		if(tagId == MillionTag.TAG_NAME) return 0;
		if(tagId == MillionTag.TAG_ARRAY) return 0;
		if(tagId == MillionTag.TAG_HASH) return 0;
		if(tagId == MillionTag.TAG_TYPE) return 0;
		if(tagId == MillionTag.TAG_SUFFIX_INC) return 2;
		if(tagId == MillionTag.TAG_SUFFIX_DEC) return 2;
		if(tagId == MillionTag.TAG_PREFIX_INC) return 2;
		if(tagId == MillionTag.TAG_PREFIX_DEC) return 2;
		if(tagId == MillionTag.TAG_PLUS) return 4;
		if(tagId == MillionTag.TAG_MINUS) return 4;
		if(tagId == MillionTag.TAG_COMPL) return 4;
		if(tagId == MillionTag.TAG_ADD) return 6;
		if(tagId == MillionTag.TAG_SUB) return 6;
		if(tagId == MillionTag.TAG_MUL) return 5;
		if(tagId == MillionTag.TAG_DIV) return 5;
		if(tagId == MillionTag.TAG_MOD) return 5;
		if(tagId == MillionTag.TAG_LEFT_SHIFT) return 7;
		if(tagId == MillionTag.TAG_RIGHT_SHIFT) return 7;
		if(tagId == MillionTag.TAG_LOGICAL_LEFT_SHIFT) return 7;
		if(tagId == MillionTag.TAG_LOGICAL_RIGHT_SHIFT) return 7;
		if(tagId == MillionTag.TAG_GREATER_THAN) return 8;
		if(tagId == MillionTag.TAG_GREATER_THAN_EQUALS) return 8;
		if(tagId == MillionTag.TAG_LESS_THAN) return 8;
		if(tagId == MillionTag.TAG_LESS_THAN_EQUALS) return 8;
		if(tagId == MillionTag.TAG_EQUALS) return 9;
		if(tagId == MillionTag.TAG_NOT_EQUALS) return 9;
		if(tagId == MillionTag.TAG_STRICT_EQUALS) return 9;
		if(tagId == MillionTag.TAG_STRICT_NOT_EQUALS) return 9;
		//if(tagId == MillionTag.TAG_COMPARE) return 9;
		//if(tagId == MillionTag.TAG_INSTANCE_OF) return 8;
		if(tagId == MillionTag.TAG_STRING_INSTANCE_OF) return 8;
		if(tagId == MillionTag.TAG_HASH_IN) return 8;
		if(tagId == MillionTag.TAG_BITWISE_AND) return 10;
		if(tagId == MillionTag.TAG_BITWISE_OR) return 12;
		if(tagId == MillionTag.TAG_BITWISE_NOT) return 4;
		if(tagId == MillionTag.TAG_BITWISE_XOR) return 11;
		if(tagId == MillionTag.TAG_LOGICAL_AND) return 13;
		if(tagId == MillionTag.TAG_LOGICAL_OR) return 14;
		if(tagId == MillionTag.TAG_LOGICAL_NOT) return 4;
		//if(tagId == MillionTag.TAG_LOGICAL_XOR) return 14;
		if(tagId == MillionTag.TAG_CONDITIONAL) return 16;
		if(tagId == MillionTag.TAG_ASSIGN) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_ADD) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_SUB) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_MUL) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_DIV) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_MOD) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_LEFT_SHIFT) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_RIGHT_SHIFT) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_LOGICAL_LEFT_SHIFT) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_LOGICAL_RIGHT_SHIFT) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_BITWISE_AND) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_BITWISE_OR) return 17;
		if(tagId == MillionTag.TAG_ASSIGN_BITWISE_XOR) return 17;
		//if(tagId == MillionTag.TAG_ASSIGN_LOGICAL_AND) return 0;
		//if(tagId == MillionTag.TAG_ASSIGN_LOGICAL_OR) return 0;
		//if(tagId == MillionTag.TAG_ASSIGN_LOGICAL_XOR) return 0;
		//if(tagId == MillionTag.TAG_MULTIPLE_ASSIGN) return 0;
		if(tagId == MillionTag.TAG_COMMA) return 18;
		//if(tagId == MillionTag.TAG_CONCAT) return 4;
		if(tagId == MillionTag.TAG_FIELD) return 1;
		if(tagId == MillionTag.TAG_INDEX) return 1;
		if(tagId == MillionTag.TAG_MULTI_INDEX) return 1;
		if(tagId == MillionTag.TAG_APPLY) return 2;
		if(tagId == MillionTag.TAG_METHOD) return 2;
		if(tagId == MillionTag.TAG_TYPE_OF) return 2;
		if(tagId == MillionTag.TAG_NEW) return 1;
		return Integer.MAX_VALUE;
	}
	
	protected boolean shouldExpressionBeWrapped(ParsingObject node){
		int precedence = getOperatorPrecedence(node.getTag().getId());
		if(precedence == 0){
			return false;
		}else{
			ParsingObject parent = node.getParent();
			if(parent != null && getOperatorPrecedence(parent.getTag().getId()) >= precedence){
				return false;
			}
		} 
		return true;
	}
	
	protected void generateExpression(ParsingObject node){
		if(this.shouldExpressionBeWrapped(node)){
			this.visit(node, '(', ')');
		}else{
			this.visit(node);
		}
	}
	
	protected void genaratePrefixUnary(ParsingObject node, String operator){
		this.currentBuilder.append(operator);
		generateExpression(node.get(0));
	}

	protected void genarateSuffixUnary(ParsingObject node, String operator){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator);
	}

	protected void generateBinary(ParsingObject node, String operator){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator);
		generateExpression(node.get(1));
	}
	
	protected void generateTrinary(ParsingObject node, String operator1, String operator2){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator1);
		generateExpression(node.get(1));
		this.currentBuilder.append(operator2);
		generateExpression(node.get(2));
	}
	
	protected void generateList(List<ParsingObject> node, String delim){
		boolean isFirst = true;
		for(ParsingObject element : node){
			if(!isFirst){
				this.currentBuilder.append(delim);
			}else{
				isFirst = false;
			}
			this.visit(element);
		}
	}
	
	protected void generateList(List<ParsingObject> node, String begin, String delim, String end){
		this.currentBuilder.append(begin);
		this.generateList(node, delim);
		this.currentBuilder.append(end);
	}
	
	public void visitSource(ParsingObject node) {
		this.visitBlock(node);
	}
	
	public void visitName(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitDecimalInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitOctalInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitHexInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitLong(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitDecimalLong(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitOctalLong(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitHexLong(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitFloat(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}

	public void visitDouble(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitHexFloat(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}

	public void visitHexDouble(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitString(ParsingObject node) {
		this.currentBuilder.appendChar('"');
		this.currentBuilder.append(node.getText());
		this.currentBuilder.appendChar('"');
	}
	
	public void visitRegularExp(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitText(ParsingObject node) {
		/* do nothing */
	}
	
	public void visitThis(ParsingObject node) {
		this.currentBuilder.append("this");
	}
	
	public void visitTrue(ParsingObject node) {
		this.currentBuilder.append("true");
	}
	
	public void visitFalse(ParsingObject node) {
		this.currentBuilder.append("false");
	}
	
	public void visitNull(ParsingObject node) {
		this.currentBuilder.append("null");
	}
	
	public void visitList(ParsingObject node) {
		generateList(node, ", ");
	}
	
	public void visitBlock(ParsingObject node) {
		for(ParsingObject element : node){
			this.currentBuilder.appendNewLine();
			this.visit(element);
			if(!this.currentBuilder.endsWith(';') && !this.currentBuilder.endsWith('}')){
				this.currentBuilder.appendChar(';');
			}
		}
	}
	
	public void generateBlock(ParsingObject node) {
		this.currentBuilder.appendChar('{');
		this.currentBuilder.indent();
		this.visit(node);
		this.currentBuilder.unIndent();
		this.currentBuilder.appendNewLine("}");
	}
	
	public void visitArray(ParsingObject node){
		this.currentBuilder.appendChar('[');
		this.generateList(node, ", ");
		this.currentBuilder.appendChar(']');
	}
	
	@Deprecated
	public void visitObject(ParsingObject node){
		this.currentBuilder.appendChar('{');
		if(node.size() < 2){
			this.generateList(node, ", ");
		}else{
			this.currentBuilder.indent();
			boolean isFirst = true;
			for(ParsingObject element : node){
				if(!isFirst){
					this.currentBuilder.appendChar(',');
				}else{
					isFirst = false;
				}
				this.currentBuilder.appendNewLine();
				this.visit(element);
				
			}
			this.currentBuilder.unIndent();
		}
		this.currentBuilder.appendChar('}');
	}
	
	public void visitProperty(ParsingObject node) {
		this.generateBinary(node, ": ");
	}

	public void visitSuffixInc(ParsingObject node) {
		this.genarateSuffixUnary(node, "++");
	}

	public void visitSuffixDec(ParsingObject node) {
		this.genarateSuffixUnary(node, "--");
	}

	public void visitPrefixInc(ParsingObject node) {
		this.genaratePrefixUnary(node, "++");
	}

	public void visitPrefixDec(ParsingObject node) {
		this.genaratePrefixUnary(node, "--");
	}

	public void visitPlus(ParsingObject node) {
		this.visit(node.get(0));
	}

	public void visitMinus(ParsingObject node) {
		this.genaratePrefixUnary(node, "-");
	}

	public void visitAdd(ParsingObject node) {
		this.generateBinary(node, "+");
	}

	public void visitSub(ParsingObject node) {
		this.generateBinary(node, "-");
	}

	public void visitMul(ParsingObject node) {
		this.generateBinary(node, "*");
	}

	public void visitDiv(ParsingObject node) {
		this.generateBinary(node, "/");
	}

	public void visitMod(ParsingObject node) {
		this.generateBinary(node, "%");
	}

	public void visitLeftShift(ParsingObject node) {
		this.generateBinary(node, "<<");
	}

	public void visitRightShift(ParsingObject node) {
		this.generateBinary(node, ">>");
	}

	public void visitLogicalLeftShift(ParsingObject node) {
		this.generateBinary(node, "<<");
	}

	public void visitLogicalRightShift(ParsingObject node) {
		this.generateBinary(node, ">>>");
	}

	public void visitGreaterThan(ParsingObject node) {
		this.generateBinary(node, ">");
	}

	public void visitGreaterThanEquals(ParsingObject node) {
		this.generateBinary(node, ">=");
	}

	public void visitLessThan(ParsingObject node) {
		this.generateBinary(node, "<");
	}

	public void visitLessThanEquals(ParsingObject node) {
		this.generateBinary(node, "<=");
	}

	public void visitEquals(ParsingObject node) {
		this.generateBinary(node, "==");
	}

	public void visitNotEquals(ParsingObject node) {
		this.generateBinary(node, "!=");
	}
	
	public void visitStrictEquals(ParsingObject node) {
		this.generateBinary(node, "===");
	}

	public void visitStrictNotEquals(ParsingObject node) {
		this.generateBinary(node, "!==");
	}

	public void visitCompare(ParsingObject node) {
		this.generateBinary(node, "-");
	}
	
	public void visitInstanceOf(ParsingObject node) {
		this.currentBuilder.append("(");
		this.visit(node.get(0));
		this.currentBuilder.append(").constructor.name === ");
		this.visit(node.get(1));
		this.currentBuilder.append(".name");
	}
	
	public void visitStringInstanceOf(ParsingObject node) {
		this.generateBinary(node, " instanceof ");
	}
	
	public void visitHashIn(ParsingObject node) {
		this.generateBinary(node, " in ");
	}

	public void visitBitwiseAnd(ParsingObject node) {
		this.generateBinary(node, "&");
	}

	public void visitBitwiseOr(ParsingObject node) {
		this.generateBinary(node, "|");
	}

	public void visitBitwiseNot(ParsingObject node) {
		this.genaratePrefixUnary(node, "~");
	}

	public void visitBitwiseXor(ParsingObject node) {
		this.generateBinary(node, "^");
	}

	public void visitLogicalAnd(ParsingObject node) {
		this.generateBinary(node, "&&");
	}

	public void visitLogicalOr(ParsingObject node) {
		this.generateBinary(node, "||");
	}

	public void visitLogicalNot(ParsingObject node) {
		this.genaratePrefixUnary(node, "!");
	}

	public void visitLogicalXor(ParsingObject node) {
		this.generateBinary(node, "^");
	}

	public void visitConditional(ParsingObject node) {
		this.generateTrinary(node, "?", ":");
	}

	public void visitAssign(ParsingObject node) {
		this.generateBinary(node, "=");
	}

	public void visitMultiAssign(ParsingObject node) {
		ParsingObject lhs = node.get(0);
		ParsingObject rhs = node.get(1);
		if(lhs.size() == 1 && rhs.size() == 1 && !rhs.get(0).is(MillionTag.TAG_APPLY) && !rhs.get(0).is(MillionTag.TAG_APPLY)){
			this.visit(lhs.get(0));
			this.currentBuilder.append(" = ");
			this.visit(rhs.get(0));
		}else{
			this.currentBuilder.append("multiAssign (");
			generateList(lhs, ", ");
			this.currentBuilder.append(") = (");
			generateList(rhs, ", ");
			this.currentBuilder.appendChar(')');
		}
	}
	
	public void visitAssignAdd(ParsingObject node) {
		this.generateBinary(node, "+=");
	}

	public void visitAssignSub(ParsingObject node) {
		this.generateBinary(node, "-=");
	}

	public void visitAssignMul(ParsingObject node) {
		this.generateBinary(node, "*=");
	}

	public void visitAssignDiv(ParsingObject node) {
		this.generateBinary(node, "/=");
	}

	public void visitAssignMod(ParsingObject node) {
		this.generateBinary(node, "%=");
	}

	public void visitAssignLeftShift(ParsingObject node) {
		this.generateBinary(node, "<<=");
	}

	public void visitAssignRightShift(ParsingObject node) {
		this.generateBinary(node, ">>=");
	}

	public void visitAssignLogicalLeftShift(ParsingObject node) {
		this.generateBinary(node, "<<<=");
	}

	public void visitAssignLogicalRightShift(ParsingObject node) {
		this.generateBinary(node, ">>>=");
	}

	public void visitAssignBitwiseAnd(ParsingObject node) {
		this.generateBinary(node, "&=");
	}

	public void visitAssignBitwiseOr(ParsingObject node) {
		this.generateBinary(node, "|=");
	}

	public void visitAssignBitwiseXor(ParsingObject node) {
		this.generateBinary(node, "^=");
	}

	public void visitAssignLogicalAnd(ParsingObject node) {
		this.generateBinary(node, "&&=");
	}

	public void visitAssignLogicalOr(ParsingObject node) {
		this.generateBinary(node, "||=");
	}

	public void visitAssignLogicalXor(ParsingObject node) {
		this.generateBinary(node, "^=");
	}

//	public void visitMultipleAssign(ParsingObject node) {
//		
//	}

	public void visitComma(ParsingObject node) {
		if(node.size() > 2){
			this.currentBuilder.appendChar('(');
			this.generateList(node, ", ");
			this.currentBuilder.appendChar(')');	
		}else{
			this.generateBinary(node, ", ");
		}
	}
	
	public void visitConcat(ParsingObject node) {
		this.generateBinary(node, " + ");
	}

	public void visitField(ParsingObject node) {
		this.generateBinary(node, ".");
	}

	public void visitIndex(ParsingObject node) {
		generateExpression(node.get(0));
		this.visit(node.get(1), '[', ']');
	}

	public void visitMultiIndex(ParsingObject node) {
		generateExpression(node.get(0));
		for(ParsingObject indexNode : node.get(1)){
			this.visit(indexNode, '[', ']');
		}
	}
	
	private boolean containsVariadicValue(ParsingObject list){
		for(ParsingObject item : list){
			if(item.is(MillionTag.TAG_VARIADIC_PARAMETER)
					|| item.is(MillionTag.TAG_APPLY)
					|| item.is(MillionTag.TAG_MULTIPLE_RETURN_APPLY)
					|| item.is(MillionTag.TAG_METHOD)
					|| item.is(MillionTag.TAG_MULTIPLE_RETURN_METHOD)){
				return true;
			}
		}
		return false;
	}

	public void visitApply(ParsingObject node) {
		ParsingObject arguments = node.get(1);
		if(arguments.is(MillionTag.TAG_UNPACK_LIST) && arguments.size() > 0 && containsVariadicValue(arguments)){
			ParsingObject functionExpr = node.get(0);
			if(functionExpr.is(MillionTag.TAG_FIELD)){
				this.visit(node.get(0));
				this.currentBuilder.append(".apply(");
				this.visit(functionExpr.get(0));
				if(arguments.size() == 1 && arguments.get(0).is(MillionTag.TAG_VARIADIC_PARAMETER)){
					this.currentBuilder.append(", __variadicParams)");
				}else{
					this.generateList(arguments, ", __unpack(", ", ", "))");
				}
			}else{
				this.visit(node.get(0));
				if(arguments.size() == 1 && arguments.get(0).is(MillionTag.TAG_VARIADIC_PARAMETER)){
					this.currentBuilder.append(".apply(null, __variadicParams)");
				}else{
					this.generateList(arguments, ".apply(null, __unpack(", ", ", "))");
				}
			}
		}else{
			this.visit(node.get(0));
			this.currentBuilder.appendChar('(');
			this.generateList(arguments, ", ");
			this.currentBuilder.appendChar(')');
		}
	}

	public void visitMethod(ParsingObject node) {
		this.generateBinary(node, ".");
		this.currentBuilder.appendChar('(');
		this.generateList(node.get(2), ", ");
		this.currentBuilder.appendChar(')');
	}
	
	public void visitTypeOf(ParsingObject node) {
		this.currentBuilder.append("typeof ");
		generateExpression(node.get(0));
	}

	public void visitIf(ParsingObject node) {
		this.visit(node.get(0), "if (", ")");
		this.generateBlock(node.get(1));
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.append("else");
			this.generateBlock(node.get(2));
		}
	}

	public void visitWhile(ParsingObject node) {
		this.visit(node.get(0), "while (", ")");
		this.generateBlock(node.get(1));
	}

	public void visitFor(ParsingObject node) {
		this.visit(node.get(0), "for (", ";");
		this.visit(node.get(1));
		this.visit(node.get(2), ';', ')');
		this.generateBlock(node.get(3));
		if(!isNullOrEmpty(node, 4)){
			this.currentBuilder.appendNewLine();
			this.visit(node.get(4));
		}
	}

	public void visitForInRange(ParsingObject node) {
		ParsingObject i = node.get(0);
		ParsingObject begin = node.get(1);
		ParsingObject end = node.get(2);
		
		this.visit(i, "for (var ", " = ");
		this.visit(begin);
		this.visit(end, ", __end = ", ";");
		this.visit(i);
		this.currentBuilder.append(" < __end;");
		if(!isNullOrEmpty(node, 3)){
			ParsingObject step = node.get(3);
			this.visit(i);
			this.currentBuilder.append(" += ");
			this.visit(step, " += ", ")");
		}else{
			this.visit(i, "", "++)");
		}
		this.generateBlock(node.get(4));
		if(!isNullOrEmpty(node, 5)){
			this.currentBuilder.appendNewLine();
			this.visit(node.get(5));
		}
	}
	
	public void visitForeach(ParsingObject node) {
		this.visit(node.get(1));
		this.visit(node.get(0), ".forEach(function(", ")");
		this.generateBlock(node.get(2));
		this.currentBuilder.append(")");
		if(!isNullOrEmpty(node, 3)){
			this.currentBuilder.appendNewLine();
			this.visit(node.get(3));
		}
	}
	
	public void visitJSForeach(ParsingObject node) {
		this.visit(node.get(0), "for (", " in ");
		this.visit(node.get(1));
		this.currentBuilder.append(")");
		this.generateBlock(node.get(2));
		if(!isNullOrEmpty(node, 3)){
			this.currentBuilder.appendNewLine();
			this.visit(node.get(3));
		}
	}

	public void visitDoWhile(ParsingObject node) {
		this.currentBuilder.append("do");
		this.generateBlock(node.get(0));
		this.visit(node.get(1), "while (", ")");
	}
	
	protected void generateJump(ParsingObject node, String keyword){
		if(!isNullOrEmpty(node, 0)){
			this.currentBuilder.appendSpace();
			ParsingObject returnValue = node.get(0);
			if(returnValue.is(MillionTag.TAG_LIST)){
				this.currentBuilder.append("multiple m");
				this.currentBuilder.append(keyword);
				this.generateList(returnValue, " (", ", ", ")");
			}else{
				this.currentBuilder.append(keyword);
				this.visit(returnValue);
			}
		}
	}

	public void visitReturn(ParsingObject node) {
		this.generateJump(node, "return");
	}

	public void visitBreak(ParsingObject node) {
		this.generateJump(node, "break");
	}

	public void visitYield(ParsingObject node) {
		this.generateJump(node, "yield");
	}

	public void visitContinue(ParsingObject node) {
		this.generateJump(node, "continue");
	}

	public void visitRedo(ParsingObject node) {
		this.generateJump(node, "/*redo*/");
	}

	public void visitSwitch(ParsingObject node) {
		this.visit(node.get(0), "switch (", ")");
		
		this.currentBuilder.appendChar('{');
		this.currentBuilder.indent();
		
		for(ParsingObject element : node.get(1)){
			this.currentBuilder.appendNewLine();
			this.visit(element);
		}
		
		this.currentBuilder.unIndent();
		this.currentBuilder.appendNewLine("}");
	}

	public void visitCase(ParsingObject node) {
		this.visit(node.get(0), "case ", ":");
		if(!isNullOrEmpty(node, 1)){
			this.currentBuilder.indent();
			this.visit(node.get(1));
			this.currentBuilder.unIndent();
		}
	}

	public void visitDefault(ParsingObject node) {
		this.currentBuilder.append("default:");
		this.currentBuilder.indent();
		this.visit(node.get(0));
		this.currentBuilder.unIndent();
	}

	public void visitTry(ParsingObject node) {
		this.currentBuilder.append("try");
		this.generateBlock(node.get(0));
		
		if(!isNullOrEmpty(node, 1)){
			for(ParsingObject element : node.get(1)){
				this.visit(element);
			}
		}
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.append("finally");
			this.visit(node.get(2));
		}
	}

	public void visitCatch(ParsingObject node) {
		this.visit(node.get(0), "catch(", ")");
		this.generateBlock(node.get(1));
	}
	
	public void visitVarDeclStmt(ParsingObject node) {
		this.currentBuilder.append("var ");
		this.visit(node.get(2));
	}
	
	public void visitVarDecl(ParsingObject node) {
		this.visit(node.get(0));
		if(node.size() > 1){
			this.currentBuilder.append(" = ");
			this.visit(node.get(1));
		}
	}
	
	public void visitMultiVarDecl(ParsingObject node) {
		ParsingObject lhs = node.get(0);
		ParsingObject rhs = node.get(1);
		if(lhs.size() == 1 && rhs.size() == 1 && !rhs.get(0).is(MillionTag.TAG_APPLY) && !rhs.get(0).is(MillionTag.TAG_APPLY)){
			this.visit(lhs.get(0));
			this.currentBuilder.append(" = ");
			this.visit(rhs.get(0));
		}else{
			this.currentBuilder.append("multiAssign var (");
			generateList(lhs, ", ");
			this.currentBuilder.append(") = (");
			generateList(rhs, ", ");
			this.currentBuilder.appendChar(')');
		}
	}
	
	public void visitFuncDecl(ParsingObject node) {
		boolean mustWrap = this.currentBuilder.isStartOfLine();
		if(mustWrap){
			this.currentBuilder.appendChar('(');
		}
		this.currentBuilder.append("function");
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.appendSpace();
			this.visit(node.get(2));
		}
		ParsingObject parameters = node.get(4);
		boolean containsVariadicParameter = false;
		boolean isFirst = true;
		int sizeOfParametersBeforeValiadic = 0;
		int sizeOfParametersAfterValiadic = 0;
		
		this.currentBuilder.appendChar('(');
		for(ParsingObject param : parameters){
			if(param.is(MillionTag.TAG_VARIADIC_PARAMETER)){
				containsVariadicParameter = true;
				sizeOfParametersAfterValiadic = 0;
				continue;
			}
			if(containsVariadicParameter){
				sizeOfParametersAfterValiadic++;
			}else{
				sizeOfParametersBeforeValiadic++;
			}
			if(!isFirst){
				this.currentBuilder.append(", ");
			}
			this.visit(param);
			isFirst = false;
		}
		this.currentBuilder.appendChar(')');
		
		this.currentBuilder.appendChar('{');
		this.currentBuilder.indent();
		
		if(containsVariadicParameter){
			this.currentBuilder.appendNewLine("var __variadicParams = __markAsVariadic([]);");
			this.currentBuilder.appendNewLine("for (var _i = ");
			this.currentBuilder.appendNumber(sizeOfParametersBeforeValiadic);
			this.currentBuilder.append(", _n = arguments.length - ");
			this.currentBuilder.appendNumber(sizeOfParametersAfterValiadic);
			this.currentBuilder.append("; _i < _n; ++_i){ __variadicParams.push(arguments[_i]); }");
		}
		
		this.visit(node.get(6));
		this.currentBuilder.unIndent();
		this.currentBuilder.appendNewLine("}");
		
		if(mustWrap){
			this.currentBuilder.appendChar(')');
		}
	}
	
	public void visitDeleteProperty(ParsingObject node) {
		this.currentBuilder.append("delete ");
		this.visit(node.get(0));
	}
	
	public void visitVoidExpression(ParsingObject node) {
		this.currentBuilder.append("void(");
		this.visit(node.get(0));
		this.currentBuilder.append(")");
	}
	
	public void visitThrow(ParsingObject node) {
		this.currentBuilder.append("throw ");
		this.visit(node.get(0));
	}
	
	public void visitNew(ParsingObject node) {
		this.currentBuilder.append("new ");
		this.visit(node.get(0));
		this.currentBuilder.appendChar('(');
		if(!isNullOrEmpty(node, 1)){
			this.generateList(node.get(1), ", ");
		}
		this.currentBuilder.appendChar(')');
	}

	public void visitEmpty(ParsingObject node) {
		this.currentBuilder.appendChar(';');
	}
	
	public void visitTable(ParsingObject node) {
		boolean isAllMemberIsArrayStyle = true;
		for (ParsingObject subnode : node) {
			boolean isArrayStyleMember =
					!subnode.is(MillionTag.TAG_TABLE_PROPERTY) &&
					!subnode.is(MillionTag.TAG_TABLE_SETTER_APPLY);
			if(!isArrayStyleMember){
				isAllMemberIsArrayStyle = false;
				break;
			}
		}
		if(isAllMemberIsArrayStyle){
			this.currentBuilder.append("__unpack(");
			this.generateList(node, ", ");
			this.currentBuilder.append(")");
		}else{
			List<ParsingObject> arrayStyleMember = new ArrayList<ParsingObject>();
			List<ParsingObject> setterStyleMember = new ArrayList<ParsingObject>();
			List<ParsingObject> dictionaryStyleMember = new ArrayList<ParsingObject>();
			for (ParsingObject subnode : node) {
				if(subnode.is(MillionTag.TAG_TABLE_PROPERTY)){
					dictionaryStyleMember.add(subnode);
				}else if(subnode.is(MillionTag.TAG_TABLE_SETTER_APPLY)){
					setterStyleMember.add(subnode);
				}else{
					arrayStyleMember.add(subnode);
				}
			}
			this.currentBuilder.append("(function(){");
			this.currentBuilder.indent();
			this.currentBuilder.appendNewLine("var ret = __unpack(");
			this.generateList(arrayStyleMember, ", ");
			this.currentBuilder.append(");");
			for (ParsingObject setter : setterStyleMember) {
				this.currentBuilder.appendNewLine("ret[");
				this.visit(setter.get(0));
				this.visit(setter.get(1), "] = ", ";");
			}
			for (ParsingObject item : dictionaryStyleMember) {
				this.currentBuilder.appendNewLine("ret['");
				this.visit(item.get(0));
				this.visit(item.get(1), "'] = ", ";");
			}
			this.currentBuilder.appendNewLine("return ret; })()");
			this.currentBuilder.unIndent();
		}
	}
	
	public void visitVariadicParameter(ParsingObject node){
		this.currentBuilder.append("__variadicParams");
	}
	
	public void visitCount(ParsingObject node){
		this.visit(node.get(0));
		this.currentBuilder.append(".length");
	}
	
//
//	public void visitTry(ParsingObject node) {
//		this.currentBuilder.append("try");
//		this.GenerateCode(null, node.TryBlock());
//		if(node.HasCatchBlock()){
//			@Var String VarName = this.NameUniqueSymbol("e");
//			this.currentBuilder.append("catch(", VarName, ")");
//			this.GenerateCode(null, node.CatchBlock());
//		}
//		if (node.HasFinallyBlock()) {
//			this.currentBuilder.append("finally");
//			this.GenerateCode(null, node.FinallyBlock());
//		}
//	}
//
//	//	public void visitCatch(ParsingObject node) {
//	//		this.currentBuilder.append("catch");
//	//		this.currentBuilder.appendWhiteSpace();
//	//		this.currentBuilder.append(node.GivenName);
//	//		this.GenerateCode(null, node.AST[ParsingObject._Block]);
//	//	}
//
//	public void visitVar(ParsingObject node) {
//		this.currentBuilder.appendToken("var");
//		this.currentBuilder.appendWhiteSpace();
//		this.currentBuilder.append(this.NameLocalVariable(node.GetName(), node.VarIndex));
//		this.currentBuilder.appendToken("=");
//		this.GenerateCode(null, node.InitValue());
//		this.currentBuilder.append(this.SemiColon);
//		this.visitStmtList(node);
//	}
//
//	public void visitParam(ParsingObject node) {
//		this.currentBuilder.append(this.NameLocalVariable(node.GetName(), node.ParamIndex));
//	}
//
//	public void visitFunction(ParsingObject node) {
//		@Var boolean IsLambda = (node.FuncName() == null);
//		@Var boolean IsInstanceMethod = (!IsLambda && node.AST.length > 1 && node.AST[1/*first param*/] instanceof ParsingObject);
//		@Var ZType SelfType = IsInstanceMethod ? node.AST[1/*first param*/].Type : null;
//		@Var boolean IsConstructor = IsInstanceMethod && node.FuncName().equals(SelfType.GetName());
//
//		if(IsConstructor){
//			@Var ParsingObject Block = node.Block();
//			Block.AST[Block.AST.length - 1].AST[0] = node.AST[1];
//		}
//		if(IsLambda) {
//			this.currentBuilder.append("(function");
//		}else{
//			this.currentBuilder.append("function ");
//			if(!node.Type.IsVoidType()) {
//				@Var String FuncName = node.GetUniqueName(this);
//				this.currentBuilder.append(FuncName);
//			}
//			else {
//				this.currentBuilder.append(node.GetSignature());
//			}
//		}
//		this.visitList("(", node, ")");
//		this.GenerateCode(null, node.Block());
//		if(IsLambda) {
//			this.currentBuilder.append(")");
//		}else{
//			this.currentBuilder.append(this.SemiColon);
//			if(IsInstanceMethod) {
//				if(this.IsUserDefinedType(SelfType) && !IsConstructor){
//					this.currentBuilder.appendLineFeed();
//					this.currentBuilder.append(SelfType.ShortName); //FIXME must use typing in param
//					this.currentBuilder.append(".prototype.");
//					this.currentBuilder.append(node.FuncName());
//					this.currentBuilder.append("__ = ");
//					this.currentBuilder.append(node.GetSignature());
//
//					this.currentBuilder.appendLineFeed();
//					this.currentBuilder.append(SelfType.ShortName); //FIXME must use typing in param
//					this.currentBuilder.append(".prototype.");
//					this.currentBuilder.append(node.FuncName());
//					this.currentBuilder.append(" = (function(){ Array.prototype.unshift.call(arguments, this); return this.");
//					this.currentBuilder.append(node.FuncName());
//					this.currentBuilder.append("__.apply(this, arguments); })");
//
//					this.currentBuilder.append(this.SemiColon);
//					this.currentBuilder.appendLineFeed();
//					this.currentBuilder.append("function ");
//					this.currentBuilder.append(SelfType.ShortName); //FIXME must use typing in param
//					this.currentBuilder.append("_");
//					this.currentBuilder.append(node.FuncName());
//					this.visitList("(", node, ")");
//					this.currentBuilder.append("{ return ");
//					this.currentBuilder.append(node.GetSignature());
//					this.visitList("(", node, "); ");
//					this.currentBuilder.append("}");
//				}
//			}
//			this.currentBuilder.appendLineFeed();
//			this.currentBuilder.appendLineFeed();
//		}
//	}
//
//	public void visitFuncCall(ParsingObject node) {
//		//this.GenerateCode(null, node.FuncName());
//		@Var ZType FuncType = node.GetFuncType();
//		if(FuncType != null){
//			@Var ZType RecvType = node.GetFuncType().GetRecvType();
//			if(this.IsUserDefinedType(RecvType) && RecvType.ShortName != null && !RecvType.ShortName.equals(node.GetStaticFuncName())){
//				this.currentBuilder.append("(");
//				this.GenerateCode(null, node.Function());
//				if(node.Function() instanceof ParsingObject){
//					this.currentBuilder.append("__ || ");
//				}else{
//					this.currentBuilder.append(" || ");
//				}
//				this.currentBuilder.append(RecvType.ShortName);
//				this.currentBuilder.append("_");
//				this.currentBuilder.append(node.GetStaticFuncName());
//				this.currentBuilder.append(")");
//			}else{
//				this.GenerateCode(null, node.Function());
//			}
//		}else{
//			this.GenerateCode(null, node.Function());
//		}
//		this.visitList("(", node, ")");
//	}
//
//	public void visitMethodCall(ParsingObject node) {
//		// (recv.method || Type_method)(...)
//		@Var ParsingObject RecvNode = node.Recv();
//
//		if(this.IsUserDefinedType(RecvNode.Type)){
//			this.currentBuilder.append("(");
//			this.GenerateSurroundCode(RecvNode);
//			this.currentBuilder.append(".");
//			this.currentBuilder.append(node.MethodName());
//			this.currentBuilder.append("__ || ");
//			this.currentBuilder.append(RecvNode.Type.ShortName);
//			this.currentBuilder.append("_");
//			this.currentBuilder.append(node.MethodName());
//			this.currentBuilder.append(")");
//		}else{
//			this.GenerateSurroundCode(RecvNode);
//			this.currentBuilder.append(".");
//			this.currentBuilder.append(node.MethodName());
//		}
//		//this.GenerateSurroundCode(node.Recv());
//		this.visitList("(", node, ")");
//	}
//
//	public void visitMapLiteral(ParsingObject node) {
//		@Var int ListSize =  node.GetListSize();
//		@Var int i = 0;
//		while(i < ListSize) {
//			@Var ParsingObject KeyNode = node.GetListAt(i);
//			if(KeyNode instanceof ParsingObject){
//				this.GenerateCode(null, KeyNode);
//				return;
//			}
//			i = i + 1;
//		}
//		this.currentBuilder.append("{");
//		while(i < ListSize) {
//			@Var ParsingObject KeyNode = node.GetListAt(i);
//			if (i > 0) {
//				this.currentBuilder.append(", ");
//			}
//			this.GenerateCode(null, KeyNode);
//			this.currentBuilder.append(": ");
//			i = i + 1;
//			if(i < node.GetListSize()){
//				@Var ParsingObject ValueNode = node.GetListAt(i);
//				this.GenerateCode(null, ValueNode);
//				i = i + 1;
//			}else{
//				this.currentBuilder.append("null");
//			}
//		}
//		this.currentBuilder.append("}");
//	}
//
//	public void visitLet(ParsingObject node) {
//		this.currentBuilder.appendNewLine("var ", node.GlobalName, " = ");
//		this.GenerateCode(null, node.InitValue());
//		this.currentBuilder.append(this.SemiColon);
//	}
//
//	private void GenerateExtendCode(ParsingObject node) {
//		this.currentBuilder.append("var __extends = this.__extends || function (d, b) {");
//		this.currentBuilder.appendLineFeed();
//		this.currentBuilder.Indent();
//		this.currentBuilder.appendIndent();
//		this.currentBuilder.append("for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];");
//		this.currentBuilder.appendLineFeed();
//		this.currentBuilder.appendIndent();
//		this.currentBuilder.append("function __() { this.constructor = d; }");
//		this.currentBuilder.appendLineFeed();
//		this.currentBuilder.appendIndent();
//		this.currentBuilder.append("__.prototype = b.prototype;");
//		this.currentBuilder.appendLineFeed();
//		this.currentBuilder.appendIndent();
//		this.currentBuilder.append("d.prototype = new __();");
//		this.currentBuilder.appendLineFeed();
//		this.currentBuilder.append("};");
//		this.currentBuilder.appendLineFeed();
//		this.currentBuilder.UnIndent();
//	}
//
//	public void visitClass(ParsingObject node) {
//		/* var ClassName = (function(_super) {
//		 *  __extends(ClassName, _super);
//		 * 	function ClassName(params) {
//		 * 		_super.call(this, params);
//		 * 	}
//		 * 	ClassName.prototype.MethodName = function(){ };
//		 * 	return ClassName;
//		 * })(_super);
//		 */
//		if(!node.SuperType().Equals(ZClassType._ObjectType) && !JavaScriptGenerator.UseExtend) {
//			JavaScriptGenerator.UseExtend = true;
//			this.GenerateExtendCode(node);
//		}
//		this.currentBuilder.appendNewLine("var ", node.ClassName(), " = ");
//		this.currentBuilder.append("(function(");
//		if(!node.SuperType().Equals(ZClassType._ObjectType)) {
//			this.currentBuilder.OpenIndent("_super) {");
//			this.currentBuilder.appendNewLine("__extends(", node.ClassName(), this.Camma);
//			this.currentBuilder.append("_super)", this.SemiColon);
//		} else {
//			this.currentBuilder.OpenIndent(") {");
//		}
//		this.currentBuilder.appendNewLine("function ", node.ClassName());
//		this.currentBuilder.OpenIndent("() {");
//		if(!node.SuperType().Equals(ZClassType._ObjectType)) {
//			this.currentBuilder.appendNewLine("_super.call(this)", this.SemiColon);
//		}
//
//		@Var int i = 0;
//		while (i < node.GetListSize()) {
//			@Var ParsingObject FieldNode = node.GetField(i);
//			@Var ParsingObject ValueNode = FieldNode.InitValue();
//			if(!(ValueNode instanceof ParsingObject)) {
//				this.currentBuilder.appendNewLine("this.");
//				this.currentBuilder.append(FieldNode.GetName());
//				this.currentBuilder.append(" = ");
//				this.GenerateCode(null, FieldNode.InitValue());
//				this.currentBuilder.append(this.SemiColon);
//			}
//			i = i + 1;
//		}
//		this.currentBuilder.closeIndent("}");
//
//		this.currentBuilder.appendNewLine("return ", node.ClassName(), this.SemiColon);
//		this.currentBuilder.closeIndent("})(");
//		if(node.SuperType() != null) {
//			this.currentBuilder.append(node.SuperType().GetName());
//		}
//		this.currentBuilder.append(")", this.SemiColon);
//	}
//
//	public void visitError(ParsingObject node) {
//		if(node instanceof ParsingObject) {
//			@Var ParsingObject ErrorNode = (ParsingObject)node;
//			this.GenerateCode(null, ErrorNode.ErrorNode);
//		}
//		else {
//			@Var String Message = ZLogger._LogError(node.SourceToken, node.ErrorMessage);
//			this.currentBuilder.appendWhiteSpace();
//			this.currentBuilder.append("LibZen.ThrowError(");
//			this.currentBuilder.append(LibZen._QuoteString(Message));
//			this.currentBuilder.append(")");
//		}
//	}
}

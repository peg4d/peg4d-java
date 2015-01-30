package org.peg4d.fsharp;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;
import org.peg4d.ParsingTag;
import org.peg4d.million.MillionTag;
import org.peg4d.million.SourceGenerator;

public class FSharpGenerator extends SourceGenerator {
	private static boolean UseExtend;
	public ArrayList<FSharpFunc> funcList = new ArrayList<FSharpFunc>();
	public ArrayList<FSharpVar> varList = new ArrayList<FSharpVar>();
	public ArrayList<String> objList = new ArrayList<String>();
	public ArrayList<FSharpScope> scopeList = new ArrayList<FSharpScope>();
	private String prefixName = "";
	public int currentNest = 0;
	private int ifResultKey = 0;
	private int lamdaKey = 0;
	private boolean forFlag = false;
	private boolean objFlag = false;
	private boolean assignFlag= false;
	private ArrayList<String> addedGetterList = new ArrayList<String>();
	private String forConunter = "";
	private FSharpScope currentScope;

	public FSharpGenerator() {
		FSharpGenerator.UseExtend = false;
	}
	
	public int nest(){
		return ++this.currentNest;
	}
	
	public int unNest(){
		return --this.currentNest;
	}
	
	protected void initialCheck(ParsingObject node){
		ParsingObject target;
		for(int i = 0; i < node.size(); i++){
			target = node.get(i);
			if(target.is(MillionTag.TAG_VAR_DECL_STMT)){
				
			}
			this.checkFuncDecl(target, new ArrayList<String>());
		}
	}
	
	protected void checkFuncDecl(ParsingObject node, ArrayList<String> path){
		boolean addScope = false;
		String name = "";
		ArrayList<String> cpath = path;
		boolean isFuncDecl = node.is(MillionTag.TAG_FUNC_DECL);
		boolean isObject = node.is(MillionTag.TAG_OBJECT);
		
		if(isFuncDecl){
			addScope = true;
			name = node.get(2).getText();
			if(name.isEmpty()){
				try{
					//sNode = superNode
					ParsingObject sNode = node.getParent();
					ParsingObject ssNode = sNode.getParent();
					ParsingObject sssNode = ssNode.getParent();
					if(sssNode.is(MillionTag.TAG_VAR_DECL_STMT)){
						name = sNode.get(0).getText();
					} else if(sNode.is(MillionTag.TAG_PROPERTY)) {
						name = sNode.get(0).getText();
					} else {
						name = "lamda" + this.lamdaKey++;
					}
				} catch(NullPointerException e) {
					name = "lamda" + this.lamdaKey++;
				}
				node.get(2).setValue(name);
			}
			String pathName = "";
			for(String p : path){
				pathName += p + ".";
			}
			this.funcList.add(new FSharpFunc(name, pathName, !path.isEmpty(), node));
			this.funcList.add(new FSharpFunc("_" + name, pathName, !path.isEmpty(), node));
		} else if(isObject){
			addScope = true;
			ParsingObject sNode = node.getParent();
			if(sNode.is(MillionTag.TAG_VAR_DECL)){
				name = sNode.get(0).getText();
			} else {
				name = "lamda" + this.lamdaKey++;
			}
		}
		if(addScope){
			FSharpScope fs = new FSharpScope(name, node, cpath);
			this.scopeList.add(fs);
			
			if(isFuncDecl){
				ParsingObject argsNode = node.get(4);
				FSharpVar argVar;
				for(int i = 0; i < argsNode.size(); i++){
					argVar = new FSharpVar(argsNode.get(i).getText(), fs.getPathName());
					fs.varList.add(argVar);
					this.varList.add(argVar);
					fs.numOfArgs++;
				}
			}
			
			cpath = new ArrayList<String>();
			//deep copy path -> cpath
			for(int i = 0; i < path.size(); i++){
				cpath.add(path.get(i));
			}
			cpath.add(name);
			
			if(isFuncDecl){
				this.checkVarDecl(node.get(6), fs);
			} else if(isObject){
				this.checkProperty(node, fs);
			}
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				this.checkFuncDecl(node.get(i), cpath);
			}
		}
	}
	
	private int indexOf(ParsingObject node){
		ParsingObject parent = node.getParent();
		for(int i = 0; i < parent.size(); i++){
			if(parent.get(i).equals(node)){
				return i;
			}
		}
		return -1;
	}
	
	protected void checkProperty(ParsingObject node, FSharpScope fs){
		ParsingObject propertyNode;
		FSharpVar fv;
		FSharpFunc ff;
		for(int i = 0; i < node.size(); i++){
			propertyNode = node.get(i);
			if(!propertyNode.get(1).is(MillionTag.TAG_FUNC_DECL)){
				fv = new FSharpVar(propertyNode.get(0).getText(), fs.getPathName(), propertyNode.get(1));
				fs.varList.add(fv);
			} else {
				ff = new FSharpFunc(propertyNode.get(0).getText(), fs.getPathName(), false, propertyNode.get(1));
				fs.funcList.add(ff);
			}
		}
	}
	
	protected boolean checkVarDecl(ParsingObject node, FSharpScope fs){
		if(node.is(MillionTag.TAG_VAR_DECL_STMT)){
			ParsingObject listNode = node.get(2);
			ParsingObject varDeclNode;
			for(int i = 0; i < listNode.size(); i++){
				varDeclNode = listNode.get(i);
				try{
					if(!varDeclNode.get(1).is(MillionTag.TAG_FUNC_DECL)){
						FSharpVar fv = new FSharpVar(varDeclNode.get(0).getText(), fs.getPathName());
						this.varList.add(fv);
						fs.varList.add(fv);
						varDeclNode.setTag(new ParsingTag("Assign"));
						node.getParent().insert(this.indexOf(node) + i, varDeclNode);
					} else {
						return false;
					}
				} catch(ArrayIndexOutOfBoundsException e){
					return false;
				}
			}
			node.getParent().remove(this.indexOf(node));
		} else
		if(node.is(MillionTag.TAG_FUNC_DECL)){
			return false;
		} else
		if(node.is(MillionTag.TAG_ASSIGN)){
			String varName = this.getFieldText(node.get(0));
			boolean isExist = false;
			for(FSharpVar searchTarget : fs.varList){
				if(searchTarget.name.contentEquals(varName)){
					isExist = true;
				}
			}
			if(!isExist){
				FSharpVar fv = new FSharpVar(varName, fs.getPathName());
				this.varList.add(fv);
				fs.varList.add(fv);
			}
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				checkVarDecl(node.get(i), fs);
			}
		}
		return true;
	}
	
	protected String typeCode(String name){
		this.currentBuilder.appendNewLine("let " + name + "0 = " + "new " + name + "()");
//		String forStr = "for i in ";
//		String argStr = name + ".GetType().GetMethods() ";
//		String doStr = "do printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString + " (" + this.currentBuilder.quoteString + "  " + this.currentBuilder.quoteString + " + i.ToString()) done";

//		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
//		String argStr = " (" + this.currentBuilder.quoteString + name + " : " + this.currentBuilder.quoteString + " + " + name + ".GetType().GetMethods().[0].ToString())";
		String printStr = "fsLib.fl.printObject " + this.currentBuilder.quoteString + name + this.currentBuilder.quoteString + " 1 2 (" + name + "0.GetType().GetMethods())";
		return printStr;
	}
	
	protected String typeCode(FSharpFunc ffunc){
		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
		String argBeginStr = " (" + this.currentBuilder.quoteString + ffunc.getFullname() + " : " + this.currentBuilder.quoteString + " + ";
		String argStr = "";
		if(ffunc.isMember){
			argStr = ffunc.getFullname() + ".GetType().ToString()";
		} else {
			argStr = ffunc.getFullname() + ".GetType().GetMethods().[0].ToString()";
		}
//		for(int i = 0; i <= ffunc.uniqueKey; i++){
//			argStr += " + " + ffunc.getFullname() + i + ".GetType().ToString()";
//		}
		String argEndStr = ")";
		return printStr + argBeginStr + argStr + argEndStr;
	}
	
	protected String typeCode(FSharpVar fvar){
		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
		String argBeginStr = " (" + this.currentBuilder.quoteString + fvar.getTrueName() + " : " + this.currentBuilder.quoteString;
		String argStr = "";
		for(int i = 0; i <= fvar.uniqueKey; i++){
			argStr += " + " + fvar.getFullname() + i + ".GetType().ToString()";
		}
		String argEndStr = ")";
		return printStr + argBeginStr + argStr + argEndStr;
	}
	
	protected void generateTypeCode(){
//		for(int i = 0; i< this.funcList.size(); i++){
//			if(i % 2 == 0){
//				this.currentBuilder.appendNewLine(typeCode(this.funcList.get(i)));
//			}
//		}
//		for(FSharpVar fvar : this.varList){
//			this.currentBuilder.appendNewLine(typeCode(fvar));
//		}
//		for(String obj : this.objList){
//			this.currentBuilder.appendNewLine(typeCode(obj));
//		}
		for(FSharpScope fs : this.scopeList){
			this.currentBuilder.appendNewLine(typeCode(fs.getScopeName()));
		}
	}
	
	protected boolean checkReturn(ParsingObject node, boolean result){
		if(result){
			return true;
		}
		result = node.is(MillionTag.TAG_RETURN);
		if(node.size() >= 1 && !result) {
			for(int i = 0; i < node.size(); i++){
				result = checkReturn(node.get(i), result);
			}
		}
		return result;
	}
	
	protected void checkAssignVarName(ParsingObject node, FSharpVar targetVar){
		if(node.size() < 1){
			if(targetVar.getTrueName().contentEquals(node.getText())){
				node.setValue(targetVar.getCurrentName());
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				if(node.get(i).size() == 0){
					if(targetVar.getTrueName().contentEquals(node.get(i).getText())){
						node.get(i).setValue(targetVar.getCurrentName());
					}
				} else {
					checkAssignVarName(node.get(i), targetVar);
				}
			}
		}
	}
	
	protected boolean checkApplyFuncName(String funcName){
		for(FSharpFunc target : this.funcList){
			if(target.getFullname().contentEquals(funcName)){
				return true;
			}
		}
		return false;
	}
	
	protected FSharpVar searchVarFromList(String varName, boolean fieldFlag){
		String prefixName = this.prefixName;
		String[] prefixNameElements = prefixName.split(".");
		if(prefixNameElements.length == 0){
			prefixNameElements = new String[1];
			prefixNameElements[0] = prefixName.substring(0, prefixName.length() - 1);
		}
		for(FSharpVar element : varList){
			if(element.getFullname().contentEquals(prefixName + varName)){
				return element;
			} else if(element.getFullname().contentEquals(varName) && fieldFlag){
				return element;
			}
		}
		if(prefixNameElements != null){
			for(int i = prefixNameElements.length - 1; i >= 0; i--){
				if(prefixName.length() > 0){
					prefixName = prefixName.substring(0, prefixName.length() - (prefixNameElements[i].length() + 1));
				}
				for(FSharpVar element : varList){
					if(element.getFullname().contentEquals(prefixName + varName)){
						return element;
					} else if(element.getFullname().contentEquals(varName) && fieldFlag){
						return element;
					}
				}
			}
		}
		return null;
	}
	
	protected String getFieldText(ParsingObject node){
		String result = "";
		if(node.is(MillionTag.TAG_FIELD)){
			for(int i = 0; i < node.size(); i++){
				result += node.get(i).getText();
				if(i == node.size() - 1){
					result += ".";
				}
			}
		} else if(node.is(MillionTag.TAG_NAME)){
			result += node.getText();
		}
		return result;
	}
	
	protected void setVarNameInBinary(ParsingObject node, boolean isAssign){
		String varName = this.getFieldText(node.get(0));
		FSharpVar targetVar = searchVarFromList(varName, node.get(0).is(MillionTag.TAG_FIELD));
		if(targetVar == null && isAssign){
			this.varList.add(new FSharpVar(varName, this.prefixName));
			targetVar = this.varList.get(this.varList.size()-1);
			checkAssignVarName(node.get(1), targetVar);
			targetVar.addChild();
		}
		varName = targetVar.getCurrentName();
		node.get(0).setValue(varName);
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
		if(!this.assignFlag){
			this.formatRightSide(node.get(0));
			this.formatRightSide(node.get(1));
		}
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
	
	protected void generateTrinaryAddHead(ParsingObject node, String operator1, String operator2, String operator3){
		this.currentBuilder.append(operator1);
		generateExpression(node.get(0));
		this.currentBuilder.append(operator2);
		generateExpression(node.get(1));
		this.currentBuilder.append(operator3);
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
	
	protected void generateClass(ParsingObject node){
		String name = node.get(0).getText();
		this.objFlag = true;
		this.currentBuilder.appendNewLine("type ClassOf" + name + "(arg_for_object:int) = class");
		this.currentBuilder.indent();
		ParsingObject objNode = node.get(1);
		ParsingObject varDeclStmtNode = new ParsingObject(new ParsingTag("VarDeclStmt"), null, 0);
		varDeclStmtNode.set(0, new ParsingObject(new ParsingTag("Text"), null, 0));
		varDeclStmtNode.set(1, new ParsingObject(new ParsingTag("Text"), null, 0));
		varDeclStmtNode.set(2, new ParsingObject(new ParsingTag("List"), null, 0));
		this.prefixName += name + ".";
		String varName = "";
		for(int i = 0; i < objNode.size(); i++){
			this.currentBuilder.appendNewLine();
			varName = objNode.get(i).get(0).getText();
			objNode.get(i).setTag(new ParsingTag("VarDecl"));
			objNode.get(i).get(0).setValue(varName + "0");
			varDeclStmtNode.get(2).set(0, objNode.get(i));
			this.visit(varDeclStmtNode);
			objNode.get(i).get(0).setValue(varName);
			this.objFlag = true;
		}
		for(String addedGetterName: this.addedGetterList){
			this.currentBuilder.appendNewLine(addedGetterName);
		}
		this.prefixName = this.prefixName.substring(0, this.prefixName.length() - (name + ".").length());
		this.currentBuilder.appendNewLine("end");
		this.currentBuilder.unIndent();
		this.varList.add(new FSharpVar(name, this.prefixName));
		this.currentBuilder.appendNewLine("let " + searchVarFromList(name, false).getCurrentName() + " = new ClassOf" + name + "(0)");
		this.objList.add(searchVarFromList(name, false).getCurrentName());
		this.objFlag = false;
	}
	
	protected void generateScope(FSharpScope fs, boolean isTopLevel){
		this.currentScope = fs;
		this.prefixName = fs.getPathName();
		String classType = isTopLevel? "type" : "and";
		this.currentBuilder.appendNewLine(classType + " " + fs.getScopeName() + " () = class");
		
		this.currentBuilder.indent();
		FSharpVar fv;
		for(int i = fs.numOfArgs; i < fs.varList.size(); i++){
			fv = fs.varList.get(i);
			if(fv.initialValue == null){
				this.currentBuilder.appendNewLine("static let " + fv.getTrueName() + " = ref None");
			} else {
				this.currentBuilder.appendNewLine("static let " + fv.getTrueName() + " = ref (Some(");
				this.visit(fv.initialValue);
				this.currentBuilder.append("))");
			}
		}
		if(fs.node.is(MillionTag.TAG_FUNC_DECL)){
			ParsingObject argsNode = fs.node.get(4);
			ParsingObject arg;
			String argsStr = "";
			for(int i = 0; i < argsNode.size(); i++){
				arg = argsNode.get(i);
				argsStr += " " + arg.getText();
				this.currentBuilder.appendNewLine("static let _" + arg.getText() + "_middle = ref None");
			}
			if(fs.recursive){
				this.currentBuilder.appendNewLine("static let rec _" + fs.name + argsStr + " =");
			} else {
				this.currentBuilder.appendNewLine("static member " + fs.name + argsStr + " =");
			}
			this.currentBuilder.indent();
			for(int i = 0; i < argsNode.size(); i++){
				arg = argsNode.get(i);
				this.currentBuilder.appendNewLine("_" + arg.getText() + "_middle := " + "!" + arg.getText());
			}
			this.currentBuilder.unIndent();
			this.generateBlock(fs.node.get(6));
			if(!this.checkReturn(fs.node.get(6), false)){
				this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "new fsLib.fl.Void(0)");
			}
			if(fs.recursive){
				this.currentBuilder.appendNewLine("static member " + fs.name + argsStr + " = _" + fs.name + argsStr);
			}
		}
		for(FSharpFunc ff : fs.funcList){
			this.currentBuilder.appendNewLine("static member " + ff.name + " " + ff.argsStr + " = ScopeOf" + fs.getFullname() + "_" + ff.name + "." + ff.name + " " + ff.argsStr);
		}
		for(int i = fs.numOfArgs; i < fs.varList.size(); i++){
			fv = fs.varList.get(i);
			this.currentBuilder.appendNewLine("static member g_" + fv.getTrueName() + " = " + fv.getTrueName());
		}
		
		this.currentBuilder.appendNewLine("end");
		this.currentBuilder.unIndent();
		this.prefixName = "";
		this.currentScope = null;
	}
	
	public void visitSource(ParsingObject node) {
		this.initialCheck(node);
		this.generateScope(this.scopeList.get(0), true);
		for(int i = 1; i < this.scopeList.size(); i++){
			this.generateScope(this.scopeList.get(i), false);
		}
//		for(ParsingObject element : node){
//			if(element.is(MillionTag.TAG_VAR_DECL) || element.is(MillionTag.TAG_VAR_DECL_STMT) || element.is(MillionTag.TAG_FUNC_DECL)){
//				this.currentBuilder.appendNewLine();
//				this.visit(element);
//			}
//		}
		this.generateTypeCode();
	}
	
	public void visitName(ParsingObject node) {
		String varName = node.getText();
		FSharpVar targetVar = searchVarFromList(varName, false);
		if(targetVar != null){
			varName = targetVar.getCurrentName();
		}
		this.currentBuilder.append(node.getText());
	}
	
	public void visitInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText() + ".0");
	}
	
	public void visitDecimalInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void visitOctalInteger(ParsingObject node) {
		this.currentBuilder.append(node.getText());
		if(node.getText().contentEquals("0") && !forFlag){
			this.currentBuilder.append(".0");
		}
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
		}
	}
	
	public void generateBlock(ParsingObject node) {
		this.currentBuilder.indent();
		this.visit(node);
		this.currentBuilder.unIndent();
	}
	
	public void visitArray(ParsingObject node){
		this.currentBuilder.append("[|");
		this.generateList(node, "; ");
		this.currentBuilder.append("|]");
	}
	
	@Deprecated
	public void visitObject(ParsingObject node){
		this.generateClass(node.getParent());
	}
	
	public void visitProperty(ParsingObject node) {
		this.generateBinary(node, ": ");
	}

	public void visitSuffixInc(ParsingObject node) {
		//this.genarateSuffixUnary(node, "++");
	}

	public void visitSuffixDec(ParsingObject node) {
		//this.genarateSuffixUnary(node, "--");
	}

	public void visitPrefixInc(ParsingObject node) {
		//this.genaratePrefixUnary(node, "++");
	}

	public void visitPrefixDec(ParsingObject node) {
		//this.genaratePrefixUnary(node, "--");
	}

	public void visitPlus(ParsingObject node) {
		this.visit(node.get(0));
	}

	public void visitMinus(ParsingObject node) {
		this.genaratePrefixUnary(node, "-");
	}

	public void visitAdd(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")+(");
		this.currentBuilder.append(")");
	}

	public void visitSub(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")-(");
		this.currentBuilder.append(")");
	}

	public void visitMul(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")*(");
		this.currentBuilder.append(")");
	}

	public void visitDiv(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")/(");
		this.currentBuilder.append(")");
	}

	public void visitMod(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")%(");
		this.currentBuilder.append(")");
	}

	public void visitLeftShift(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<<<(");
		this.currentBuilder.append(")");
	}

	public void visitRightShift(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>>>(");
		this.currentBuilder.append(")");
	}

	public void visitLogicalLeftShift(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<<<(");
		this.currentBuilder.append(")");
	}

	public void visitLogicalRightShift(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>>>(");
		this.currentBuilder.append(")");
	}

	public void visitGreaterThan(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>(");
		this.currentBuilder.append(")");
	}

	public void visitGreaterThanEquals(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>=(");
		this.currentBuilder.append(")");
	}

	public void visitLessThan(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<(");
		this.currentBuilder.append(")");
	}

	public void visitLessThanEquals(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<=(");
		this.currentBuilder.append(")");
	}

	public void visitEquals(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")=(");
		this.currentBuilder.append(")");
	}

	public void visitNotEquals(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<>(");
		this.currentBuilder.append(")");
	}
	
	public void visitStrictEquals(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")=(");
		this.currentBuilder.append(")");
	}

	public void visitStrictNotEquals(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<>(");
		this.currentBuilder.append(")");
	}

	//none
	public void visitCompare(ParsingObject node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")-(");
		this.currentBuilder.append(")");
	}
	
	public void visitInstanceOf(ParsingObject node) {
		this.currentBuilder.append("(");
		this.visit(node.get(0));
		this.currentBuilder.append(").constructor.name === ");
		this.visit(node.get(1));
		this.currentBuilder.append(".name");
	}
	
	public void visitStringInstanceOf(ParsingObject node) {
		//this.generateBinary(node, " instanceof ");
	}
	
	public void visitHashIn(ParsingObject node) {
		//this.generateBinary(node, " in ");
	}

	public void visitBitwiseAnd(ParsingObject node) {
		this.generateBinary(node, "&&&");
	}

	public void visitBitwiseOr(ParsingObject node) {
		this.generateBinary(node, "|||");
	}

	public void visitBitwiseNot(ParsingObject node) {
		this.generateBinary(node, "not");
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
		this.genaratePrefixUnary(node, "not");
	}

	public void visitLogicalXor(ParsingObject node) {
		this.generateBinary(node, "^^^");
	}

	public void visitConditional(ParsingObject node) {
		this.generateTrinaryAddHead(node, "if", "then", "else");
	}
	
	protected FSharpScope searchScopeFromList(String name){
		String pathName;
		String prefixName = this.prefixName;
		String[] prefixNameElements = prefixName.split(".");
		if(prefixNameElements.length == 0){
			prefixNameElements = new String[1];
			prefixNameElements[0] = prefixName.substring(0, prefixName.length() - 1);
		}
		for(FSharpScope element : this.scopeList){
			pathName = element.getPathName();
			if(pathName.substring(0, pathName.length()-1).contentEquals(prefixName + name)){
				return element;
			}
		}
		if(prefixNameElements != null){
			for(int i = prefixNameElements.length - 1; i >= 0; i--){
				if(prefixName.length() > 0){
					prefixName = prefixName.substring(0, prefixName.length() - (prefixNameElements[i].length() + 1));
				}
				for(FSharpScope element : this.scopeList){
					pathName = element.getPathName();
					if(pathName.substring(0, pathName.length()-1).contentEquals(prefixName + name)){
						return element;
					}
				}
			}
		}
		return null;
	}
	
	protected void formatRightSide(ParsingObject node){
		if(node.is(MillionTag.TAG_APPLY)){
			this.formatRightSide(node.get(1));
		} else if(node.is(MillionTag.TAG_FIELD)){
			String fieldValue = "(!(";
			FSharpScope fs;
			FSharpVar targetVar = null;
			for(int i = 0; i < node.size()-1; i++){
				fs = this.searchScopeFromList(node.get(i).getText());
				if(fs != null){
					fieldValue += fs.getScopeName() + ".";
					targetVar = fs.searchVar(node.get(node.size()-1).getText());
				}
				fs = null;
				node.remove(i);
			}
			
			if(targetVar != null){
				fieldValue += "g_" + node.get(0).getText() + ")).Value";
			} else {
				fieldValue += node.get(0).getText() + ")).Value";
			}
			node.remove(0);
			node.setTag(new ParsingTag("Name"));
			node.setValue(fieldValue);
		} else if(node.is(MillionTag.TAG_NAME)){
			String name = node.getText();
//			FSharpScope target = this.searchVarOrFuncFromScopeList(this.currentScope, name);
//			if(target == null){
//				node.setValue("(!(" + name + ")).Value");
//			} else {
//				node.setValue("(!(" + target.getPathName() + name + ")).Value");
//			}
			node.setValue("(!(" + name + ")).Value");
		} else {
			for(int i = 0; i < node.size(); i++){
				this.formatRightSide(node.get(i));
			}
		}
	}
	
	protected FSharpScope searchVarOrFuncFromScopeList(FSharpScope targetScope, String targetName){
		ArrayList<String> scopePath = new ArrayList<String>();
		ArrayList<String> pathes = new ArrayList<String>();
		FSharpScope result = null;
		for(String element : targetScope.path){
			scopePath.add(element);
		}
		
		while(scopePath.size() > 0){
			pathes.add(scopePath.toString());
			scopePath.remove(scopePath.size() - 1);
		}
		
		for(int scope_i = 0; scope_i < this.scopeList.size(); scope_i++){
			if(!pathes.isEmpty()){
				for(int path_i = 0; path_i < pathes.size(); path_i++){
					if(pathes.get(path_i).contentEquals(this.scopeList.get(scope_i).path.toString())){
						if(this.scopeList.get(scope_i).searchFunc(targetName) != null || this.scopeList.get(scope_i).searchVar(targetName) != null){
							result = this.scopeList.get(scope_i);
						}
					}
				}
			} else {
				if(("[]").contentEquals(this.scopeList.get(scope_i).path.toString())){
					if(this.scopeList.get(scope_i).searchFunc(targetName) != null || this.scopeList.get(scope_i).searchVar(targetName) != null){
						result = this.scopeList.get(scope_i);
					}
				}
			}
		}
		return result;
	}
	
	public void visitAssign(ParsingObject node) {
		this.assignFlag = true;
		this.formatRightSide(node.get(1));
		//this.setVarNameInBinary(node, true);
		String varName = this.getFieldText(node.get(0));
		FSharpVar targetVar = searchVarFromList(varName, node.get(0).is(MillionTag.TAG_FIELD));
		if(targetVar == null){
			this.varList.add(new FSharpVar(varName, this.prefixName));
			targetVar = this.varList.get(this.varList.size()-1);
			this.currentBuilder.append("let ");
		}
		//checkAssignVarName(node.get(1), targetVar);
		targetVar.addChild();
		
		this.generateBinary(node, " := Some(");
		this.currentBuilder.append(")");
		this.assignFlag = false;
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
	
	//none
	public void visitConcat(ParsingObject node) {
		this.generateBinary(node, " + ");
	}

	public void visitField(ParsingObject node) {
		ParsingObject field;
		FSharpScope fs;
		for(int i = 0; i < node.size()-1; i++){
			field = node.get(i);
			fs = this.searchScopeFromList(field.getText());
			if(fs != null){
				field.setValue(fs.getScopeName());
			}
			fs = null;
		}
		this.generateBinary(node, ".");
	}

	public void visitIndex(ParsingObject node) {
		generateExpression(node.get(0));
		this.visit(node.get(1), ".[(int (", "))]");
	}

	//none
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
	
	protected void formatApplyFuncName(ParsingObject node){
		if(node.is(MillionTag.TAG_FIELD)){
			ParsingObject field;
			FSharpScope fs;
			for(int i = 0; i < node.size() - 1; i++){
				field = node.get(i);
				fs = this.searchScopeFromList(field.getText());
				field.setValue(fs.getScopeName());
			}
		} else if(node.is(MillionTag.TAG_NAME)){
			FSharpScope fs;
			fs = this.searchVarOrFuncFromScopeList(this.currentScope, node.getText());
			if(fs != null){
				node.setValue(fs.getScopeName() + "." + node.getText());
			}
		}
	}
	
	public void visitApply(ParsingObject node) {
		ParsingObject func = node.get(0);
		ParsingObject arguments = node.get(1);
		if(this.assignFlag){
			this.currentBuilder.append("!(");
		}
		if(this.checkApplyFuncName(getFieldText(func))){
			this.formatApplyFuncName(func);
			this.visit(func);
			this.currentBuilder.appendSpace();
			this.currentBuilder.append("(ref(Some(");
			this.generateList(arguments, "))) (ref(Some(");
			this.currentBuilder.append(")))");
		}
		if(this.assignFlag){
			this.currentBuilder.append(")");
		}
	}

	//none
	public void visitMethod(ParsingObject node) {
		this.generateBinary(node, ".");
		this.currentBuilder.appendChar('(');
		this.generateList(node.get(2), ", ");
		this.currentBuilder.appendChar(')');
	}
	
	public void visitTypeOf(ParsingObject node) {
		this.currentBuilder.append("(");
		generateExpression(node.get(0));
		this.currentBuilder.append(")");
		this.currentBuilder.append(".GetType().GetMethod().[0].toString()");
	}

	public void visitIf(ParsingObject node) {
		this.visit(node.get(0), "if (", ")");
		this.currentBuilder.appendNewLine("then");
		this.generateBlock(node.get(1));
		if(!this.checkReturn(node.get(1), false)){
			this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "printfn(" + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString + ")");
		} else {
			if(!isNullOrEmpty(node, 2)){
				ParsingObject elseBlock = node.get(2);
				if(!this.checkReturn(elseBlock, false)){
					ParsingObject parent = node.getParent();
					ParsingObject element;
					long currentPosition = node.getSourcePosition();
					for(int i = 0; i < parent.size(); i++){
						element = parent.get(i);
						if(currentPosition < element.getSourcePosition()){
							elseBlock.add(element);
							parent.remove(i);
						}
					}
				}
			} else {
				ParsingObject elseBlock = new ParsingObject(new ParsingTag("Block"), null, 0);
				node.set(2, elseBlock);
				ParsingObject parent = node.getParent();
				ParsingObject element;
				long currentPosition = node.getSourcePosition();
				for(int i = 0; i < parent.size(); i++){
					element = parent.get(i);
					if(currentPosition < element.getSourcePosition()){
						elseBlock.add(element);
						parent.remove(i);
					}
				}
			}
		}
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.appendNewLine("else");
			this.generateBlock(node.get(2));
			if(!this.checkReturn(node.get(2), false)){
				this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "printfn(" + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString + ")");
			}
		}
	}

	public void visitWhile(ParsingObject node) {
		int begin, end;
		String thenBlock;
		this.visit(node.get(0), "if ", "");
		this.currentBuilder.appendNewLine("then");
		begin = this.currentBuilder.getPosition();
		this.generateBlock(node.get(1));
		if(!this.checkReturn(node, false)){
			this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
		}
		end = this.currentBuilder.getPosition();
		this.currentBuilder.appendNewLine("else");
		this.currentBuilder.append(this.currentBuilder.substring(begin, end));
//		this.currentBuilder.indent();
//		this.currentBuilder.append("printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
//		this.currentBuilder.appendNewLine("done");
//		this.currentBuilder.unIndent();
	}

	public void visitFor(ParsingObject node) {
		int begin, end;
		String thenBlock;
		this.forFlag = true;
//		this.visit(node.get(0), "for ", " to ");
//		this.visit(node.get(1));
//		this.visit(node.get(2), ';', ')');
//		this.generateBlock(node.get(3));
//		if(!isNullOrEmpty(node, 4)){
//			this.currentBuilder.appendNewLine();
//			this.visit(node.get(4));
//		}
		ParsingObject exp1 = node.get(0).get(0);
		this.currentBuilder.append("for ");
//		ParsingObject exp1VarDeclNode = null;
//		for(int i = 0; i < exp1.getLength() - 1; i++){
//			if(exp1.get(i).is(MillionTag.TAG_VAR_DECL)){
//				exp1VarDeclNode = exp1.get(i);
//			}
//		}
//		if(exp1.is(MillionTag.TAG_LIST) && exp1VarDeclNode != null){
//			this.visitAssign(exp1VarDeclNode);
//		}
		if(exp1.is(MillionTag.TAG_VAR_DECL)){
			this.currentBuilder.append(exp1.get(0).getText());
			this.forConunter = exp1.get(0).getText();
			this.formatForCounter(node.get(1));
			this.formatForCounter(node.get(3));
		}
		
		this.forFlag = false;
		this.currentBuilder.append("=0 to 1 do");
		this.currentBuilder.indent();
		this.currentBuilder.appendNewLine("if ");
		this.visit(node.get(1), "(", ") ");
		this.currentBuilder.appendNewLine("then");
		begin = this.currentBuilder.getPosition();
		this.generateBlock(node.get(3));
		if(!this.checkReturn(node.get(3), false)){
			this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
		}
		end = this.currentBuilder.getPosition();
		thenBlock = this.currentBuilder.substring(begin, end);
		this.currentBuilder.appendNewLine("else");
		this.currentBuilder.indent();
		this.currentBuilder.append(thenBlock);
		this.currentBuilder.unIndent();
		this.currentBuilder.unIndent();
		this.currentBuilder.appendNewLine("done");	
		this.forConunter = "";
	}
	
	public void visitForCounter(ParsingObject node){
		this.currentBuilder.append(node.getText());
	}
	
	protected void formatForCounter(ParsingObject node){
		if(node.is(MillionTag.TAG_NAME)){
			if(node.getText().contentEquals(this.forConunter)){
				node.setValue("double " + this.forConunter);
				node.setTag(new ParsingTag("ForCounter"));
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				this.formatForCounter(node.get(i));
			}
		}
	}

	//none
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
	
	//none
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
		ParsingObject param1 = node.get(0);
		if(param1.is(MillionTag.TAG_LIST)){
			this.currentBuilder.append("for " + param1.get(0).get(0).getText() + " in ");
		} else if(param1.is(MillionTag.TAG_NAME)){
			this.currentBuilder.append("for " + param1.getText() + " in ");
		}
		this.visit(node.get(1));
		this.currentBuilder.append(" do");
		this.generateBlock(node.get(2));
		this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "done");
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
		//this.generateJump(node, "return");
		this.assignFlag = true;
		this.formatRightSide(node.get(0));
		this.visit(node.get(0), "ref(", ")");
		this.assignFlag = false;
		//this.currentBuilder.append(node.get(0).getText());
	}

	public void visitBreak(ParsingObject node) {
		//this.generateJump(node, "break");
	}

	public void visitYield(ParsingObject node) {
		//this.generateJump(node, "yield");
	}

	public void visitContinue(ParsingObject node) {
		//this.generateJump(node, "continue");
	}

	public void visitRedo(ParsingObject node) {
		this.generateJump(node, "/*redo*/");
	}

	public void visitSwitch(ParsingObject node) {
		this.visit(node.get(0), "match ", " with");
		
		this.currentBuilder.indent();
		
		for(ParsingObject element : node.get(1)){
			this.currentBuilder.appendNewLine();
			this.visit(element);
		}
		
		this.currentBuilder.unIndent();
	}

	public void visitCase(ParsingObject node) {
		this.visit(node.get(0), "| ", "->");
		this.currentBuilder.indent();
		this.currentBuilder.appendNewLine();
		if(!isNullOrEmpty(node, 1)){
			this.visit(node.get(1));
		}
		if(this.checkReturn(node, false)){
			this.currentBuilder.appendNewLine("printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
		}
		this.currentBuilder.unIndent();
	}

	public void visitDefault(ParsingObject node) {
		this.currentBuilder.append("| _ ->");
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

	//TODO
	public void visitCatch(ParsingObject node) {
		this.visit(node.get(0), "with(", ")");
		this.generateBlock(node.get(1));
	}
	
	public void visitVarDeclStmt(ParsingObject node) {
		boolean objLet = this.objFlag;
		ParsingObject listNode = node.get(2);
		ParsingObject varDeclNode = listNode.get(0);
		try{
			ParsingObject varStmtNode = varDeclNode.get(1);
			if(!varStmtNode.is(MillionTag.TAG_FUNC_DECL) && !varStmtNode.is(MillionTag.TAG_OBJECT)){
//				if(!objFlag){
//					this.currentBuilder.append("let ");
//				} else {
//					this.currentBuilder.append("val ");
//				}
				this.currentBuilder.append("let ");
				this.objFlag = false;
				this.visit(listNode);
				String name = varDeclNode.get(0).getText();
				if(objLet){
					this.addedGetterList.add("member this." + name + " = " + this.searchVarFromList(name, false).getCurrentName());
				}
			} else if(varStmtNode.is(MillionTag.TAG_OBJECT)){
				this.visit(varStmtNode);
			} else {	
				varStmtNode.set(2, varDeclNode.get(0));
				this.visit(varStmtNode);
			}
		} catch(ArrayIndexOutOfBoundsException e){
			this.currentBuilder.append("//let " + varDeclNode.getText() + "0");
			this.varList.add(new FSharpVar(varDeclNode.get(0).getText(), this.prefixName));
		}
	}
	
	public void visitVarDecl(ParsingObject node) {
		this.varList.add(new FSharpVar(node.get(0).getText(), this.prefixName));
		this.visit(node.get(0));
		if(node.size() > 1){
			this.currentBuilder.append(" = ");
			this.visit(node.get(1), "ref(", ")");
		}
	}
	
	private boolean isRecursiveFunc(ParsingObject node, String name, boolean result){
		if(result){
			return true;
		}
		boolean res = false;
		if(node.is(MillionTag.TAG_APPLY)){
			res = this.getFieldText(node.get(0)).contentEquals(name);
		} else {
			res = false;
		}
		if(node.size() >= 1 && !result){
			for(int i = 0; i < node.size(); i++){
				res = this.isRecursiveFunc(node.get(i), name, result);
			}
		}
		return res;
	}
	
	public void visitFuncDecl(ParsingObject node) {
		//boolean mustWrap = this.currentBuilder.isStartOfLine();
		boolean mustWrap = false;
		boolean notLamda = node.get(2).is(MillionTag.TAG_NAME);
		boolean memberFlag = objFlag;
		
		this.funcList.add(new FSharpFunc(node.get(2).getText(), this.prefixName, memberFlag, node));		
		
		if(mustWrap){
			this.currentBuilder.appendChar('(');
		}
		if(notLamda && !objFlag){
			this.currentBuilder.append("let");
			if(this.isRecursiveFunc(node.get(6), this.prefixName + node.get(2).getText(), false)){
				this.currentBuilder.append(" rec");
			}
		} else if(!notLamda && !objFlag){
			this.currentBuilder.append("fun");
		} else if(objFlag){
			this.currentBuilder.append("member");
		}
		String addName = node.get(2).getText() + ".";
		this.prefixName += addName;
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.appendSpace();
			if(this.objFlag){
				this.currentBuilder.append("this.");
			}
			this.visit(node.get(2));
		}
		
		ParsingObject parameters = node.get(4);
		boolean containsVariadicParameter = false;
		boolean isFirst = true;
		int sizeOfParametersBeforeValiadic = 0;
		int sizeOfParametersAfterValiadic = 0;
		
		this.currentBuilder.appendChar(' ');
		//this.prefixName = this.nameList.get(this.nameList.size() - 1);
		
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
				this.currentBuilder.append(" ");
			}
			this.varList.add(new FSharpVar(param.getText(), this.prefixName));
			this.visit(param);
			isFirst = false;
		}
		this.currentBuilder.appendChar(' ');
		
		if(notLamda){
			this.currentBuilder.appendChar('=');
		} else {
			this.currentBuilder.append("->");
		}
		this.currentBuilder.indent();
		if(containsVariadicParameter){
			this.currentBuilder.appendNewLine("var __variadicParams = __markAsVariadic([]);");
			this.currentBuilder.appendNewLine("for (var _i = ");
			this.currentBuilder.appendNumber(sizeOfParametersBeforeValiadic);
			this.currentBuilder.append(", _n = arguments.length - ");
			this.currentBuilder.appendNumber(sizeOfParametersAfterValiadic);
			this.currentBuilder.append("; _i < _n; ++_i){ __variadicParams.push(arguments[_i]); }");
		}
		this.objFlag = false;
		this.visit(node.get(6));
		if(!checkReturn(node.get(6), false)){
			if(!memberFlag){
				this.currentBuilder.appendNewLine("printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
			} else {
				this.currentBuilder.appendNewLine("new fsLib.fl.Void(0)");
			}
		}
		this.currentBuilder.unIndent();
		if(mustWrap){
			this.currentBuilder.appendChar(')');
		}
		
		this.prefixName = this.prefixName.substring(0, this.prefixName.length() - addName.length());
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
	
	public void visitVariadicParameter(ParsingObject node){
		this.currentBuilder.append("__variadicParams");
	}
	
	public void visitCount(ParsingObject node){
		this.visit(node.get(0));
		this.currentBuilder.append(".length");
	}

}

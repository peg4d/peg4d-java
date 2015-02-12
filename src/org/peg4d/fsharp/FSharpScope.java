package org.peg4d.fsharp;

import java.util.ArrayList;

import org.peg4d.ParsingObject;
import org.peg4d.million.MillionTag;

public class FSharpScope {
	public String name;
	public ArrayList<String> path;
	public ParsingObject node;
	public ArrayList<FSharpVar> varList;
	public ArrayList<FSharpFunc> funcList;
	public int numOfArgs = 0;
	public boolean recursive = false;
	public enum ScopeType{
		OBJECT,
		FUNCTION
	}
	public ScopeType type;
	public ArrayList<ParsingObject> returnList;
	
	public FSharpScope(String name){
		this.name = name;
	}
	
	public FSharpScope(String name, ParsingObject node, ArrayList<String> path){
		this.name = name;
		this.node = node;
		this.varList = new ArrayList<FSharpVar>();
		this.funcList = new ArrayList<FSharpFunc>();
		this.returnList = new ArrayList<ParsingObject>();
		this.path = new ArrayList<String>();
		//deep copy
		for(int i = 0; i < path.size(); i++){
			this.path.add(path.get(i));
		}
		this.recursive = this.isRecursiveFunc(this.node, this.name, false);
		if(node.is(MillionTag.TAG_FUNC_DECL)){
			this.type = ScopeType.FUNCTION;
		} else if(node.is(MillionTag.TAG_OBJECT)){
			this.type = ScopeType.OBJECT;
		}
	}
	
	public String getScopeName(){
		return "ScopeOf" + this.getFullname();
	}
	
	public String getFullname(){
		String fullname = "";
		for(int i = 0; i < this.path.size(); i++){
			fullname += this.path.get(i) + "_";
		}
		fullname += this.name;
		return fullname;
	}
	
	public String getPathName(){
		String pathName = "";
		for(int i = 0; i < this.path.size(); i++){
			pathName += this.path.get(i) + ".";
		}
		pathName += this.name + ".";
		return pathName;
	}
	
	public boolean isRecursive(){
		return this.recursive;
	}
	
	private boolean isRecursiveFunc(ParsingObject node, String name, boolean result){
		boolean res = false;
		if(node.is(MillionTag.TAG_APPLY)){
			res = this.getFieldText(node.get(0)).contentEquals(name);
			if(res){
				node.get(0).setValue("_" + name);
			}
		} else {
			res = false;
		}
		if(node.size() >= 1 && !result){
			for(int i = 0; i < node.size(); i++){
				result = this.isRecursiveFunc(node.get(i), name, result);
			}
		}
		if(result){
			return true;
		}
		return res;
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
	
	public FSharpVar searchVar(String name){
		FSharpVar result = null;
		for(FSharpVar fv : this.varList){
			if(fv.getTrueName().contentEquals(name)){
				result = fv;
			}
		}
		return result;
	}
	
	public FSharpFunc searchFunc(String name){
		FSharpFunc result = null;
		for(FSharpFunc ff : this.funcList){
			if(ff.getTrueName().contentEquals(name)){
				result = ff;
			}
		}
		if(this.name.contentEquals(name)){
			result = new FSharpFunc("ok");
		}
		return result;
	}
}
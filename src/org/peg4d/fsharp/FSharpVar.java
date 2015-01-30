package org.peg4d.fsharp;

import java.util.ArrayList;

import org.peg4d.ParsingObject;

public class FSharpVar {
	public String name;
	public String fullname;
	public FSharpVar parent = null;	
	public int uniqueKey = 0;
	ParsingObject initialValue;
	
	public FSharpVar(String name, String prefixName){
		this.name = name;
		this.fullname = prefixName + name;
	}
	
	public FSharpVar(String name, String prefixName, ParsingObject initialValue){
		this.name = name;
		this.fullname = prefixName + name;
		this.initialValue = initialValue;
	}
	
	public String addChild(){
		String name = this.name + this.uniqueKey;
		this.uniqueKey++;
		return name;
	}
	
	public String getCurrentName(){
		return this.name + this.uniqueKey;
	}
	
	public String getTrueName(){
		return this.name;
	}
	
	public String getFullname(){
		return this.fullname;
	}
}
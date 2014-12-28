package org.peg4d.million;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingObjectVisitor;
import org.peg4d.ParsingTag;

public class SourceGenerator extends ParsingObjectVisitor{
	public String tab = "  ";
	public String newLine = "\n";
	public String lineComment = "//";
	public String commentBegin = "/*";
	public String commentEnd = "*/";
	public String eol = ";";
	public String listDelim = ", ";

	public String stringLiteralPrefix = "";
	public String intLiteralSuffix = "";

	public boolean readableCode = true;
	
	public SourceGenerator(){
		this.initBuilderList();
	}
	
	private List<SourceBuilder> builderList = new ArrayList<SourceBuilder>();
	protected SourceBuilder headerBuilder;
	protected SourceBuilder currentBuilder;
	
	protected final void visit(ParsingObject node, String prefix, String suffix){
		this.currentBuilder.append(prefix);
		this.visit(node);
		this.currentBuilder.append(suffix);
	}

	protected final void visit(ParsingObject node, char prefix, char suffix){
		this.currentBuilder.appendChar(prefix);
		this.visit(node);
		this.currentBuilder.appendChar(suffix);
	}

	protected void initBuilderList() {
		this.builderList.clear();
		this.headerBuilder = this.appendNewSourceBuilder();
		this.currentBuilder = this.appendNewSourceBuilder();
	}

	protected final SourceBuilder appendNewSourceBuilder() {
		SourceBuilder builder = new SourceBuilder();
		this.builderList.add(builder);
		return builder;
	}

	protected final SourceBuilder insertNewSourceBuilder() {
		SourceBuilder builder = new SourceBuilder();
		this.builderList.add(this.builderList.indexOf(this.currentBuilder), builder);
		return builder;
	}

	protected void generateImportLibrary(String LibName) {
		this.headerBuilder.appendNewLine("require ", LibName, this.eol);
	}
	
	@Override
	public void onUnsupported(ParsingObject po){
		System.err.println(this.getClass().getName() + ": Unsupported tag: " + po.getTag().toString());
		for(ParsingObject sub : po){
			this.visit(sub);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(SourceBuilder sourceElement : builderList){
			builder.append(sourceElement.toString());
		}
		return builder.toString();
	}

}

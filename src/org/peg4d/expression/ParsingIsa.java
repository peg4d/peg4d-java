package org.peg4d.expression;

import org.peg4d.ParsingContext;
import org.peg4d.ParsingTag;
import org.peg4d.pegcode.GrammarVisitor;

public class ParsingIsa extends ParsingCommand {
	int tableType;
	ParsingIsa(int tableType) {
		super("isa");
		this.tableType = tableType;
		this.minlen = 1;
	}
	@Override
	public String getParameters() {
		return " " + ParsingTag.tagName(this.tableType);
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		return context.matchSymbolTable(tableType);
	}
	@Override
	public void visit(GrammarVisitor visitor) {
		visitor.visitIsa(this);
	}
	
	public int getTableType() {
		return this.tableType;
	}
}
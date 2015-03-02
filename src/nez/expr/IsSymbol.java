package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;

public class IsSymbol extends Terminal {
	Tag table;
	IsSymbol(SourcePosition s, Tag table) {
		super(s);
		this.table = table;
	}
	@Override
	public String getPredicate() {
		return "is " + table.name;
	}
	@Override
	public String getInterningKey() {
		return "is " + table.name;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTableTop(table);
	}
}
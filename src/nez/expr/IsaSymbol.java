package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.util.UMap;

public class IsaSymbol extends Terminal {
	Tag table;
	IsaSymbol(SourcePosition s, Tag table) {
		super(s);
		this.table = table;
	}
	@Override
	public String getPredicate() {
		return "isa " + table.name;
	}
	@Override
	public String getInterningKey() {
		return "isa " + table.name;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return Accept;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTable(table);
	}
}
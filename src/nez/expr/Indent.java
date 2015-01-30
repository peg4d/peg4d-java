package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;

public class Indent extends Terminal {
	Indent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "indent";
	}
	@Override
	public String getInterningKey() {
		return "indent";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return Accept;
		}
		return Unconsumed;
	}
	@Override
	public boolean match(SourceContext context) {
		return context.matchSymbolTableTop(NezTag.Indent);
	}
}
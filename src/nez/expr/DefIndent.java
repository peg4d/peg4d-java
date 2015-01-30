package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;

public class DefIndent extends Unconsumed {
	DefIndent(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "defindent";
	}
	@Override
	public boolean match(SourceContext context) {
		String indent = context.getIndentText(context.pos);
		context.pushSymbolTable(NezTag.Indent, indent);
		return true;
	}
}
package nez.ast;

import nez.SourceContext;
import nez.expr.Expression;

public interface Node {
	public Node newNode(SourceContext source, long pos, Expression e);
	public Tag  getTag();
	public void setTag(Tag tag);
	public void setValue(Object value);
	public void setEndingPosition(long pos);
	public void expandAstToSize(int newSize);
	public void commitChild(int index, Node child);
}

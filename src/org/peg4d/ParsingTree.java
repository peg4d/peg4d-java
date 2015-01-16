package org.peg4d;

import org.peg4d.expression.ParsingExpression;

public interface ParsingTree {
	public ParsingTree newParsingTree(ParsingSource source, long pos, ParsingExpression e);
	public void setTag(ParsingTag tag);
	public void setValue(Object value);
	public void setEndingPosition(long pos);
	public void expandAstToSize(int newSize);
	public void commitChild(int index, ParsingTree child);
}

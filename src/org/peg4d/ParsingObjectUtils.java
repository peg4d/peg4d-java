package org.peg4d;

public class ParsingObjectUtils {
	public final static ParsingSource newStringSource(ParsingObject po) {
		ParsingSource s = po.getSource();
		return new StringSource(null, s.getResourceName(), s.linenum(po.getSourcePosition()), po.getText());
	}
}

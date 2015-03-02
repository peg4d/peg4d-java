package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.util.UList;

public class ByteMap extends Terminal {
	private boolean[] charMap; // Immutable
	ByteMap(SourcePosition s, int beginChar, int endChar) {
		super(s);
		this.charMap = newMap();
		appendRange(this.charMap, beginChar, endChar);
	}
	ByteMap(SourcePosition s, boolean[] b) {
		super(s);
		this.charMap = b;
	}
	
	public final static boolean[] newMap() {
		return new boolean[SourceContext.BinaryEOF];
	}

	public final static void appendRange(boolean[] b, int beginChar, int endChar) {
		for(int c = beginChar; c <= endChar; c++) {
			b[c] = true;
		}
	}

	@Override
	public String getPredicate() {
		return "byte " + StringUtils.stringfyByteMap(this.charMap);
	}
	@Override
	public String getInterningKey() { 
		return "[" +  StringUtils.stringfyByteMap(this.charMap);
	}
	
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return (charMap[ch]) ? Accept : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		if(this.charMap[context.byteAt(context.pos)]) {
			context.consume(1);
			return true;
		}
		return context.failure2(this);
	}


}

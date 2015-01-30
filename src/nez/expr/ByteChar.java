package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;

public class ByteChar extends Terminal {
	public int byteChar;
	ByteChar(SourcePosition s, int ch) {
		super(s);
		this.byteChar = ch;
	}
	@Override
	public String getPredicate() {
		return "byte " + byteChar;
	}
	@Override
	public String getInterningKey() { 
		return "'" + byteChar;
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public short acceptByte(int ch) {
		return (byteChar == ch) ? Accept : Reject;
	}
	@Override
	public boolean match(SourceContext context) {
		if(context.byteAt(context.pos) == this.byteChar) {
			context.consume(1);
			return true;
		}
		return context.failure2(this);
	}
}

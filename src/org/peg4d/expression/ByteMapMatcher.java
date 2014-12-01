package org.peg4d.expression;

import org.peg4d.ParsingContext;

public class ByteMapMatcher extends ParsingMatcher {
	public boolean bitMap[];
	ByteMapMatcher(int[] c) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < c.length; i++) { 
			if(c[i] > 0) {
				this.bitMap[i] = true;
			}
		}
	}
	ByteMapMatcher(int beginChar, int endChar) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < 256; i++) {
			if(beginChar <= i && i <= endChar) {
				this.bitMap[i] = true;
			}
			else {
				this.bitMap[i] = false;
			}
		}
	}
	ByteMapMatcher(int NotChar) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < 256; i++) { 
			this.bitMap[i] = true;
		}
		this.bitMap[NotChar] = false;
	}
	ByteMapMatcher(ByteMapMatcher notByteChoice, boolean eof) {
		this.bitMap = new boolean[257];
		for(int i = 0; i < 256; i++) { 
			this.bitMap[i] = !notByteChoice.bitMap[i];
		}
		this.bitMap[256] = eof;
	}
	@Override
	public boolean simpleMatch(ParsingContext context) {
		int c = context.source.byteAt(context.pos);
		if(this.bitMap[c]) {
			context.consume(1);
			return true;
		}
		context.failure(this);
		return false;
	}
}


package org.peg4d.query;

import java.io.UnsupportedEncodingException;

import org.peg4d.Grammar;
import org.peg4d.ParsingCharset;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingObjectUtils;
import org.peg4d.ParsingSource;
import org.peg4d.ParsingTag;

class StringSource extends ParsingSource {
	private byte[] utf8;
	long textLength;
	
	StringSource(Grammar peg, String sourceText) {
		super(peg, "(string)", 1);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length-1;
	}
	
	StringSource(Grammar peg, String fileName, long linenum, String sourceText) {
		super(peg, fileName, linenum);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length-1;
	}
	
	private final byte[] toZeroTerminalByteSequence(String s) {
		byte[] b = ParsingCharset.toUtf8(s);
		byte[] b2 = new byte[b.length+1];
		System.arraycopy(b, 0, b2, 0, b.length);
		return b2;
	}
	
	@Override
	public final long length() {
		return this.textLength;
	}

	@Override
	public final int byteAt(long pos) {
		if(pos < this.textLength) {
			return this.utf8[(int)pos] & 0xff;
		}
		return ParsingSource.EOF;
	}

	@Override
	public final int fastByteAt(long pos) {
		return this.utf8[(int)pos] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if(pos + text.length > this.textLength) {
			return false;
		}
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.utf8[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.utf8, (int)(startIndex), (int)(endIndex - startIndex), ParsingCharset.DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		int end = (int)pos;
		if(end >= this.utf8.length) {
			end = this.utf8.length;
		}
		for(int i = 0; i < end; i++) {
			if(this.utf8[i] == '\n') {
				count++;
			}
		}
		return count;
	}
}

// helper utility. future may be removed
public class Helper {
	public static ParsingSource loadLine(Grammar peg, String fileName, long linenum, String sourceText) {
		return new StringSource(peg, fileName, linenum, sourceText);
	}

	public static ParsingObject dummyRoot(ParsingObject target) {
		ParsingObjectUtils.newStringSource(target);
		ParsingObject dummyRoot = new ParsingObject(new ParsingTag("#$dummy_root$"), target.getSource(), 0);
		dummyRoot.append(target);
		return dummyRoot;
	}
}

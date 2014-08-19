package org.peg4d;

import java.io.UnsupportedEncodingException;

class StringSource extends ParsingSource {
	private byte[] textBuffer;
	StringSource(Grammar peg, String fileName, long linenum, String sourceText) {
		super(peg, fileName, linenum);
		this.textBuffer = UCharset.toUtf8(sourceText);
	}

//	StringSource(Grammar peg, String fileName) {
//		super(peg, fileName, 1);
//		try {
//			RandomAccessFile f = new RandomAccessFile(fileName, "r");
//			this.textBuffer = new byte[(int)f.length()];
//			f.read(this.textBuffer);
//			f.close();
//		}
//		catch(IOException e) {
//		}
//	}
	
	@Override
	public final long length() {
		return this.textBuffer.length;
	}

	@Override
	public final int charAt(long n) {
		return this.textBuffer[(int)n] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.textBuffer[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.textBuffer, (int)(startIndex), (int)(endIndex - startIndex), "UTF8");
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
}


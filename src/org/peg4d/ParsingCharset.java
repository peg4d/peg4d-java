package org.peg4d;

import java.io.UnsupportedEncodingException;

public abstract class ParsingCharset {
	public final static int MAX = 256;

	abstract ParsingCharset dup();
	public abstract int consume(ParsingSource s, long pos);
	abstract boolean hasByte(int c);
	abstract ParsingCharset appendByte(int c, int c2);
	abstract ParsingCharset appendChar(int c, int c2);
	
	public String key() {
		StringBuilder sb = new StringBuilder();
		this.key(sb);
		return sb.toString();
	}
	abstract void key(StringBuilder sb);

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		this.stringfy(sb);
		sb.append("]");
		return sb.toString();
	}
	abstract void stringfy(StringBuilder sb);

	public final static int parseAscii(String t) {
		if(t.startsWith("\\x")) {
			int c = ParsingCharset.hex(t.charAt(2));
			c = (c * 16) + ParsingCharset.hex(t.charAt(3));
			return c;
		}
		if(t.startsWith("\\u")) {
			return -1;
		}
		if(t.startsWith("\\") && t.length() > 1) {
			int c = t.charAt(1);
			switch (c) {
//			case 'a':  return '\007'; /* bel */
//			case 'b':  return '\b';  /* bs */
//			case 'e':  return '\033'; /* esc */
			case 'f':  return '\f';   /* ff */
			case 'n':  return '\n';   /* nl */
			case 'r':  return '\r';   /* cr */
			case 't':  return '\t';   /* ht */
			case 'v':  return '\013'; /* vt */
			}
			return c;
		}
		return -1;
	}

	public final static int parseUnicode(String t) {
		if(t.startsWith("\\u")) {
			int c = ParsingCharset.hex(t.charAt(2));
			c = (c * 16) + ParsingCharset.hex(t.charAt(3));
			c = (c * 16) + ParsingCharset.hex(t.charAt(4));
			c = (c * 16) + ParsingCharset.hex(t.charAt(5));
			return c;
		}
		return t.charAt(0);
	}

	private final static int E = 1;
	private final static int[] utf8len = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, E, E,
        0 /* EOF */
	};

	public final static int lengthOfUtf8(byte ch) {
		return utf8len[ch & 0xff];
	}

	public final static int lengthOfUtf8(int ch) {
		return utf8len[ch];
	}

	public final static String DefaultEncoding = "UTF8";
	
	public final static byte[] toUtf8(String text) {
		try {
			return text.getBytes(DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
			Main._Exit(1, "unsupported character: " + e);
		}
		return text.getBytes();
	}

	static int hex(int c) {
		if('0' <= c && c <= '9') {
			return c - '0';
		}
		if('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		if('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		return 0;
	}
}


package org.peg4d;



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


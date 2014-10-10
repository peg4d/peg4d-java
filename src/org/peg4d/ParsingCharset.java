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

	final static ParsingCharset newParsingCharset(String text) {
		ParsingCharset u = null;
		CharacterReader r = new CharacterReader(text);
		char ch = r.readChar();
		while(ch != 0) {
			char next = r.readChar();
			if(next == '-') {
				int ch2 = r.readChar();
				if(ch > 0 && ch2 < 128) {
					u = newParsingCharset(u, ch, ch2);
				}
				ch = r.readChar();
			}
			else {
				if(ch > 0 && ch < 128) {
					u = newParsingCharset(u, ch, ch);
				}
				ch = next; //r.readChar();
			}
		}
		if(u == null) {
			return new ByteCharset();
		}
		return u;
	}
	
	private static ParsingCharset newParsingCharset(ParsingCharset u, int c, int c2) {
		if(u == null) {
			if(c < 128 && c2 < 128) {
				return new ByteCharset(c, c2);
			}
			else {
				return new UnicodeRange(c, c2);
			}
		}
		return u.appendChar(c, c2);
	}
	
	
	final static ParsingCharset addText(ParsingCharset u, String t, String t2) {
		int c = parseAscii(t);
		int c2 = parseAscii(t2);
		if(c != -1 && c2 != -1) {
			if(u == null) {
				return new ByteCharset(c, c2);			
			}
			return u.appendByte(c, c2);
		}
		c = parseUnicode(t);
		c2 = parseUnicode(t2);
		if(u == null) {
			if(c < 128 && c2 < 128) {
				return new ByteCharset(c, c2);
			}
			else {
				return new UnicodeRange(c, c2);
			}
		}
		return u.appendChar(c, c2);
	}

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

	public final static String quoteString(char OpenChar, String Text, char CloseChar) {
		StringBuilder sb = new StringBuilder();
		formatQuoteString(sb, OpenChar, Text, CloseChar);
		return sb.toString();
	}

	public final static void formatQuoteString(StringBuilder sb, char OpenChar, String Text, char CloseChar) {
		char SlashChar = '\\';
		sb.append(OpenChar);
		int i = 0;
		for(; i < Text.length(); i = i + 1) {
			char ch = Main._GetChar(Text, i);
			if(ch == '\n') {
				sb.append(SlashChar);
				sb.append("n");
			}
			else if(ch == '\t') {
				sb.append(SlashChar);
				sb.append("t");
			}
			else if(ch == CloseChar) {
				sb.append(SlashChar);
				sb.append(ch);
			}
			else if(ch == '\\') {
				sb.append(SlashChar);
				sb.append(SlashChar);
			}
			else {
				sb.append(ch);
			}
		}
		sb.append(CloseChar);
	}
	
	final static String unquoteString(String text) {
		if(text.indexOf("\\") == -1) {
			return text;
		}
		CharacterReader r = new CharacterReader(text);
		StringBuilder sb = new StringBuilder();
		while(r.hasChar()) {
			char ch = r.readChar();
			if(ch == '0') {
				break;
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	final static int parseInt(String text, int defval) {
		if(text.length() > 0) {
			try {
				return Integer.parseInt(text);
			}
			catch(NumberFormatException e) {
				//e.printStackTrace();
			}
		}
		return defval;
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

	public static int getFirstChar(byte[] text) {
		return text[0] & 0xff;
	}
	public static int getFirstChar(String text) {
		char ch = text.charAt(0);
		if(ch < 128) {
			return ch;
		}
		return getFirstChar(toUtf8(text));
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

class ByteCharset extends ParsingCharset {
	boolean[] bitMap;
	UList<UnicodeRange> unicodeRangeList;

	ByteCharset() {
		this.bitMap = new boolean[ParsingSource.EOF+1];
	}

	ByteCharset(int c, int c2) {
		this();
		this.appendByte(c, c2);
	}
	
	@Override
	ParsingCharset dup() {
		ByteCharset b = new ByteCharset();
		for(int i = 0; i < bitMap.length; i++) {
			b.bitMap[i] = this.bitMap[i];
		}
		if(this.unicodeRangeList != null) {
			for(int i = 0; i < unicodeRangeList.size(); i++) {
				b.addRange((UnicodeRange)unicodeRangeList.ArrayValues[i].dup());
			}
		}
		return b;
	}

	void addRange(UnicodeRange r) {
		if(this.unicodeRangeList == null) {
			this.unicodeRangeList = new UList<UnicodeRange>(new UnicodeRange[2]);
		}
		this.unicodeRangeList.add(r);
	}
	
	@Override
	public final int consume(ParsingSource s, long pos) {
		int c = s.byteAt(pos);
		if(this.unicodeRangeList == null) {
			return this.bitMap[c] ? 1 : 0;
		}
		if(!this.bitMap[c]) {
			int u = s.charAt(pos);
			for(int i = 0; i < this.unicodeRangeList.size(); i++) {
				UnicodeRange r = this.unicodeRangeList.ArrayValues[i];
				if(r.matchChar(u)) {
					return s.charLength(pos);
				}
			}
			return 0;
		}
		return 1;
	}

	@Override
	public boolean hasByte(int c) {
		return this.bitMap[c];
	}

	@Override
	public final ParsingCharset appendByte(int c, int c2) {
		for(int i = c; i <= c2; i++) {
			this.bitMap[i] = true;
		}
		return this;
	}
			
	@Override
	public final ParsingCharset appendChar(int c, int c2) {
		if(c < 128 && c2 < 128) {
			return this.appendByte(c, c2);
		}
		if(this.unicodeRangeList != null) {
			for(int i = 0; i < unicodeRangeList.size(); i++) {
				UnicodeRange r = unicodeRangeList.ArrayValues[i];
				if(r.canMargeRange(c, c2)) {
					return this;
				}
				if(c2 < r.beginChar) {
					this.unicodeRangeList.add(i, new UnicodeRange(c, c2));
					return this;
				}
			}
		}
		this.addRange(new UnicodeRange(c, c2));
		return this;
	}
	
	@Override
	void stringfy(StringBuilder sb) {
		for(int ch = 0; ch < this.bitMap.length; ch++) {
			if(!this.bitMap[ch]) {
				continue;
			}
			sb.append(stringfy(ch));
			int ch2 = findLetterOrDigitRange(ch+1);
			if(ch2 > ch) {
				sb.append("-");
				sb.append(stringfy(ch2));
				ch = ch2;
			}
		}
		if(this.unicodeRangeList != null) {
			for(int i = 0; i < unicodeRangeList.size(); i++) {
				unicodeRangeList.ArrayValues[i].stringfy(sb);
			}
		}
	}

	private int findLetterOrDigitRange(int start) {
		if(!Character.isLetter(start) && !Character.isDigit(start)) {
			return start - 1;
		}
		for(int ch = start; ch < this.bitMap.length; ch++) {
			if(!this.bitMap[ch]) {
				return ch - 1;
			}
			if(!Character.isLetter(ch) && !Character.isDigit(ch)) {
				return ch - 1;
			}
		}
		return this.bitMap.length;
	}

	private String stringfy(int c) {
		char ch = (char)c;
		switch(c) {
		case '\n' : return "\\n";
		case '\t' : return "\\t";
		case '\r' : return "\\r";
		case '\\' : return "\\\\";
		case '-' : return "\\-";
		case ']' : return "\\]";
		}
		if(Character.isISOControl(ch)) {
			return String.format("\\x%x", c);
		}
		return "" + ch;
	}
	
	private void decodeByte(int start, int b) {
		this.bitMap[start]   = ((b & 128) == 128);
		this.bitMap[start+1] = ((b &  64) ==  64);
		this.bitMap[start+2] = ((b &  32) ==  32);
		this.bitMap[start+3] = ((b &  16) ==  16);
		this.bitMap[start+4] = ((b &   8) ==   8);
		this.bitMap[start+5] = ((b &   4) ==   4);
		this.bitMap[start+6] = ((b &   2) ==   2);
		this.bitMap[start+7] = ((b &   1) ==   1);
	}

	private int encodeByte(int start) {
		int b = 0;
		b |= (this.bitMap[start]   ? 128 : 0);
		b |= (this.bitMap[start+1] ?  64 : 0);
		b |= (this.bitMap[start+2] ?  32 : 0);
		b |= (this.bitMap[start+3] ?  16 : 0);
		b |= (this.bitMap[start+4] ?   8 : 0);
		b |= (this.bitMap[start+5] ?   4 : 0);
		b |= (this.bitMap[start+6] ?   2 : 0);
		b |= (this.bitMap[start+7] ?   1 : 0);
		return b;
	}
	
	void decode(byte[] b32) {
		for(int i = 0; i < b32.length; i++) {
			decodeByte(i*8, b32[i] & 0xff);
		}
	}

	void encode(byte[] b32) {
		for(int i = 0; i < b32.length; i++) {
			b32[i] = (byte)encodeByte(i*8);
		}
	}
	
	final boolean hasBinary() {
		for(int i = 128; i < 256; i++) {
			if(bitMap[i]) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	void key(StringBuilder sb) {
		for(int i = 0; i < 16; i++) {
			sb.append(String.format("%02x", encodeByte(i*8)));
		}
		if(hasBinary()) {
			for(int i = 16; i < 32; i++) {
				sb.append(String.format("%02x", encodeByte(i*8)));
			}
		}
		if(this.unicodeRangeList != null) {
			for(int i = 0; i < this.unicodeRangeList.size(); i++) {
				UnicodeRange r = this.unicodeRangeList.ArrayValues[i];
				r.key(sb);
			}
		}
	}

	final int size() {
		int count = 0;
		for(int i = 0; i < 256; i++) {
			if(bitMap[i]) {
				count++;
			}
		}
		return count;
	}
}

class UnicodeRange extends ParsingCharset {
	int beginChar;
	int endChar;
	UnicodeRange(int c, int c2) {
		this.beginChar = c;
		this.endChar = c2;
	}
	
	@Override
	ParsingCharset dup() {
		return new UnicodeRange(beginChar, endChar);
	}
	
	@Override
	public final int consume(ParsingSource s, long pos) {
		int c = s.charAt(pos);
		if(matchChar(c)) {
			return s.charLength(pos);
		}
		return 0;
	}
	
	final boolean matchChar(int c) {
		return this.beginChar <= c && c <= this.endChar;
	}
	
	@Override
	public boolean hasByte(int c) {
		return false; // TODO
	}

	@Override
	public ParsingCharset appendByte(int c, int c2) {
		ByteCharset b = new ByteCharset(c, c2);
		b.addRange(this);
		return b;
	}
	@Override
	public ParsingCharset appendChar(int c, int c2) {
		if(this.canMargeRange(c, c2)) {
			return this;
		}
		ByteCharset b = new ByteCharset();
		b.addRange(new UnicodeRange(c, c2));
		return b;
	}
	final boolean canMargeRange(int begin1, int end1) {
		boolean res = false;
		//System.out.println("merge.char begin=" + beginChar + "," + endChar);
		if(end1 + 1 >= beginChar && begin1 < endChar) {
			//System.out.println("end1="+end1+" > , begin="+ beginChar);
			this.beginChar = Math.min(beginChar, begin1);
			this.endChar = Math.max(endChar, end1);
			res = true;
		}
		else if(endChar + 1 >= begin1 && beginChar < end1) {
			//System.out.println("end="+endChar+" > , begin1="+ begin1);
			this.beginChar = Math.min(beginChar, begin1);
			this.endChar = Math.max(endChar, end1);
			res = true;
		}
		//System.out.println("merge.char c=" + begin1 + "," + end1 + " res=" + res);
		return res;
	}
	@Override
	void stringfy(StringBuilder sb) {
		if(this.beginChar == this.endChar) {
			sb.append(String.format("\\u%04x", this.beginChar));
		}
		else {
			sb.append(String.format("\\u%04x", this.beginChar));
			sb.append("-");
			sb.append(String.format("\\u%04x", this.endChar));
		}
	}

	@Override
	void key(StringBuilder sb) {
		if(this.beginChar == this.endChar) {
			sb.append(String.format("U+%04x", this.beginChar));
		}
		else {
			sb.append(String.format("U+%04x", this.beginChar));
			sb.append("-");
			sb.append(String.format("U+%04x", this.endChar));
		}
	}
		
}



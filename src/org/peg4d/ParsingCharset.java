package org.peg4d;

import java.io.UnsupportedEncodingException;

public abstract class ParsingCharset {
	public final static int MAX = 256;

	public abstract int consume(ParsingSource s, long pos);
	public abstract boolean hasByte(int c);
	public abstract ParsingCharset appendByte(int c, int c2);
	public abstract boolean mergeChar(int c, int c2);
	public abstract ParsingCharset appendChar(int c, int c2);
	public abstract ParsingCharset merge(ParsingCharset u);

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
				return new RangeCharset(c, c2);
			}
		}
		return u.appendChar(c, c2);
	}
		
	static ParsingCharset addText(ParsingCharset u, String t, String t2) {
		int c = parseAscii(t);
		int c2 = parseAscii(t2);
		if(c != -1 && c2 != -1) {
			if(u == null) {
				return new ByteCharset(c, c2);			
			}
			return u.appendByte(c, c2);
		}
		c = parseUnicode(t);
		c2 = parseUnicode(t);
		if(u == null) {
			if(c < 128 && c2 < 128) {
				return new ByteCharset(c, c2);
			}
			else {
				return new RangeCharset(c, c2);
			}
		}
		return u.appendChar(c, c2);
	}

	static int parseAscii(String t) {
		if(t.startsWith("\\x")) {
			int c = ParsingCharset.hex(t.charAt(2));
			c = (c * 16) + ParsingCharset.hex(t.charAt(3));
			return c;
		}
		if(t.startsWith("\\")) {
			int c = t.charAt(1);
			switch (c) {
			case 'a':  return '\007'; /* bel */
			case 'b':  return '\b';  /* bs */
			case 'e':  return '\033'; /* esc */
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

	static int parseUnicode(String t) {
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
	
//	final static String _QuoteString(String OpenQuote, String Text, String CloseQuote) {
//		StringBuilder sb = new StringBuilder();
//		sb.append(OpenQuote);
//		int i = 0;
//		for(; i < Text.length(); i = i + 1) {
//			char ch = Main._GetChar(Text, i);
//			if(ch == '\n') {
//				sb.append("\\n");
//			}
//			else if(ch == '\t') {
//				sb.append("\\t");
//			}
//			else if(ch == '"') {
//				sb.append("\\\"");
//			}
//			else if(ch == '\'') {
//				sb.append("\\'");
//			}
//			else if(ch == '\\') {
//				sb.append("\\\\");
//			}
//			else {
//				sb.append(ch);
//			}
//		}
//		sb.append(CloseQuote);
//		return sb.toString();
//	}

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

	public static int parseHex2(String t) {
		return hex(t.charAt(t.length()-2)) * 16 + hex(t.charAt(t.length()-1)); 
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

class ChoiceCharset extends ParsingCharset {
	UList<ParsingCharset> choice;
	
	ChoiceCharset(ParsingCharset a, ParsingCharset b) {
		this.choice = new UList<ParsingCharset>(new ParsingCharset[2]);
		this.choice.add(a);
		this.choice.add(b);
	}
	
	@Override
	public final int consume(ParsingSource s, long pos) {
		for(int i = 0; i < choice.size(); i++) {
			int c = choice.ArrayValues[i].consume(s, pos);
			if(c > 0) {
				return c;
			}
		}
		return 0;
	}

	@Override
	public boolean hasByte(int c) {
		for(int i = 0; i < choice.size(); i++) {
			if(choice.ArrayValues[i].hasByte(c)) {
				return true;
			}
		}
		return false;
	}
	
	private void append(ParsingCharset b) {
		this.choice.add(b);
	}
	
	@Override
	public final ParsingCharset appendByte(int c, int c2) {
		for(int i = 0; i < choice.size(); i++) {
			if(choice.ArrayValues[i] instanceof ByteCharset) {
				choice.ArrayValues[i].appendByte(c, c2);
				return this;
			}
		}
		this.append(new ByteCharset(c, c2));
		return this;
	}
		
	@Override
	public final ParsingCharset appendChar(int c, int c2) {
		if(c < 128 && c2 < 128) {
			return this.appendByte(c, c2);
		}
		for(int i = 0; i < choice.size(); i++) {
			if(choice.ArrayValues[i].mergeChar(c, c2)) {
				return this;
			}
		}
		this.append(new RangeCharset(c, c2));
		return this;
	}

	@Override
	public boolean mergeChar(int c, int c2) {
		for(int i = 0; i < choice.size(); i++) {
			if(choice.ArrayValues[i].mergeChar(c, c2)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < choice.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			sb.append(choice.ArrayValues[i].toString());
		}
		return sb.toString();
	}

	@Override
	public ParsingCharset merge(ParsingCharset u) {
		// TODO Auto-generated method stub
		return null;
	}

}

class RangeCharset extends ParsingCharset {
	int begin;
	int end;
	RangeCharset(int c, int c2) {
		this.begin = c;
		this.end = c2;
	}
	@Override
	public final int consume(ParsingSource s, long pos) {
		int c = s.charAt(pos);
		if(this.begin <= c && c <= this.end) {
			return s.charLength(pos);
		}
		return 0;
	}
	@Override
	public boolean hasByte(int c) {
		return false;
	}
	@Override
	public ParsingCharset appendByte(int c, int c2) {
		return new ChoiceCharset(new ByteCharset(c, c2), this);	
	}
	@Override
	public boolean mergeChar(int c, int c2) {
		boolean res = false;
		if(begin <= c2 + 1) {
			if(c < begin) {
				begin = c;
			}
			res = true;
		}
		if(c + 1 <= end) {
			if(end < c2) {
				end = c2;
			}
			res = true;
		}
		return res;
	}
	@Override
	public ParsingCharset appendChar(int c, int c2) {
		if(this.mergeChar(c, c2)) {
			return this;
		}
		return new ChoiceCharset(this, new RangeCharset(c, c2));	
	}
	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		if(this.begin == this.end) {
			sb.append("'");
			sb.append((char)this.begin);
			sb.append("'");
		}
		else {
			sb.append("[");
			sb.append((char)this.begin);
			sb.append("-");
			sb.append((char)this.end);
			sb.append("]");
		}
		return sb.toString();
	}
	@Override
	public ParsingCharset merge(ParsingCharset u) {
		// TODO Auto-generated method stub
		return null;
	}
}

class ByteCharset extends ParsingCharset {
	boolean[] asciiBitMap;
	
	ByteCharset() {
		this.asciiBitMap = new boolean[ParsingSource.EOF+1];
	}
	ByteCharset(int c, int c2) {
		this();
		this.appendByte(c, c2);
	}
	@Override
	public final int consume(ParsingSource s, long pos) {
		return this.asciiBitMap[s.byteAt(pos)] ? 1 : 0;
	}

	@Override
	public boolean hasByte(int c) {
		return this.asciiBitMap[c];
	}

	@Override
	public final ParsingCharset appendByte(int c, int c2) {
		for(int i = c; i <= c2; i++) {
			this.asciiBitMap[i] = true;
		}
		return this;
	}
	
	@Override
	public boolean mergeChar(int c, int c2) {
		return false;
	}

	@Override
	public final ParsingCharset appendChar(int c, int c2) {
		if(c < 128 && c2 < 128) {
			return this.appendByte(c, c2);
		}
		return new ChoiceCharset(this, new RangeCharset(c, c2));
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int ch = 0; ch < this.asciiBitMap.length; ch++) {
			if(!this.asciiBitMap[ch]) {
				continue;
			}
			sb.append(stringfy(ch));
			int ch2 = findRange(ch+1);
			if(ch2 > ch) {
				sb.append("-");
				sb.append(stringfy(ch2));
				ch = ch2;
			}
		}
		sb.append("]");
		return sb.toString();
	}

	private int findRange(int start) {
		for(int ch = start; ch < this.asciiBitMap.length; ch++) {
			if(!this.asciiBitMap[ch]) {
				return ch - 1;
			}
		}
		return this.asciiBitMap.length;
	}

	private String stringfy(int c) {
		char ch = (char)c;
		switch(c) {
		case '\n' : return "\\n";
		case '\t' : return "\\t";
		case '\r' : return "\\r";
		case '\\' : return "\\\\";
		}
		if(Character.isISOControl(ch)) {
			return String.format("\\x%x", c);
		}
		return "" + ch;
	}
	
	@Override
	public ParsingCharset merge(ParsingCharset u) {
		if(u instanceof ByteCharset) {
			ByteCharset ub = (ByteCharset)u;
			for(int i = 0; i < ub.asciiBitMap.length; i++) {
				if(ub.asciiBitMap[i]) {
					this.asciiBitMap[i] = true;
				}
			}
			return this;
		}
		if(u instanceof ChoiceCharset) {
			ChoiceCharset cc = (ChoiceCharset)u;
			ParsingCharset p = this;
			for(int i = 0; i < cc.choice.size(); i++) {
				p = p.merge(cc.merge(cc.choice.ArrayValues[i]));
			}
			return p;
		}
		return new ChoiceCharset(this, u);
	}
	
}

//class ByteCharset2 extends ParsingCharset {
//	String    text;
//	boolean[] asciiBitMap;
//	int size = 0;
//
//	public ByteCharset2(String charSet) {
//		this.text = charSet;
//		this.asciiBitMap = new boolean[MAX];
//		if(charSet.length() > 0) {
//			this.parse(charSet);
//		}
//		//System.out.println("U: " + text);
//	}
//
//
//
//	public final boolean hasChar(int ch) {
//		if(ch < MAX) {
//			return this.asciiBitMap[ch];
//		}
//		return false;
//	}
//	
//	public final String key() {
//		return text;  // fixme
//	}
//
//	public final boolean match(int ch) {
//		return this.asciiBitMap[ch];
//	}
//
//	final void set(int ch) {
//		assert(ch < 127);
//		if(this.asciiBitMap[ch] == false) {
//			this.size += 1;
//			this.asciiBitMap[ch] = true;
//		}
//	}
//	
//	private void setRange(int ch, int ch2) {
//		for(;ch <= ch2; ch++) {
//			this.set(ch);
//		}
//	}
//
//
//	public final void append(ParsingCharset charset) {
//		for(int i = 0; i < MAX; i++) {
//			if(charset.asciiBitMap[i]) {
//				this.set(i);
//			}
//		}
//		this.text += charset.text;
//	}
//
//	public final void append(int ch) {
//		this.set(ch);
//		this.text += (char)ch;
//		//System.out.println("aU: " + text);
//	}
//
//
//}

class CharacterReader {
	String text;
	int pos;
	CharacterReader(String text) {
		this.text = text;
		this.pos = 0;
	}

	final boolean hasChar() {
		return (pos < this.text.length());
	}
	
	final char readChar() {
		if(this.pos < this.text.length()) {
			char ch = this.read(this.pos);
			if(ch == '\\') {
				char ch1 = this.read(this.pos+1);
				if(ch1 == 'u' || ch1 == 'U') {
					ch = this.readUtf(this.read(this.pos+2), this.read(this.pos+3), this.read(this.pos+4), this.read(this.pos+5));
					this.pos = this.pos + 5;
				}
				else if(ch1 == 'x' || ch1 == 'X') {
					ch = this.readAscii(this.read(this.pos+2), this.read(this.pos+3));
					this.pos = this.pos + 3;
				}
				else {
					ch = this.readEsc(ch1);
					this.pos = this.pos + 1;
				}
			}
			this.pos = this.pos + 1;
			return ch;
		}
		return '\0';
	}
	
	private char read(int pos) {
		if(pos < this.text.length()) {
			return Main._GetChar(this.text, pos);
		}
		return 0;
	}

	private char readEsc(char ch1) {
		switch (ch1) {
		case 'a':  return '\007'; /* bel */
		case 'b':  return '\b';  /* bs */
		case 'e':  return '\033'; /* esc */
		case 'f':  return '\f';   /* ff */
		case 'n':  return '\n';   /* nl */
		case 'r':  return '\r';   /* cr */
		case 't':  return '\t';   /* ht */
		case 'v':  return '\013'; /* vt */
		}
		return ch1;
	}

	private char readUtf(char ch1, char ch2, char ch3, char ch4) {
		int c = ParsingCharset.hex(ch1);
		c = (c * 16) + ParsingCharset.hex(ch2);
		c = (c * 16) + ParsingCharset.hex(ch3);
		c = (c * 16) + ParsingCharset.hex(ch4);
		return (char)c;
	}

	private char readAscii(char ch1, char ch2) {
		int c = ParsingCharset.hex(ch1);
		c = (c * 16) + ParsingCharset.hex(ch2);
		return (char)c;
	}


}



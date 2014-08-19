package org.peg4d;

import java.io.UnsupportedEncodingException;

public class UCharset {
	public final static int MAX = 256;
	String    text;
	boolean[] asciiBitMap;
	int size = 0;

	public UCharset(String charSet) {
		this.text = charSet;
		this.asciiBitMap = new boolean[MAX];
		if(charSet.length() > 0) {
			this.parse(charSet);
		}
		//System.out.println("U: " + text);
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		for(int ch = 0; ch < MAX; ch++) {
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
		return sb.toString();
	}

	private int findRange(int start) {
		for(int ch = start; ch < MAX; ch++) {
			if(!this.asciiBitMap[ch]) {
				return ch - 1;
			}
		}
		return MAX-1;
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

	public final boolean hasChar(int ch) {
		if(ch < MAX) {
			return this.asciiBitMap[ch];
		}
		return false;
	}
	
	public final String key() {
		return text;  // fixme
	}

	public final boolean match(int ch) {
		return this.asciiBitMap[ch];
	}

	final void set(int ch) {
		assert(ch < 127);
		if(this.asciiBitMap[ch] == false) {
			this.size += 1;
			this.asciiBitMap[ch] = true;
		}
	}
	
	private void setRange(int ch, int ch2) {
		for(;ch <= ch2; ch++) {
			this.set(ch);
		}
	}

	private void parse(String text) {
		CharacterReader r = new CharacterReader(text);
		char ch = r.readChar();
		while(ch != 0) {
			char next = r.readChar();
			if(next == '-') {
				this.setRange(ch, r.readChar());
				ch = r.readChar();
			}
			else {
				this.set(ch);
				ch = next; //r.readChar();
			}
		}
	}

	public final void append(UCharset charset) {
		for(int i = 0; i < MAX; i++) {
			if(charset.asciiBitMap[i]) {
				this.set(i);
			}
		}
		this.text += charset.text;
	}

	public final void append(int ch) {
		this.set(ch);
		this.text += (char)ch;
		//System.out.println("aU: " + text);
	}

	public static final String _QuoteString(char OpenChar, String Text, char CloseChar) {
		char SlashChar = '\\';
		StringBuilder sb = new StringBuilder();
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
		return sb.toString();
	}

	public static final String _QuoteString(String OpenQuote, String Text, String CloseQuote) {
		StringBuilder sb = new StringBuilder();
		sb.append(OpenQuote);
		int i = 0;
		for(; i < Text.length(); i = i + 1) {
			char ch = Main._GetChar(Text, i);
			if(ch == '\n') {
				sb.append("\\n");
			}
			else if(ch == '\t') {
				sb.append("\\t");
			}
			else if(ch == '"') {
				sb.append("\\\"");
			}
			else if(ch == '\'') {
				sb.append("\\'");
			}
			else if(ch == '\\') {
				sb.append("\\\\");
			}
			else {
				sb.append(ch);
			}
		}
		sb.append(CloseQuote);
		return sb.toString();
	}

	public static final String _UnquoteString(String text) {
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

	public static final String _QuoteString(String Text) {
		return UCharset._QuoteString("\"", Text, "\"");
	}

	public final static int parseInt(String text, int defval) {
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

	private final static int[] utf8len = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0,
	};

	public final static int lengthOfUtf8(byte ch) {
		return utf8len[ch & 0xff];
	}

	public final static int lengthOfUtf8(int ch) {
		return utf8len[ch];
	}

	public final static byte[] toUtf8(String text) {
		try {
			return text.getBytes("UTF8");
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
}

class CharacterReader {
	String text;
	int pos;
	public CharacterReader(String text) {
		this.text = text;
		this.pos = 0;
	}

	public final boolean hasChar() {
		return (pos < this.text.length());
	}
	
	public final char readChar() {
		if(this.pos < this.text.length()) {
			char ch = this.read(this.pos);
			if(ch == '\\') {
				char ch1 = this.read(this.pos+1);
				if(ch1 == 'u' || ch1 == 'U') {
					ch = this.readUtf(this.read(this.pos+2), this.read(this.pos+3), this.read(this.pos+4), this.read(this.pos+5));
					this.pos = this.pos + 5;
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
		int c = this.hex(ch1);
		c = (c * 16) + this.hex(ch2);
		c = (c * 16) + this.hex(ch3);
		c = (c * 16) + this.hex(ch4);
		return (char)c;
	}

	private int hex(int c) {
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



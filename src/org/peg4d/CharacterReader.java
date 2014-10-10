package org.peg4d;

public class CharacterReader {
	String text;
	int pos;
	public CharacterReader(String text) {
		this.text = text;
		this.pos = 0;
	}

	final boolean hasChar() {
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
		case 'b':  return '\b';   /* bs */
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

}
package org.peg4d;



public class Utils {
	public final static String quoteString(char OpenChar, String Text, char CloseChar) {
		StringBuilder sb = new StringBuilder();
		Utils.formatQuoteString(sb, OpenChar, Text, CloseChar);
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
}

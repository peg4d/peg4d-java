package org.peg4d;


public class UStringBuilder {
	public final UList<String> slist = new UList<String>(new String[128]);
	protected int indentLevel = 0;
	protected String currentIndentString = "";
	protected char lastChar   = '\n';
	protected String LineFeed = "\n";
	protected String Tabular  = "   ";

	public UStringBuilder() {
	}

	public final boolean isEmpty(String Text) {
		return (Text == null || Text.length() == 0);
	}

	public final void append(String Source) {
		if(!this.isEmpty(Source)) {
			this.slist.add(Source);
			this.lastChar = MainOption._GetChar(Source, Source.length()-1);
		}
	}

	public final void append(String Text, String Text2) {
		this.slist.add(Text);
		this.slist.add(Text2);
	}

	public final void append(String Text, String Text2, String Text3) {
		this.slist.add(Text);
		this.slist.add(Text2);
		this.slist.add(Text3);
	}

	public final void appendInt(int Value) {
		this.slist.add("" + Value);
	}

	public final void AppendQuotedText(String Text) {
		this.slist.add(UCharset._QuoteString(Text));
	}

	public final void AppendLineFeed() {
		if(this.lastChar != '\n') {
			this.slist.add(this.LineFeed);
		}
	}

	public final void openIndent() {
		this.indentLevel = this.indentLevel + 1;
		this.currentIndentString = null;
	}

	public final void openIndent(String Text) {
		if(Text != null && Text.length() > 0) {
			this.append(Text);
		}
		this.openIndent();
	}

	public final void closeIndent() {
		this.indentLevel = this.indentLevel - 1;
		this.currentIndentString = null;
		assert(this.indentLevel >= 0);
	}

	public final void closeIndent(String Text) {
		this.closeIndent();
		if(Text != null && Text.length() > 0) {
			this.appendNewLine(Text);
		}
	}

	public final int SetIndentLevel(int IndentLevel) {
		int Level = this.indentLevel;
		this.indentLevel = IndentLevel;
		this.currentIndentString = null;
		return Level;
	}

	private final void AppendIndentString() {
		if (this.currentIndentString == null) {
			this.currentIndentString = this.joinStrings(this.Tabular, this.indentLevel);
		}
		this.slist.add(this.currentIndentString);
	}

	public final String joinStrings(String Unit, int Times) {
		String s = "";
		int i = 0;
		while(i < Times) {
			s = s + Unit;
			i = i + 1;
		}
		return s;
	}

	public final void appendNewLine() {
		this.AppendLineFeed();
		this.AppendIndentString();
	}

	public final void appendNewLine(String Text) {
		this.appendNewLine();
		this.append(Text);
	}

	public final void appendNewLine(String Text, String Text2) {
		this.appendNewLine();
		this.append(Text);
		this.append(Text2);
	}

	public final void appendNewLine(String Text, String Text2, String Text3) {
		this.appendNewLine();
		this.append(Text);
		this.append(Text2);
		this.append(Text3);
	}

	public final void clear() {
		this.slist.clear(0);
	}

	@Override public final String toString() {
		return MainOption._SourceBuilderToString(this);
	}
	
	public final void show() {
		String s = this.toString();
		MainOption._PrintLine(s);
	}
}

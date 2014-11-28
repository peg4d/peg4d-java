package org.peg4d.konoha;

public class KSourceBuilder {
	private StringBuilder builder;
	private StringBuilder commentOfThisLine;
	
	public String newLineString = "\n";
	public String quoteString = "\"";
	public String indentString = "  ";
	public String singleLineCommentString = "// ";
	public String commentBeginString = "/* ";
	public String commentEndString = " */";

	public KSourceBuilder(){
		this(4096);
	}
	
	public KSourceBuilder(int capacity){
		this.builder = new StringBuilder(capacity);
	}
	
	int IndentLevel = 0;
	String currentIndentString = "";
	String BufferedLineComment = "";

	public final int GetPosition() {
		return this.builder.length();
	}

	public final String substring(int startIndex, int endIndex) {
		return this.builder.substring(startIndex, endIndex);
	}

	public final void append(String text) {
		this.builder.append(text);
	}

	public final void appendNumber(int value) {
		this.builder.append(value);
	}

	public final void append(String... texts) {
		for(int i = 0, n = texts.length; i < n; ++i){
			this.append(texts[i]);
		}
	}
	
	public final void appendChar(char c){
		this.builder.appendCodePoint(c);
	}
	
	public final void appendSpace(){
		this.builder.appendCodePoint(' ');
	}
	
	public final void appendQuoted(String text) {
		this.builder.append(this.quoteString);
		this.builder.append(text);
		this.builder.append(this.quoteString);
	}

	public final void appendLineFeed() {
		this.appendLineFeed(true);
	}

	public final void appendLineFeed(boolean appendIndent) {
		if (this.commentOfThisLine != null) {
			this.builder.append(this.commentOfThisLine);
			this.commentOfThisLine = null;
		}
		this.builder.append(this.newLineString);
		if(appendIndent) {
			this.AppendIndent();
		}
	}

	public final boolean endsWith(String str) {
		return this.builder.indexOf(str, this.builder.length() - str.length()) > -1;
	}
	
	public final boolean endsWith(char c) {
		if(this.builder.length() == 0) return false;
		return this.builder.charAt(this.builder.length() - 1) == c;
	}

	public final void appendSmartSpace() {
		if(this.builder.length() == 0) return;
		char last = this.builder.charAt(this.builder.length() - 1);
		if(last == ' ' || last == '\n' || last == '\r' || last == '\t') return;
		this.builder.appendCodePoint(' ');
	}

	public final void appendToken(String str) {
		this.appendSmartSpace();
		this.append(str);
	}

	public final void appendCommentLine(String str) {
		this.appendCommentLine(str, true);
	}
	
	public final void appendCommentLine(String str, boolean appendIndent) {
		if(this.commentOfThisLine != null){
			this.commentOfThisLine = new StringBuilder(128);
		}
		if (this.singleLineCommentString == null) {
			this.commentOfThisLine.append(this.commentBeginString);
			this.commentOfThisLine.append(str);
			this.commentOfThisLine.append(this.commentEndString);
		} else {
			this.builder.append(this.singleLineCommentString);
			this.builder.append(str);
		}
		this.builder.append(this.newLineString);
	}

	public final void indent() {
		this.IndentLevel = this.IndentLevel + 1;
		this.currentIndentString = null;
	}

	public final void unIndent() {
		this.IndentLevel = this.IndentLevel - 1;
		this.currentIndentString = null;
		if(this.IndentLevel < 0){
			this.IndentLevel = 0;
		}
	}

	private final String getIndentString() {
		if(this.currentIndentString == null){
			StringBuilder indentBuilder = new StringBuilder(64);
			for(int i = 0; i < this.IndentLevel; ++i){
				indentBuilder.append(this.indentString);
			}
			this.currentIndentString = indentBuilder.toString();
		}
		return this.currentIndentString;
	}

	public final void AppendIndent() {
		this.builder.append(this.getIndentString());
	}

	public final void appendNewLine() {
		this.builder.append(this.newLineString);
		this.builder.append(this.getIndentString());
	}

	public final void appendNewLine(String Text) {
		this.appendNewLine();
		this.append(Text);
	}
	
	public final void appendNewLine(String... texts) {
		this.appendNewLine();
		for(int i = 0, n = texts.length; i < n; ++i){
			this.append(texts[i]);
		}
	}

	public final void openIndent(String text) {
		this.append(text);
		this.indent();
	}

	public final void closeIndent(String text) {
		this.unIndent();
		if(text != null && text.length() > 0) {
			this.appendNewLine();
			this.append(text);
		}
	}


	public final void appendIndentAndText(String str) {
		this.builder.append(this.getIndentString());
		this.builder.append(str);
	}

	@Override public final String toString() {
		return this.builder.toString();
	}
	
}

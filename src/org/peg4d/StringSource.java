package org.peg4d;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

public class StringSource extends ParserSource {
	public String  sourceText;
	public StringSource(String fileName, long linenum, String sourceText) {
		super(fileName, linenum);
		this.sourceText = sourceText;
	}
	public StringSource(String fileName) {
		super(fileName, 1);
		try {
			RandomAccessFile f = new RandomAccessFile(fileName, "r");
			byte[] b = new byte[(int)f.length()];
			f.read(b);
			this.sourceText = new String(b);
			f.close();
		}
		catch(IOException e) {
		}
	}
	public final long length() {
		return this.sourceText.length();
	}
	public final long getFileLength() {
		try {
			return new File(this.fileName).length();
		}
		catch(Exception e) {
		}
		return this.sourceText.getBytes().length;
	}
	public final char charAt(long n) {
		if(0 <= n && n < this.length()) {
			return this.sourceText.charAt((int)n);
		}
		return '\0';
	}
	public final String substring(long startIndex, long endIndex) {
		//System.out.println("sourceText: " + this.sourceText + ", " + startIndex + ", " + endIndex);
		return this.sourceText.substring((int)startIndex, (int)endIndex);
	}
	@Override
	public final ParserSource trim(long startIndex, long endIndex) {
		return this;
	}
}


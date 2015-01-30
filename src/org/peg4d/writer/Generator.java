package org.peg4d.writer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import nez.util.UList;
import nez.util.UMap;

import org.peg4d.Main;
import org.peg4d.ParsingObject;
import org.peg4d.Utils;

public class Generator {
	private class Counter {
		int count = 1;
		ParsingObject lastRoot = null;
	}

	private OutputStream out;
	private String CHARSET = "UTF8";
	private final static String TAB = " ";
	private final static String LF = "\n";
	private final static String CRLF = "\r\n";
	
	public Generator(String fileName) {
		if(fileName != null) {
			try {
				this.out = new BufferedOutputStream(new FileOutputStream(fileName));
			} catch (IOException e) {
				e.printStackTrace();
				Main._Exit(1, "cannot open output file: " + fileName);
			}
		}
	}

	public void write(String text) {
		if(out == null) {
			System.out.print(text);
		}
		else {
			try {
				out.write(text.getBytes(CHARSET));
			} catch (IOException e) {
				Main._Exit(1, "IO error: " + e.getMessage());
			}
		}
	}

	public void write(String lf, String indent, String text) {
		if(out == null) {
			System.out.print(lf);
			System.out.print(indent);
			System.out.print(text);
		}
		else {
			try {
				out.write(lf.getBytes(CHARSET));
				out.write(indent.getBytes(CHARSET));
				out.write(text.getBytes(CHARSET));
			} catch (IOException e) {
				Main._Exit(1, "IO error: " + e.getMessage());
			}
		}
	}

	public void close() {
		if(out == null) {
			System.out.flush();
		}
		else {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				Main._Exit(1, "IO error: " + e.getMessage());
			}
			out = null;
		}
	}
	
	public final void writePego(ParsingObject pego) {
		this.writePego(pego, "");
		this.write(LF);
		this.close();
	}

	private void writePego(ParsingObject pego, String indent) {
		if(pego.size() == 0) {
			this.write(LF);
			this.write(indent);
			this.write("{#" + pego.getTag()+ " "); 
			this.write(Utils.quoteString('\'', pego.getText(), '\''));
			this.write("}");
		}
		else {
			this.write(LF);
			this.write(indent);
			this.write("{#" + pego.getTag().toString());
			String nindent = TAB + indent;
			for(int i = 0; i < pego.size(); i++) {
				this.writePego(pego.get(i), nindent);
			}
			this.write(LF);
			this.write(indent);
			this.write("}");
		}
	}
	
	public final void writeCommaSeparateValue(ParsingObject pego, double ratio) {
		UList<String> names = new UList<String>(new String[8]);
		UMap<Counter> schema =  new UMap<Counter>();
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			extractSchema(p, p, "", names, schema);
		}
		Main.printVerbose("CSV Schema Extraction", pego.size());
		UList<String> headers = new UList<String>(new String[8]);
		for(int i = 0; i < names.size(); i++) {
			String key = names.ArrayValues[i];
			Counter c = schema.get(key);
			double r = (double)c.count / pego.size();
			Main.printVerbose(key, r);
			if(r >= ratio) {
				headers.add(key);
			}
		}
		printCSVHeader(headers);
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			printCSVBody(headers, p);
		}
		this.close();
	}
	
	private void extractSchema(ParsingObject root, ParsingObject pego, String prefix, UList<String> names, UMap<Counter> schema) {
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			String key = prefix + p.getTag();
			if(p.size() == 0) {
				Counter c = schema.get(key);
				if(c == null) {
					c = new Counter();
					names.add(key);
					schema.put(key, c);
				}
				else if(c.lastRoot != root) {
					c.count += 1;
				}
				c.lastRoot = root;
			}
			else {
				extractSchema(root, p, key, names, schema);
			}
		}
	}

	private void printCSVHeader(UList<String> headers) {
		for(int i = 0; i < headers.size(); i++) {
			if(i > 0) {
				write(",");
			}
			String h = headers.ArrayValues[i];
			h = h.replace('#', '.').substring(1);
			printCSV(h);
		}
		write(CRLF);
	}

	private void printCSVBody(UList<String> headers, ParsingObject pego) {
		for(int i = 0; i < headers.size(); i++) {
			if(i > 0) {
				write(",");
			}
			String key = headers.ArrayValues[i];
			ParsingObject p = pego.getPath(key);
			if(p != null) {
				printCSV(p.getText());
			}
			else {
				printCSV("");
			}
		}
		write(CRLF);
	}

	private void printCSV(String text) {
		if(this.needsCsvQuote(text)) {
			text = quotedCsvString(text);
		}
		write(text);
	}
	
	private final boolean needsCsvQuote(String text) {
		if(text.length() > 0) {
			for(int i = 0; i < text.length(); i++) {
				char ch = text.charAt(i);
				if(!(ch >= '0' && ch <= '9')) {
					return true;
				}
			}
		}
		return false;
	}
	
	private final String quotedCsvString(String text) {
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		char prev = 0;
		for(int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if(ch == '\n') {
				if(prev != '\r') {
					sb.append('\r');
				}
				sb.append(ch);
			}
			else if(ch == '"') {
				sb.append(ch);
				sb.append(ch);
			}
			else {
				sb.append(ch);
			}
			prev = ch;
		}
		sb.append('"');
		return sb.toString();
	}
	
	
	public final void writeJSON(ParsingObject pego) {
		writeJSON("", "", pego);
		close();
	}

	private final void writeJSON(String lf, String indent, ParsingObject pego) {
		if(pego.size() > 0) {
//			if(pego.size() == 1) {
//				writeJSON(lf, indent, pego.get(0));  // flatten
//			}
//			else 
			if(isJSONArray(pego)) {
				writeJSONArray(lf, indent, pego);
			}
			else {
				writeJSONObject(lf, indent, pego);
			}
		}
		else {
			String text = pego.getText();
			text = Utils.quoteString('"', text, '"');
			write(lf, "", text);
		}
	}
	
	private boolean isJSONArray(ParsingObject pego) {
		UMap<Counter> schema =  new UMap<Counter>();
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			String tag = p.getTag().toString();
			Counter c = schema.get(tag);
			if(c != null) { // found duplicated
				return true;
			}
			c = new Counter();
			schema.put(tag, c);
		}
		return false;
	}
	
	private void writeJSONArray(String lf, String indent, ParsingObject pego) {
		write(lf, "", "[");
		String nindent = TAB + indent;
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			writeJSON(LF + nindent, nindent, p);
			if(i + 1 < pego.size()) {
				write(",");
			}
		}
		write(LF, indent, "]");
	}
	
	private void writeJSONObject(String lf, String indent, ParsingObject pego) {
		write(lf, "", "{");
		String nindent = TAB + indent;
		for(int i = 0; i < pego.size(); i++) {
			ParsingObject p = pego.get(i);
			write(LF, nindent, "\"" + p.getTag() + "\": ");
			writeJSON("", nindent, p);
			if(i + 1 < pego.size()) {
				write(",");
			}
		}
		write(LF, indent, "}");
	}
	
	
}

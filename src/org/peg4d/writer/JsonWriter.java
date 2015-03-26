package org.peg4d.writer;

import java.util.HashSet;
import org.peg4d.ParsingObject;
import org.peg4d.Utils;

public class JsonWriter extends ParsingWriter {

	private final static String TAB = " ";
	private final static String LF = System.lineSeparator();
	
	@Override
	protected void write(ParsingObject po) {
		writeJSON("", "", po);
	}

	private final void writeJSON(String lf, String indent, ParsingObject pego) {
		if(pego.size() > 0) {
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
	
	public void write(String lf, String indent, String text) {
		this.out.print(lf);
		this.out.print(indent);
		this.out.print(text);
	}
	
	private boolean isJSONArray(ParsingObject pego) {
		HashSet<Integer> tagSet =  new HashSet<>();
		for(ParsingObject p : pego) {
			Integer tagId = p.getTag().getId();
			if(tagSet.contains(tagId)) { // found duplicated
				return true;
			}
			tagSet.add(tagId);
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
				this.out.print(",");
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
				this.out.print(",");
			}
		}
		write(LF, indent, "}");
	}

}

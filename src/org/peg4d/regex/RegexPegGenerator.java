package org.peg4d.regex;

import java.util.Map;
import java.util.Map.Entry;

import org.peg4d.writer.Generator;

public class RegexPegGenerator extends Generator {

	private Map<String, RegexObject> rules;

	public RegexPegGenerator(String fileName, Map<String, RegexObject> rules) {
		super(fileName);
		this.rules = rules;//new HashMap<String, List<RegexObject>>();
	}

	private void writeLn(String s) {
		this.write(s + "\n");
	}

	public void writePeg() {
		writeLn("// regex PEG\n");
		writeHeader();
		RegexObject r = rules.get("TopLevel");
		write("TopLevel = { ");
		this.write(r.toString());
		writeLn(" #Matched }");
		writeLn("");

		for(Entry<String, RegexObject> s: rules.entrySet()) {
			if(s.getKey().equals("TopLevel")) {
				continue;
			}
			writeLn(s.getKey());
			write("    ");
			write("= ");
			writeLn(s.getValue().toString());
		}
	}

	private void writeHeader() {
		writeLn("File  = { @TopLevel #Source } _");
		writeLn("");
		writeLn("Chunk = TopLevel");
		writeLn("");
		writeLn("_ = [ \\t\\r\\n]*");
		writeLn("");
	}

}

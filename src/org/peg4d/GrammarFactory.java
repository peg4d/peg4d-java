package org.peg4d;

import nez.util.UMap;

public class GrammarFactory {

	public GrammarFactory() {
	}

	public Grammar newGrammar(String name) {
		return new Grammar(this, name);
	}

	public Grammar newGrammar(String name, String fileName) {
		Grammar peg = new Grammar(this, name);
		peg.loadGrammarFile(fileName, null);
		return peg;
	}

	public Grammar newGrammar(String name, String fileName, NezLogger stats) {
		Grammar peg = new Grammar(this, name);
		peg.loadGrammarFile(fileName, stats);
		return peg;
	}

	UMap<Grammar> grammarMap = new UMap<Grammar>();
	public final static Grammar Grammar = new PEG4dGrammar2();

	Grammar getGrammar(String filePath) {
		Grammar peg = grammarMap.get(filePath);
		if(peg != null) {
			return peg;
		}
		peg = loadLibraryGrammar(filePath);
		if(peg != null) {
			setGrammar(filePath, peg);
		}
		return peg;
	}

	void setGrammar(String path, Grammar peg) {
		this.grammarMap.put(path, peg);
	}

	private final static String LibraryPath = "org/peg4d/lib/";

	private Grammar loadLibraryGrammar(String filePath) {
		if(!filePath.endsWith(".p4d")) {
			filePath = filePath + ".p4d";
			if(!filePath.startsWith(LibraryPath)) {
				filePath = LibraryPath + filePath;
			}
		}
		return this.newGrammar(filePath, filePath);
	}

}

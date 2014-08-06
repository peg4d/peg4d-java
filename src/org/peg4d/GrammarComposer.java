package org.peg4d;

public class GrammarComposer {
	UMap<Grammar> pegMap = new UMap<Grammar>();
	
	Grammar getGrammar(String path) {
		Grammar peg = pegMap.get(path);
		if(peg != null) {
			return peg;
		}
		peg = loadLibraryGrammar(path);
		if(peg != null) {
			setGrammar(path, peg);
		}
		return peg;
	}
	
	void setGrammar(String path, Grammar peg) {
		this.pegMap.put(path, peg);
	}
	
	private Grammar loadLibraryGrammar(String path) {
		if(!path.endsWith(".peg")) {
			path = "lib/" + path + ".peg";
		}
		return Grammar.load(this, path);
	}

	
	int memoId = 0;
	PegMemo newMemo(Peg e) {
		return null;
	}
	
}

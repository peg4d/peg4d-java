package org.peg4d;

public class GrammarComposer {
	UMap<Grammar> pegMap = new UMap<Grammar>();
	
	Grammar getGrammar(String filePath) {
		Grammar peg = pegMap.get(filePath);
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
		this.pegMap.put(path, peg);
	}
	
	private Grammar loadLibraryGrammar(String filePath) {
		if(!filePath.endsWith(".peg")) {
			filePath = "lib/" + filePath + ".peg";
		}
		if(Main.VerbosePeg) {
			System.out.println("importing " + filePath);
		}
		return Grammar.load(this, filePath);
	}

	
	int memoId = 0;
	PegMemo newMemo(Peg e) {
		return null;
	}
	
}

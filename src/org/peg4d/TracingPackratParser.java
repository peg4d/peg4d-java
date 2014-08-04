package org.peg4d;


public class TracingPackratParser extends RecursiveDecentParser {

	public TracingPackratParser(Grammar peg, ParserSource source) {
		super(peg, source);
	}
	
	@Override
	public void initMemo() {
		if(Main.MemoFactor < 0) {
			int initSize = 512 * 1024;
			if(source.length() < 512 * 1024) {
				initSize = (int)source.length();
			}
			//this.memoMap = new PackratMemo(initSize);
			this.memoMap = new DebugMemo(new PackratMemo(initSize), new OpenHashMemo(100));
			Main.printVerbose("memo", "packrat-style");
		}
		else if(Main.MemoFactor == 0) {
			this.memoMap = new NoMemo(); //new PackratMemo(this.source.length());
		}
		else {
			this.memoMap = new OpenHashMemo(Main.MemoFactor);
		}
	}

}

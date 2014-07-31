package org.peg4d;


public class TracingPackratParser extends RecursiveDecentParser {

	public TracingPackratParser(Grammar peg, ParserSource source) {
		super(peg, source);
	}
	
	@Override
	public void initMemo() {
		if(Main.MemoFactor == -1) {  /* default */
			this.memoMap = new OpenHashMemo(256);
		}
		else if(Main.MemoFactor == 0) {
			this.memoMap = new NoMemo(); //new PackratMemo(this.source.length());
		}
		else {
			this.memoMap = new OpenHashMemo(Main.MemoFactor);
		}
	}

}

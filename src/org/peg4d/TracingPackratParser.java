package org.peg4d;


public class TracingPackratParser extends RecursiveDecentParser {
	int memo = 0;
	public TracingPackratParser(Grammar peg, ParserSource source) {
		super(peg, source);
		this.memo = Main.MemoFactor;
	}
	public TracingPackratParser(Grammar peg, ParserSource source, int memo) {
		super(peg, source);
		this.memo = memo;
	}
	
	@Override
	public void initMemo() {
		if(memo < 0) {
			int initSize = 512 * 1024;
			if(source.length() < 512 * 1024) {
				initSize = (int)source.length();
			}
			//this.memoMap = new PackratMemo(initSize);
			//this.memoMap = new DebugMemo(new PackratMemo(initSize), new OpenHashMemo(100));
			this.memoMap = new DebugMemo(new OpenHashMemo(256), new OpenHashMemo(256));
			Main.printVerbose("memo", "packrat-style");
		}
		else if(memo == 0) {
			this.memoMap = new NoMemo(); //new PackratMemo(this.source.length());
		}
		else {
			this.memoMap = new OpenHashMemo(memo);
		}
	}

}

package org.peg4d;

import java.util.Map;

public class RecursiveDecentParser extends ParserContext {
//	protected UList<Peg>       pegList;
//	private UMap<Peg>        pegCache;
	
	public RecursiveDecentParser(Grammar peg, ParserSource source) {
		super(peg, source, 0, source.length());
		this.initMemo();
	}
	
	public void initMemo() {
		this.memoMap = new NoMemo();
	}

}


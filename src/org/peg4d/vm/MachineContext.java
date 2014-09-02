package org.peg4d.vm;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public final class MachineContext {
	public ParsingObject left;
	public ParsingSource source;
	public long pos;
	public long fpos = 0;
	public int  bstacktop;
	public MachineContext(ParsingObject left, ParsingSource s, long pos) {
		this.left = left;
		this.source = s;
		this.pos  = pos;
		this.fpos = 0;
		this.lstack = new long[4096*8];
		this.lstack[0] = -1;
		this.lstacktop = 1;
		this.ostack = new ParsingObject[4096];
		this.ostacktop = 0;
	}

	public long[]   lstack;
	public int      lstacktop;
	
	public final void lpush(long v) {
		this.lstack[lstacktop] = v;
		lstacktop++;
	}

	public ParsingObject[] ostack;
	public int      ostacktop;

	public final void opush(ParsingObject left) {
		this.ostack[ostacktop] = left;
		ostacktop++;
	}
	
	

	
	
}
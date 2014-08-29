package org.peg4d.vm;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

final class MachineContext {
	ParsingObject left;
    ParsingSource source;
    long pos;
    long fpos = 0;
    long[]   lstack = new long[4096];
    int      lstacktop = 1;
    ParsingObject[] ostack = new ParsingObject[512];
    int      ostacktop = 0;
    int      bstacktop;
    public MachineContext(ParsingObject left, ParsingSource s, long pos) {
    	this.left = left;
    	this.source = s;
    	this.pos = pos;
    	this.lstack[0] = -1;
    }
}
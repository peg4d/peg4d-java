package org.peg4d;

public class PEGUtils {
	
	public final static long objectId(long pos, short pegid) {
		return (pos << 16) | pegid;
	}
	
	public final static long objectId(long pos, Peg e) {
		return (pos << 16) | e.uniqueId;
	}
	
	public final static long objectIdNull(long pos, Peg e) {
		if(e == null) {
			return objectId(pos, (short)0);
		}
		else {
			return objectId(pos, e);
		}
	}
	
	public final static long memoKey(long pos, Peg e) {
		return (pos << 16) | e.semanticId;
	}
	public final static long failure(long pos, Peg e) {
		return (pos << 16) | e.uniqueId;
	}
	public final static long getpos(long pospeg) {
		return (pospeg >> 16);
	}
	public final static short getpegid(long pospeg) {
		return (short)pospeg;
	}
}

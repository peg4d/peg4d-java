package org.peg4d;

import org.peg4d.expression.ParsingExpression;

public class ParsingUtils {
	
	public final static long objectId(long pos, short pegid) {
		return (pos << 16) | pegid;
	}
	
	public final static long objectId(long pos, ParsingExpression e) {
		return (pos << 16) | (short)e.uniqueId;
	}
	
	public final static long objectIdNull(long pos, ParsingExpression e) {
		if(e == null) {
			return objectId(pos, (short)0);
		}
		else {
			return objectId(pos, e);
		}
	}
	
	public final static long memoKey(long pos, ParsingExpression e) {
		return (pos << 16) | (short)e.uniqueId;
	}
//	public final static long failure(long pos, ParsingExpression e) {
//		return (pos << 16) | e.uniqueId;
//	}
	public final static long getpos(long pospeg) {
		return (pospeg >> 16);
	}
	public final static short getpegid(long pospeg) {
		return (short)pospeg;
	}
}

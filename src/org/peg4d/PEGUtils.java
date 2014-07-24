package org.peg4d;

public class PEGUtils {
	public final static long objectId(long pos, Peg e) {
		return (pos << 48) | (short)e.pegid2;
	}
	public final static long failure(long pos, Peg e) {
		return (pos << 48) | 0;
	}
	public final static long getpos(long oid) {
		return (oid >> 48);
	}
	public final static short getpegid(long oid) {
		return (short)oid;
	}
	public final static boolean isFailure(long oid) {
		return ((short)oid) == 0;
	}
}

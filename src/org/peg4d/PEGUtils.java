package org.peg4d;

public class PEGUtils {
	public final static long memoKey(long pos, Peg e) {
		return (pos << 16) | e.uniqueId;
	}
	public final static long objectId(long pos, Peg e) {
		return (pos << 16) | e.uniqueId;
	}
	public final static long failure(long pos, Peg e) {
		return (pos << 16) | e.uniqueId;
	}
	public final static long getpos(long oid) {
		return (oid >> 16);
	}
	public final static short getpegid(long oid) {
		return (short)oid;
	}
	public final static boolean isFailure(int oid) {
		return oid == 0;
	}
	
//	String s0(long oid) {
//		if(oid == 1) {
//			return "toplevel";
//		}
//		long pos = PEGUtils.getpos(oid);
//		if(PEGUtils.isFailure(oid)) {
//			return this.source.formatErrorMessage("syntax error", pos, "");
//		}
//		else {
//			Peg e = this.peg.getPeg(oid);
//			return "object " + pos + " peg=" + e;
//		}
//	}
//
//	String S(long oid) {
//		if(oid == 1) {
//			return "toplevel";
//		}
//		long pos = PEGUtils.getpos(oid);
//		if(PEGUtils.isFailure(oid)) {
//			return this.source.formatErrorMessage("syntax error", pos, "failure:");
//		}
//		else {
//			Peg e = this.peg.getPeg(oid);
//			return "pego(" + pos + "," + e.pegid2 + ")";
//		}
//	}

}

//package org.peg4d;
//
//public class ParsingStream extends ParsingContext {
//	public    Grammar       peg = null;
//	
//	ParsingStream(Grammar peg, ParsingSource source) {
//		super(null, source, 0);
//		this.peg = peg;
//		this.emptyTag = this.peg.getModelTag("#empty");
//	}
//	
//	
//	@Override
//	public final String toString() {
////		if(this.endPosition > this.pos) {
////			return this.source.substring(this.pos, this.endPosition);
////		}
//		return "";
//	}
//
//	public final String formatErrorMessage(String msg1, String msg2) {
//		return this.source.formatPositionLine(msg1, this.pos, msg2);
//	}
//
//	public final void showPosition(String msg) {
//		showPosition(msg, this.getPosition());
//	}
//
//	public final void showPosition(String msg, long pos) {
//		System.out.println(this.source.formatPositionLine("debug", pos, msg));
//	}
//
//	public final void showErrorMessage(String msg) {
//		System.out.println(this.source.formatPositionLine("error", this.fpos, msg));
//		Main._Exit(1, msg);
//	}
//	
//			
////	private long  failurePosition = 0;
//	private final ParsingObject failureResult = null; //new ParsingObject(null, this.source, 0);
//
//	public final ParsingObject newErrorObject() {
//		ParsingObject pego = new ParsingObject(this.peg.getModelTag("#error"), this.source, this.fpos); // FIXME
//		String msg = this.source.formatPositionLine("syntax error", pego.getSourcePosition(), "");
//		pego.setValue(msg);
//		return pego;
//	}
//	
//	public final void foundFailure(PExpression e) {
////		if(this.pos >= ParsingUtils.getpos(this.failurePosition)) {  // adding error location
////			this.failurePosition = ParsingUtils.failure(this.pos, e);
////		}
//		this.opFailure();
//		this.left = this.failureResult;
//	}
//
//	public final ParsingObject refoundFailure(PExpression e, long pos) {
////		this.failurePosition = ParsingUtils.failure(pos, e);
//		return this.failureResult;
//	}
//
//
//}
//

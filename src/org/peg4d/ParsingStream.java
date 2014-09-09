package org.peg4d;

public class ParsingStream extends ParsingContext {
	public    Grammar       peg = null;
	
	ParsingStream(Grammar peg, ParsingSource source) {
		super(null, source, 0);
		this.peg = peg;
		this.emptyTag = this.peg.getModelTag("#empty");
	}
	
	public void resetSource(ParsingSource source) {
		this.peg = source.peg;
		this.source = source;
		this.pos = 0;
		this.emptyTag = this.peg.getModelTag("#empty");
	}
	
	@Override
	public final String toString() {
//		if(this.endPosition > this.pos) {
//			return this.source.substring(this.pos, this.endPosition);
//		}
		return "";
	}

	public final String formatErrorMessage(String msg1, String msg2) {
		return this.source.formatPositionLine(msg1, this.pos, msg2);
	}

	public final void showPosition(String msg) {
		showPosition(msg, this.getPosition());
	}

	public final void showPosition(String msg, long pos) {
		System.out.println(this.source.formatPositionLine("debug", pos, msg));
	}

	public final void showErrorMessage(String msg) {
		System.out.println(this.source.formatPositionLine("error", this.fpos, msg));
		Main._Exit(1, msg);
	}
	
	public final boolean hasByteChar() {
		return this.source.byteAt(this.pos) != ParsingSource.EOF;
	}

	public final int getByteChar() {
		return this.source.byteAt(pos);
	}
	
	public final ParsingObject parseChunk(String startPoint) {
		this.initMemo();
		PExpression start = this.peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		long spos = this.getPosition();
		long fpos = -1;
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		while(hasByteChar()) {
			long ppos = this.getPosition();
			po.setSourcePosition(this.pos);
			this.left = po;
			start.simpleMatch(this);
			if(this.isFailure() || ppos == this.getPosition()) {
				if(fpos == -1) {
					fpos = this.fpos;
				}
				this.consume(1);
				continue;
			}
			if(spos < ppos) {
				System.out.println(source.formatPositionLine("error", fpos, "syntax error"));
				System.out.println("skipped[" + spos + "]: " + this.source.substring(spos, ppos));
			}
			return this.left;
		}
		return null;
	}

	public final ParsingObject parseChunk() {
		return this.parseChunk("Chunk");
	}

	public final ParsingObject parse(String startPoint) {
		this.initMemo();
		PExpression start = this.peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		this.left = po;
		start.simpleMatch(this);
		return this.left;
	}
			
//	private long  failurePosition = 0;
	private final ParsingObject failureResult = null; //new ParsingObject(null, this.source, 0);

	public final ParsingObject newErrorObject() {
		ParsingObject pego = new ParsingObject(this.peg.getModelTag("#error"), this.source, this.fpos); // FIXME
		String msg = this.source.formatPositionLine("syntax error", pego.getSourcePosition(), "");
		pego.setValue(msg);
		return pego;
	}
	
	public final void foundFailure(PExpression e) {
//		if(this.pos >= ParsingUtils.getpos(this.failurePosition)) {  // adding error location
//			this.failurePosition = ParsingUtils.failure(this.pos, e);
//		}
		this.opFailure();
		this.left = this.failureResult;
	}

	public final ParsingObject refoundFailure(PExpression e, long pos) {
//		this.failurePosition = ParsingUtils.failure(pos, e);
		return this.failureResult;
	}

	public void beginPeformStat() {
		if(Main.StatLevel >= 0) {
			this.stat = new Stat(this.peg, this.source);
			if(Main.StatLevel == 2) {
				this.stat.initRepeatCounter();
			}
			this.stat.start();
		}
	}

	public void endPerformStat(ParsingObject pego) {
		if(stat != null) {
			stat.end(pego, this);
		}
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

}


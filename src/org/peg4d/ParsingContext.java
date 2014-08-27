package org.peg4d;

import java.util.concurrent.BlockingQueue;

public class ParsingContext {
	public    Grammar       peg = null;
	public    ParsingSource source;
	protected long          pos = 0;
	protected Stat          stat   = null;
	
	ParsingContext(Grammar peg, ParsingSource source) {
		this.peg = peg;
		this.source = source;
		this.pos = 0;
	}
	
	public void resetSource(ParsingSource source) {
		this.peg = source.peg;
		this.source = source;
		this.pos = 0;
	}

	protected final long getPosition() {
		return this.pos;
	}
	
	protected final void setPosition(long pos) {
		this.pos = pos;
	}
	
	protected final void rollback(long pos) {
		if(stat != null && this.pos > pos) {
			stat.statBacktrack1(pos, this.pos);
		}
		this.pos = pos;
	}
	
	@Override
	public final String toString() {
//		if(this.endPosition > this.pos) {
//			return this.source.substring(this.pos, this.endPosition);
//		}
		return "";
	}
	
	public final boolean hasUnconsumedCharacter() {
		return this.source.byteAt(this.pos) != ParsingSource.EOF;
	}

//	public final boolean hasChar(int len) {
//		return this.pos + len <= this.endPosition;
//	}

	protected final int byteAt(long pos) {
		return this.source.byteAt(pos);
	}

	protected final int getByteChar() {
		return this.byteAt(this.pos);
	}

	protected final int getUChar() {
		return this.source.charAt(this.pos);
	}

	public String substring(long startIndex, long endIndex) {
		return this.source.substring(startIndex, endIndex);
	}

	protected final void consume(int plus) {
		this.pos = this.pos + plus;
	}
//
//	protected final boolean match(int ch) {
//		if(ch == this.byteAt(this.pos)) {
//			this.consume(1);
//			return true;
//		}
//		return false;
//	}
//
//	protected final boolean match(byte[] utf8) {
//		long pos = this.pos;
//		if(pos + utf8.length <= this.endPosition && this.source.match(pos, utf8)) {
//			this.consume(utf8.length);
//			return true;
//		}
//		return false;
//	}
//	
//	protected final boolean match(ParsingCharset charset) {
//		if(charset.match(this.getByte())) {
//			this.consume(1);
//			return true;
//		}
//		return false;
//	}
	
	public final String formatErrorMessage(String msg1, String msg2) {
		return this.source.formatErrorMessage(msg1, this.pos, msg2);
	}

	public final void showPosition(String msg) {
		showPosition(msg, this.getPosition());
	}

	public final void showPosition(String msg, long pos) {
		System.out.println(this.source.formatErrorMessage("debug", pos, msg));
	}

	public final void showErrorMessage(String msg) {
		System.out.println(this.source.formatErrorMessage("error", this.pos, msg));
		Main._Exit(1, msg);
	}
	
	public boolean hasNode() {
		return this.hasUnconsumedCharacter();
	}

	public ParsingObject match(String startPoint) {
		this.initMemo();
		PExpression start = this.peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		return start.simpleMatch(ParsingObject.newSource("#toplevel", this.source, 0), this);
	}

	
	public ParsingObject parseNode(String startPoint) {
		ParsingObject pego = this.match(startPoint);
		if(pego.isFailure()) {
			pego = this.newErrorObject();
			String msg = this.source.formatErrorMessage("syntax error", pego.getSourcePosition(), "");
			pego.setMessage(msg);
			//this.pos = this.endPosition;
		}
		return pego;
	}
	
	protected MemoMap memoMap = null;
	public void initMemo() {
		this.memoMap = new NoMemo();
	}
	
	private boolean isMatchingOnly = false;
	protected ParsingObject successResult = ParsingObject.newSource("#success", this.source, 0);
	
	public final boolean isRecognitionMode() {
		return this.isMatchingOnly;
	}

	public final boolean setRecognitionMode(boolean recognitionMode) {
		boolean b = this.isMatchingOnly;
		this.isMatchingOnly = recognitionMode;
		return b;
	}
	
	public final ParsingObject newParsingObject(String tagName, long pos, PConstructor created) {
		if(this.isRecognitionMode()) {
			this.successResult.setSourcePosition(pos);
			return this.successResult;
		}
		else {
			return ParsingObject.newSource(tagName, this.source, pos, created);
		}
	}
	
	private long  failurePosition = 0;
	private final ParsingObject failureResult = ParsingObject.newSource(null, this.source, 0);

	public final ParsingObject newErrorObject() {
		return ParsingObject.newErrorSource(this.source, this.failurePosition);
	}
	
	public final ParsingObject foundFailure(PExpression e) {
		if(this.pos >= ParsingUtils.getpos(this.failurePosition)) {  // adding error location
			this.failurePosition = ParsingUtils.failure(this.pos, e);
		}
		return this.failureResult;
	}

	public final ParsingObject refoundFailure(PExpression e, long pos) {
		this.failurePosition = ParsingUtils.failure(pos, e);
		return this.failureResult;
	}

	final long rememberFailure() {
		return this.failurePosition;
	}
	
	final void forgetFailure(long f) {
		this.failurePosition = f;
	}
	
	private class LinkLog {
		LinkLog next;
		int  index;
		ParsingObject childNode;
	}

	LinkLog logStack = null;
	LinkLog unusedLog = null;
	int stackSize = 0;
	
	private LinkLog newLog() {
		if(this.unusedLog == null) {
			return new LinkLog();
		}
		LinkLog l = this.unusedLog;
		this.unusedLog = l.next;
		l.next = null;
		return l;
	}

	private void disposeLog(LinkLog log) {
		log.childNode = null;
		log.next = this.unusedLog;
		this.unusedLog = log;
	}
	
	protected int markObjectStack() {
		return stackSize;
	}

	protected void rollbackObjectStack(int mark) {
		while(mark < this.stackSize) {
			LinkLog l = this.logStack;
			this.logStack = this.logStack.next;
			this.stackSize--;
			disposeLog(l);
		}
		assert(mark == this.stackSize);
	}
	
	final void pushConnection(ParsingObject parentNode, int index, ParsingObject childNode) {
		assert(!this.isRecognitionMode());
		LinkLog l = this.newLog();
		l.childNode  = childNode;
		childNode.parent = parentNode;
		l.index = index;
		l.next = this.logStack;
		this.logStack = l;
		this.stackSize += 1;
	}

	final void popConnection(ParsingObject newnode, long startIndex, int mark) {
		assert(!this.isRecognitionMode());
		LinkLog first = null;
		int objectSize = 0;
		while(mark < this.stackSize) {
			LinkLog cur = this.logStack;
			this.logStack = this.logStack.next;
			this.stackSize--;
			if(cur.childNode.parent == newnode) {
				cur.next = first;
				first = cur;
				objectSize += 1;
			}
			else {
				disposeLog(cur);
			}
		}
		if(objectSize > 0) {
			newnode = ParsingObject.newAst(newnode, objectSize);
			for(int i = 0; i < objectSize; i++) {
				LinkLog cur = first;
				first = first.next;
				if(cur.index == -1) {
					cur.index = i;
				}
				newnode.set(cur.index, cur.childNode);
				this.disposeLog(cur);
			}
			newnode.checkNullEntry();
		}
		newnode.setLength((int)(this.getPosition() - startIndex));
	}
	
//	long statExportCount = 0;
//	long statExportSize  = 0;
//	long statExportFailure  = 0;

	public ParsingObject matchExport(ParsingObject left, PExport e) {
		ParsingObject pego = e.inner.simpleMatch(left, this);
		if(!pego.isFailure()) {
//			this.statExportCount += 1;
//			this.statExportSize += pego.getLength();
			this.pushBlockingQueue(pego);
		}
		else {
//			this.statExportFailure += 1;
		}
		return left;
	}

	private BlockingQueue<ParsingObject> queue = null; 
	protected void pushBlockingQueue(ParsingObject pego) {
		if(this.queue != null) {
			try {
				this.queue.put(pego);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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

	public byte[] getIndentSequence(long pos) {
		return this.source.getIndentText(pos).getBytes();
	}

}


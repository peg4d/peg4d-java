package org.peg4d;

import java.util.concurrent.BlockingQueue;

public class ParserContext {
	public    Grammar     peg = null;
	public    ParsingSource source;
	protected long        sourcePosition = 0;
	public    long        endPosition;
	protected Stat stat   = null;
	
	public ParserContext(Grammar peg, ParsingSource source, long startIndex, long endIndex) {
		this.peg = peg;
		this.source = source;
		this.sourcePosition = startIndex;
		this.endPosition = endIndex;
	}
	
	public void resetSource(ParsingSource source) {
		this.peg = source.peg;
		this.source = source;
		this.sourcePosition = 0;
		this.endPosition = source.length();
	}

	protected final long getPosition() {
		return this.sourcePosition;
	}
	
	protected final void setPosition(long pos) {
		this.sourcePosition = pos;
	}
	
	protected final void rollback(long pos) {
		if(stat != null && this.sourcePosition > pos) {
			stat.statBacktrack1(pos, this.sourcePosition);
		}
		this.sourcePosition = pos;
	}
	
	@Override
	public final String toString() {
		if(this.endPosition > this.sourcePosition) {
			return this.source.substring(this.sourcePosition, this.endPosition);
		}
		return "";
	}
	
	public final boolean hasChar() {
		return this.sourcePosition < this.endPosition;
	}

	public final boolean hasChar(int len) {
		return this.sourcePosition + len <= this.endPosition;
	}

	protected final int charAt(long pos) {
		if(pos < this.endPosition) {
			return this.source.charAt(pos);
		}
		return '\0';
	}

	protected final int getChar() {
		return this.charAt(this.sourcePosition);
	}

	protected final int getUChar() {
		System.out.println("TODO: getUChar()");
		return 0;
	}

	public String substring(long startIndex, long endIndex) {
		if(endIndex <= this.endPosition) {
			return this.source.substring(startIndex, endIndex);
		}
		return "";
	}

	protected final void consume(long plus) {
		this.sourcePosition = this.sourcePosition + plus;
	}

	protected final boolean match(int ch) {
		if(ch == this.charAt(this.sourcePosition)) {
			this.consume(1);
			return true;
		}
		return false;
	}

	protected final boolean match(byte[] utf8) {
		long pos = this.sourcePosition;
		if(pos + utf8.length <= this.endPosition && this.source.match(pos, utf8)) {
			this.consume(utf8.length);
			return true;
		}
		return false;
	}
	
	protected final boolean match(UCharset charset) {
		if(charset.match(this.getChar())) {
			this.consume(1);
			return true;
		}
		return false;
	}
	
	public final String formatErrorMessage(String msg1, String msg2) {
		return this.source.formatErrorMessage(msg1, this.sourcePosition, msg2);
	}

	public final void showPosition(String msg) {
		showPosition(msg, this.getPosition());
	}

	public final void showPosition(String msg, long pos) {
		System.out.println(this.source.formatErrorMessage("debug", pos, msg));
	}

	public final void showErrorMessage(String msg) {
		System.out.println(this.source.formatErrorMessage("error", this.sourcePosition, msg));
		Main._Exit(1, msg);
	}
	
	public boolean hasNode() {
		return this.sourcePosition < this.endPosition;
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
			this.sourcePosition = this.endPosition;
		}
		return pego;
	}
	
	protected MemoMap memoMap = null;
	public void initMemo() {
		this.memoMap = new NoMemo();
	}
	
	protected ParsingObject successResult = null;
	
	public final void setRecognitionOnly(boolean checkMode) {
		if(checkMode) {
			this.successResult = ParsingObject.newSource("#success", this.source, 0);
		}
		else {
			this.successResult = null;
		}
	}
	
	public final boolean isRecognitionOnly() {
		return this.successResult != null;
	}
	
	public final ParsingObject newPegObject1(String tagName, long pos, PConstructor created) {
		if(this.isRecognitionOnly()) {
			this.successResult.setSourcePosition(pos);
			return this.successResult;
		}
		else {
			return ParsingObject.newSource(tagName, this.source, pos, created);
		}
	}
	
	private long  failurePosition = 0;
	private final ParsingObject foundFailureNode = ParsingObject.newSource(null, this.source, 0);

	public final ParsingObject newErrorObject() {
		return ParsingObject.newErrorSource(this.source, this.failurePosition);
	}
	
	public final ParsingObject foundFailure(PExpression e) {
		if(this.sourcePosition >= PEGUtils.getpos(this.failurePosition)) {  // adding error location
			this.failurePosition = PEGUtils.failure(this.sourcePosition, e);
		}
		return this.foundFailureNode;
	}

	public final ParsingObject refoundFailure(PExpression e, long pos) {
		this.failurePosition = PEGUtils.failure(pos, e);
		return this.foundFailureNode;
	}

	final long rememberFailure() {
		return this.failurePosition;
	}
	
	final void forgetFailure(long f) {
		this.failurePosition = f;
	}

//	public final Peg getRule(String name) {
//		return this.peg.getRule(name);
//	}
	
	private class LinkLog {
		LinkLog next;
		int  index;
		ParsingObject childNode;
	}

	LinkLog logStack = null;  // needs first logs
	LinkLog unusedLog = null;
	int stackSize = 0;
//	int usedLog = 0;
	
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
	
	protected final void logSetter(ParsingObject parentNode, int index, ParsingObject childNode) {
		if(!this.isRecognitionOnly()) {
			LinkLog l = this.newLog();
//			l.parentNode = parentNode;
			l.childNode  = childNode;
			childNode.parent = parentNode;
			l.index = index;
			l.next = this.logStack;
			this.logStack = l;
			this.stackSize += 1;
		}
	}

	protected final void popNewObject(ParsingObject newnode, long startIndex, int mark) {
		if(!this.isRecognitionOnly()) {
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

}


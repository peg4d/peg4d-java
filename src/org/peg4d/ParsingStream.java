package org.peg4d;


public class ParsingStream extends ParsingContext {
	public    Grammar       peg = null;
//	public    ParsingSource source;
//	protected long          pos = 0;
//	private ParsingTag emptyTag ;
//	
//	protected Stat          stat   = null;
	
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

//	@Override
//	protected final long getPosition() {
//		return this.pos;
//	}
//	
//	@Override
//	protected final void setPosition(long pos) {
//		this.pos = pos;
//	}
//	
//	@Override
//	protected final void rollback(long pos) {
//		if(stat != null && this.pos > pos) {
//			stat.statBacktrack1(pos, this.pos);
//		}
//		this.pos = pos;
//	}
	
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

//	@Override
//	protected final void consume(int plus) {
//		this.pos = this.pos + plus;
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
		this.left = new ParsingObject(this.emptyTag, this.source, 0);
		start.simpleMatch(this);
		return this.left;
	}
	
	public ParsingObject parseNode(String startPoint) {
		ParsingObject pego = this.match(startPoint);
		if(this.isFailure()) {
			pego = this.newErrorObject();
			//this.pos = this.endPosition;
		}
		return pego;
	}
	
	protected MemoMap memoMap = null;
	public void initMemo() {
		this.memoMap = new NoMemo();
	}
			
//	private long  failurePosition = 0;
	private final ParsingObject failureResult = null; //new ParsingObject(null, this.source, 0);

	public final ParsingObject newErrorObject() {
		ParsingObject pego = new ParsingObject(this.peg.getModelTag("#error"), this.source, this.fpos); // FIXME
		String msg = this.source.formatErrorMessage("syntax error", pego.getSourcePosition(), "");
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
	
//	private class LinkLog {
//		LinkLog next;
//		int  index;
//		ParsingObject childNode;
//	}
//
//	LinkLog logStack = null;
//	LinkLog unusedLog = null;
//	int stackSize = 0;
//	
//	private LinkLog newLog() {
//		if(this.unusedLog == null) {
//			return new LinkLog();
//		}
//		LinkLog l = this.unusedLog;
//		this.unusedLog = l.next;
//		l.next = null;
//		return l;
//	}
//
//	private void disposeLog(LinkLog log) {
//		log.childNode = null;
//		log.next = this.unusedLog;
//		this.unusedLog = log;
//	}
//	
//	@Override
//	protected int markObjectStack() {
//		return stackSize;
//	}
//
//	protected void rollbackObjectStack(int mark) {
//		while(mark < this.stackSize) {
//			LinkLog l = this.logStack;
//			this.logStack = this.logStack.next;
//			this.stackSize--;
//			disposeLog(l);
//		}
//		assert(mark == this.stackSize);
//	}
//	
//	final void pushConnection(ParsingObject parentNode, int index, ParsingObject childNode) {
//		assert(!this.isRecognitionMode());
//		LinkLog l = this.newLog();
//		l.childNode  = childNode;
//		childNode.parent = parentNode;
//		l.index = index;
//		l.next = this.logStack;
//		this.logStack = l;
//		this.stackSize += 1;
//	}
//
//	final void popConnection(ParsingObject newnode, long startIndex, int mark) {
//		assert(!this.isRecognitionMode());
//		LinkLog first = null;
//		int objectSize = 0;
//		while(mark < this.stackSize) {
//			LinkLog cur = this.logStack;
//			this.logStack = this.logStack.next;
//			this.stackSize--;
//			if(cur.childNode.parent == newnode) {
//				cur.next = first;
//				first = cur;
//				objectSize += 1;
//			}
//			else {
//				disposeLog(cur);
//			}
//		}
//		if(objectSize > 0) {
//			newnode = this.newAst(newnode, objectSize);
//			for(int i = 0; i < objectSize; i++) {
//				LinkLog cur = first;
//				first = first.next;
//				if(cur.index == -1) {
//					cur.index = i;
//				}
//				newnode.set(cur.index, cur.childNode);
//				this.disposeLog(cur);
//			}
//			checkNullEntry(newnode);
//		}
//		newnode.setLength((int)(this.getPosition() - startIndex));
//	}
//	
//	private final void checkNullEntry(ParsingObject o) {
//		for(int i = 0; i < o.size(); i++) {
//			if(o.get(i) == null) {
//				o.set(i, new ParsingObject(emptyTag, this.source, 0));
//			}
//		}
//	}

//	long statExportCount = 0;
//	long statExportSize  = 0;
//	long statExportFailure  = 0;

//	public ParsingObject matchExport(ParsingObject left, PExport e) {
//		ParsingObject pego = e.inner.simpleMatch(left, this);
//		if(!this.isFailure()) {
////			this.statExportCount += 1;
////			this.statExportSize += pego.getLength();
//			this.pushBlockingQueue(pego);
//		}
//		else {
////			this.statExportFailure += 1;
//		}
//		return left;
//	}
//
//	private BlockingQueue<ParsingObject> queue = null; 
//	protected void pushBlockingQueue(ParsingObject pego) {
//		if(this.queue != null) {
//			try {
//				this.queue.put(pego);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}

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


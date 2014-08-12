package org.peg4d;

import java.util.concurrent.BlockingQueue;

public class ParserContext {
	public    Grammar     peg = null;
	
	public final          PegInput source;
	protected long        sourcePosition = 0;
	public    long        endPosition;
	protected Stat stat   = null;
	
	public ParserContext(Grammar peg, PegInput source, long startIndex, long endIndex) {
		this.peg = peg;
		this.source = source;
		this.sourcePosition = startIndex;
		this.endPosition = endIndex;
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

	protected final int charAt(long pos) {
		if(pos < this.endPosition) {
			return this.source.charAt(pos);
		}
		return '\0';
	}

	protected final int getChar() {
		return this.charAt(this.sourcePosition);
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
		if(ch == this.getChar()) {
			this.consume(1);
			return true;
		}
		return false;
	}

	protected final boolean match(byte[] text) {
		if(this.endPosition - this.sourcePosition >= text.length) {
			for(int i = 0; i < text.length; i++) {
				if(text[i] != this.source.charAt(this.sourcePosition + i)) {
					return false;
				}
			}
			this.consume(text.length);
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

	public Pego parseNode(String startPoint) {
		this.initMemo();
		Peg start = this.peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		Pego pego = start.simpleMatch(Pego.newSource("#toplevel", this.source, 0), this);
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
	
	protected Pego successResult = null;
	
	public final void setRecognitionOnly(boolean checkMode) {
		if(checkMode) {
			this.successResult = Pego.newSource("#success", this.source, 0);
		}
		else {
			this.successResult = null;
		}
	}
	
	public final boolean isRecognitionOnly() {
		return this.successResult != null;
	}
	
	public final Pego newPegObject1(String tagName, long pos, PegNewObject created) {
		if(this.isRecognitionOnly()) {
			this.successResult.setSourcePosition(pos);
			return this.successResult;
		}
		else {
			return Pego.newSource(tagName, this.source, pos, created);
		}
	}
	
	private long failurePosition = 0;
	private final Pego foundFailureNode = Pego.newSource(null, this.source, 0);

	public final Pego newErrorObject() {
		return Pego.newErrorSource(this.source, this.failurePosition);
	}
	
	public final Pego foundFailure(Peg e) {
		if(this.sourcePosition >= PEGUtils.getpos(this.failurePosition)) {  // adding error location
			this.failurePosition = PEGUtils.failure(this.sourcePosition, e);
		}
		return this.foundFailureNode;
	}

	public final Pego refoundFailure(Peg e, long pos) {
		this.failurePosition = PEGUtils.failure(pos, e);
		return this.foundFailureNode;
	}

//	public final Peg getRule(String name) {
//		return this.peg.getRule(name);
//	}
	
	private class LinkLog {
		LinkLog next;
		int  index;
		Pego childNode;
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
	
	protected final void logSetter(Pego parentNode, int index, Pego childNode) {
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

	protected final void popNewObject(Pego newnode, long startIndex, int mark) {
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
				newnode = Pego.newAst(newnode, objectSize);
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
	
//	public Pego matchNewObject(Pego left, PegNewObject e) {
//		Pego leftNode = left;
//		long startIndex = this.getPosition();
////		if(e.predictionIndex > 0) {
//		for(int i = 0; i < e.prefetchIndex; i++) {
//			Pego node = e.get(i).simpleMatch(left, this);
//			if(node.isFailure()) {
//				this.rollback(startIndex);
//				return node;
//			}
//			assert(left == node);
//		}
////		}
//		int mark = this.markObjectStack();
//		Pego newnode = this.newPegObject(e.tagName, startIndex, e);
//		if(e.leftJoin) {
//			this.logSetter(newnode, -1, leftNode);
//		}
//		for(int i = e.prefetchIndex; i < e.size(); i++) {
//			Pego node = e.get(i).simpleMatch(newnode, this);
//			if(node.isFailure()) {
//				this.rollbackObjectStack(mark);
//				this.rollback(startIndex);
//				return node;
//			}
//			//			if(node != newnode) {
//			//				e.warning("dropping @" + newnode.name + " " + node);
//			//			}
//		}
//		this.popNewObject(newnode, startIndex, mark);
//		if(this.stat != null) {
//			this.stat.countObjectCreation();
//		}
//		return newnode;
//	}
	
//	long statExportCount = 0;
//	long statExportSize  = 0;
//	long statExportFailure  = 0;

	public Pego matchExport(Pego left, PegExport e) {
		Pego pego = e.inner.simpleMatch(left, this);
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

	private BlockingQueue<Pego> queue = null; 
	protected void pushBlockingQueue(Pego pego) {
		if(this.queue != null) {
			try {
				this.queue.put(pego);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

//	public Pego matchSetter(Pego left, PegSetter e) {
//		long pos = left.getSourcePosition();
//		Pego node = e.inner.simpleMatch(left, this);
//		if(node.isFailure() || left == node) {
//			return node;
//		}
//		if(this.isRecognitionOnly()) {
//			left.setSourcePosition(pos);
//		}
//		else {
//			this.logSetter(left, e.index, node);
//		}
//		return left;
//	}

//	public Pego matchTag(Pego left, PegTagging e) {
//		left.setTag(e.symbol);
//		return left;
//	}
//
//	public Pego matchMessage(Pego left, PegMessage e) {
//		left.setMessage(e.symbol);
//		//left.startIndex = this.getPosition();
//		return left;
//	}
//	
//	public Pego matchIndent(Pego left, PegIndent e) {
//	}
//
//	public Pego matchIndex(Pego left, PegIndex e) {
////		String text = left.textAt(e.index, null);
////		if(text != null) {
////			if(this.match(text)) {
////				return left;
////			}
////		}
//		return this.foundFailure(e);
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

	public void endPerformStat(Pego pego) {
		if(stat != null) {
			stat.end(pego, this);
		}
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}
}


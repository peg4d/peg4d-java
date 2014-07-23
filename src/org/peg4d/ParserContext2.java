package org.peg4d;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public abstract class ParserContext2 {
	public final          ParserSource source;
	protected long        sourcePosition = 0;
	public    long        endPosition;
	public    Grammar     rules = null;
	
	long statBacktrackCount = 0;
	long statBacktrackSize = 0;
	long statWorstBacktrack = 0;
	int  statObjectCount = 0;

	public ParserContext2(ParserSource source, long startIndex, long endIndex) {
		this.source = source;
		this.sourcePosition = startIndex;
		this.endPosition = endIndex;
	}
	
	public abstract void setRuleSet(Grammar ruleSet);

	protected final long getPosition() {
		return this.sourcePosition;
	}
	protected final void setPosition(long pos) {
		this.sourcePosition = pos;
	}
	protected final void rollback(long pos) {
		long len = this.sourcePosition - pos;
		if(len > 0) {
			this.statBacktrackCount = this.statBacktrackCount + 1;
			this.statBacktrackSize = this.statBacktrackSize + len;
			if(len > this.statWorstBacktrack) {
				this.statWorstBacktrack = len;
			}
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
	protected final char charAt(long pos) {
		if(pos < this.endPosition) {
			return this.source.charAt(pos);
		}
		return '\0';
	}

	protected final char getChar() {
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

	protected final boolean match(char ch) {
		if(ch == this.getChar()) {
			this.consume(1);
			return true;
		}
		return false;
	}

	protected final boolean match(String text) {
		if(this.endPosition - this.sourcePosition >= text.length()) {
			for(int i = 0; i < text.length(); i++) {
				if(text.charAt(i) != this.source.charAt(this.sourcePosition + i)) {
					return false;
				}
			}
			this.consume(text.length());
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
	
	protected final long matchZeroMore(UCharset charset) {
		for(;this.hasChar(); this.consume(1)) {
			char ch = this.source.charAt(this.sourcePosition);
			if(!charset.match(ch)) {
				break;
			}
		}
		return this.sourcePosition;
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
		MainOption._Exit(1, msg);
	}
	
	public boolean hasNode() {
		this.matchZeroMore(UCharset.WhiteSpaceNewLine);
		return this.sourcePosition < this.endPosition;
	}

	public Pego parseNode(String startPoint) {
		this.initMemo();
		Peg start = this.getRule(startPoint);
		if(start == null) {
			MainOption._Exit(1, "undefined start rule: " + startPoint );
		}
		long oid = start.fastMatch(1, this);
		Pego pego = null;
		if(PEGUtils.isFailure(oid)) {
			pego = new Pego("#error", this.source, this.rules.getPeg(oid), PEGUtils.getpos(oid));
			pego.message = this.source.formatErrorMessage("syntax error", PEGUtils.getpos(oid), "");
			System.out.println(pego.message);
		}
		else {
			pego = new Pego("#toplevel", this.source, this.rules.getPeg(oid), PEGUtils.getpos(oid));
			pego.expandAstToSize(this.objectList.size());
			for(int i = 0; i < this.objectList.size(); i++) {
				pego.append(this.objectList.ArrayValues[i]);
			}
		}
		return pego;
	}
	
	protected Memo memoMap = null;
	public abstract void initMemo();
	
//	
//	protected long successResult = null;
//	
//	public final void setRecognitionOnly(boolean checkMode) {
//		if(checkMode) {
//			this.successResult = new long("#success", this.source, null, 0);
//		}
//		else {
//			this.successResult = null;
//		}
//	}
//	
//	public final boolean isRecognitionOnly() {
//		return this.successResult != null;
//	}
//	
//	public final long newPegObject(String name, Peg created, long pos) {
//		if(this.isRecognitionOnly()) {
//			this.successResult.startIndex = pos;
//			return this.successResult;
//		}
//		else {
//			this.statObjectCount = this.statObjectCount + 1;
//			long node = new long(name, this.source, created, pos);
//			return node;
//		}
//	}
//	
//	public final long newErrorObject() {
//		long node = newPegObject("#error", this.foundFailureNode.createdPeg, this.foundFailureNode.startIndex);
//		node.matched = Functor.ErrorFunctor;
//		return node;
//	}
	
	private long failureObjectId = 0;
	public final long foundFailure(Peg e) {
		if(this.sourcePosition >= PEGUtils.getpos(this.failureObjectId)) {  // adding error location
			this.failureObjectId = PEGUtils.merge(this.sourcePosition, e);
		}
		return this.failureObjectId;
	}
	
	public final long refoundFailure(Peg e, long pos) {
		this.failureObjectId = PEGUtils.merge(pos, e);
		return this.failureObjectId;
	}

	public abstract Peg getRule(String symbol);
	long[] logTriple = new long[256 * 3];
	int stackTop = 0;
	UList<Pego> objectList = new UList<Pego>(new Pego[16]);
	
	public final static int OpPosition = -2;
	public final static int OpTagging  = -3;
	public final static int OpMessage  = -3;
	
	void log(int op, long parent, long data) {
		if(!(stackTop < this.logTriple.length)) {
			long[] newlog = new long[logTriple.length * 2];
			System.arraycopy(this.logTriple, 0, newlog, 0, this.logTriple.length);
			this.logTriple = newlog;
		}
		this.logTriple[this.stackTop] = parent;
		this.logTriple[this.stackTop+1] = op;
		this.logTriple[this.stackTop+2] = data;
		this.stackTop += 3;
	}

	protected final int pushNewMarker() {
		return this.stackTop;
	}

	protected final void popBack(int markerId) {
		this.stackTop = markerId;
	}
	
	protected final void pushSetter(long parent, int index, long child) {
		if(parent != 1) { // root!!
			log(index, parent, child);
		}
		Pego o = createObject(this.stackTop, child);
		this.stackTop = 0;
		this.objectList.add(o);
	}
	
	protected final Pego createObject(int top, long oid) {
		int size = 0;
		Peg created = this.rules.getPeg(oid);
		Pego o = new Pego(null, this.source, created, PEGUtils.getpos(oid));
		for(int i = 0; i < top; i += 3) {
			if(this.logTriple[i] == oid) {
				int op = (int)this.logTriple[i+1];
				if(op == -1) {
					this.logTriple[i+1] = size;
					size++;
				}
				else if(op >= 0) {
					if(size < op + 1) {
						size = op + 1;
					}
				}
				else if(op == OpPosition) {
					o.setEndPosition(this.logTriple[i+2]);
				}
				else if(op == OpTagging) {
					PegTagging tagging = (PegTagging)this.rules.getPeg(this.logTriple[i+2]);
					o.tag = tagging.symbol;
				}
				else if(op == OpMessage) {
					PegMessage message = (PegMessage)this.rules.getPeg(this.logTriple[i+2]);
					o.message = message.symbol;
				}
			}
		}
		if(size > 0) {
			o.expandAstToSize(size);		
			for(int i = 0; i < top; i += 3) {
				if(this.logTriple[i] == oid) {
					int op = (int)this.logTriple[i+1];
					if(op >= 0) {
						Pego node = createObject(i, this.logTriple[i+2]);
						o.set(op, node);
					}
				}
			}
			o.checkNullEntry();
		}
		return o;
	}
	
	public long matchNewObject(long left, PegNewObject e) {
		long leftNode = left;
		long startIndex = this.getPosition();
		if(MainOption.VerboseStatCall) {
			this.count(e, startIndex);
		}
		if(e.predictionIndex > 0) {
			for(int i = 0; i < e.predictionIndex; i++) {
				long node = e.get(i).fastMatch(left, this);
				if(PEGUtils.isFailure(node)) {
					this.rollback(startIndex);
					return node;
				}
				assert(left == node);
			}
		}
		int markerId = this.pushNewMarker();
		long newnode = PEGUtils.merge(startIndex, e);
		if(e.leftJoin) {
			this.pushSetter(newnode, -1, leftNode);
		}
		for(int i = e.predictionIndex; i < e.size(); i++) {
			long node = e.get(i).fastMatch(newnode, this);
			if(PEGUtils.isFailure(node)) {
				this.popBack(markerId);
				this.rollback(startIndex);
				return node;
			}
		}
		this.log(OpPosition, newnode, this.getPosition());
		return newnode;
	}
	
	long statExportCount = 0;
	long statExportSize  = 0;
	long statExportFailure  = 0;

	public long matchExport(long left, PegExport e) {
		int markerId = this.pushNewMarker();
		long node = e.inner.fastMatch(left, this);
		if(!PEGUtils.isFailure(node)) {
			Pego pego = createObject(this.pushNewMarker(), node);
			this.statExportCount += 1;
			this.statExportSize += pego.length;
			this.pushBlockingQueue(pego);
		}
		else {
			this.statExportFailure += 1;
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

	public long matchSetter(long left, PegSetter e) {
		long node = e.inner.fastMatch(left, this);
		if(PEGUtils.isFailure(node) || left == node) {
			return node;
		}
		this.pushSetter(left, e.index, node);
		return left;
	}
	
	public long matchIndent(long left, PegIndent e) {
		String indent = this.source.getIndentText(PEGUtils.getpos(left));
		if(this.match(indent)) {
			return left;
		}
		return this.foundFailure(e);
	}

	public long matchIndex(long left, PegIndex e) {
//		TODO
//		String text = left.textAt(e.index, null);
//		if(text != null) {
//			if(this.match(text)) {
//				return left;
//			}
//		}
		return this.foundFailure(e);
	}


	Map<Long, Peg> countMap = null;
	private UList<Peg> statCalledPegList = null;

	private int statCallCount = 0;
	private int statRepeatCount = 0;
	
	private void checkCountMap() {
		if(this.countMap == null) {
			this.statCallCount = 0;
			this.statRepeatCount = 0;
			this.countMap = new HashMap<Long, Peg>();
			this.statCalledPegList = new UList<Peg>(new Peg[256]);
		}
	}
	
	protected final void count(Peg e, long pos) {
		e.statCallCount += 1;
		statCallCount += 1;
		if(MainOption.VerboseStatCall) {
			checkCountMap();
			Long key = Memo.makekey(pos, e);
			Peg p = this.countMap.get(key);
			if(p != null) {
				assert(p == e);
				p.statRepeatCount += 1;
				statRepeatCount += 1;
			}
			else {
				this.countMap.put(key, e);
			}
			if(e.statCallCount == 1) {
				this.statCalledPegList.add(e);
			}
		}
	}
	
	
	long statErapsedTime = 0;
	long usedMemory;
	int statOptimizedPeg = 0;
	
		
	private String ratio(double num) {
		return String.format("%.3f", num);
	}
	private String Punit(String unit) {
		return "[" + unit +"]";
	}

	private String Kunit(long num) {
		return String.format("%.3f", (double)num / 1024);
	}
	
	private String Munit(double num) {
		return String.format("%.3f", num/(1024*1024));
	}

	private String Nunit(long num, String unit) {
		return num + Punit(unit);
	}

	private String Kunit(long num, String unit) {
		return ratio((double)num / 1024) + Punit(unit);
	}
	
	private String Munit(long num, String unit) {
		return ratio((double)num/(1024*1024)) + Punit(unit);
	}

	private String KMunit(long num, String unit, String unit2) {
		return Kunit(num, unit) + " " + Munit(num, unit2);
	}

	private String kpx(double num) {
		return ratio(num / 1024);
	}

	private String kpx(double num, String unit) {
		return kpx(num) + Punit(unit);
	}

	private String mpx(double num) {
		return ratio(num / (1024*1024));
	}

	private String mpx(double num, String unit) {
		return mpx(num) + Punit(unit);
	}


	
	public void beginStatInfo() {
		System.gc(); // meaningless ?
		this.statBacktrackSize = 0;
		this.statBacktrackCount = 0;
		long total = Runtime.getRuntime().totalMemory();
		long free =  Runtime.getRuntime().freeMemory();
		usedMemory =  total - free;
		statErapsedTime = System.currentTimeMillis();
	}

	public void endStatInfo(long parsedObject) {
		statErapsedTime = (System.currentTimeMillis() - statErapsedTime);
		System.gc(); // meaningless ?
		if(MainOption.VerboseStat) {
			System.gc(); // meaningless ?
			if(MainOption.VerbosePeg) {
				System.out.println("parsed:\n" + parsedObject);
				if(this.hasChar()) {
					System.out.println("** uncosumed: '" + this.source + "' **");
				}
			}
			long statCharLength = this.getPosition();
			long statFileLength = this.source.getFileLength();
			long statReadLength = this.source.statReadLength;
			double fileKps = (statFileLength) / (statErapsedTime / 1000.0);
			System.out.println("parser: " + this.getClass().getSimpleName() + " -O" + MainOption.OptimizedLevel + " -Xw" + MainOption.MemoFactor + " optimized peg: " + this.statOptimizedPeg );
			System.out.println("file: " + this.source.fileName + " filesize: " + KMunit(statFileLength, "Kb", "Mb"));
			System.out.println("IO: " + this.source.statIOCount +" read/file: " + ratio((double)statReadLength/statFileLength) + " pagesize: " + Nunit(FileSource.PageSize, "bytes") + " read: " + KMunit(statReadLength, "Kb", "Mb"));
			System.out.println("erapsed time: " + Nunit(statErapsedTime, "msec") + " speed: " + kpx(fileKps,"KiB/s") + " " + mpx(fileKps, "MiB/s"));
			System.out.println("backtrack raito: " + ratio((double)this.statBacktrackSize / statCharLength) + " backtrack: " + this.statBacktrackSize + " length: " + this.source.length() + ", consumed: " + statCharLength);
			System.out.println("backtrack_count: " + this.statBacktrackCount + " average: " + ratio((double)this.statBacktrackSize / this.statBacktrackCount) + " worst: " + this.statWorstBacktrack);
//			int usedObject = parsedObject.count(); 
//			System.out.println("object: created: " + this.statObjectCount + " used: " + usedObject + " disposal ratio u/c " + ratio((double)usedObject/this.statObjectCount) + " stacks: " + maxLog);
//			System.out.println("stream: exported: " + this.statExportCount + ", size: " + this.statExportSize + " failure: " + this.statExportFailure);
			System.out.println("calls: " + this.statCallCount + " repeated: " + this.statRepeatCount + " r/c: " + ratio((double)this.statRepeatCount/this.statCallCount));
			System.out.println("memo hit: " + this.memoMap.memoHit + ", miss: " + this.memoMap.memoMiss + 
					", ratio: " + ratio(((double)this.memoMap.memoHit / (this.memoMap.memoMiss))) + ", consumed memo:" + this.memoMap.memoSize +" slots: " + this.memoMap.statMemoSlotCount);
			long total = Runtime.getRuntime().totalMemory();
			long free =  Runtime.getRuntime().freeMemory();
			long heap =  total - free;
			long used =  heap - usedMemory;
			System.out.println("heap: " + KMunit(heap, "KiB", "MiB") + " used: " + KMunit(used, "KiB", "MiB") + " heap/file: " + ratio((double) heap/ (statFileLength)));
			System.out.println();
		}
	}
	
//	private void showCallCounterList() {
//		if(this.statCallCounterList != null) {
//			for(int i = 0; i < this.statCallCounterList.size(); i++) {
//				CallCounter c = this.statCallCounterList.ArrayValues[i];
//				statCallCounter += c.total;
//				statCallRepeated += c.repeated;
//				if(Main.VerboseStat) {
//					System.out.println("\t"+c.ruleName+" calls: " + c.total + " repeated: " + c.repeated + " r/c: " + ratio((double)c.repeated/c.total));
//				}
//			}
//		}
//	}


	
}


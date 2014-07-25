package org.peg4d;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

public class MonadicParser extends ParserContext {

	public MonadicParser(Grammar peg, ParserSource source) {
		super(peg, source, 0, source.length());
		if(!this.isRecognitionOnly()) {
			this.quadLog = new int[4096 * 4];
		}
	}

	@Override
	public void initMemo() {
		this.memoMap = new NoMemo();
	}

	@Override
	public Pego parseNode(String startPoint) {
		this.initMemo();
		Peg start = this.getRule(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		long pos = this.getPosition();
		int oid = start.fastMatch(1, this);
		Pego pego = null;
		if(PEGUtils.isFailure(oid)) {
			Peg e = this.peg.getPeg(this.failurePosition);
			pego = Pego.newSource("#error", this.source, PEGUtils.getpos(this.failurePosition));
			String msg = this.source.formatErrorMessage("syntax error", PEGUtils.getpos(this.failurePosition), " by " + e);
			pego.setMessage(msg);
			System.out.println(msg);
//			if(pos == this.getPosition()) {
			this.setPosition(this.endPosition);  // skip 
//			}
		}
		else {
			if(oid == this.bigDataOid) {
				return bigPego;
			}
			else {
				pego = this.createObject(oid);
			}
		}
//		else {
//			pego = new Pego("#toplevel", this.source, this.peg.getPeg(oid), PEGUtils.getpos(oid));
//			pego.expandAstToSize(this.objectList.size());
//			for(int i = 0; i < this.objectList.size(); i++) {
//				pego.append(this.objectList.ArrayValues[i]);
//			}
//		}
		return pego;
	}
	
	private long failurePosition = 0;
	public final int foundFailure2(Peg e) {
		if(this.sourcePosition >= PEGUtils.getpos(this.failurePosition)) {  // adding error location
			this.failurePosition = PEGUtils.failure(this.sourcePosition, e);
		}
		return 0; //this.failureObjectId;
	}
	
	public final long refoundFailure2(Peg e, long pos) {
		this.failurePosition = PEGUtils.failure(pos, e);
		return 0; //this.failureObjectId;
	}

	//[0]
	//new oid pos len
	//tag oid tag msg
	//set oid idx child
	//add oid child
	private int[] quadLog = null;
	int stackTop = 0;

	public final static int OpNew2 = 0;
	public final static int OpTag2 = 1;
	public final static int OpSet2 = 2;
	public final static int OpNop2 = 3;
	public final static int OpMask2 = 7; // 1 | (1 << 1) | 1 << 2;
	
	private int getop2(int op) {
		return op & OpMask2;
	}
	
	private int getext3(int op) {
		return op >> 3;
	}

	private int makeop2(int op, int ext3) {
		return ext3 << 3 | op;
	}
	
	void log4(int op, int ext, int oid, int data1, int data2) {
		if(this.quadLog != null) {
			if(!(stackTop + 4 < this.quadLog.length)) {
				int[] newlog = new int[quadLog.length * 4];
				System.arraycopy(this.quadLog, 0, newlog, 0, this.quadLog.length);
				this.quadLog = newlog;
			}
			this.quadLog[this.stackTop]   = oid;
			this.quadLog[this.stackTop+1] = makeop2(op, ext);
			this.quadLog[this.stackTop+2] = data1;
			this.quadLog[this.stackTop+3] = data2;
			this.stackTop += 4;
		}
	}

	private int readOid(int mark) {
		return this.quadLog[mark];
	}

	private int readOp(int mark) {
		return this.getop2(this.quadLog[mark+1]);
	}

	private int readExt3(int mark) {
		return this.getext3(this.quadLog[mark+1]);
	}

	private void writeExt3(int mark, int ext3) {
		this.quadLog[mark+1] = makeop2(this.getop2(this.quadLog[mark+1]), ext3);
	}

	private void writeData(int mark, int data) {
		this.quadLog[mark+2] = data;
	}

	private void writeData2(int mark, int data) {
		this.quadLog[mark+3] = data;
	}

	private int readData(int mark) {
		return this.quadLog[mark+2];
	}

	private int readData2(int mark) {
		return this.quadLog[mark+3];
	}

	int unusedObjectId = 2;
	int lazyNewObject(long pos) {
		int oid = this.unusedObjectId;
		log4(OpNew2, 0, oid, (int)pos, 0);
		this.unusedObjectId += 1;
		return oid;
	}

	int searchOid(int oid) {
		for(int i = this.stackTop - 4; i >= 0; i -= 4) {
			if(readOid(i) == oid) {
				if(readOp(i) == OpNew2) {
					return i;
				}
				int diff = readExt3(i);
				if(diff > 0) {
					return i - diff * 4;
				}
			}
		}
		assert(oid == 0); // this not happens
		return -1;
	}
	
	int makediff(int oid) {
		return (this.stackTop - this.searchOid(oid)) / 4;
	}

	void lazyTagging(int oid, PegTagging e) {
		if(bigDataOid == oid) {
			//this.bigPego.tag = e.symbol;
			return;
		}
		if(readOid(this.stackTop) == oid && readOp(this.stackTop) == OpTag2) {
			writeData(this.stackTop, e.uniqueId);
		}
		else {
			log4(OpTag2, makediff(oid), oid, e.uniqueId, 0);
		}
	}

	void lazyMessaging(int oid, PegMessage e) {
		if(bigDataOid == oid) {
			this.bigPego.setMessage(e.symbol);
			return;
		}
		if(readOid(this.stackTop) == oid && readOp(this.stackTop) == OpTag2) {
			writeData2(this.stackTop, e.uniqueId);
		}
		else {
			log4(OpTag2, makediff(oid), oid, 0, e.uniqueId);
		}
	}
	
	private static int BigDataSize = 256;
	int bigDataOid = 0;
	Pego bigPego = null;
	UList<Pego> bigList = null;
	
	void lazySetter(int oid, int index, int child) {
		if(bigDataOid == oid) {
			//System.out.println("child big data: " + mark + " " + child + " stackTop" + stackTop);
			Pego p = createObject(child);
			bigList.add(p);
		}
		else {
			int mark = this.searchOid(oid);
			int size = readExt3(mark);
			if(index == -1) {
				size++;
				index = size-1;
			}
			else {
				if(index < size) {
					size = index + 1;
				}
			}
			writeExt3(mark, size);
			log4(OpSet2, makediff(oid), oid, index, child);
			if(size > BigDataSize && this.bigDataOid == 0) {
				this.bigDataOid = oid;
				this.bigPego = createObject(oid);
				this.bigList = new UList<Pego>(new Pego[BigDataSize*5]);
				System.out.println("found big data: " + oid + " " + size);
			}
		}
	}

	void lazyCapture(int mark, int length) {
		writeData2(mark, length);
	}
	
	void dumpStack(int s, int top) {
		for(int i = s; i < top; i += 4) {
			int op = this.readOp(i);
			int oid = this.readOid(i);
			int n = i / 4;
			if(op == OpNew2) {
				System.out.println("[" + n + "] NEW:" + oid + " pos=" + readData(i) + " length = " + readData2(i) + " size=" + readExt3(i));
			}
			else if(op >= OpSet2) {
				System.out.println("[" + n + "] SET:" + oid + " " + readData(i) + " " + readData2(i) + " diff= " + readExt3(i));
			}
			else if(op == OpTag2) {
				String tag = " ";
				if(readData(i) > 0) {
					PegTagging tagging = (PegTagging)this.peg.getPeg(readData(i));
					tag = tagging.symbol + " ";
				}
				String msg = " ";
				if(readData2(i) > 0) {
					PegMessage message = (PegMessage)this.peg.getPeg(readData2(i));
					tag = "`"+message.symbol + "` ";
				}
				System.out.println("[" + n + "] TAG:" + oid + " " + tag + " " + msg + " diff= " + readExt3(i));
			}
		}
	}
	
	public Pego createObjectImpl(int s, int top, int getoid) {
		long offset = 0;
		HashMap<Integer, Pego> m = new HashMap<Integer, Pego>();
		Pego cur = null;
		for(int i = s; i < top; i += 4) {
			int op = this.readOp(i);
			int oid = this.readOid(i);
			if(op == OpNew2) {
				cur = Pego.newPego(this.source, offset + readData(i), readData2(i), readExt3(i));
				m.put(oid, cur);
			}
			else if(op >= OpSet2) {
				int index = readData(i);
				Integer cid = readData2(i);
				Pego parent = m.get(oid);
				Pego child = m.get(cid);
				parent.set(index, child);
				child.checkNullEntry();
				m.remove(cid);
//				System.out.println("[" + n + "] SET:" + oid + " " + readData(i) + " " + readData2(i) + " diff= " + readExt3(i));
			}
			else if(op == OpTag2) {
				if(readData(i) > 0) {
					PegTagging tagging = (PegTagging)this.peg.getPeg(readData(i));
					Pego p = m.get(oid);
					p.setTag(tagging.symbol);
				}
				if(readData2(i) > 0) {
					PegMessage message = (PegMessage)this.peg.getPeg(readData2(i));
					Pego p = m.get(oid);
					p.setTag(message.symbol);
				}
				//System.out.println("[" + n + "] TAG:" + oid + " " + tag + " " + msg + " diff= " + readExt3(i));
			}
		}
		Integer[] key = m.keySet().toArray(new Integer[1]);
		if(m.size() > 1) {
			System.out.println("droped size: " + (m.size() - 1));
			for(Integer k : key) {
				if(k != getoid) {
					System.out.println("oid: " + k + " " + m.get(k));
				}
			}
		}
		return m.get(getoid);
	}

	
	@Override
	protected final int markObjectStack() {
		return this.stackTop;
	}

	@Override
	protected final void rollbackObjectStack(int mark) {
		if(mark < this.stackTop) {
			assert(readOp(mark) == OpNew2);
			//System.out.println("dispose " + this.readOid(mark));
			this.unusedObjectId = this.readOid(mark);
		}
		this.stackTop = mark;
	}
	
	protected final Pego createObject(int oid) {
		Pego pego = null;
		//dumpStack(0, top);
		if(this.quadLog != null) {
			pego = createObjectImpl(this.searchOid(oid), this.stackTop, oid);
		}
		else {
			pego = Pego.newSource("#empty", this.source, 0);
		}
		if(pego == null) {
			//dumpStack(0, top);
			System.out.println("created: " + pego);
			assert(pego != null);
		}
		return pego;
	}
	
	public int matchNewObject(int left, PegNewObject e) {
		long startIndex = this.getPosition();
		if(Main.VerboseStatCall) {
			this.count(e, startIndex);
		}
		if(e.predictionIndex > 0) {
			for(int i = 0; i < e.predictionIndex; i++) {
				int right = e.get(i).fastMatch(left, this);
				if(PEGUtils.isFailure(right)) {
					this.rollback(startIndex);
					return right;
				}
				assert(left == right);
			}
		}
		int mark = this.markObjectStack();
		int newnode = lazyNewObject(startIndex);
		this.statObjectCount += 1;
		//this.showPosition("new " + newnode + " " + e + mark/4);
		if(e.leftJoin) {
			this.lazySetter(newnode, 0, left);
		}
		for(int i = e.predictionIndex; i < e.size(); i++) {
			//System.out.println("newO B i= " +i + ", pos=" + this.getPosition() + ", "+ newnode + " e=" + e.get(i));
			int right = e.get(i).fastMatch(newnode, this);
			if(PEGUtils.isFailure(right)) {
//				this.showPosition("dispose " + newnode + " " + e + mark/4);				
				this.rollbackObjectStack(mark);
				this.rollback(startIndex);
				return right;
			}
			if(newnode != right) {
				//this.showPosition("dropping new oid " + newnode +"=>" + right + " by " + e.get(i) + " IN " + e);
				//System.out.println("mark=" + markToIgnoreUnsetObject + " search.. " + this.searchOid(right) + " < " + this.stackTop);
				this.rollbackObjectStack(this.searchOid(right));
			}
		}
		this.lazyCapture(mark, (int)(this.getPosition() - startIndex));
		return newnode;
	}
	
	long statExportCount = 0;
	long statExportSize  = 0;
	long statExportFailure  = 0;

	public int matchExport(int left, PegExport e) {
		int markerId = this.markObjectStack();
		int node = e.inner.fastMatch(left, this);
		if(!PEGUtils.isFailure(node)) {
			Pego pego = createObject(node);
			this.statExportCount += 1;
			this.statExportSize += pego.getLength();
			this.pushBlockingQueue(pego);
		}
		else {
			this.statExportFailure += 1;
		}
		return left;
	}

	private BlockingQueue<Pego> queue = null; 
	
	@Override
	protected void pushBlockingQueue(Pego pego) {
		if(this.queue != null) {
			try {
				this.queue.put(pego);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public int matchSetter(int left, PegSetter e) {
		int mark = this.markObjectStack();
		int right = e.inner.fastMatch(left, this);
		if(PEGUtils.isFailure(right) || left == right) {
			this.rollbackObjectStack(mark);
			return right;
		}
		this.lazySetter(left, e.index, right);
		return left;
	}
	
	public int matchIndent(int left, PegIndent e) {
		String indent = this.source.getIndentText(PEGUtils.getpos(left));
		if(this.match(indent)) {
			return left;
		}
		return this.foundFailure2(e);
	}

	public int matchIndex(int left, PegIndex e) {
//		TODO
//		String text = left.textAt(e.index, null);
//		if(text != null) {
//			if(this.match(text)) {
//				return left;
//			}
//		}
		return this.foundFailure2(e);
	}

}


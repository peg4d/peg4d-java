//package org.peg4d;
//
//import java.util.HashMap;
//import java.util.concurrent.BlockingQueue;
//
//public class MonadicParser extends ParserContext {
//
//	public MonadicParser(Grammar peg, ParserSource source) {
//		super(peg, source, 0, source.length());
//		this.log = new OLog();
//	}
//
//	@Override
//	public void initMemo() {
//		this.memoMap = new NoMemo();
//	}
//
//	@Override
//	public Pego parseNode(String startPoint) {
//		this.initMemo();
//		Peg start = this.getRule(startPoint);
//		if(start == null) {
//			Main._Exit(1, "undefined start rule: " + startPoint );
//		}
//		int oid = start.fastMatch(1, this);
//		Pego pego = null;
//		if(PEGUtils.isFailure(oid)) {
//			Peg e = this.peg.getPeg(this.failurePosition);
//			pego = Pego.newSource("#error", this.source, PEGUtils.getpos(this.failurePosition));
//			String msg = this.source.formatErrorMessage("syntax error", PEGUtils.getpos(this.failurePosition), " by " + e);
//			pego.setMessage(msg);
//			System.out.println(msg);
//			this.setPosition(this.endPosition);  // skip 
//		}
//		else {
//			pego = this.log.createObject(oid);
//		}
//		return pego;
//	}
//	
//	private long failurePosition = 0;
//	public final int foundFailure2(Peg e) {
//		if(this.sourcePosition >= PEGUtils.getpos(this.failurePosition)) {  // adding error location
//			this.failurePosition = PEGUtils.failure(this.sourcePosition, e);
//		}
//		return 0; //this.failureObjectId;
//	}
//	
//	public final long refoundFailure2(Peg e, long pos) {
//		this.failurePosition = PEGUtils.failure(pos, e);
//		return 0; //this.failureObjectId;
//	}
//
//	//[0]
//	//new oid pos len
//	//tag oid tag msg
//	//set oid idx child
//	//add oid child
//
//	OLog log;
//	
//	@Override
//	protected final int markObjectStack() {
//		return this.log.stackTop;
//	}
//
//	@Override
//	protected final void rollbackObjectStack(int mark) {
//		this.log.rollbackObjectStack(mark);
//	}
//	
//	class OLog {
//		private int[] quadLog = null;
//		private int stackTop = 0;
//
//		OLog () {
//			if(!isRecognitionOnly()) {
//				this.quadLog = new int[4096 * 4];
//			}
//		}
//	
//		public final static int OpNew = 0;
//		public final static int OpTag = 1;
//		public final static int OpSet = 2;
//		public final static int OpNop2 = 3;
//		public final static int OpMask2 = 7; // 1 | (1 << 1) | 1 << 2;
//	
//		private int getop2(int op) {
//			return op & OpMask2;
//		}
//		
//		private int getext3(int op) {
//			return op >> 3;
//		}
//	
//		private int makeop2(int op, int ext3) {
//			return ext3 << 3 | op;
//		}
//	
//		void log4(int op, int ext, int oid, int data1, int data2) {
//			if(this.quadLog != null) {
//				if(!(stackTop + 4 < this.quadLog.length)) {
//					int[] newlog = new int[quadLog.length * 4];
//					System.arraycopy(this.quadLog, 0, newlog, 0, this.quadLog.length);
//					this.quadLog = newlog;
//				}
//				this.quadLog[this.stackTop]   = oid;
//				this.quadLog[this.stackTop+1] = makeop2(op, ext);
//				this.quadLog[this.stackTop+2] = data1;
//				this.quadLog[this.stackTop+3] = data2;
//				this.stackTop += 4;
//			}
//		}
//		
//		private int readObjectIdAt(int mark) {
//			return this.quadLog[mark];
//		}
//
//		private int readLogOperatorAt(int mark) {
//			return this.getop2(this.quadLog[mark+1]);
//		}
//
//		private int readShortDataAt(int mark) {
//			return this.getext3(this.quadLog[mark+1]);
//		}
//
//		private void writeExt3(int mark, int ext3) {
//			this.quadLog[mark+1] = makeop2(this.getop2(this.quadLog[mark+1]), ext3);
//		}
//
//		private void writeData(int mark, int data) {
//			this.quadLog[mark+2] = data;
//		}
//	
//		private void writeData2(int mark, int data) {
//			this.quadLog[mark+3] = data;
//		}
//	
//		private int readData(int mark) {
//			return this.quadLog[mark+2];
//		}
//	
//		private int readData2(int mark) {
//			return this.quadLog[mark+3];
//		}
//		
//		void dump(int bottom, int top) {
//			for(int i = bottom; i < top; i += 4) {
//				int op = this.readLogOperatorAt(i);
//				int oid = this.readObjectIdAt(i);
//				int n = i / 4;
//				if(op == OpNew) {
//					System.out.println("[" + n + "] NEW:" + oid + " pos=" + readData(i) + " length = " + readData2(i) + " size=" + readShortDataAt(i));
//				}
//				else if(op >= OpSet) {
//					System.out.println("[" + n + "] SET:" + oid + " " + readData(i) + " " + readData2(i) + " diff= " + readShortDataAt(i));
//				}
//				else if(op == OpTag) {
//					String tag = " ";
//					if(readData(i) > 0) {
//						PegTagging tagging = (PegTagging)peg.getPeg(readData(i));
//						tag = tagging.symbol + " ";
//					}
//					String msg = " ";
//					if(readData2(i) > 0) {
//						PegMessage message = (PegMessage)peg.getPeg(readData2(i));
//						tag = "`"+message.symbol + "` ";
//					}
//					System.out.println("[" + n + "] TAG:" + oid + " " + tag + " " + msg + " diff= " + readShortDataAt(i));
//				}
//				else {
//					System.out.println("[" + n + "] OP?:" + oid + " " + op + "  diff= " + readShortDataAt(i));
//				}
//			}
//		}
//	
//		int unusedObjectId = 2;
//		
//		int newObjectId(long pos) {
//			int oid = this.unusedObjectId;
//			log4(OpNew, 0, oid, (int)pos, 0);
//			this.unusedObjectId += 1;
//			return oid;
//		}
//		
//		void rollbackObjectStack(int mark) {
//			assert(mark <= this.stackTop);
//			if(mark < this.stackTop) {
//				assert(readLogOperatorAt(mark) == OpNew);
//				this.unusedObjectId = this.readObjectIdAt(mark);
//				System.out.println("BACK to " + mark + " unused_oid=" + this.unusedObjectId);
//			}
//			this.stackTop = mark;
//		}
//	
//		int searchMarkByObjectId(int oid) {
//			for(int i = this.stackTop - 4; i >= 0; i -= 4) {
//				if(readObjectIdAt(i) == oid) {
//					if(readLogOperatorAt(i) == OpNew) {
//						return i;
//					}
//					int diff = readShortDataAt(i);
//					if(diff > 0) {
//						return i - diff * 4;
//					}
//				}
//			}
//			assert(oid == 0); // this not happens
//			return -1;
//		}
//		
//		int makediff(int oid) {
//			return (this.stackTop - this.searchMarkByObjectId(oid)) / 4;
//		}
//
//		void lazyTagging(int oid, PegTagging e) {
//			if(readObjectIdAt(this.stackTop) == oid && readLogOperatorAt(this.stackTop) == OpTag) {
//				writeData(this.stackTop, e.uniqueId);
//			}
//			else {
//				log4(OpTag, makediff(oid), oid, e.uniqueId, 0);
//			}
//		}
//	
//		void lazyMessaging(int oid, PegMessage e) {
//			if(readObjectIdAt(this.stackTop) == oid && readLogOperatorAt(this.stackTop) == OpTag) {
//				writeData2(this.stackTop, e.uniqueId);
//			}
//			else {
//				log4(OpTag, makediff(oid), oid, 0, e.uniqueId);
//			}
//		}
//				
//		void lazySetter(int oid, int index, int child) {
//			int mark = this.searchMarkByObjectId(oid);
//			int size = readShortDataAt(mark);
//			if(index == -1) {
//				size++;
//				index = size-1;
//			}
//			else {
//				if(index < size) {
//					size = index + 1;
//				}
//			}
//			writeExt3(mark, size);
//			log4(OpSet, makediff(oid), oid, index, child);
//		}
//	
//		void lazyCaptureString(int mark, int oid, int length) {
//			writeData2(mark, length);
//			this.dump(mark, this.stackTop);
//			this.createObjectImpl(mark, this.stackTop);
//		}
//
//		private HashMap<Integer, Pego> objectMap = new HashMap<Integer, Pego>();
//		
//		private Pego get(int oid) {
//			Pego p = this.objectMap.get(oid);
//			if(p == null) {
//				System.out.println("GET?: "+ oid);
//			}
//			return p;
//		}
//		
//		private void put(int oid, Pego pego) {
//			System.out.println("PUT: "+ oid);
//			this.objectMap.put(oid, pego);
//		}
//
//		private Pego get_and_remove(int parent, int oid) {
//			Integer key = oid;
//			Pego p = this.objectMap.get(key);
//			this.objectMap.remove(key);
//			System.out.println("REMOVE: "+ oid + " to parent " + parent);
//			return p;
//		}
//
//		private void createObjectImpl(int bottom, int top) {
//			long offset = 0;
//			for(int i = bottom; i < top; i += 4) {
//				int op = this.readLogOperatorAt(i);
//				int oid = this.readObjectIdAt(i);
//				if(op == OpNew) {
//					Pego pego = Pego.newPego(source, offset + readData(i), readData2(i), readShortDataAt(i));
//					this.put(oid, pego);
//				}
//				else if(op >= OpSet) {
//					int index = readData(i);
//					int cid = readData2(i);
//					Pego parent = this.get(oid);
//					Pego child = this.get_and_remove(oid, cid);
//					parent.set(index, child);
//					child.checkNullEntry();
//	//				System.out.println("[" + n + "] SET:" + oid + " " + readData(i) + " " + readData2(i) + " diff= " + readExt3(i));
//				}
//				else if(op == OpTag) {
//					if(readData(i) > 0) {
//						PegTagging tagging = (PegTagging)peg.getPeg(readData(i));
//						Pego p = this.get(oid);
//						p.setTag(tagging.symbol);
//					}
//					if(readData2(i) > 0) {
//						PegMessage message = (PegMessage)peg.getPeg(readData2(i));
//						Pego p = this.get(oid);
//						p.setTag(message.symbol);
//					}
//					//System.out.println("[" + n + "] TAG:" + oid + " " + tag + " " + msg + " diff= " + readExt3(i));
//				}
//			}
//			this.rollbackObjectStack(bottom);
////			Integer[] key = m.keySet().toArray(new Integer[1]);
////			if(m.size() > 1) {
////				System.out.println("droped size: " + (m.size() - 1));
////				for(Integer k : key) {
////					if(k != getoid) {
////						System.out.println("oid: " + k + " " + m.get(k));
////					}
////				}
////			}
////			return m.get(getoid);
//		}
//				
//		protected final Pego createObject(int oid) {
//			Pego pego = null;
//			if(this.quadLog != null) {
//				dump(0, this.stackTop);
//				createObjectImpl(0, this.stackTop);
//				pego = get(oid);
//			}
//			if(pego == null) {  // debug
//				//dumpStack(0, top);
//				pego = Pego.newSource("#empty", source, 0);
//			}
//			return pego;
//		}
//	}
//	
//	public int matchNewObject(int left, PegNewObject e) {
//		long startIndex = this.getPosition();
//		if(this.stat != null) {
//			this.stat.countRepeatCall(e, startIndex);
//		}
//		if(e.prefetchIndex > 0) {
//			for(int i = 0; i < e.prefetchIndex; i++) {
//				int right = e.get(i).fastMatch(left, this);
//				if(PEGUtils.isFailure(right)) {
//					this.rollback(startIndex);
//					return right;
//				}
//				assert(left == right);
//			}
//		}
//		int mark = this.markObjectStack();
//		int newnode = log.newObjectId(startIndex);
//		//this.showPosition("new " + newnode + " " + e + mark/4);
//		if(e.leftJoin) {
//			this.log.lazySetter(newnode, 0, left);
//		}
//		for(int i = e.prefetchIndex; i < e.size(); i++) {
//			//System.out.println("newO B i= " +i + ", pos=" + this.getPosition() + ", "+ newnode + " e=" + e.get(i));
//			int right = e.get(i).fastMatch(newnode, this);
//			if(PEGUtils.isFailure(right)) {
////				this.showPosition("dispose " + newnode + " " + e + mark/4);				
//				this.rollbackObjectStack(mark);
//				this.rollback(startIndex);
//				return right;
//			}
//			if(newnode != right) {
//				//this.showPosition("dropping new oid " + newnode +"=>" + right + " by " + e.get(i) + " IN " + e);
//				//System.out.println("mark=" + markToIgnoreUnsetObject + " search.. " + this.searchOid(right) + " < " + this.stackTop);
//				this.rollbackObjectStack(this.log.searchMarkByObjectId(right));
//			}
//		}
//		this.log.lazyCaptureString(mark, newnode, (int)(this.getPosition() - startIndex));
//		if(this.stat != null) {
//			this.stat.countObjectCreation();
//		}
//		return newnode;
//	}
//	
//	long statExportCount = 0;
//	long statExportSize  = 0;
//	long statExportFailure  = 0;
//
//	public int matchExport(int left, PegExport e) {
//		int markerId = this.markObjectStack();
//		int node = e.inner.fastMatch(left, this);
//		if(!PEGUtils.isFailure(node)) {
//			Pego pego = log.createObject(node);
//			this.statExportCount += 1;
//			this.statExportSize += pego.getLength();
//			this.pushBlockingQueue(pego);
//		}
//		else {
//			this.statExportFailure += 1;
//		}
//		return left;
//	}
//
//	private BlockingQueue<Pego> queue = null; 
//	
//	@Override
//	protected void pushBlockingQueue(Pego pego) {
//		if(this.queue != null) {
//			try {
//				this.queue.put(pego);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	public int matchSetter(int left, PegSetter e) {
//		int mark = this.markObjectStack();
//		int right = e.inner.fastMatch(left, this);
//		if(PEGUtils.isFailure(right) || left == right) {
//			this.rollbackObjectStack(mark);
//			return right;
//		}
//		this.log.lazySetter(left, e.index, right);
//		return left;
//	}
//	
//	public int matchIndent(int left, PegIndent e) {
////		String indent = this.source.getIndentText(PEGUtils.getpos(left));
////		if(this.match(indent)) {
////			return left;
////		}
//		return this.foundFailure2(e);
//	}
//
//	public int matchIndex(int left, PegIndex e) {
////		TODO
////		String text = left.textAt(e.index, null);
////		if(text != null) {
////			if(this.match(text)) {
////				return left;
////			}
////		}
//		return this.foundFailure2(e);
//	}
//
//}
//

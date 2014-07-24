package org.peg4d;

import java.util.concurrent.BlockingQueue;

public class MonadicParser extends ParserContext {

	public MonadicParser(Grammar peg, ParserSource source) {
		super(peg, source, 0, source.length());
	}

	@Override
	public void initMemo() {
		this.memoMap = new NoMemo();
	}

	String s0(long oid) {
		if(oid == 1) {
			return "toplevel";
		}
		long pos = PEGUtils.getpos(oid);
		if(PEGUtils.isFailure(oid)) {
			return this.source.formatErrorMessage("syntax error", pos, "");
		}
		else {
			Peg e = this.peg.getPeg(oid);
			return "object " + pos + " peg=" + e;
		}
	}

	String S(long oid) {
		if(oid == 1) {
			return "toplevel";
		}
		long pos = PEGUtils.getpos(oid);
		if(PEGUtils.isFailure(oid)) {
			return this.source.formatErrorMessage("syntax error", pos, "failure:");
		}
		else {
			Peg e = this.peg.getPeg(oid);
			return "pego(" + pos + "," + e.pegid2 + ")";
		}
	}

	@Override
	public Pego parseNode(String startPoint) {
		this.initMemo();
		Peg start = this.getRule(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		long pos = this.getPosition();
		long oid = start.fastMatch(1, this);
		Pego pego = null;
		if(PEGUtils.isFailure(oid)) {
			pego = new Pego("#error", this.source, null, PEGUtils.getpos(oid));
			pego.message = this.source.formatErrorMessage("syntax error", PEGUtils.getpos(oid), "");
			System.out.println(pego.message);
//			if(pos == this.getPosition()) {
			this.setPosition(this.endPosition);  // skip 
//			}
		}
		else {
			pego = this.createObject(this.markObjectStack(), oid);
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
		
	private long failureObjectId = 0;
	public final long foundFailure2(Peg e) {
		if(this.sourcePosition >= PEGUtils.getpos(this.failureObjectId)) {  // adding error location
			this.failureObjectId = PEGUtils.failure(this.sourcePosition, e);
		}
		return this.failureObjectId;
	}
	
	public final long refoundFailure2(Peg e, long pos) {
		this.failureObjectId = PEGUtils.failure(pos, e);
		return this.failureObjectId;
	}

	long[] logTriple = new long[256 * 3];
	int stackTop = 0;
	UList<Pego> objectList = new UList<Pego>(new Pego[16]);
	
	public final static int OpPosition = 0;
	public final static int OpTagging  = 1;
	public final static int OpMessage  = 2;
	public final static int OpAppend   = 3;
	public final static int OpSetter   = 4;
	
	private int getop(long op) {
		return (int)op;
	}

	private int getindex(long op) {
		return getop(op)-OpSetter;
	}

	private int getmark(long op) {
		return (int)(op >> 32);
	}

	private long makeop(int mark, int index) {
		return ((long)mark << 32) | (index + OpSetter);
	}
	
	void log(long op, long parent, long data) {
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

	@Override
	protected final int markObjectStack() {
		return this.stackTop;
	}

	@Override
	protected final void rollbackObjectStack(int markerId) {
		if(this.stackTop > markerId) {
//			System.out.println("**released: ");
//			dumpStack(markerId, this.stackTop);
//			System.out.println("****: " + Main._GetStackInfo(4));
		}
		this.stackTop = markerId;
	}
	
	protected final void pushSetter(long parent, int mark, int index, long child) {
		long op = makeop(mark, index);
		//System.out.println("mark, index = " + mark + ", " + index + " <> " + getindex(op) + ", " + getmark(op));
		assert(index == getindex(op));
		assert(mark  == getmark(op));
		log(op, parent, child);
	}
	
	void dumpStack(int s, int top) {
		for(int i = s; i < top; i += 3) {
			long oid = this.logTriple[i];
			long op = this.logTriple[i+1];
			int  iop = getop(op);
			int n = i / 3;
			if(iop == OpAppend) {
				System.out.println("[" + n + "] ADD:" + S(oid) + " " + S(this.logTriple[i+2]) + " mark= " + getmark(op));
			}
			else if(iop >= OpSetter) {
				System.out.println("[" + n + "] SET:" + S(oid) + " " + getindex(op) + " " + S(this.logTriple[i+2]) + " mark= " + getmark(op));
			}
			else if(iop == OpPosition) {
				System.out.println("[" + n + "] POS:" + S(oid) + " pos=" + logTriple[i+2]);
			}
			else if(iop == OpTagging) {
				PegTagging tagging = (PegTagging)this.peg.getPeg(this.logTriple[i+2]);
				System.out.println("[" + n + "] TAG:" + S(oid) + " " + tagging.symbol);
			}
			else if(iop == OpMessage) {
				PegMessage message = (PegMessage)this.peg.getPeg(this.logTriple[i+2]);
				System.out.println("[" + n + "] ALT:" + S(oid) + " `" + message.symbol + "`");
			}
		}
	}
	
	protected final Pego createObjectImpl(int bottom, int top, long oid) {
		int size = 0;
		//System.out.println("creating: "+ S(oid) + " range[" + 0 + ", " + top + "]");
		Peg created = this.peg.getPeg(oid);
		Pego o = new Pego(null, this.source, created, PEGUtils.getpos(oid));
		for(int i = bottom; i < top; i += 3) {
//			System.out.println("oid: [" + i + "] op:" + this.logTriple[i+1] + "  " + S(this.logTriple[i]) + " ?= " + S(oid));
			if(this.logTriple[i] == oid) {
				long op = this.logTriple[i+1];
				int  iop = getop(op);
				if(iop == OpAppend) {
					int mark = getmark(op);
					this.logTriple[i+1] = makeop(mark, size);
					size++;
				}
				else if(iop >= OpSetter) {
					if(size < getindex(op) + 1) {
						size = getindex(op) + 1;
					}
				}
				else if(iop == OpPosition) {
					o.setEndPosition(this.logTriple[i+2]);
				}
				else if(iop == OpTagging) {
					PegTagging tagging = (PegTagging)this.peg.getPeg(this.logTriple[i+2]);
					o.tag = tagging.symbol;
				}
				else if(iop == OpMessage) {
					PegMessage message = (PegMessage)this.peg.getPeg(this.logTriple[i+2]);
					o.message = message.symbol;
				}
			}
		}
		if(size > 0) {
			o.expandAstToSize(size);		
			for(int i = bottom; i < top; i += 3) {
				if(this.logTriple[i] == oid) {
					long op = (int)this.logTriple[i+1];
					if(getindex(op) >= 0) {
						int mark = getmark(op);
						Pego node = createObjectImpl(mark, i, this.logTriple[i+2]);
						o.set(getindex(op), node);
					}
				}
			}
			o.checkNullEntry();
		}
		return o;
	}
	protected final Pego createObject(int top, long oid) {
		//dumpStack(0, top);
		return createObjectImpl(0, top, oid);
	}
	
	public long matchNewObject(long left, PegNewObject e) {
		long leftNode = left;
		long startIndex = this.getPosition();
		if(Main.VerboseStatCall) {
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
		int markerId = this.markObjectStack();
		long newnode = PEGUtils.objectId(startIndex, e);
		//System.out.println("new " + S(newnode));
		if(e.leftJoin) {
			this.pushSetter(newnode, 0, -1, leftNode);
		}
		for(int i = e.predictionIndex; i < e.size(); i++) {
			//System.out.println("newO B i= " +i + ", pos=" + this.getPosition() + ", "+ S(newnode) + " e=" + e.get(i));
			long node = e.get(i).fastMatch(newnode, this);
			//System.out.println("newO A i= " +i + ", pos=" + this.getPosition() + ", " + S(newnode) +"=>" +S(node));
			if(PEGUtils.isFailure(node)) {
				this.rollbackObjectStack(markerId);
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
		int markerId = this.markObjectStack();
		long node = e.inner.fastMatch(left, this);
		if(!PEGUtils.isFailure(node)) {
			Pego pego = createObject(this.markObjectStack(), node);
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

	public long matchSetter(long left, PegSetter e) {
		int mark = this.markObjectStack();
		long right = e.inner.fastMatch(left, this);
		if(PEGUtils.isFailure(right) || left == right) {
			return right;
		}
		this.pushSetter(left, mark, e.index, right);
		return left;
	}
	
	public long matchIndent(long left, PegIndent e) {
		String indent = this.source.getIndentText(PEGUtils.getpos(left));
		if(this.match(indent)) {
			return left;
		}
		return this.foundFailure2(e);
	}

	public long matchIndex(long left, PegIndex e) {
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


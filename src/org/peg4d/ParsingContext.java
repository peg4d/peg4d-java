package org.peg4d;

import java.util.HashMap;


public class ParsingContext {
	ParsingObject left;
	ParsingSource source;

	ParsingTag    emptyTag;	
	ParsingStat          stat   = null;

	public ParsingContext(ParsingSource s, long pos, int stacksize, ParsingMemo memo) {
		this.left = null;
		this.source = s;
		//this.lstack = new long[stacksize*8];
		//this.ostack = new ParsingObject[stacksize];
		this.resetSource(s, pos);
		this.memoMap = memo != null ? memo : new NoParsingMemo();
	}

	public ParsingContext(ParsingSource s) {
		this(s, 0, 4096, null);
	}

	public void resetSource(ParsingSource source, long pos) {
		this.source = source;
		this.pos = pos;
		this.fpos = -1;
//		this.lstack[0] = -1;
//		this.lstacktop = 1;
//		this.ostacktop = 0;
	}
	
	
	public final boolean hasByteChar() {
		return this.source.byteAt(this.pos) != ParsingSource.EOF;
	}

	public final int getByteChar() {
		return this.source.byteAt(pos);
	}
		
	public final ParsingObject parseChunk(Grammar peg, String startPoint) {
		this.initMemo(null, peg.getRuleSize());
		ParsingExpression start = peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		long spos = this.getPosition();
		long fpos = -1;
		this.emptyTag = peg.newStartTag();
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		while(hasByteChar()) {
			long ppos = this.getPosition();
			po.setSourcePosition(this.pos);
			this.left = po;
			start.debugMatch(this);
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
			checkUnusedText(po);
			return this.left;
		}
		return null;
	}
	
	private final void checkUnusedText(ParsingObject po) {
		if(this.left == po) {
			po.setEndPosition(this.pos);
		}
	}

	public final ParsingObject parseChunk(Grammar peg) {
		return this.parseChunk(peg, "Chunk");
	}

	public final ParsingObject parse(Grammar peg, String startPoint) {
		return this.parse(peg, startPoint, null);
	}

	public final ParsingObject parse(Grammar peg, String startPoint, ParsingMemoConfigure conf) {
		this.initMemo(conf, peg.getRuleSize());
		ParsingRule start = peg.getRule(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		if(conf != null) {
			conf.exploitMemo(start);
		}
		this.emptyTag = peg.newStartTag();
		
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		this.left = po;
		if(start.expr.debugMatch(this)) {
			this.commitLinkLog(0, this.left);
		}
		else {
			this.abortLinkLog(0);
			this.unusedLog = null;
		}
		checkUnusedText(po);
		if(conf != null && this.stat != null) {
			conf.show2(this.stat);
		}
		return this.left;
	}

	public final boolean match(Grammar peg, String startPoint, ParsingMemoConfigure conf) {
		this.initMemo(conf, peg.getRuleSize());
		ParsingExpression start = peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		ParsingRule r = peg.getLexicalRule(startPoint);
		start = r.expr;
		this.emptyTag = peg.newStartTag();
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		this.left = po;
		return start.debugMatch(this);
	}
	
	public final void initStat(ParsingStat stat) {
		this.stat = stat;
		if(stat != null) {
			if(Main.StatLevel == 2) {
				this.stat.initRepeatCounter();
			}
			this.stat.start();
		}
	}

	public final void recordStat(ParsingObject pego) {
		if(stat != null) {
			stat.end(pego, this);
		}
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}	
	
	long[]   lstack;
	int      lstacktop;
	
	public final void lpush(long v) {
		this.lstack[lstacktop] = v;
		lstacktop++;
	}

	public final void lpop() {
		lstacktop--;
	}

	public final int lpop2i() {
		lstacktop--;
		return (int)this.lstack[this.lstacktop];
	}

//	ParsingObject[] ostack;
//	int      ostacktop;
//
	public final void opush(ParsingObject left) {
//		this.ostack[ostacktop] = left;
//		ostacktop++;
	}

	public final ParsingObject opop() {
//		ostacktop--;
//		return this.ostack[ostacktop];
		return null;
	}

	long pos;
	
	final long getPosition() {
		return this.pos;
	}
	
	final void setPosition(long pos) {
		this.pos = pos;
	}
	
	final void consume(int length) {
		this.pos += length;
	}

	final void rollback(long pos) {
		if(stat != null && this.pos > pos) {
			stat.statBacktrack1(pos, this.pos);
		}
		this.pos = pos;
	}

	long fpos = 0;
	ParsingMatcher[] errorbuf = new ParsingMatcher[512];
	long[] posbuf = new long[errorbuf.length];

	public final boolean isFailure() {
		return this.left == null;
	}
	
	final long rememberFailure() {
		return this.fpos;
	}
	
	final void forgetFailure(long fpos) {
		if(this.fpos != fpos) {
			this.removeErrorInfo(this.fpos);
		}
		this.fpos = fpos;
	}
	
	private ParsingMatcher getErrorInfo(long fpos) {
		int index = (int)this.pos % errorbuf.length;
		if(posbuf[index] == fpos) {
			return errorbuf[index];
		}
		return null;
	}

	private void setErrorInfo(ParsingMatcher errorInfo) {
		int index = (int)this.pos % errorbuf.length;
		errorbuf[index] = errorInfo;
		posbuf[index] = this.pos;
		//System.out.println("push " + this.pos + " @" + errorInfo);
	}

	private void removeErrorInfo(long fpos) {
		int index = (int)this.pos % errorbuf.length;
		if(posbuf[index] == fpos) {
			//System.out.println("pop " + fpos + " @" + errorbuf[index]);
			errorbuf[index] = null;
		}
	}

	String getErrorMessage() {
		ParsingMatcher errorInfo = this.getErrorInfo(this.fpos);
		if(errorInfo == null) {
			return "syntax error";
		}
		return "syntax error: expecting " + errorInfo.expectedToken() + " <- Never believe this";
	}
	
	public final void failure(ParsingMatcher errorInfo) {
		if(this.pos > fpos) {  // adding error location
			this.fpos = this.pos;
			this.setErrorInfo(errorInfo);
		}
		this.left = null;
	}
	
//	boolean isMatchingOnly = false;
//	ParsingObject successResult = new ParsingObject(this.emptyTag, this.source, 0);
//	
//	final boolean isRecognitionMode() {
//		return this.isMatchingOnly;
//	}
//
//	final boolean canTransCapture() {
//		return !this.isMatchingOnly;
//	}
//	
//	final boolean setRecognitionMode(boolean recognitionMode) {
//		boolean b = this.isMatchingOnly;
//		this.isMatchingOnly = recognitionMode;
//		return b;
//	}
	
	final ParsingObject newParsingObject(long pos, ParsingConstructor created) {
//		if(this.isRecognitionMode()) {
//			this.successResult.setSourcePosition(pos);
//			return this.successResult;
//		}
//		else {
			//System.out.println("created pos="+pos + " mark=" + this.markObjectStack());
		return new ParsingObject(this.emptyTag, this.source, pos, created);
//		}
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
	
	int markObjectStack() {
		return stackSize;
	}

	void abortLinkLog(int mark) {
		while(mark < this.stackSize) {
			LinkLog l = this.logStack;
			this.logStack = this.logStack.next;
			this.stackSize--;
			disposeLog(l);
		}
		assert(mark == this.stackSize);
	}
	
	final void logLink(ParsingObject parent, int index, ParsingObject child) {
		LinkLog l = this.newLog();
		l.childNode  = child;
		child.parent = parent;
		l.index = index;
		l.next = this.logStack;
		this.logStack = l;
		this.stackSize += 1;
		parent = null; // for GC
		child = null;  // for GC
	}
	
	void lazyCommit(ParsingObject left) {
		LinkLog l = this.newLog();
		l.childNode  = left;
		l.index = -9;
		l.next = this.logStack;
		this.logStack = l;
		this.stackSize += 1;
		left = null;
	}

//	final void capture(ParsingObject newnode, long startIndex) {
//		newnode.setLength((int)(this.getPosition() - startIndex));
//		newnode = null;
//	}
//
//	final void commitLinkLog2(ParsingObject newnode, long startIndex, int mark) {
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
//			newnode.expandAstToSize(objectSize);
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
//		newnode = null;
//	}

	final void commitLinkLog(int mark, ParsingObject newnode) {
		LinkLog first = null;
		int objectSize = 0;
		while(mark < this.stackSize) {
			LinkLog cur = this.logStack;
			this.logStack = this.logStack.next;
			this.stackSize--;
			if(cur.index == -9) { // lazyCommit
				commitLinkLog(mark, cur.childNode);
				disposeLog(cur);
				break;
			}
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
			newnode.expandAstToSize(objectSize);
			for(int i = 0; i < objectSize; i++) {
				LinkLog cur = first;
				first = first.next;
				if(cur.index == -1) {
					cur.index = i;
				}
				newnode.set(cur.index, cur.childNode);
				this.disposeLog(cur);
			}
			checkNullEntry(newnode);
		}
		newnode = null;
	}

	private final void checkNullEntry(ParsingObject o) {
		for(int i = 0; i < o.size(); i++) {
			if(o.get(i) == null) {
				o.set(i, new ParsingObject(emptyTag, this.source, 0));
			}
		}
		o = null;
	}

	
	
	//	public final void opStringfy() {
//		if(this.canTransCapture()) {
//			StringBuilder sb = new StringBuilder();
//			joinText(this.left, sb);
//			this.left.setValue(sb.toString());
//		}
//	}
//	
//	private void joinText(ParsingObject po, StringBuilder sb) {
//		if(po.size() == 0) {
//			sb.append(po.getText());
//		}
//		else {
//			for(int i = 0; i < po.size(); i++) {
//				joinText(po.get(i), sb);
//			}
//		}
//	}
	
	// <indent Expr>  <indent>
	
	private class StringStack {
		int tagId;
		String token;
		byte[] utf8;
		StringStack(int tagId, String indent) {
			this.tagId = tagId;
			this.token = indent;
			this.utf8 = indent.getBytes();
		}
	}
	
	private UList<StringStack> tokenStack = new UList<StringStack>(new StringStack[4]);
	
	public final int pushTokenStack(int tagId, String s) {
		int stackTop = this.tokenStack.size();
		this.tokenStack.add(new StringStack(tagId, s));
		return stackTop;
	}

	public final void popTokenStack(int stackTop) {
		this.tokenStack.clear(stackTop);
	}

	public final boolean matchTokenStackTop(int tagId) {
		for(int i = tokenStack.size() - 1; i >= 0; i--) {
			StringStack s = tokenStack.ArrayValues[i];
			if(s.tagId == tagId) {
				if(this.source.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
				break;
			}
		}
		this.failure(null);
		return false;
	}

	public final boolean matchTokenStack(int tagId) {
		for(int i = tokenStack.size() - 1; i >= 0; i--) {
			StringStack s = tokenStack.ArrayValues[i];
			if(s.tagId == tagId) {
				if(this.source.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
			}
		}
		this.failure(null);
		return false;
	}
	
	UList<String> terminalStack = new UList<String>(new String[8]);
	
	public int pushCallStack(String uniqueName) {
		int pos = this.terminalStack.size();
		this.terminalStack.add(uniqueName);
		return pos;
	}

	public void popCallStack(int stacktop) {
		this.terminalStack.clear(stacktop);
	}

	public void dumpCallStack(String header) {
		System.out.print(header);
		for(String t : this.terminalStack) {
			System.out.print(" ");
			System.out.print(t);
		}
		System.out.println();
	}
	
	protected ParsingMemo memoMap = null;

	public void initMemo(ParsingMemoConfigure conf, int rules) {
		this.memoMap = (conf == null) ? new NoParsingMemo() : conf.newMemo(rules);
	}

	final MemoEntry getMemo(long keypos, ParsingExpression e) {
		return this.memoMap.getMemo(keypos, e);
	}

	final void setMemo(long keypos, ParsingExpression e, ParsingObject result, int length) {
		this.memoMap.setMemo(keypos, e, result, length);
	}

	private HashMap<String,Boolean> flagMap = new HashMap<String,Boolean>();
	
	final void setFlag(String flagName, boolean flag) {
		this.flagMap.put(flagName, flag);
	}
	
	final boolean getFlag(String flagName) {
		return this.isFlag(flagMap.get(flagName));
	}
	
	final boolean isFlag(Boolean f) {
		return f == null || f.booleanValue();
	}
		
}


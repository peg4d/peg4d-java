package org.peg4d;

import java.util.HashMap;


public class ParsingContext {
	ParsingObject left;
	ParsingSource source;

	ParsingTag    emptyTag;	
	ParsingStatistics          stat   = null;

	public ParsingContext(ParsingSource s, long pos, int stacksize, MemoTable memo) {
		this.left = null;
		this.source = s;
		this.resetSource(s, pos);
		this.memoMap = memo != null ? memo : new NoParsingMemo();
	}

	public ParsingContext(ParsingSource s) {
		this(s, 0, 4096, null);
	}

	public void resetSource(ParsingSource source, long pos) {
		this.source = source;
		this.pos = pos;
		this.fpos = 0;
		if(ParsingExpression.VerboseStack) {
			this.initCallStack();
		}
	}
	
	public final boolean hasByteChar() {
		return this.source.byteAt(this.pos) != ParsingSource.EOF;
	}

	public final int getByteChar() {
		return this.source.byteAt(pos);
	}
		
	public final ParsingObject parseChunk(Grammar peg, String startPoint) {
		this.initMemo(null);
		ParsingExpression start = peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		long spos = this.getPosition();
		long fpos = 0;
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
		ParsingRule start = peg.getRule(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		if(conf != null) {
			conf.exploitMemo(start);
			this.initMemo(conf);
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
		ParsingExpression start = peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		ParsingRule r = peg.getLexicalRule(startPoint);
		if(conf != null) {
			conf.exploitMemo(r);
			this.initMemo(conf);
		}
		start = r.expr;
		this.emptyTag = peg.newStartTag();
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		this.left = po;
		boolean res = start.debugMatch(this);
		if(conf != null && this.stat != null) {
			conf.show2(this.stat);
		}
		return res;
	}
	
	public final void initStat(ParsingStatistics stat) {
		this.stat = stat;
		if(stat != null) {
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
	
	long pos;
	long head_pos;
	long fpos;

	boolean enableTrace = false;
	String headTrace    = null;
	String failureTrace = null;
	String failureInfo  = null;
	
	final long getPosition() {
		return this.pos;
	}
	
	final void setPosition(long pos) {
		this.pos = pos;
	}
	
	final void consume(int length) {
		this.pos += length;
		if(head_pos < pos) {
			this.head_pos = pos;
			if(this.enableTrace) {
				headTrace = this.stringfyCallStack();
			}
		}
	}

	final void rollback(long pos) {
		if(stat != null && this.pos > pos) {
			stat.statBacktrack(pos, this.pos);
		}
		this.pos = pos;
	}
	
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
			if(this.enableTrace) {
				this.failureTrace = this.stringfyCallStack();
				this.failureInfo = errorInfo.toString();
			}
		}
		this.left = null;
	}
		
	final ParsingObject newParsingObject(long pos, ParsingConstructor created) {
		return new ParsingObject(this.emptyTag, this.source, pos, created);
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
	
	UList<String> terminalStack;
	int[]         callPositions;

	void initCallStack() {
		if(ParsingExpression.VerboseStack) {
			this.terminalStack = new UList<String>(new String[256]);
			this.callPositions = new int[4096];
		}
	}
	
	int pushCallStack(String uniqueName) {
		int pos = this.terminalStack.size();
		this.terminalStack.add(uniqueName);
		callPositions[pos] = (int)this.pos;
		return pos;
	}

	void popCallStack(int stacktop) {
		this.terminalStack.clear(stacktop);
	}

	String stringfyCallStack() {
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for(String t : this.terminalStack) {
			if(n > 0) {
				sb.append(" ");
			}
			sb.append(t);
			sb.append("#");
			sb.append(this.callPositions[n]); n++;
		}
		return sb.toString();
	}
 		
	protected MemoTable memoMap = null;

	public void initMemo(ParsingMemoConfigure conf) {
		this.memoMap = (conf == null) ? new NoParsingMemo() : conf.newMemo();
	}

	final MemoEntry getMemo(long keypos, int memoPoint) {
		return this.memoMap.getMemo(keypos, memoPoint);
	}

	final void setMemo(long keypos, int memoPoint, ParsingObject result, int length) {
		this.memoMap.setMemo(keypos, memoPoint, result, length);
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


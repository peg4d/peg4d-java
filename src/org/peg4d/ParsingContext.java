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
		this.initMemo(null);
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
		this.initMemo(conf);
		ParsingExpression start = peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		this.emptyTag = peg.newStartTag();
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		this.left = po;
		start.debugMatch(this);
		checkUnusedText(po);
		return this.left;
	}

	public final boolean match(Grammar peg, String startPoint, ParsingMemoConfigure conf) {
		this.initMemo(conf);
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
	
	public final boolean isFailure() {
		return this.left == null;
	}
	
	final long rememberFailure() {
		return this.fpos;
	}
	
	final void forgetFailure(long f) {
		//System.out.println("forget fpos: " + this.fpos + " <- " + f);
		this.fpos = f;
	}
		
	boolean isMatchingOnly = false;
	ParsingObject successResult = new ParsingObject(this.emptyTag, this.source, 0);
	
	final boolean isRecognitionMode() {
		return this.isMatchingOnly;
	}

	final boolean canTransCapture() {
		return !this.isMatchingOnly;
	}
	
	final boolean setRecognitionMode(boolean recognitionMode) {
		boolean b = this.isMatchingOnly;
		this.isMatchingOnly = recognitionMode;
		return b;
	}
	
	final ParsingObject newParsingObject(long pos, ParsingConstructor created) {
		if(this.isRecognitionMode()) {
			this.successResult.setSourcePosition(pos);
			return this.successResult;
		}
		else {
			//System.out.println("created pos="+pos + " mark=" + this.markObjectStack());
			return new ParsingObject(this.emptyTag, this.source, pos, created);
		}
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
		assert(!this.isRecognitionMode());
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

	final void commitLinkLog(ParsingObject newnode, long startIndex, int mark) {
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
		newnode.setLength((int)(this.getPosition() - startIndex));
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

	public final void opRememberPosition() {
		lpush(this.pos);
	}

	public final void opCommitPosition() {
		lpop();
	}

	public final void opBacktrackPosition() {
		lpop();
		rollback(this.lstack[this.lstacktop]);
	}
	
	public final void opRememberSequencePosition() {
		lpush(this.pos);
		lpush(this.markObjectStack());
		opush(this.left);
	}

	public final void opCommitSequencePosition() {
		opop();
		lpop();
		lpop();
	}

	public final void opBackTrackSequencePosition() {
		this.left = opop();
		lpop();
		this.abortLinkLog((int)this.lstack[this.lstacktop]);
		lpop();
		this.rollback(this.lstack[this.lstacktop]);
	}

	public final void opFailure() {
		if(this.pos > fpos) {  // adding error location
			//System.out.println("fpos: " + this.fpos + " -> " + this.pos);
			this.fpos = this.pos;
		}
		this.left = null;
	}

	HashMap<Long, Object> errorMap = new HashMap<Long, Object>();
	private void setErrorInfo(Object errorInfo) {
		Long key = this.pos;
//		if(!this.errorMap.containsKey(key)) {
//			this.errorMap.put(key, errorInfo);
//		}
	}

	private void removeErrorInfo() {
		Long key = this.pos;
		this.errorMap.remove(key);
	}

	String getErrorMessage() {
		Object errorInfo = this.errorMap.get(this.fpos);
		if(errorInfo == null) {
			return "syntax error";
		}
		if(errorInfo instanceof ParsingExpression) {
			return "syntax error: unrecognized " + errorInfo;
		}
		return errorInfo.toString();
	}
	
	public final void opFailure(ParsingExpression errorInfo) {
		if(this.pos > fpos) {  // adding error location
			this.fpos = this.pos;
			//System.out.println("fpos: " + this.fpos + " -> " + this.pos + " " + errorInfo);		
			this.setErrorInfo(errorInfo);
		}
		this.left = null;
	}

	public final void opFailure(String errorInfo) {
		if(this.pos > fpos) {  // adding error location
			this.fpos = this.pos;
			//System.out.println("fpos: " + this.fpos + " -> " + this.pos + " " + errorInfo);
			this.setErrorInfo(errorInfo);
		}
		this.left = null;
	}

	public final void opRememberFailurePosition() {
		lpush(this.fpos);
	}

	public final void opUpdateFailurePosition() {
		lpop();
	}

	public final void opForgetFailurePosition() {
		lpop();
		//System.out.println("forget fpos: " + this.fpos + " <- " + this.lstack[this.lstacktop]);
		this.fpos = this.lstack[this.lstacktop];
	}
	
	public final void opCatch() {
		if(this.canTransCapture()) {
			this.left.setSourcePosition(this.fpos);
			this.left.setValue(this.getErrorMessage());
		}
	}

	
	
	public final void opMatchText(byte[] t) {
		if(this.source.match(this.pos, t)) {
			this.consume(t.length);
		}
		else {
			this.opFailure();
		}
	}

	public final void opMatchByteChar(int c) {
		if(this.source.byteAt(this.pos) == c) {
			this.consume(1);
		}
		else {
			this.opFailure();
		}
	}

	public final void opMatchCharset(ParsingCharset u) {
		int consume = u.consume(this.source, pos);
		if(consume > 0) {
			this.consume(consume);
		}
		else {
			this.opFailure();
		}
	}

	public final void opMatchAnyChar() {
		if(this.source.charAt(this.pos) != -1) {
			int len = this.source.charLength(this.pos);
			this.consume(len);
		}
		else {
			this.opFailure();
		}
	}

	public final void opMatchTextNot(byte[] t) {
		if(this.source.match(this.pos, t)) {
			this.opFailure();
		}
	}

	public final void opMatchByteCharNot(int c) {
		if(this.source.byteAt(this.pos) == c) {
			this.opFailure();
		}
	}

	public final void opMatchCharsetNot(ParsingCharset u) {
		int consume = u.consume(this.source, pos);
		if(consume > 0) {
			this.opFailure();
		}
	}

	public final void opMatchOptionalText(byte[] t) {
		if(this.source.match(this.pos, t)) {
			this.consume(t.length);
		}
	}

	public final void opMatchOptionalByteChar(int c) {
		if(this.source.byteAt(this.pos) == c) {
			this.consume(1);
		}
	}

	public final void opMatchOptionalCharset(ParsingCharset u) {
		int consume = u.consume(this.source, pos);
		this.consume(consume);
	}


	public final void opStoreObject() {
		this.opush(this.left);
	}

	public final void opDropStoredObject() {
		this.opop();
	}

	public final void opRestoreObject() {
		this.left = this.opop();
	}

	public final void opRestoreObjectIfFailure() {
		if(this.isFailure()) {
			this.left = opop();
		}
		else {
			this.opop();
		}
	}

	public final void opRestoreNegativeObject() {
		if(this.isFailure()) {
			this.left = this.opop();
		}
		else {
			this.opop();
			this.opFailure();
		}
	}

	public void opConnectObject(int index) {
		ParsingObject parent = this.opop();
		if(!this.isFailure()) {
			if(this.canTransCapture() && parent != this.left) {
				this.logLink(parent, index, this.left);
			}
			this.left = parent;
		}
	}

	public final void opDisableTransCapture() {
		this.opStoreObject();
		lpush(this.isRecognitionMode() ? 1 : 0);
		this.setRecognitionMode(true);
	}

	public final void opEnableTransCapture() {
		lpop();
		this.setRecognitionMode((this.lstack[lstacktop] == 1));
		this.opRestoreObjectIfFailure();
	}

	private HashMap<String,Boolean> flagMap = new HashMap<String,Boolean>();
	
	public final void setFlag(String flagName, boolean flag) {
		this.flagMap.put(flagName, flag);
	}
	
	private final boolean isFlag(Boolean f) {
		return f == null || f.booleanValue();
	}
	
	public final void opEnableFlag(String flag) {
		Boolean f = this.flagMap.get(flag);
		lpush(isFlag(f) ? 1 : 0);
		this.flagMap.put(flag, true);
	}

	public final void opDisableFlag(String flag) {
		Boolean f = this.flagMap.get(flag);
		lpush(isFlag(f) ? 1 : 0);
		this.flagMap.put(flag, false);
	}

	public final void opPopFlag(String flag) {
		lpop();
		this.flagMap.put(flag, (this.lstack[lstacktop] == 1) ? true : false);
	}

	public final void opCheckFlag(String flag) {
		Boolean f = this.flagMap.get(flag);
		if(!isFlag(f)) {
			this.opFailure();
		}
	}
	
	public void opNewObject(ParsingConstructor e) {
		if(this.canTransCapture()) {
			lpush(this.markObjectStack());
			this.left = new ParsingObject(this.emptyTag, this.source, this.pos, e);
		}
		opush(this.left);
	}

	public void opLeftJoinObject(ParsingConstructor e) {
		if(this.canTransCapture()) {
			lpush(this.markObjectStack());
			ParsingObject left = new ParsingObject(this.emptyTag, this.source, this.pos, e);
			this.logLink(left, 0, this.left);
			this.left = left;
		}
		opush(this.left);
	}

	public void opCommitObject() {
		ParsingObject left = this.opop();
		if(!this.isFailure()) {
			this.left = left;
			if(this.canTransCapture()) {
				this.lpop();
				int mark = (int)this.lstack[this.lstacktop];
				this.commitLinkLog(this.left, this.left.getSourcePosition(), mark);
			}
		}
	}

	public final void opRefreshStoredObject() {
//		ostack[ostacktop-1] = this.left;
	}

	public final void opTagging(ParsingTag tag) {
		if(this.canTransCapture()) {
			this.left.setTag(tag);
		}
	}

	public final void opValue(Object value) {
		if(this.canTransCapture()) {
			this.left.setValue(value);
		}
	}
	
	public final void opStringfy() {
		if(this.canTransCapture()) {
			StringBuilder sb = new StringBuilder();
			joinText(this.left, sb);
			this.left.setValue(sb.toString());
		}
	}
	
	private void joinText(ParsingObject po, StringBuilder sb) {
		if(po.size() == 0) {
			sb.append(po.getText());
		}
		else {
			for(int i = 0; i < po.size(); i++) {
				joinText(po.get(i), sb);
			}
		}
	}
	
	// <indent Expr>  <indent>
	
	private class IndentStack {
		String indent;
		byte[] utf8;
		IndentStack prev;
		IndentStack(String indent, IndentStack prev) {
			this.indent = indent;
			this.utf8 = indent.getBytes();
			this.prev = prev;
		}
		IndentStack pop() {
			return this.prev;
		}
	}
	
	private IndentStack indentStack = null;
	
	public final void opPushIndent() {
		String s = this.source.getIndentText(this.pos);
		//System.out.println("Push indent: '"+s+"'");
		this.indentStack = new IndentStack(s, this.indentStack);
	}

	public final void opPopIndent() {
		this.indentStack = this.indentStack.pop();
	}
	
	public final void opIndent() {
		if(indentStack != null) {
			this.opMatchText(indentStack.utf8);
			//System.out.println("indent isFailure? " + this.isFailure() + " indent=" + indentStack.utf8.length + " '" + "'" + " left=" + this.left);
		}
	}
	
	public final ParsingObject getResult() {
		return this.left;
	}

	public void opDebug(ParsingExpression inner) {
		this.opDropStoredObject();
//		ParsingObject left = this.ostack[ostacktop];
		this.opUpdateFailurePosition();
		long fpos = this.lstack[lstacktop];
		this.opCommitPosition();
		long pos = this.lstack[lstacktop];
		if(this.isFailure()) {
			System.out.println(source.formatPositionLine("debug", this.pos, "failure in " + inner));
			return;
		}
		if(this.left != left) {
			System.out.println(source.formatPositionLine("debug", pos,
				"transition #" + this.left.getTag() + " => #" + left.getTag() + " in " + inner));
			return;
		}
		if(this.pos != pos) {
			System.out.println(source.formatPositionMessage("debug", pos,
				"consumed pos=" + pos + " => " + this.pos + " in " + inner));
			return;
		}
		System.out.println(source.formatPositionLine("debug", pos, "pass in " + inner));
	}
	
	protected ParsingMemo memoMap = null;

	public void initMemo(ParsingMemoConfigure conf) {
		this.memoMap = (conf == null) ? new NoParsingMemo() : conf.newMemo();
	}

	final MemoEntry getMemo(long keypos, ParsingExpression e) {
		return this.memoMap.getMemo(keypos, e);
	}

	final void setMemo(long keypos, ParsingExpression e, ParsingObject result, int length) {
		this.memoMap.setMemo(keypos, e, result, length);
	}
	
}


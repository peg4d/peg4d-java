package org.peg4d;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.peg4d.ParsingContextMemo.ObjectMemo;

public class ParsingContext {
	ParsingObject left;
	ParsingSource source;

	ParsingTag    emptyTag;	
	ParsingStat          stat   = null;

	public ParsingContext(ParsingSource s, long pos, int stacksize, ParsingContextMemo memo) {
		this.left = null;
		this.source = s;
		this.lstack = new long[stacksize*8];
		this.ostack = new ParsingObject[stacksize];
		this.resetSource(s, pos);
		this.memoMap = memo != null ? memo : new NoMemo();
	}

	public ParsingContext(ParsingSource s) {
		this(s, 0, 4096, null);
	}

	public void resetSource(ParsingSource source, long pos) {
		this.source = source;
		this.pos = pos;
		this.fpos = -1;
		this.lstack[0] = -1;
		this.lstacktop = 1;
		this.ostacktop = 0;
	}
	
	
	public final boolean hasByteChar() {
		return this.source.byteAt(this.pos) != ParsingSource.EOF;
	}

	public final int getByteChar() {
		return this.source.byteAt(pos);
	}
		
	public final ParsingObject parseChunk(Grammar peg, String startPoint) {
		this.initMemo();
		PExpression start = peg.getExpression(startPoint);
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
			start.simpleMatch(this);
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
		this.initMemo();
		PExpression start = peg.getExpression(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		this.emptyTag = peg.newStartTag();
		ParsingObject po = new ParsingObject(this.emptyTag, this.source, 0);
		this.left = po;
		start.simpleMatch(this);
		checkUnusedText(po);
		return this.left;
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

	ParsingObject[] ostack;
	int      ostacktop;

	public final void opush(ParsingObject left) {
		this.ostack[ostacktop] = left;
		ostacktop++;
	}

	public final ParsingObject opop() {
		ostacktop--;
		return this.ostack[ostacktop];
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
	
	final ParsingObject newParsingObject(long pos, PConstructor created) {
		if(this.isRecognitionMode()) {
			this.successResult.setSourcePosition(pos);
			return this.successResult;
		}
		else {
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
			newnode = this.newAst(newnode, objectSize);
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
	}
	
	private final ParsingObject newAst(ParsingObject pego, int size) {
		pego.expandAstToSize(size);
		return pego;
	}

	private final void checkNullEntry(ParsingObject o) {
		for(int i = 0; i < o.size(); i++) {
			if(o.get(i) == null) {
				o.set(i, new ParsingObject(emptyTag, this.source, 0));
			}
		}
	}

	protected ParsingContextMemo memoMap = null;
	public void initMemo() {
//		this.memoMap = new DebugMemo(new PackratMemo(4096), new OpenFifoMemo(100));
		this.memoMap = new NoMemo();
	}

	final ObjectMemo getMemo(PExpression e, long keypos) {
		return this.memoMap.getMemo(e, keypos);
	}

	final void setMemo(long keypos, PExpression e, ParsingObject po, int length) {
		this.memoMap.setMemo(keypos, e, po, length);
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
		if(!this.errorMap.containsKey(key)) {
			this.errorMap.put(key, errorInfo);
		}
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
		if(errorInfo instanceof PExpression) {
			return "syntax error: unrecognized " + errorInfo;
		}
		return errorInfo.toString();
	}
	
	public final void opFailure(PExpression errorInfo) {
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
	
	public void opNewObject(PConstructor e) {
		if(this.canTransCapture()) {
			lpush(this.markObjectStack());
			this.left = new ParsingObject(this.emptyTag, this.source, this.pos, e);
		}
		opush(this.left);
	}

	public void opLeftJoinObject(PConstructor e) {
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
		ostack[ostacktop-1] = this.left;
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

	public void opDebug(PExpression inner) {
		this.opDropStoredObject();
		ParsingObject left = this.ostack[ostacktop];
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
}

abstract class ParsingContextMemo {
	protected final static int FifoSize = 64;
	long AssuredLength = Integer.MAX_VALUE;

	int MemoHit = 0;
	int MemoMiss = 0;
	int MemoSize = 0;
//	int statMemoSlotCount = 0;

	public final class ObjectMemo {
		ObjectMemo next;
		PExpression  keypeg;
		ParsingObject generated;
		int  consumed;
		long key;
	}

	private ObjectMemo UnusedMemo = null;

	protected final ObjectMemo newMemo() {
		if(UnusedMemo != null) {
			ObjectMemo m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			return m;
		}
		else {
			ObjectMemo m = new ObjectMemo();
//			this.memoSize += 1;
			return m;
		}
	}
	
	protected long getpos(long keypos) {
		return keypos;
	}

	protected final void unusedMemo(ObjectMemo m) {
		this.appendMemo2(m, UnusedMemo);
		UnusedMemo = m;
	}

	protected final ObjectMemo findTail(ObjectMemo m) {
		while(m.next != null) {
			m = m.next;
		}
		return m;
	}			

	private void appendMemo2(ObjectMemo m, ObjectMemo n) {
		while(m.next != null) {
			m = m.next;
		}
		m.next = n;
	}			

	protected abstract void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed);
	protected abstract ObjectMemo getMemo(PExpression keypeg, long keypos);

//	public final static long makekey(long pos, Peg keypeg) {
//		return (pos << 24) | keypeg.uniqueId;
//	}
	
	class FifoMap extends LinkedHashMap<Long, ObjectMemo> {
		private static final long serialVersionUID = 6725894996600788028L;
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, ObjectMemo> eldest)  {
			if(this.size() < FifoSize) {
				unusedMemo(eldest.getValue());
				return true;			
			}
			return false;
		}
	}

	class AdaptiveLengthMap extends LinkedHashMap<Long, ObjectMemo> {
		private static final long serialVersionUID = 6725894996600788028L;
		long lastPosition = 0;
		int worstLength = 0;
		
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, ObjectMemo> eldest)  {
			long diff = this.lastPosition - getpos(eldest.getKey());
			if(diff > worstLength) {
				unusedMemo(eldest.getValue());
				return true;			
			}
			return false;
		}
		@Override
		public ObjectMemo put(Long key, ObjectMemo value) {
			long pos = getpos(key);
			if(this.lastPosition < pos) {
				this.lastPosition = pos;
			}
			return super.put(key, value);
		}
	}
	
	protected void stat(ParsingStat stat) {
		stat.setCount("MemoHit", this.MemoHit);
		stat.setCount("MemoMiss", this.MemoMiss);
		stat.setRatio("Hit/Miss", this.MemoHit, this.MemoMiss);
	}
}

class NoMemo extends ParsingContextMemo {
	@Override
	protected void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed) {
	}

	@Override
	protected ObjectMemo getMemo(PExpression keypeg, long keypos) {
		this.MemoMiss += 1;
		return null;
	}
}

class PackratMemo extends ParsingContextMemo {
	protected Map<Long, ObjectMemo> memoMap;
	protected PackratMemo(Map<Long, ObjectMemo> memoMap) {
		this.memoMap = memoMap;
	}
	public PackratMemo(int initSize) {
		this(new HashMap<Long, ObjectMemo>(initSize));
	}
	@Override
	protected final void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed) {
		ObjectMemo m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
		m.next = this.memoMap.get(keypos);
		this.memoMap.put(keypos, m);
	}
	@Override
	protected final ObjectMemo getMemo(PExpression keypeg, long keypos) {
		ObjectMemo m = this.memoMap.get(keypos);
		while(m != null) {
			if(m.keypeg == keypeg) {
				this.MemoHit += 1;
				return m;
			}
			m = m.next;
		}
		this.MemoMiss += 1;
		return m;
	}
}


class FifoMemo extends ParsingContextMemo {
	protected Map<Long, ObjectMemo> memoMap;
	protected long farpos = 0;
	
	protected FifoMemo(int slot) {
		this.memoMap = new LinkedHashMap<Long, ObjectMemo>(slot) {  //FIFO
			private static final long serialVersionUID = 6725894996600788028L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, ObjectMemo> eldest)  {
				long pos = ParsingUtils.getpos(eldest.getKey());
				//System.out.println("diff="+(farpos - pos));
				if(farpos - pos > 256) {
					unusedMemo(eldest.getValue());
					return true;		
				}
				return false;
			}
		};
	}

	@Override
	protected final void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed) {
		ObjectMemo m = null;
		m = newMemo();
		long key = ParsingUtils.memoKey(keypos, keypeg);
		m.key = key;
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
		this.memoMap.put(key, m);
		if(keypos > this.farpos) {
			this.farpos = keypos;
		}
	}

	@Override
	protected final ObjectMemo getMemo(PExpression keypeg, long keypos) {
		ObjectMemo m = this.memoMap.get(ParsingUtils.memoKey(keypos, keypeg));
		if(m != null) {
			this.MemoHit += 1;
		}
		else {
			this.MemoMiss += 1;
		}
		return m;
	}
}

class OpenFifoMemo extends ParsingContextMemo {
	private ObjectMemo[] memoArray;
	private long statSetCount = 0;
	private long statExpireCount = 0;

	OpenFifoMemo(int slotSize) {
		this.memoArray = new ObjectMemo[slotSize * 111 + 1];
		for(int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new ObjectMemo();
		}
	}
	
	@Override
	protected final void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed) {
		long key = ParsingUtils.memoKey(keypos, keypeg);
		int hash =  (Math.abs((int)key) % memoArray.length);
		ObjectMemo m = this.memoArray[hash];
//		if(m.key != 0) {
//			long diff = keypos - PEGUtils.getpos(m.key);
//			if(diff > 0 && diff < 80) {
//				this.statExpireCount += 1;
//			}
//		}
		m.key = key;
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
	}

	@Override
	protected final ObjectMemo getMemo(PExpression keypeg, long keypos) {
		long key = ParsingUtils.memoKey(keypos, keypeg);
		int hash =  (Math.abs((int)key) % memoArray.length);
		ObjectMemo m = this.memoArray[hash];
		if(m.key == key) {
			//System.out.println("GET " + key + "/"+ hash + " kp: " + keypeg.uniqueId);
			this.MemoHit += 1;
			return m;
		}
		this.MemoMiss += 1;
		return null;
	}

	@Override
	protected final void stat(ParsingStat stat) {
		super.stat(stat);
		stat.setCount("MemoSize", this.memoArray.length);
		stat.setRatio("MemoCollision80", this.statExpireCount, this.statSetCount);
	}
}

class DebugMemo extends ParsingContextMemo {
	ParsingContextMemo m1;
	ParsingContextMemo m2;
	protected DebugMemo(ParsingContextMemo m1, ParsingContextMemo m2) {
		this.m1 = m1;
		this.m2 = m2;
	}
	@Override
	protected final void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed) {
		this.m1.setMemo(keypos, keypeg, generated, consumed);
		this.m2.setMemo(keypos, keypeg, generated, consumed);
	}
	@Override
	protected final ObjectMemo getMemo(PExpression keypeg, long keypos) {
		ObjectMemo o1 = this.m1.getMemo(keypeg, keypos);
		ObjectMemo o2 = this.m2.getMemo(keypeg, keypos);
		if(o1 == null && o2 == null) {
			return null;
		}
		if(o1 != null && o2 == null) {
			System.out.println("diff: 1 null " + "pos=" + keypos + ", e=" + keypeg);
		}
		if(o1 == null && o2 != null) {
			System.out.println("diff: 2 null " + "pos=" + keypos + ", e=" + keypeg);
		}
		if(o1 != null && o2 != null) {
			if(o1.generated != o2.generated) {
				System.out.println("diff: generaetd " + "pos1=" + keypos + ", p1=" + keypeg);
			}
			if(o1.consumed != o2.consumed) {
				System.out.println("diff: consumed " + "pos1=" + keypos + ", p1=" + keypeg);
			}
		}
		return o1;
	}
}

////@Override
//public void initMemo2() {
//if(memo < 0) {
//	int initSize = 512 * 1024;
//	if(source.length() < 512 * 1024) {
//		initSize = (int)source.length();
//	}
//	this.memoMap = new PackratMemo(initSize);
////	this.memoMap = new DebugMemo(new PackratMemo(initSize), new OpenHashMemo(100));
////	this.memoMap = new DebugMemo(new OpenHashMemo(256), new OpenHashMemo(256));
//	Main.printVerbose("memo", "packrat-style");
//}
//else if(memo == 0) {
//	this.memoMap = new NoMemo(); //new PackratMemo(this.source.length());
//}
//else {
//	if(Main.UseFifo) {
//		this.memoMap = new FifoMemo(memo);
//	}
//	else {
//		this.memoMap = new OpenFifoMemo(memo);
//	}
//}
//}



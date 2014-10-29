package org.peg4d;

import java.util.HashMap;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingMatcher;


public class ParsingContext {
	public ParsingObject left;
	public ParsingSource source;

	ParsingTag    emptyTag;	
	ParsingStatistics          stat   = null;

	public ParsingContext(ParsingSource s, long pos, int stacksize, MemoTable memo) {
		this.left = null;
		this.source = s;
		this.resetSource(s, pos);
		this.memoMap = memo != null ? memo : new NoMemoTable(0, 0);
	}

	public ParsingContext(ParsingSource s) {
		this(s, 0, 4096, null);
	}

	public void resetSource(ParsingSource source, long pos) {
		this.source = source;
		this.pos = pos;
		this.fpos = 0;
		this.initCallStack();
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

	public final ParsingObject parse(Grammar peg, String startPoint, MemoizationManager conf) {
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
			this.commitLog(0, this.left);
		}
		else {
			this.abortLog(0);
			this.unusedLog = null;
		}
		checkUnusedText(po);
		if(conf != null) {
			conf.removeMemo(start);
			conf.show2(this.stat);
		}
		return this.left;
	}

	public final boolean match(Grammar peg, String startPoint, MemoizationManager conf) {
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
		if(conf != null) {
			conf.removeMemo(r);
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
	
	public long pos;
	long head_pos;
	public long fpos;

	StackTrace maximumFailureTrace = null;
	String failureInfo  = null;
	
	public final long getPosition() {
		return this.pos;
	}
	
	final void setPosition(long pos) {
		this.pos = pos;
	}
	
	public final void consume(int length) {
		this.pos += length;
		if(head_pos < pos) {
			this.head_pos = pos;
			if(this.stackedNonTerminals != null) {
				this.maximumFailureTrace = new StackTrace();
			}
		}
	}

	public final void rollback(long pos) {
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
	
	public final long rememberFailure() {
		return this.fpos;
	}
	
	public final void forgetFailure(long fpos) {
//		if(this.fpos != fpos) {
//			this.removeErrorInfo(this.fpos);
//		}
//		this.fpos = fpos;
	}
	
	private ParsingMatcher getErrorInfo(long fpos) {
		int index = (int)this.pos % errorbuf.length;
		if(posbuf[index] == fpos) {
			return errorbuf[index];
		}
		return null;
	}

//	private void setErrorInfo(ParsingMatcher errorInfo) {
//		int index = (int)this.pos % errorbuf.length;
//		errorbuf[index] = errorInfo;
//		posbuf[index] = this.pos;
//		//System.out.println("push " + this.pos + " @" + errorInfo);
//	}
//
//	private void removeErrorInfo(long fpos) {
//		int index = (int)this.pos % errorbuf.length;
//		if(posbuf[index] == fpos) {
//			//System.out.println("pop " + fpos + " @" + errorbuf[index]);
//			errorbuf[index] = null;
//		}
//	}

	public String getErrorMessage() {
		ParsingMatcher errorInfo = this.getErrorInfo(this.fpos);
		if(errorInfo == null) {
			return "syntax error";
		}
		return "syntax error: expecting " + errorInfo.expectedToken() + " <- Never believe this";
	}
	
	public final void failure(ParsingMatcher errorInfo) {
		if(this.pos > fpos) {  // adding error location
			this.fpos = this.pos;
		}
		this.left = null;
	}
		
	public final ParsingObject newParsingObject(long pos, ParsingConstructor created) {
		return new ParsingObject(this.emptyTag, this.source, pos, created);
	}

	private class ParsingLog {
		ParsingLog next;
		int  index;
		ParsingObject childNode;
	}

	ParsingLog logStack = null;
	ParsingLog unusedLog = null;
	int   logStackSize = 0;
	
	private ParsingLog newLog() {
		if(this.unusedLog == null) {
			return new ParsingLog();
		}
		ParsingLog l = this.unusedLog;
		this.unusedLog = l.next;
		l.next = null;
		return l;
	}

	private void unuseLog(ParsingLog log) {
		log.childNode = null;
		log.next = this.unusedLog;
		this.unusedLog = log;
	}
	
	public int markLogStack() {
		return logStackSize;
	}

	public final void lazyLink(ParsingObject parent, int index, ParsingObject child) {
		ParsingLog l = this.newLog();
		l.childNode  = child;
		child.parent = parent;
		l.index = index;
		l.next = this.logStack;
		this.logStack = l;
		this.logStackSize += 1;
	}
	
	public final void lazyJoin(ParsingObject left) {
		ParsingLog l = this.newLog();
		l.childNode  = left;
		l.index = -9;
		l.next = this.logStack;
		this.logStack = l;
		this.logStackSize += 1;
	}

	private final void checkNullEntry(ParsingObject o) {
		for(int i = 0; i < o.size(); i++) {
			if(o.get(i) == null) {
				o.set(i, new ParsingObject(emptyTag, this.source, 0));
			}
		}
	}

	public final void commitLog(int mark, ParsingObject newnode) {
		ParsingLog first = null;
		int objectSize = 0;
		while(mark < this.logStackSize) {
			ParsingLog cur = this.logStack;
			this.logStack = this.logStack.next;
			this.logStackSize--;
			if(cur.index == -9) { // lazyCommit
				commitLog(mark, cur.childNode);
				unuseLog(cur);
				break;
			}
			if(cur.childNode.parent == newnode) {
				cur.next = first;
				first = cur;
				objectSize += 1;
			}
			else {
				unuseLog(cur);
			}
		}
		if(objectSize > 0) {
			newnode.expandAstToSize(objectSize);
			for(int i = 0; i < objectSize; i++) {
				ParsingLog cur = first;
				first = first.next;
				if(cur.index == -1) {
					cur.index = i;
				}
				newnode.set(cur.index, cur.childNode);
				this.unuseLog(cur);
			}
			checkNullEntry(newnode);
		}
	}
	
	public final void abortLog(int mark) {
		while(mark < this.logStackSize) {
			ParsingLog l = this.logStack;
			this.logStack = this.logStack.next;
			this.logStackSize--;
			unuseLog(l);
		}
		assert(mark == this.logStackSize);
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
	
	UList<NonTerminal> stackedNonTerminals;
	int[]         stackedPositions;
	
	class StackTrace {
		StackTrace prev;
		NonTerminal[] NonTerminals;
		int[]    Positions;
		StackTrace() {
			this.NonTerminals = new NonTerminal[stackedNonTerminals.size()];
			this.Positions = new int[stackedNonTerminals.size()];
			System.arraycopy(stackedNonTerminals.ArrayValues, 0, NonTerminals , 0, NonTerminals.length);
			System.arraycopy(stackedPositions, 0, Positions, 0, Positions.length);
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for(int n = 0; n < NonTerminals.length; n++) {
				if(n > 0) {
					sb.append("\n");
				}
				sb.append(source.formatPositionLine(this.NonTerminals[n].ruleName, this.Positions[n], "pos="+this.Positions[n]));
//				sb.append(this.NonTerminals[n]);
//				sb.append("#");
//				source.linenum()
//				sb.append()this.Positions[n]);
			}
			return sb.toString();
		}
	}

	void initCallStack() {
		if(Main.DebugLevel > 0) {
			this.stackedNonTerminals = new UList<NonTerminal>(new NonTerminal[256]);
			this.stackedPositions = new int[4096];
		}
	}
	
	public int pushCallStack(NonTerminal e) {
		int pos = this.stackedNonTerminals.size();
		this.stackedNonTerminals.add(e);
		stackedPositions[pos] = (int)this.pos;
		return pos;
	}

	public void popCallStack(int stacktop) {
		this.stackedNonTerminals.clear(stacktop);
	}
 		
	protected MemoTable memoMap = null;

	public void initMemo(MemoizationManager conf) {
		this.memoMap = (conf == null) ? new NoMemoTable(0, 0) : conf.newTable(this.source.length());
	}

	final MemoEntry getMemo(long keypos, int memoPoint) {
		return this.memoMap.getMemo(keypos, memoPoint);
	}

	final void setMemo(long keypos, int memoPoint, ParsingObject result, int length) {
		this.memoMap.setMemo(keypos, memoPoint, result, length);
	}

	private HashMap<String,Boolean> flagMap = new HashMap<String,Boolean>();
	
	public final void setFlag(String flagName, boolean flag) {
		this.flagMap.put(flagName, flag);
	}
	
	public final boolean getFlag(String flagName) {
		return this.isFlag(flagMap.get(flagName));
	}
	
	public final boolean isFlag(Boolean f) {
		return f == null || f.booleanValue();
	}
		
}


package nez;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import nez.ast.Node;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.NonTerminal;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class SourceContext {
	public final static int TextEOF   = 0;
	public final static int BinaryEOF = 256; 
	
	private String     fileName;
	protected long     startLineNum = 1;

	protected SourceContext(String fileName, long linenum) {
		this.fileName = fileName;
		this.startLineNum = linenum;
	}
	
	public abstract int     byteAt(long pos);
	public abstract long    length();

	public abstract boolean match(long pos, byte[] text);
	public abstract String  substring(long startIndex, long endIndex);
	public abstract long    linenum(long pos);

	/* handling input stream */
	
	final String getResourceName() {
		return fileName;
	}

	final String getFilePath(String fileName) {
		int loc = this.getResourceName().lastIndexOf("/");
		if(loc > 0) {
			return this.getResourceName().substring(0, loc+1) + fileName; 
		}
		return fileName;
	}

	public final int charAt(long pos) {
		int c = byteAt(pos), c2, c3, c4;
		int len = StringUtils.lengthOfUtf8(c);
		switch(len) {
		case 1:
			return c;
		case 2:
			// 0b11111 = 31
			// 0b111111 = 63
			c2 = byteAt(pos + 1) & 63;
			return ((c & 31) << 6) | c2;
		case 3:
			c2 = byteAt(pos + 1) & 63;
			c3 = byteAt(pos + 2) & 63;
			return ((c & 15) << 12) | c2 << 6 | c3;
		case 4:
			c2 = byteAt(pos + 1) & 63;
			c3 = byteAt(pos + 2) & 63;
			c4 = byteAt(pos + 3) & 63;
			return ((c & 7) << 18) | c2 << 12 | c3 << 6 | c4;
		}
		return -1;
	}

	public final int charLength(long pos) {
		int c = byteAt(pos);
		return StringUtils.lengthOfUtf8(c);
	}

	private final long getLineStartPosition(long fromPostion) {
		long startIndex = fromPostion;
		if(!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			int ch = byteAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getIndentText(long fromPosition) {
		long startPosition = this.getLineStartPosition(fromPosition);
		long i = startPosition;
		String indent = "";
		for(; i < fromPosition; i++) {
			int ch = this.byteAt(i);
			if(ch != ' ' && ch != '\t') {
				if(i + 1 != fromPosition) {
					for(long j = i;j < fromPosition; j++) {
						indent = indent + " ";
					}
				}
				break;
			}
		}
		indent = this.substring(startPosition, i) + indent;
		return indent;
	}

	public final String formatPositionMessage(String messageType, long pos, String message) {
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ") [" + messageType +"] " + message;
	}

	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}

	public final String getTextAround(long pos, String delim) {
		int ch = 0;
		if(pos < 0) {
			pos = 0;
		}
		while(this.byteAt(pos) == Source.EOF && pos > 0) {
			pos -= 1;
		}
		long startIndex = pos;
		while(startIndex > 0) {
			ch = byteAt(startIndex);
			if(ch == '\n' && pos - startIndex > 0) {
				startIndex = startIndex + 1;
				break;
			}
			if(pos - startIndex > 60 && ch < 128) {
				break;
			}
			startIndex = startIndex - 1;
		}
		long endIndex = pos + 1;
		while((ch = byteAt(endIndex)) != Source.EOF) {
			if(ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
				break;
			}
			endIndex = endIndex + 1;
		}
		StringBuilder source = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for(long i = startIndex; i < endIndex; i++) {
			ch = byteAt(i);
			if(ch == '\n') {
				source.append("\\N");
				if(i == pos) {
					marker.append("^^");
				}
				else {
					marker.append("\\N");
				}
			}
			else if(ch == '\t') {
				source.append("    ");
				if(i == pos) {
					marker.append("^^^^");
				}
				else {
					marker.append("    ");
				}
			}
			else {
				source.append((char)ch);
				if(i == pos) {
					marker.append("^");
				}
				else {
					marker.append(" ");
				}
			}
		}
		return delim + source.toString() + delim + marker.toString();
	}
	
	/* parsing position */
	public long pos;
	private long   head_pos;
	Recorder    stat   = null;
	
//	public SourceContext(Source s, long pos, int stacksize, MemoTable memo) {
//		this.left = null;
//		this.source = s;
//		this.resetSource(s, pos);
//		this.memoTable = memo != null ? memo : new NoMemoTable(0, 0);
//	}
//
//	public SourceContext(Source s) {
//		this(s, 0, 4096, null);
//	}
//
//	public void resetSource(Source source, long pos) {
//		this.source = source;
//		this.pos = pos;
//		this.fpos = 0;
//		this.initCallStack();
//	}

	public final long getPosition() {
		return this.pos;
	}
	
	final void setPosition(long pos) {
		this.pos = pos;
	}
	
	public final boolean consume(int length) {
		this.pos += length;
		if(head_pos < pos) {
			this.head_pos = pos;
//			if(this.stackedNonTerminals != null) {
//				this.maximumFailureTrace = new StackTrace();
//			}
		}
		return true;
	}

	public final void rollback(long pos) {
		if(stat != null && this.pos > pos) {
			stat.statBacktrack(pos, this.pos);
		}
		this.pos = pos;
	}

//	public long fpos;
//	StackTrace maximumFailureTrace = null;
//	String failureInfo  = null;	
//	Recognizer[] errorbuf = new Recognizer[512];
//	long[] posbuf = new long[errorbuf.length];

	public final boolean isFailure() {
		return this.left == null;
	}
	
//	public final void failure(Recognizer errorInfo) {
//		if(this.pos > fpos) {  // adding error location
//			this.fpos = this.pos;
//		}
//		this.left = null;
//	}
//	
	public boolean failure2(nez.expr.Expression e) {
//		if(this.pos > fpos) {  // adding error location
//			this.fpos = this.pos;
//		}
		this.left = null;
		return false;
	}

//	public final long rememberFailure() {
//		return this.fpos;
//	}
//	
//	public final void forgetFailure(long fpos) {
////		if(this.fpos != fpos) {
////			this.removeErrorInfo(this.fpos);
////		}
////		this.fpos = fpos;
//	}
//	
//	private Recognizer getErrorInfo(long fpos) {
//		int index = (int)this.pos % errorbuf.length;
//		if(posbuf[index] == fpos) {
//			return errorbuf[index];
//		}
//		return null;
//	}

//	private void setErrorInfo(Matcher errorInfo) {
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
//
//	public String getErrorMessage() {
//		Recognizer errorInfo = this.getErrorInfo(this.fpos);
//		if(errorInfo == null) {
//			return "syntax error";
//		}
//		return "syntax error: expecting " + errorInfo.toString() + " <- Never believe this";
//	}
	
	/* PEG4d : AST construction */

	private Node base;
	public Node left;

	public final Node newNode(long pos, Expression created) {
		return this.base.newNode(this, pos, created);
	}

	private class ConstructionLog {
		ConstructionLog next;
		Node parentNode;
		int  index;
		Node childNode;
	}

	private ConstructionLog logStack = null;
	private ConstructionLog unusedLog = null;
	private int        logStackSize = 0;
	
	private ConstructionLog newLog() {
		if(this.unusedLog == null) {
			return new ConstructionLog();
		}
		ConstructionLog l = this.unusedLog;
		this.unusedLog = l.next;
		l.next = null;
		return l;
	}

	private void unuseLog(ConstructionLog log) {
		log.childNode = null;
		log.next = this.unusedLog;
		this.unusedLog = log;
	}
	
	public int markLogStack() {
		return logStackSize;
	}

	public final void lazyLink(Node parent, int index, Node child) {
		ConstructionLog l = this.newLog();
		l.parentNode = parent;
		l.childNode  = child;
		l.index = index;
		l.next = this.logStack;
		this.logStack = l;
		this.logStackSize += 1;
	}
	
	public final void lazyJoin(Node left) {
		ConstructionLog l = this.newLog();
		l.childNode  = left;
		l.index = -9;
		l.next = this.logStack;
		this.logStack = l;
		this.logStackSize += 1;
	}

//	private final void checkNullEntry(Node o) {
//		for(int i = 0; i < o.size(); i++) {
//			if(o.get(i) == null) {
//				o.set(i, new Node(emptyTag, this.source, 0));
//			}
//		}
//	}

	public final void commitLog(int mark, Node newnode) {
		ConstructionLog first = null;
		int objectSize = 0;
		while(mark < this.logStackSize) {
			ConstructionLog cur = this.logStack;
			this.logStack = this.logStack.next;
			this.logStackSize--;
			if(cur.index == -9) { // lazyCommit
				commitLog(mark, cur.childNode);
				unuseLog(cur);
				break;
			}
			if(cur.parentNode == newnode) {
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
				ConstructionLog cur = first;
				first = first.next;
				if(cur.index == -1) {
					cur.index = i;
				}
				newnode.commitChild(cur.index, cur.childNode);
				this.unuseLog(cur);
			}
			//checkNullEntry(newnode);
		}
	}
	
	public final void abortLog(int mark) {
		while(mark < this.logStackSize) {
			ConstructionLog l = this.logStack;
			this.logStack = this.logStack.next;
			this.logStackSize--;
			unuseLog(l);
		}
		assert(mark == this.logStackSize);
	}

	/* context-sensitivity parsing */
	/* <block e> <indent> */
	/* <def T e>, <is T>, <isa T> */
	
	public int stateValue = 0;
	private int stateCount = 0;
	private UList<SymbolTableEntry> stackedSymbolTable = new UList<SymbolTableEntry>(new SymbolTableEntry[4]);

	private class SymbolTableEntry {
		Tag table;  // T in <def T e>
		byte[] utf8;
		SymbolTableEntry(Tag table, String indent) {
			this.table = table;
			this.utf8 = indent.getBytes();
		}
	}
	
	public final int pushSymbolTable(Tag table, String s) {
		int stackTop = this.stackedSymbolTable.size();
		this.stackedSymbolTable.add(new SymbolTableEntry(table, s));
		this.stateCount += 1;
		this.stateValue = stateCount;
		return stackTop;
	}

	public final void popSymbolTable(int stackTop) {
		this.stackedSymbolTable.clear(stackTop);
	}

	public final boolean matchSymbolTableTop(Tag table) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == table) {
				if(this.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
				break;
			}
		}
		return this.failure2(null);
	}

	public final boolean matchSymbolTable(Tag table) {
		for(int i = stackedSymbolTable.size() - 1; i >= 0; i--) {
			SymbolTableEntry s = stackedSymbolTable.ArrayValues[i];
			if(s.table == table) {
				if(this.match(this.pos, s.utf8)) {
					this.consume(s.utf8.length);
					return true;
				}
			}
		}
		return this.failure2(null);
	}

//	public final boolean hasByteChar() {
//		return this.byteAt(this.pos) != ParsingSource.EOF;
//	}
//
//	public final int getByteChar() {
//		return this.byteAt(pos);
//	}
//		
//	private final void checkUnusedText(Node po) {
//		if(this.left == po) {
//			po.setEndingPosition(this.pos);
//		}
//	}
	
//	@SuppressWarnings("unchecked")
//	public <T extends Node> T parse2(Grammar peg, String startPoint, T base, MemoizationManager conf) {
//		Rule start = peg.getRule(startPoint);
//		if(start == null) {
//			Main._Exit(1, "undefined start rule: " + startPoint );
//		}
//		if(conf != null) {
//			conf.exploitMemo(start);
//			this.initMemo(conf);
//		}
//		this.base = base;
//		Node po = newNode(this.pos, null);
//		this.left = po;
//		if(start.expr.debugMatch(this)) {
//			this.commitLog(0, this.left);
//		}
//		else {
//			this.abortLog(0);
//			this.unusedLog = null;
//		}
//		checkUnusedText(po);
//		if(conf != null) {
//			conf.removeMemo(start);
//			conf.show2(this.stat);
//		}
//		return (T)this.left;
//	}
//
//	public final boolean match(Grammar peg, String startPoint, MemoizationManager conf) {
//		ParsingExpression start = peg.getExpression(startPoint);
//		if(start == null) {
//			Main._Exit(1, "undefined start rule: " + startPoint );
//		}
//		ParsingRule r = peg.getLexicalRule(startPoint);
//		if(conf != null) {
//			conf.exploitMemo(r);
//			this.initMemo(conf);
//		}
//		start = r.expr;
//		this.base = new ParsingObject();
//		this.left = this.base;
//		boolean res = start.debugMatch(this);
//		if(conf != null) {
//			conf.removeMemo(r);
//			conf.show2(this.stat);
//		}
//		return res;
//	}
//	
//	public final void setLogger(Recorder stat) {
//		this.stat = stat;
//		if(stat != null) {
//			stat.init();
//		}
//	}
	
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
				sb.append(formatPositionLine(this.NonTerminals[n].ruleName, this.Positions[n], "pos="+this.Positions[n]));
//				sb.append(this.NonTerminals[n]);
//				sb.append("#");
//				source.linenum()
//				sb.append()this.Positions[n]);
			}
			return sb.toString();
		}
	}

	void initCallStack() {
//		if(Main.DebugLevel > 0) {
//			this.stackedNonTerminals = new UList<NonTerminal>(new NonTerminal[256]);
//			this.stackedPositions = new int[4096];
//		}
	}
		
	public final boolean matchNonTerminal(NonTerminal e) {
		if(this.stackedNonTerminals != null) {
			int pos = this.stackedNonTerminals.size();
			this.stackedNonTerminals.add(e);
			stackedPositions[pos] = (int)this.pos;
			boolean b = e.deReference().matcher.match(this);
			this.stackedNonTerminals.clear(pos);
			return b;
		}
		return e.deReference().matcher.match(this);
	}
	
//	protected MemoTable memoTable = null;
//
//	public void initMemo(MemoizationManager conf) {
//		this.memoTable = (conf == null) ? new NoMemoTable(0, 0) : conf.newTable(this.source.length());
//	}
//
//	final MemoEntry getMemo(long keypos, int memoPoint) {
//		return this.memoTable.getMemo(keypos, memoPoint);
//	}
//
//	final void setMemo(long keypos, int memoPoint, Node result, int length) {
//		this.memoTable.setMemo(keypos, memoPoint, result, length);
//	}
//
//	final MemoEntry getMemo2(long keypos, int memoPoint, int stateValue) {
//		return this.memoTable.getMemo2(keypos, memoPoint, stateValue);
//	}
//
//	final void setMemo2(long keypos, int memoPoint, int stateValue, Node result, int length) {
//		this.memoTable.setMemo2(keypos, memoPoint, stateValue, result, length);
//	}
	
//	HashMap<String, Integer> repeatMap = new HashMap<String, Integer>();
//	
//	public final void setRepeatExpression(String rule, int value) {
//		this.repeatMap.put(rule, value);
//	}
//	
//	public final int getRepeatValue(String rule) {
//		return this.repeatMap.get(rule);
//	}
//	
//	public final String getRepeatByteString(long startIndex) {
//		return this.source.substring(startIndex, this.pos);
//	}
	
	public final static SourceContext newStringSourceContext(String str) {
		return new StringSourceContext(str);
	}

	public final static SourceContext newStringSourceContext(String resource, long linenum, String str) {
		return new StringSourceContext(resource, linenum, str);
	}

	
}

class StringSourceContext extends SourceContext {
	private byte[] utf8;
	long textLength;

	StringSourceContext(String sourceText) {
		super("(string)", 1);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length-1;
	}

	StringSourceContext(String resource, long linenum, String sourceText) {
		super(resource, linenum);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length-1;
	}

	private final byte[] toZeroTerminalByteSequence(String s) {
		byte[] b = StringUtils.toUtf8(s);
		byte[] b2 = new byte[b.length+1];
		System.arraycopy(b, 0, b2, 0, b.length);
		return b2;
	}

	@Override
	public final long length() {
		return this.textLength;
	}

	@Override
	public final int byteAt(long pos) {
		return this.utf8[(int)pos] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if(pos + text.length > this.textLength) {
			return false;
		}
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.utf8[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.utf8, (int)(startIndex), (int)(endIndex - startIndex), StringUtils.DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		int end = (int)pos;
		if(end >= this.utf8.length) {
			end = this.utf8.length;
		}
		for(int i = 0; i < end; i++) {
			if(this.utf8[i] == '\n') {
				count++;
			}
		}
		return count;
	}
}

class FileSourceContext extends SourceContext {
	public final static int PageSize = 4096;

	private RandomAccessFile file;
	private long fileLength = 0;
	private long buffer_offset;
	private byte[] buffer;
	private long lines[];

	private final int FifoSize = 8; 
	private LinkedHashMap<Long, byte[]> fifoMap = null;

	FileSourceContext(String fileName) throws IOException {
		super(fileName, 1);
		this.file = new RandomAccessFile(fileName, "r");
		this.fileLength = this.file.length();
		this.buffer_offset = 0;
		lines = new long[((int)this.fileLength / PageSize) + 1];
		lines[0] = 1;
		if(this.FifoSize > 0) {
			this.fifoMap = new LinkedHashMap<Long, byte[]>(FifoSize) {  //FIFO
				private static final long serialVersionUID = 6725894996600788028L;
				@Override
				protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest)  {
					if(this.size() > FifoSize) {
						return true;			
					}
					return false;
				}
			};
			this.buffer = null;
		}
		else {
			this.fifoMap = null;
			this.buffer = new byte[PageSize];
		}
		this.readMainBuffer(this.buffer_offset);

	}
	@Override
	public final long length() {
		return this.fileLength;
	}

	private long buffer_alignment(long pos) {
		return (pos / PageSize) * PageSize;
	}

	@Override
	public final int byteAt(long pos) {
		int buffer_pos = (int)(pos - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(pos);
			this.readMainBuffer(this.buffer_offset);
			buffer_pos = (int)(pos - this.buffer_offset);
		}
		return this.buffer[buffer_pos] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		int offset = (int)(pos - this.buffer_offset);
		if(offset >= 0 && offset + text.length <= PageSize) {
			switch(text.length) {
			case 0:
				break;
			case 1:
				if(text[0] == this.buffer[offset]) {
					return true;
				}
				return false;
			case 2:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1]) {
					return true;
				}
				return false;
			case 3:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1] && text[2] == this.buffer[offset+2]) {
					return true;
				}
				return false;
			case 4:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1] && text[2] == this.buffer[offset+2] && text[3] == this.buffer[offset+3]) {
					return true;
				}
				return false;
			default:
				for(int i = 0; i < text.length; i++) {
					if(text[i] != this.buffer[offset+i]) {
						return false;
					}
				}
			}
			return true;
		}
		for(int i = 0; i < text.length; i++) {
			if((text[i] & 0xff) != this.byteAt(pos + i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		if(endIndex > startIndex) {
			try {
				long off_s = buffer_alignment(startIndex);
				long off_e = buffer_alignment(endIndex);
				if(off_s == off_e) {
					if(this.buffer_offset != off_s) {
						this.buffer_offset = off_s;
						this.readMainBuffer(this.buffer_offset);
					}
					return new String(this.buffer, (int)(startIndex - this.buffer_offset), (int)(endIndex - startIndex), StringUtils.DefaultEncoding);
				}
				else {
					byte[] b = new byte[(int)(endIndex - startIndex)];
					this.readStringBuffer(startIndex, b);
					return new String(b, StringUtils.DefaultEncoding);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	private int lineIndex(long pos) {
		return (int)(pos / PageSize);
	}

	private long startLineNum(long pos) {
		int index = lineIndex(pos);
		return this.lines[index];
	}

	@Override
	public final long linenum(long pos) {
		long count = startLineNum(pos);
		byteAt(pos); // restore buffer at pos
		int offset = (int)(pos - this.buffer_offset);
		for(int i = 0; i < offset; i++) {
			if(this.buffer[i] == '\n') {
				count++;
			}
		}
		return count;
	}

	private void readMainBuffer(long pos) {
		int index = lineIndex(pos);
		if(this.lines[index] == 0) {
			long count = this.lines[index-1];
			for(int i = 0; i < this.buffer.length; i++) {
				if(this.buffer[i] == '\n') {
					count++;
				}
			}
			this.lines[index] = count;
		}
		if(this.fifoMap != null) {
			Long key = pos;
			byte[] buf = this.fifoMap.get(key);
			if(buf == null) {
				buf = new byte[PageSize];
				this.readBuffer(pos, buf);
				this.fifoMap.put(key, buf);
				this.buffer = buf;
			}
			else {
				this.buffer = buf;
			}
		}
		else {
			this.readBuffer(pos, this.buffer);
		}
	}

	private void readBuffer(long pos, byte[] b) {
		try {
			this.file.seek(pos);
			int readsize = this.file.read(b);
			for(int i = readsize; i < b.length; i++) {
				b[i] = 0;
			}
			//		if(this.stat != null) {
			//			stat.readFile(b.length);
			//		}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readStringBuffer(long pos, byte[] buf) {
		if(this.fifoMap != null) {
			int copied = 0;
			long start = pos;
			long end = pos + buf.length;
			while(start < end) {
				long offset = this.buffer_alignment(start);
				if(this.buffer_offset != offset) {
					this.buffer_offset = offset;
					this.readMainBuffer(offset);
				}
				int start_off = (int)(start - offset);
				int end_off = (int)(end - offset);				
				if(end_off <= PageSize) {
					int len = end_off - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					assert(copied == buf.length);
					return;
				}
				else {
					int len = PageSize - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					start += len;
				}
			}
		}
		else {
			this.readBuffer(pos, buf);
		}
	}

	//public final String substringDebug(long startIndex, long endIndex) {
	//	String s= this.substring(startIndex, endIndex);
	//	if(this.debug != null) {
	//		String s2= this.debug.substring(startIndex, endIndex);
	//		if(!s.equals(s2)) {
	//			System.out.println("s1: " + s);
	//			System.out.println("s2: " + s2);
	//			Main._Exit(1, "different " + this.fileName + " pos=" + startIndex + " end=" + endIndex);
	//		}
	//	}
	//	return s;
	//}
	//
	//public final int charAtDebug(long n) {
	//	//assert(n < this.fileLength);
	//	int c = this.charAt(n);
	//	if(this.debug != null) {
	//		int c2 = this.debug.charAt(n);
	//		if(c != c2) {
	//			Main._Exit(1, "different " + this.fileName + " pos=" + n + "c='"+c+"', c2='"+c2+"'");
	//		}
	//	}
	//	return c;
	//}

}



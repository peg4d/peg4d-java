package org.peg4d;

import java.util.concurrent.BlockingQueue;

public abstract class ParserContext {
	public    Grammar     peg = null;
	
	public final          ParserSource source;
	protected long        sourcePosition = 0;
	public    long        endPosition;
	protected Stat stat   = null;
	
	public ParserContext(Grammar peg, ParserSource source, long startIndex, long endIndex) {
		this.peg = peg;
		this.source = source;
		this.sourcePosition = startIndex;
		this.endPosition = endIndex;
	}
	
	protected final long getPosition() {
		return this.sourcePosition;
	}
	
	protected final void setPosition(long pos) {
		this.sourcePosition = pos;
	}
	
	protected final void rollback(long pos) {
		if(stat != null) {
			long len = this.sourcePosition - pos;
			if(len > 0) {
				stat.statBacktrack(pos, len);
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
		Main._Exit(1, msg);
	}
	
	public boolean hasNode() {
		return this.sourcePosition < this.endPosition;
	}

	public Pego parseNode(String startPoint) {
		this.initMemo();
		Peg start = this.getRule(startPoint);
		if(start == null) {
			Main._Exit(1, "undefined start rule: " + startPoint );
		}
		Pego pego = start.simpleMatch(Pego.newSource("#toplevel", this.source, 0), this);
		if(pego.isFailure()) {
			pego = this.newErrorObject();
			String msg = this.source.formatErrorMessage("syntax error", pego.getSourcePosition(), "");
			pego.setMessage(msg);
			System.out.println(msg);
			this.sourcePosition = this.endPosition;
		}
		return pego;
	}
	
	protected MemoMap memoMap = null;
	public abstract void initMemo();
	
	protected Pego successResult = null;
	
	public final void setRecognitionOnly(boolean checkMode) {
		if(checkMode) {
			this.successResult = Pego.newSource("#success", this.source, 0);
		}
		else {
			this.successResult = null;
		}
	}
	
	public final boolean isRecognitionOnly() {
		return this.successResult != null;
	}
	
	public final Pego newPegObject(String name, Peg created, long pos) {
		if(this.isRecognitionOnly()) {
			this.successResult.setSourcePosition(pos);
			return this.successResult;
		}
		else {
			Pego node = Pego.newSource("#new", this.source, pos);
			return node;
		}
	}
	
	private long failurePosition = 0;
	private final Pego foundFailureNode = Pego.newSource(null, this.source, 0);

	public final Pego newErrorObject() {
		Pego node = newPegObject("#error", this.peg.getPeg(failurePosition), PEGUtils.getpos(this.failurePosition));
		return node;
	}
	
	public final Pego foundFailure(Peg e) {
		if(this.sourcePosition >= PEGUtils.getpos(this.failurePosition)) {  // adding error location
			this.failurePosition = PEGUtils.failure(this.sourcePosition, e);
		}
		return this.foundFailureNode;
	}

	public final Pego refoundFailure(Peg e, long pos) {
		this.failurePosition = PEGUtils.failure(pos, e);
		return this.foundFailureNode;
	}

	public final Peg getRule(String symbol) {
		return this.peg.optimizedPegMap.get(symbol);
	}

	public Pego matchNonTerminal(Pego left, PegNonTerminal e) {
		Peg next = this.getRule(e.symbol);
		return next.simpleMatch(left, this);
	}

	public final Pego matchString(Pego left, PegString e) {
		if(this.match(e.text)) {
			return left;
		}
		return this.foundFailure(e);
	}

	public final Pego matchCharacter(Pego left, PegCharacter e) {
		char ch = this.getChar();
		if(!e.charset.match(ch)) {
			return this.foundFailure(e);
		}
		this.consume(1);
		return left;
	}

	public final Pego matchAny(Pego left, PegAny e) {
		if(this.hasChar()) {
			this.consume(1);
			return left;
		}
		return this.foundFailure(e);
	}

	public Pego matchOptional(Pego left, PegOptional e) {
		long pos = this.getPosition();
		int markerId = this.markObjectStack();
		Pego parsedNode = e.inner.simpleMatch(left, this);
		if(parsedNode.isFailure()) {
			this.rollbackObjectStack(markerId);
			this.rollback(pos);
			return left;
		}
		return parsedNode;
	}

	public Pego matchRepeat(Pego left, PegRepeat e) {
		Pego prevNode = left;
		int count = 0;
		int markerId = this.markObjectStack();
		while(this.hasChar()) {
			long pos = this.getPosition();
			markerId = this.markObjectStack();
			Pego node = e.inner.simpleMatch(prevNode, this);
			if(node.isFailure()) {
				assert(pos == this.getPosition());
				if(count < e.atleast) {
					this.rollbackObjectStack(markerId);
					return node;
				}
				break;
			}
			prevNode = node;
			//System.out.println("startPostion=" + startPosition + ", current=" + this.getPosition() + ", count = " + count);
			if(!(pos < this.getPosition())) {
				if(count < e.atleast) {
					return this.foundFailure(e);
				}
				break;
			}
			count = count + 1;
			if(!this.hasChar()) {
				markerId = this.markObjectStack();
			}
		}
		this.rollbackObjectStack(markerId);
		return prevNode;
	}

	public Pego matchAnd(Pego left, PegAnd e) {
		Pego node = left;
		long pos = this.getPosition();
		int markerId = this.markObjectStack();
		node = e.inner.simpleMatch(node, this);
		this.rollbackObjectStack(markerId);
		this.rollback(pos);
		return node;
	}

	public Pego matchNot(Pego left, PegNot e) {
		Pego node = left;
		long pos = this.getPosition();
		int markerId = this.markObjectStack();
		node = e.inner.simpleMatch(node, this);
		this.rollbackObjectStack(markerId);
		this.rollback(pos);
		if(node.isFailure()) {
			return left;
		}
		return this.foundFailure(e);
	}

	public Pego matchSequence(Pego left, PegSequence e) {
		long pos = this.getPosition();
		int markerId = this.markObjectStack();
		for(int i = 0; i < e.size(); i++) {
			Pego parsedNode = e.get(i).simpleMatch(left, this);
			if(parsedNode.isFailure()) {
				this.rollbackObjectStack(markerId);
				this.rollback(pos);
				return parsedNode;
			}
			left = parsedNode;
		}
		return left;
	}

	public Pego matchChoice(Pego left, PegChoice e) {
		Pego node = left;
		long pos = this.getPosition();
		for(int i = 0; i < e.size(); i++) {
			int markerId = this.markObjectStack();
			node = e.get(i).simpleMatch(left, this);
			if(!node.isFailure()) {
				break;
			}
			this.rollbackObjectStack(markerId);
			this.setPosition(pos);
		}
		return node;
	}
	
	private class ObjectLog {
		ObjectLog next;
		int  id;
		Pego parentNode;
		int  index;
		Pego childNode;
	}

	ObjectLog logStack = new ObjectLog();  // needs first logs
	ObjectLog unusedLog = null;
	int usedLog = 0;
	int maxLog  = 0;
	
	private ObjectLog newLog() {
		if(this.unusedLog == null) {
			maxLog = maxLog + 1;
			return new ObjectLog();
		}
		ObjectLog l = this.unusedLog;
		this.unusedLog = l.next;
		l.next = null;
		return l;
	}
	
	protected int markObjectStack() {
		return this.logStack.id;
	}

	protected void rollbackObjectStack(int markerId) {
		ObjectLog cur = this.logStack;
		if(cur.id > markerId) {
			ObjectLog unused = this.logStack;
			while(cur != null) {
				//System.out.println("pop cur.id="+cur.id + ", marker="+markerId);
				if(cur.id == markerId + 1) {
					this.logStack = cur.next;
					cur.next = this.unusedLog;
					this.unusedLog = unused;
					break;
				}
				cur.parentNode = null;
				cur.childNode = null;
				cur = cur.next;
			}
		}
	}
	
	protected final void pushSetter(Pego parentNode, int index, Pego childNode) {
		if(!this.isRecognitionOnly()) {
			ObjectLog l = this.newLog();
			l.parentNode = parentNode;
			l.childNode  = childNode;
			l.index = index;
			l.id = this.logStack.id + 1;
			l.next = this.logStack;
			this.logStack = l;
			//System.out.println("push " + l.id + ", index= " + index);
		}
	}

	protected final void popNewObject(Pego newnode, long startIndex, int marker) {
		if(!this.isRecognitionOnly()) {
			ObjectLog cur = this.logStack;
			if(cur.id > marker) {
				UList<ObjectLog> entryList = new UList<ObjectLog>(new ObjectLog[8]);
				ObjectLog unused = this.logStack;
				while(cur != null) {
					//System.out.println("object cur.id="+cur.id + ", marker="+marker);
					if(cur.parentNode == newnode) {
						entryList.add(cur);
					}
					if(cur.id == marker + 1) {
						this.logStack = cur.next; 
						cur.next = this.unusedLog;
						this.unusedLog = unused;
						break;
					}
					cur = cur.next;
				}
				if(entryList.size() > 0) {
					newnode = Pego.newAst(newnode, entryList.size());
					int index = 0;
					for(int i = entryList.size() - 1; i >= 0; i--) {
						ObjectLog l = entryList.ArrayValues[i];
						if(l.index == -1) {
							l.index = index;
						}
						index += 1;
					}
					for(int i = entryList.size() - 1; i >= 0; i--) {
						ObjectLog l = entryList.ArrayValues[i];
						newnode.set(l.index, l.childNode);
						l.childNode = null;
					}
					newnode.checkNullEntry();
				}
				entryList = null;
			}
			newnode.setLength((int)(this.getPosition() - startIndex));
		}
	}
	
	public Pego matchNewObject(Pego left, PegNewObject e) {
		Pego leftNode = left;
		long startIndex = this.getPosition();
		if(this.stat != null) {
			this.stat.countRepeatCall(e, startIndex);
		}
		if(e.predictionIndex > 0) {
			for(int i = 0; i < e.predictionIndex; i++) {
				Pego node = e.get(i).simpleMatch(left, this);
				if(node.isFailure()) {
					this.rollback(startIndex);
					return node;
				}
				assert(left == node);
			}
		}
		int markerId = this.markObjectStack();
		Pego newnode = this.newPegObject(e.nodeName, e, startIndex);
		if(e.leftJoin) {
			this.pushSetter(newnode, -1, leftNode);
		}
		for(int i = e.predictionIndex; i < e.size(); i++) {
			Pego node = e.get(i).simpleMatch(newnode, this);
			if(node.isFailure()) {
				this.rollbackObjectStack(markerId);
				this.rollback(startIndex);
				return node;
			}
			//			if(node != newnode) {
			//				e.warning("dropping @" + newnode.name + " " + node);
			//			}
		}
		this.popNewObject(newnode, startIndex, markerId);
		if(this.stat != null) {
			this.stat.countObjectCreation();
		}
		return newnode;
	}
	
//	long statExportCount = 0;
//	long statExportSize  = 0;
//	long statExportFailure  = 0;

	public Pego matchExport(Pego left, PegExport e) {
		Pego pego = e.inner.simpleMatch(left, this);
		if(!pego.isFailure()) {
//			this.statExportCount += 1;
//			this.statExportSize += pego.getLength();
			this.pushBlockingQueue(pego);
		}
		else {
//			this.statExportFailure += 1;
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

	public Pego matchSetter(Pego left, PegSetter e) {
		long pos = left.getSourcePosition();
		Pego node = e.inner.simpleMatch(left, this);
		if(node.isFailure() || left == node) {
			return node;
		}
		if(this.isRecognitionOnly()) {
			left.setSourcePosition(pos);
		}
		else {
			this.pushSetter(left, e.index, node);
		}
		return left;
	}

	public Pego matchTag(Pego left, PegTagging e) {
		left.setTag(e.symbol);
		return left;
	}

	public Pego matchMessage(Pego left, PegMessage e) {
		left.setMessage(e.symbol);
		//left.startIndex = this.getPosition();
		return left;
	}
	
	public Pego matchIndent(Pego left, PegIndent e) {
		String indent = left.getSource().getIndentText(left.getSourcePosition());
		//System.out.println("###" + indent + "###");
		if(this.match(indent)) {
			return left;
		}
		return this.foundFailure(e);
	}

	public Pego matchIndex(Pego left, PegIndex e) {
		String text = left.textAt(e.index, null);
		if(text != null) {
			if(this.match(text)) {
				return left;
			}
		}
		return this.foundFailure(e);
	}

	public void beginPeformStat() {
		if(Main.StatLevel > 0) {
			this.stat = new Stat(this.peg, this.source);
			if(Main.StatLevel == 2) {
				this.stat.initRepeatCounter();
			}
			this.stat.start();
		}
	}

	public void endPerformStat(Pego pego) {
		if(stat != null) {
			stat.end(pego, this);
		}
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

}


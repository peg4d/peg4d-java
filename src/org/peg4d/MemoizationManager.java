package org.peg4d;

import java.util.HashMap;
import java.util.Map;

import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingMatcher;

public class MemoizationManager {
	public final static ParsingObject NonTransition = new ParsingObject(null, null, 0);
	public static boolean NoMemo = false;
	public static boolean PackratParsing = false;
	public static boolean SlidingWindowParsing = false;
	public static boolean SlidingLinkedParsing = false;
	public static int     BacktrackBufferSize = 64;
	public static boolean Tracing = true;
	public static boolean VerboseMemo = false;
	
	UList<MemoMatcher> memoList = new UList<MemoMatcher>(new MemoMatcher[16]);
	
	MemoizationManager() {
		
	}
	
	HashMap<Integer, MemoPoint> memoMap = new HashMap<Integer, MemoPoint>();
    
	MemoPoint getMemoPoint(ParsingExpression e) {
		Integer key = e.uniqueId;
		assert(e.uniqueId != 0);
		MemoPoint m = this.memoMap.get(key);
		if(m == null) {
			m = new MemoPoint(e);
			m.memoPoint = this.memoMap.size();
			this.memoMap.put(key, m);
		}
		return m;
	}

	void setMemoMatcher(ParsingExpression e, MemoMatcher m) {
		memoList.add(m);
	}

	class MemoPoint {
		ParsingExpression e;
		int memoPoint;
		int memoHit = 0;
		long hitLength = 0;
		int  maxLength = 0;
		int memoMiss = 0;
		MemoPoint(ParsingExpression e) {
			this.e = e;
		}
		final double ratio() {
			if(this.memoMiss == 0.0) return 0.0;
			return (double)this.memoHit / this.memoMiss;
		}

		final double length() {
			if(this.memoHit == 0) return 0.0;
			return (double)this.hitLength / this.memoHit;
		}

		final int count() {
			return this.memoMiss + this.memoHit;
		}

		protected boolean checkUseless() {
			if(this.memoMiss == 32) {
				if(this.memoHit < 2) {          
					return true;
				}
			}
			if(this.memoMiss % 64 == 0) {
				if(this.memoHit == 0) {
					return true;
				}
//				if(this.hitLength < this.memoHit) {
//					enableMemo = false;
//					disabledMemo();
//					return;
//				}
				if(this.memoMiss / this.memoHit > 10) {
					return true;
				}
			}
			return false;
		}

		void hit(int consumed) {
			this.memoHit += 1;
			this.hitLength += consumed;
			if(this.maxLength < consumed) {
				this.maxLength = consumed;
			}
		}
	}
	
	void exploitMemo(ParsingExpression e) {
		for(int i = 0; i < e.size(); i++) {
			exploitMemo(e.get(i));
		}
		if(!(e.matcher instanceof MemoMatcher)) {
			if(e instanceof ParsingConnector) {
				ParsingConnector ne = (ParsingConnector)e;
				MemoPoint mp = getMemoPoint(ne.inner);
				MemoMatcher m = new ConnectorMemoMatcher(ne, mp);
				memoList.add(m);
				ne.matcher = m;
			}
			if(e instanceof NonTerminal) {
				NonTerminal ne = (NonTerminal)e;
				if(ne.getRule().type == ParsingRule.LexicalRule) {
					ParsingExpression deref = Optimizer2.resolveNonTerminal(ne);
					MemoPoint mp = getMemoPoint(deref);
					MemoMatcher m = new NonTerminalMemoMatcher(ne, mp);
					memoList.add(m);
					ne.matcher = m;
				}
			}
		}
	}

	void exploitMemo(ParsingRule rule) {
//		if(NoMemo) {
//			System.out.println("skip memoiztion");
//			return;
//		}
		for(ParsingRule r : rule.subRule()) {
			exploitMemo(r.expr);
		}
	}

	void removeMemo(ParsingExpression e) {
		for(int i = 0; i < e.size(); i++) {
			removeMemo(e.get(i));
		}
		if(e.matcher instanceof MemoMatcher) {
			MemoMatcher m = (MemoMatcher)e.matcher;
			e.matcher = m.matchRef;
		}
	}

	void removeMemo(ParsingRule rule) {
		if(NoMemo) {
			
		}
		for(ParsingRule r : rule.subRule()) {
			removeMemo(r.expr);
		}
	}
	
	void show2(ParsingStatistics stat) {
		for(int i = 0; i < memoList.size() - 1; i++) {
			for(int j = i + 1; j < memoList.size(); j++) {
				if(memoList.ArrayValues[i].compareTo(memoList.ArrayValues[j]) > 0) {
					MemoMatcher m = memoList.ArrayValues[i];
					memoList.ArrayValues[i] = memoList.ArrayValues[j];
					memoList.ArrayValues[j] = m;
				}
			}
		}
		int hit = 0;
		int miss = 0;
		int unused = 0;
		int deactivated = 0;
		for(MemoMatcher m : memoList) {
			if(VerboseMemo) {
				System.out.println(m);
			}
			hit += m.memo.memoHit;
			miss += m.memo.memoMiss;
			if(!m.enableMemo) {
				deactivated +=1;
			}
			else if(m.memo.count() < 33) {
				unused += 1;
			}
		}
		if(stat != null) {
			if(VerboseMemo) {
				System.out.println("Total: " + ((double)hit / miss) + " WorstCaseBackTrack=" + stat.WorstBacktrackSize);
			}
			stat.setCount("UnusedNonTerminal", unused);
			stat.setCount("DeactivatedNonTerminal", deactivated);
		}
	}
	
	MemoTable newTable(long inputLength) {
		int size = MemoizationManager.BacktrackBufferSize;
		int rules = this.memoList.size();
		if(MemoizationManager.NoMemo || size == 0) {
			return new NoMemoTable(size, rules);
		}
		if(MemoizationManager.PackratParsing) {
			return new PackratHashTable(size, rules);
		}
		if(MemoizationManager.SlidingWindowParsing) {
			return new SlidingWindowTable(size, rules);
		}
		if(MemoizationManager.SlidingLinkedParsing) {
			return new SlidingLinkedListTable(size, rules);
		}
		return new TracingPackratTable(size, rules);
	}
	
	abstract class MemoMatcher extends ParsingMatcher {
        final MemoPoint memo;
		ParsingExpression holder = null;
		ParsingExpression key = null;
		ParsingMatcher matchRef = null;
		boolean enableMemo = true;

		MemoMatcher(MemoPoint memo) {
			this.memo = memo;
		}

		private int memoPoint() {
			return memo.memoPoint;
			//return key.uniqueId;
		}
		
		final boolean memoMatch(ParsingContext context, ParsingMatcher ma) {
			long pos = context.getPosition();
			MemoEntry m = context.getMemo(pos, memoPoint());
			if(m != null) {
				this.memo.hit(m.consumed);
				context.setPosition(pos + m.consumed);
				if(m.result != MemoizationManager.NonTransition) {
					context.left = m.result;
				}
				return !(context.isFailure());
			}
			ParsingObject left = context.left;
			boolean b = ma.simpleMatch(context);
			int length = (int)(context.getPosition() - pos);
			context.setMemo(pos, memoPoint(), (context.left == left) ? MemoizationManager.NonTransition : context.left, length);
			this.memo.memoMiss += 1;
			if(Tracing && memo.checkUseless()) {
				enableMemo = false;
				disabledMemo();
			}
			left = null;
			return b;
		}
		
		public int compareTo(MemoMatcher m) {
			if(this.memo.ratio() == m.memo.ratio()) {
				return this.memo.count() > m.memo.count() ? 1 : -1;
			}
			return (this.memo.ratio() > m.memo.ratio()) ? 1 : -1;
		}

		void disabledMemo() {
			this.holder.matcher = new DisabledMemoMatcher(this);
		}
				
		@Override
		public String toString() {
			return String.format("MEMO[%d,%s] r=%2.5f #%d len=%.2f %d %s", 
                    this.memo.memoPoint, this.enableMemo,this.memo.ratio(), 
                    this.memo.count(), this.memo.length(), this.memo.maxLength, holder);
		}
	}
	
	class ConnectorMemoMatcher extends MemoMatcher {
		int index;
		ConnectorMemoMatcher(ParsingConnector holder, MemoPoint memo) {
			super(memo);
			this.holder = holder;
			this.key = holder.inner;
			this.matchRef = holder.inner.matcher;
			this.index = holder.index;
		}
		
		@Override
		void disabledMemo() {
			this.holder.matcher = holder;
		}
		
		@Override
		public boolean simpleMatch(ParsingContext context) {
			ParsingObject left = context.left;
			int mark = context.markLogStack();
			if(this.memoMatch(context, this.matchRef)) {
				if(context.left != left) {
					//System.out.println("Linked: " + this.holder + " " + left.oid + " => " + context.left.oid);
					context.commitLog(mark, context.left);
					context.lazyLink(left, this.index, context.left);
				}
				else {
					System.out.println("FIXME nothing linked: " + this.holder + " " + left.oid + " => " + context.left.oid);
					context.abortLog(mark);					
				}
				context.left = left;
				left = null;
				return true;
			}
			context.abortLog(mark);
			left = null;
			return false;
		}
	}

	class NonTerminalMemoMatcher extends MemoMatcher {
		NonTerminalMemoMatcher(NonTerminal inner, MemoPoint memo) {
			super(memo);
			this.holder = inner;
			this.key = Optimizer2.resolveNonTerminal(inner);
			this.matchRef = key.matcher;
		}
		
		@Override
		public boolean simpleMatch(ParsingContext context) {
			return memoMatch(context, this.matchRef);
		}
	}

	class DisabledMemoMatcher extends MemoMatcher {
		DisabledMemoMatcher(MemoMatcher m) {
			super(m.memo);
			this.holder = m.holder;
			this.matchRef = m.matchRef;
			this.enableMemo = false;
		}
		
		@Override
		public boolean simpleMatch(ParsingContext context) {
			return this.matchRef.simpleMatch(context);
		}
	}

}

final class MemoEntry {
	long key = -1;
	ParsingObject result;
	int  consumed;
	int  memoPoint;
	MemoEntry next;
}

abstract class MemoTable {
	int size;
	int rules;
	int shift;
	MemoTable(int size, int rules) {
		this.size  = size;
		this.rules = rules;
		this.shift = (int)(Math.log(rules) / Math.log(2.0)) + 1;
	}
	
	int MemoStored = 0;
	int MemoUsed   = 0;
	int MemoMissed = 0;
	
	
	protected final MemoEntry newMemo() {
		if(UnusedMemo != null) {
			MemoEntry m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			m.key = -1;
			return m;
		}
		else {
			return new MemoEntry();
		}
	}
	
	private MemoEntry UnusedMemo = null;
	protected final void unusedMemo(MemoEntry m) {
		this.appendMemo2(m, UnusedMemo);
		UnusedMemo = m;
	}

//	protected final MemoEntry findTail(MemoEntry m) {
//		while(m.next != null) {
//			m = m.next;
//		}
//		return m;
//	}			

	private void appendMemo2(MemoEntry m, MemoEntry n) {
		while(m.next != null) {
			m = m.next;
		}
		m.next = n;
	}			

	protected long longkey(long pos, int memoPoint, int shift) {
		return ((pos << shift) | memoPoint) & Long.MAX_VALUE;
	}
	
	MemoEntry get(long pos, int memoPoint) { return null;}
	void put(long pos, int memoPoint, MemoEntry m) {  }

	protected void setMemo(long pos, int memoPoint, ParsingObject result, int consumed) {
		MemoEntry m = newMemo();
		m.memoPoint = memoPoint;
		m.result = result;
		m.consumed = consumed;
		m.next = this.get(pos, memoPoint);
		this.put(pos, memoPoint, m);
		this.MemoStored += 1;
	}

	protected MemoEntry getMemo(long pos, int memoPoint) {
		MemoEntry m = this.get(pos, memoPoint);
		while(m != null) {
			if(m.memoPoint == memoPoint) {
				this.MemoUsed += 1;
				return m;
			}
			m = m.next;
		}
		this.MemoMissed += 1;
		return m;
	}

	protected void stat(ParsingStatistics stat) {
		stat.setText("Memo", this.getClass().getSimpleName());
		stat.setCount("MemoUsed",    this.MemoUsed);
		stat.setCount("MemoStored",  this.MemoStored);
		stat.setRatio("Used/Stored", this.MemoUsed, this.MemoStored);
		stat.setRatio("HitRatio",    this.MemoUsed, this.MemoMissed);
	}
}

class NoMemoTable extends MemoTable {
	NoMemoTable(int size, int rules) {
		super(size, rules);
	}
}

class PackratHashTable extends MemoTable {
	protected Map<Long, MemoEntry> memoMap;
	
	PackratHashTable(int size, int rules) {
		super(size, rules);
		this.memoMap = new HashMap<Long, MemoEntry>(size);
	}
	
	@Override
	MemoEntry get(long pos, int memoPoint) { return this.memoMap.get(pos); }
	@Override
	void put(long pos, int memoPoint, MemoEntry m) { this.memoMap.put(pos, m); }
}

class SlidingWindowTable extends MemoTable {
	private MemoEntry[][] memoArray;
	private long head = 0;
	
	SlidingWindowTable(int size, int rules) {
		super(size, rules);
		this.memoArray = new MemoEntry[size][rules];
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < rules; j++) {
				this.memoArray[i][j] = new MemoEntry();
				this.memoArray[i][j].key = -1;
			}
		}
	}
	
	@Override
	protected final MemoEntry getMemo(long pos, int memoPoint) {
		if(head < pos) {
			head = pos;
		}
		if(pos - head < size) {
			int index = (int)(pos % size);
			MemoEntry m = this.memoArray[index][memoPoint];
			if(m.key == pos) {
				this.MemoUsed += 1;
				return m;
			}
		}
		this.MemoMissed += 1;
		return null;
	}
	
	@Override
	protected final void setMemo(long pos, int memoPoint, ParsingObject result, int consumed) {
		int index = (int)(pos % size);
		MemoEntry m = this.memoArray[index][memoPoint];
		m.key = pos;
		m.result = result;
		m.consumed = consumed;
		this.MemoStored += 1;
	}

	@Override
	protected final void stat(ParsingStatistics stat) {
		super.stat(stat);
	}
}

class SlidingLinkedListTable extends MemoTable {
	private MemoEntry[] memoArray;
	private long head = 0;
	
	SlidingLinkedListTable(int size, int rules) {
		super(size, rules);
		this.memoArray = new MemoEntry[size];
//		for(int i = 0; i < this.memoArray.length; i++) {
//			this.memoArray[i] = new MemoEntry();
//			this.memoArray[i].key = -1;
//		}
	}
	
	@Override
	MemoEntry get(long pos, int memoPoint) { 
		if(head < pos) {
			head = pos;
		}
		if(pos - head < memoArray.length) {
			int index = (int)(pos % memoArray.length);
			MemoEntry m = this.memoArray[index];
			if(m != null) {
				if(m.key == pos) {
					return m;
				}
				this.unusedMemo(m);
				this.memoArray[index] = null;
			}
		}
		return null;
	}
	@Override
	void put(long pos, int memoPoint, MemoEntry m) { 
		int index = (int)(pos % memoArray.length);
		this.memoArray[index] = m;
		m.key = pos;
	}

	@Override
	protected final void stat(ParsingStatistics stat) {
		super.stat(stat);
	}
}

class TracingPackratTable extends MemoTable {
	private MemoEntry[] memoArray;

	TracingPackratTable(int size, int rules) {
		super(size, rules);
		this.memoArray = new MemoEntry[size * rules + 1];
		for(int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry();
			this.memoArray[i].key = -1;
		}
	}
	
	@Override
	protected final void setMemo(long pos, int memoPoint, ParsingObject result, int consumed) {
		long key = longkey(pos, memoPoint, shift);
		int hash =  (int)(key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		m.key = key;
		m.memoPoint = memoPoint;
		m.result = result;
		m.consumed = consumed;
		this.MemoStored += 1;
	}

	@Override
	protected final MemoEntry getMemo(long pos, int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash =  (int)(key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		if(m.key == key) {
			this.MemoUsed += 1;
			return m;
		}
		this.MemoMissed += 1;
		return null;
	}

	@Override
	protected final void stat(ParsingStatistics stat) {
		super.stat(stat);
		stat.setCount("MemoSize", this.memoArray.length);
	}
}

class DebugMemo extends MemoTable {
	MemoTable m1;
	MemoTable m2;
	protected DebugMemo(MemoTable m1, MemoTable m2) {
		super(m1.size, m1.rules);
		this.m1 = m1;
		this.m2 = m2;
	}
	@Override
	protected final void setMemo(long pos, int memoPoint, ParsingObject result, int consumed) {
		this.m1.setMemo(pos, memoPoint, result, consumed);
		this.m2.setMemo(pos, memoPoint, result, consumed);
	}
	@Override
	protected final MemoEntry getMemo(long pos, int memoPoint) {
		MemoEntry o1 = this.m1.getMemo(pos, memoPoint);
		MemoEntry o2 = this.m2.getMemo(pos, memoPoint);
		if(o1 == null && o2 == null) {
			return null;
		}
		if(o1 != null && o2 == null) {
			System.out.println("diff: 1 null " + "pos=" + pos + ", e=" + memoPoint);
		}
		if(o1 == null && o2 != null) {
			System.out.println("diff: 2 null " + "pos=" + pos + ", e=" + memoPoint);
		}
		if(o1 != null && o2 != null) {
			if(o1.result != o2.result) {
				System.out.println("diff: generaetd " + "pos1=" + pos + ", p1=" + memoPoint);
			}
			if(o1.consumed != o2.consumed) {
				System.out.println("diff: consumed " + "pos1=" + pos + ", p1=" + memoPoint);
			}
		}
		return o1;
	}
}

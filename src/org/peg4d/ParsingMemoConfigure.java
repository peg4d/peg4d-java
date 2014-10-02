package org.peg4d;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParsingMemoConfigure {
	public final static ParsingObject NonTransition = new ParsingObject(null, null, 0);
	public static boolean NoMemo = false;
	public static boolean PackratParsing = false;
	public static boolean FifoPackratParsing = false;
	public static int     BacktrackDistance = 256;
	public static boolean NonTracing = false;
	public static boolean VerboseMemo = false;
	
	UList<MemoMatcher> memoList = new UList<MemoMatcher>(new MemoMatcher[16]);
	
	ParsingMemoConfigure() {
	}
	
	void exploitMemo1(ParsingExpression e) {
		for(int i = 0; i < e.size(); i++) {
			exploitMemo1(e.get(i));
		}
		if(!(e.matcher instanceof MemoMatcher)) {
			if(e instanceof ParsingConnector) {
				ParsingConnector ne = (ParsingConnector)e;
				assert(e.isUnique());
				MemoMatcher m = new ConnectorMemoMatcher(ne);
				memoList.add(m);
				ne.matcher = m;
			}
			if(e instanceof NonTerminal) {
				NonTerminal ne = (NonTerminal)e;
				assert(e.isUnique());
				if(ne.getRule().type == ParsingRule.LexicalRule) {
					MemoMatcher m = new NonTerminalMemoMatcher(ne);
					memoList.add(m);
					ne.matcher = m;
				}
			}
		}
	}

	void exploitMemo(ParsingRule rule) {
		if(!NoMemo) {
			for(ParsingRule r : rule.subRule()) {
				exploitMemo1(r.expr);
			}
		}
	}

	void show2(ParsingStat stat) {
		if(VerboseMemo) {
			for(int i = 0; i < memoList.size() - 1; i++) {
				for(int j = i + 1; j < memoList.size(); j++) {
					if(memoList.ArrayValues[i].ratio() > memoList.ArrayValues[j].ratio()) {
						MemoMatcher m = memoList.ArrayValues[i];
						memoList.ArrayValues[i] = memoList.ArrayValues[j];
						memoList.ArrayValues[j] = m;
					}
					if(memoList.ArrayValues[i].ratio() == memoList.ArrayValues[j].ratio() && memoList.ArrayValues[i].count() > memoList.ArrayValues[j].count()) {
						MemoMatcher m = memoList.ArrayValues[i];
						memoList.ArrayValues[i] = memoList.ArrayValues[j];
						memoList.ArrayValues[j] = m;
					}
				}
			}
			if(stat != null) {
				int hit = 0;
				int miss = 0;
				for(MemoMatcher m : memoList) {
					System.out.println(m);
					hit += m.memoHit;
					miss += m.memoMiss;
				}
				System.out.println("Total: " + ((double)hit / miss) + " WorstCaseBackTrack=" + stat.WorstBacktrackSize);
			}
		}
	}
	
	ParsingMemo newMemo(int rules) {
		if(ParsingMemoConfigure.NoMemo) {
			return new NoParsingMemo();
		}
		if(ParsingMemoConfigure.PackratParsing) {
			return new PackratParsingMemo(512);
		}
		if(ParsingMemoConfigure.FifoPackratParsing) {
			return new FifoPackratParsingMemo(ParsingMemoConfigure.BacktrackDistance);
		}
		return new TracingPackratParsingMemo(ParsingMemoConfigure.BacktrackDistance, rules);
	}
	
	abstract class MemoMatcher extends ParsingMatcher {
		ParsingExpression holder = null;
		ParsingExpression key = null;
		ParsingMatcher matchRef = null;
		boolean enableMemo = true;
		int memoHit = 0;
		long hitLength = 0;
		int memoMiss = 0;

		final boolean memoMatch(ParsingContext context, ParsingMatcher ma) {
			long pos = context.getPosition();
			MemoEntry m = context.getMemo(pos, key);
			if(m != null) {
				this.memoHit += 1;
				this.hitLength += m.consumed;
				context.setPosition(pos + m.consumed);
				if(m.result != ParsingMemoConfigure.NonTransition) {
					context.left = m.result;
				}
				return !(context.isFailure());
			}
			ParsingObject left = context.left;
			boolean b = ma.simpleMatch(context);
			int length = (int)(context.getPosition() - pos);
			context.setMemo(pos, key, (context.left == left) ? ParsingMemoConfigure.NonTransition : context.left, length);
			this.memoMiss += 1;
			if(!NonTracing) {
				this.tryTracing();
			}
			left = null;
			return b;
		}

		public int count() {
			return this.memoMiss + this.memoHit;
		}

		protected void tryTracing() {
			if(this.memoMiss == 32) {
				if(this.memoHit < 2) {		
					enableMemo = false;
					disabledMemo();
					return;
				}
			}
			if(this.memoMiss % 64 == 0) {
//				if(this.memoHit == 0) {
//					enableMemo = false;
//					disabledMemo();
//					return;
//				}
//				if(this.hitLength < this.memoHit) {
//					enableMemo = false;
//					disabledMemo();
//					return;
//				}
				if(this.memoMiss / this.memoHit > 10) {
					enableMemo = false;
					disabledMemo();
					return;
				}
			}
		}
		
		void disabledMemo() {
			this.holder.matcher = new DisabledMemoMatcher(this);
		}
		
		final double ratio() {
			if(this.memoMiss == 0.0) return 0.0;
			return (double)this.memoHit / this.memoMiss;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			double f = (this.memoHit == 0) ? 0.0 : (double)this.hitLength / this.memoHit;
			sb.append("MEMO[" + this.enableMemo + "] r=" + this.ratio());
			sb.append(" #" + (this.memoMiss + this.memoHit) + " len=" + f);
			sb.append(" " + holder);
			return sb.toString();
		}
	}
	
	class ConnectorMemoMatcher extends MemoMatcher {
		int index;
		ConnectorMemoMatcher(ParsingConnector holder) {
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
			int mark = context.markObjectStack();
			if(this.memoMatch(context, this.matchRef)) {
				if(context.left != left) {
					//System.out.println("Linked: " + this.holder + " " + left.oid + " => " + context.left.oid);
					context.commitLinkLog(mark, context.left);
					context.logLink(left, this.index, context.left);
				}
				else {
					System.out.println("DEBUG nothing linked: " + this.holder + " " + left.oid + " => " + context.left.oid);
					context.abortLinkLog(mark);					
				}
				context.left = left;
				left = null;
				return true;
			}
			context.abortLinkLog(mark);
			left = null;
			return false;
		}
	}

	class NonTerminalMemoMatcher extends MemoMatcher {
		NonTerminalMemoMatcher(NonTerminal inner) {
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
			this.holder = m.holder;
			this.matchRef = m.matchRef;
			this.enableMemo = false;
			this.memoHit = m.memoHit;
			this.memoMiss = m.memoMiss;
			this.hitLength = m.hitLength;
		}
		
		@Override
		public boolean simpleMatch(ParsingContext context) {
			return this.matchRef.simpleMatch(context);
		}
	}

}


final class MemoEntry {
	long key;
	ParsingObject result;
	int  consumed;
	ParsingExpression  keypeg;
	MemoEntry next;
}

abstract class ParsingMemo {
	protected final static int FifoSize = 64;
	long AssuredLength = Integer.MAX_VALUE;

	int MemoHit = 0;
	int MemoMiss = 0;
	int MemoSize = 0;
//	int statMemoSlotCount = 0;


	private MemoEntry UnusedMemo = null;

	protected final MemoEntry newMemo() {
		if(UnusedMemo != null) {
			MemoEntry m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			return m;
		}
		else {
			MemoEntry m = new MemoEntry();
//			this.memoSize += 1;
			return m;
		}
	}
	
	protected final void unusedMemo(MemoEntry m) {
		this.appendMemo2(m, UnusedMemo);
		UnusedMemo = m;
	}

	protected final MemoEntry findTail(MemoEntry m) {
		while(m.next != null) {
			m = m.next;
		}
		return m;
	}			

	private void appendMemo2(MemoEntry m, MemoEntry n) {
		while(m.next != null) {
			m = m.next;
		}
		m.next = n;
	}			

	protected abstract void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed);
	protected abstract MemoEntry getMemo(long pos, ParsingExpression keypeg);

	protected void stat(ParsingStat stat) {
		stat.setCount("MemoUsed", this.MemoHit);
		stat.setCount("MemoStored", this.MemoMiss);
		stat.setRatio("Used/Stored", this.MemoHit, this.MemoMiss);
	}
}

class NoParsingMemo extends ParsingMemo {
	@Override
	protected void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed) {
	}

	@Override
	protected MemoEntry getMemo(long pos, ParsingExpression keypeg) {
		this.MemoMiss += 1;
		return null;
	}
}

class PackratParsingMemo extends ParsingMemo {
	protected Map<Long, MemoEntry> memoMap;
	protected PackratParsingMemo(Map<Long, MemoEntry> memoMap) {
		this.memoMap = memoMap;
	}
	PackratParsingMemo(int initSize) {
		this(new HashMap<Long, MemoEntry>(initSize));
	}
	@Override
	protected final void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed) {
		MemoEntry m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.result = result;
		m.consumed = consumed;
		m.next = this.memoMap.get(pos);
		this.memoMap.put(pos, m);
	}
	@Override
	protected final MemoEntry getMemo(long pos, ParsingExpression keypeg) {
		MemoEntry m = this.memoMap.get(pos);
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

class FifoMemo extends ParsingMemo {
	protected Map<Long, MemoEntry> memoMap;
	protected long farpos = 0;
	
	protected FifoMemo(int slot) {
		this.memoMap = new LinkedHashMap<Long, MemoEntry>(slot) {  //FIFO
			private static final long serialVersionUID = 6725894996600788028L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, MemoEntry> eldest)  {
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
	protected final void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed) {
		MemoEntry m = null;
		m = newMemo();
		long key = ParsingUtils.objectId(pos, keypeg);
		m.key = key;
		m.keypeg = keypeg;
		m.result = result;
		m.consumed = consumed;
		this.memoMap.put(key, m);
		if(pos > this.farpos) {
			this.farpos = pos;
		}
	}

	@Override
	protected final MemoEntry getMemo(long pos, ParsingExpression keypeg) {
		MemoEntry m = this.memoMap.get(ParsingUtils.objectId(pos, keypeg));
		if(m != null) {
			this.MemoHit += 1;
		}
		else {
			this.MemoMiss += 1;
		}
		return m;
	}
}

class FifoPackratParsingMemo extends ParsingMemo {
	private MemoEntry[] memoArray;
	private long statSetCount = 0;
	private long statExpireCount = 0;

	FifoPackratParsingMemo(int slotSize) {
		this.memoArray = new MemoEntry[slotSize];
		for(int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry();
		}
	}
	
	@Override
	protected final void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed) {
		int index = (int)(pos % memoArray.length);
		long key = pos;
		MemoEntry m = this.memoArray[index];
		if(m.key != key) {
			m.key = key;
			m.keypeg = keypeg;
			m.result = result;
			m.consumed = consumed;
			if(m.next != null) {
				this.unusedMemo(m.next);
				m.next = null;
			}
		}
		else {
			MemoEntry m2 = newMemo();
			m2.key = key;
			m2.keypeg = keypeg;
			m2.result = result;
			m2.consumed = consumed;
			m.next = m2;
		}
	}

	@Override
	protected final MemoEntry getMemo(long pos, ParsingExpression keypeg) {
		int index = (int)(pos % memoArray.length);
		long key = pos;
		MemoEntry m = this.memoArray[index];
		if(m.key == key) {
			while(m != null) {
				if(m.keypeg == keypeg) {
					this.MemoHit += 1;
					return m;
				}
				m = m.next;
			}
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


class TracingPackratParsingMemo extends ParsingMemo {
	private MemoEntry[] memoArray;
	private long statSetCount = 0;
	private long statExpireCount = 0;

	TracingPackratParsingMemo(int distance, int rules) {
		this.memoArray = new MemoEntry[distance * rules + 1];
		for(int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry();
		}
	}
	
	@Override
	protected final void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed) {
		long key = ParsingUtils.objectId(pos, keypeg);
		int hash =  (Math.abs((int)key) % memoArray.length);
		MemoEntry m = this.memoArray[hash];
//		if(m.key != 0) {
//			long diff = keypos - PEGUtils.getpos(m.key);
//			if(diff > 0 && diff < 80) {
//				this.statExpireCount += 1;
//			}
//		}
		m.key = key;
		m.keypeg = keypeg;
		m.result = result;
		m.consumed = consumed;
	}

	@Override
	protected final MemoEntry getMemo(long pos, ParsingExpression keypeg) {
		long key = ParsingUtils.objectId(pos, keypeg);
		int hash =  (Math.abs((int)key) % memoArray.length);
		MemoEntry m = this.memoArray[hash];
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

class DebugMemo extends ParsingMemo {
	ParsingMemo m1;
	ParsingMemo m2;
	protected DebugMemo(ParsingMemo m1, ParsingMemo m2) {
		this.m1 = m1;
		this.m2 = m2;
	}
	@Override
	protected final void setMemo(long pos, ParsingExpression keypeg, ParsingObject result, int consumed) {
		this.m1.setMemo(pos, keypeg, result, consumed);
		this.m2.setMemo(pos, keypeg, result, consumed);
	}
	@Override
	protected final MemoEntry getMemo(long pos, ParsingExpression keypeg) {
		MemoEntry o1 = this.m1.getMemo(pos, keypeg);
		MemoEntry o2 = this.m2.getMemo(pos, keypeg);
		if(o1 == null && o2 == null) {
			return null;
		}
		if(o1 != null && o2 == null) {
			System.out.println("diff: 1 null " + "pos=" + pos + ", e=" + keypeg);
		}
		if(o1 == null && o2 != null) {
			System.out.println("diff: 2 null " + "pos=" + pos + ", e=" + keypeg);
		}
		if(o1 != null && o2 != null) {
			if(o1.result != o2.result) {
				System.out.println("diff: generaetd " + "pos1=" + pos + ", p1=" + keypeg);
			}
			if(o1.consumed != o2.consumed) {
				System.out.println("diff: consumed " + "pos1=" + pos + ", p1=" + keypeg);
			}
		}
		return o1;
	}
}

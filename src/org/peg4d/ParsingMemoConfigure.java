package org.peg4d;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParsingMemoConfigure {
	public final static ParsingObject NonTransition = new ParsingObject(null, null, 0);
	UMap<NonTerminal> memoMap = new UMap<NonTerminal>();
	
	ParsingMemoConfigure() {
	}
	
	void exploitMemo(ParsingExpression e) {
		for(int i = 0; i < e.size(); i++) {
			exploitMemo(e.get(i));
		}
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal)e;
			assert(e.isUnique());
			String un = ne.getUniqueName();
			NonTerminal memoed = memoMap.get(un);
			if(memoed == null) {
				System.out.println("memo: " + un);
				memoMap.put(un, ne);
				embedMemo(ne);
				exploitMemo(ne.getRule().expr);
			}
		}
	}
	
	void embedMemo(NonTerminal ne) {
//		ne.calling = Optimizer2.dereferenceNonTerminal(ne);
//		ne.matcher = ne.calling.matcher; // inlining
//		ne.matcher = new NonTerminalMemoMatcher(ne);
	}


	void show() {
		UList<String> nameList = this.memoMap.keys();
		for(int i = 0; i < nameList.size(); i++) {
			NonTerminal ne = this.memoMap.get(nameList.ArrayValues[i]);
			System.out.println(ne.matcher);
		}
	}
	
	ParsingMemo newMemo() {
		return new NoParsingMemo();
	}
}

class NonTerminalMemoMatcher implements Matcher {
	NonTerminal inner;
	Matcher matchRef;
	boolean enableMemo = true;
	int memoHit = 0;
	int memoMiss = 0;

	NonTerminalMemoMatcher(NonTerminal inner) {
		this.inner = inner;
		this.matchRef = inner.matcher;
	}
	
	@Override
	public boolean simpleMatch(ParsingContext context) {
		long pos = context.getPosition();
		MemoEntry m = context.getMemo(pos, inner);
		if(m != null) {
			this.memoHit += 1;
			context.setPosition(pos + m.consumed);
			if(m.result != ParsingMemoConfigure.NonTransition) {
				context.left = m.result;
			}
			return !(context.isFailure());
		}
		ParsingObject left = context.left;
		boolean b = this.matchRef.simpleMatch(context);
		int length = (int)(context.getPosition() - pos);
		context.setMemo(pos, inner, (context.left == left) ? ParsingMemoConfigure.NonTransition : context.left, length);
		this.memoMiss += 1;
//		this.tryTracing();
		left = null;
		return b;
	}

	private void tryTracing() {
		if(Main.TracingMemo) {
			if(this.memoMiss == 32) {
				if(this.memoHit < 2) {
					disabledMemo();
					return;
				}
			}
			if(this.memoMiss % 64 == 0) {
				if(this.memoHit == 0) {
					disabledMemo();
					return;
				}
				if(this.memoMiss / this.memoHit > 10) {
					disabledMemo();
					return;
				}
			}
		}		
	}
	
	private void disabledMemo() {
		//this.show();
		this.enableMemo = false;
//		this.base.DisabledMemo += 1;
//		int factor = this.base.EnabledMemo / 10;
//		if(factor != 0 && this.base.DisabledMemo % factor == 0) {
//			this.base.memoRemover.removeDisabled();
//		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		double f = (double)this.memoHit / this.memoMiss;
		sb.append("cache-hit: " + f);
		sb.append(" #" + this.memoMiss);
		sb.append(" " + this.inner.getRule());
		return sb.toString();
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
		stat.setCount("MemoHit", this.MemoHit);
		stat.setCount("MemoMiss", this.MemoMiss);
		stat.setRatio("Hit/Miss", this.MemoHit, this.MemoMiss);
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
	public PackratParsingMemo(int initSize) {
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
		int index = (int)(pos / memoArray.length);
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
		int index = (int)(pos / memoArray.length);
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


class OpenFifoMemo extends ParsingMemo {
	private MemoEntry[] memoArray;
	private long statSetCount = 0;
	private long statExpireCount = 0;

	OpenFifoMemo(int slotSize) {
		this.memoArray = new MemoEntry[slotSize * 111 + 1];
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

package org.peg4d;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class MemoMap {
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
	
	protected void stat(Stat stat) {
		stat.setCount("MemoHit", this.MemoHit);
		stat.setCount("MemoMiss", this.MemoMiss);
		stat.setRatio("Hit/Miss", this.MemoHit, this.MemoMiss);
	}
}

class NoMemo extends MemoMap {
	@Override
	protected void setMemo(long keypos, PExpression keypeg, ParsingObject generated, int consumed) {
	}

	@Override
	protected ObjectMemo getMemo(PExpression keypeg, long keypos) {
		this.MemoMiss += 1;
		return null;
	}
}

class PackratMemo extends MemoMap {
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


class FifoMemo extends MemoMap {
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

class OpenFifoMemo extends MemoMap {
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
	protected final void stat(Stat stat) {
		super.stat(stat);
		stat.setCount("MemoSize", this.memoArray.length);
		stat.setRatio("MemoCollision80", this.statExpireCount, this.statSetCount);
	}
}

class DebugMemo extends MemoMap {
	MemoMap m1;
	MemoMap m2;
	protected DebugMemo(MemoMap m1, MemoMap m2) {
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
				System.out.println("diff: generaetd " + "pos1=" + keypos + ", p1=" + keypeg.uniqueId);
			}
			if(o1.consumed != o2.consumed) {
				System.out.println("diff: consumed " + "pos1=" + keypos + ", p1=" + keypeg.uniqueId);
			}
		}
		return o1;
	}
}


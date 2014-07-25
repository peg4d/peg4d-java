package org.peg4d;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.peg4d.Memo.ObjectMemo;

public abstract class Memo {
	protected final static int FifoSize = 64;

	int memoHit = 0;
	int memoMiss = 0;
	int memoSize = 0;
	int statMemoSlotCount = 0;

	public final class ObjectMemo {
		ObjectMemo next;
		Peg  keypeg;
		Pego generated;
		int  consumed;
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
			this.memoSize += 1;
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

	protected abstract void setMemo(long keypos, Peg keypeg, Pego generated, int consumed);
	protected abstract ObjectMemo getMemo(Peg keypeg, long keypos);

	public final static long makekey(long pos, Peg keypeg) {
		return (pos << 24) | keypeg.uniqueId;
	}
	
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
}

class NoMemo extends Memo {
	@Override
	protected void setMemo(long keypos, Peg keypeg, Pego generated, int consumed) {
	}

	@Override
	protected ObjectMemo getMemo(Peg keypeg, long keypos) {
		return null;
	}
	
}

class PackratMemo extends Memo {
	protected Map<Long, ObjectMemo> memoMap;
	protected PackratMemo(Map<Long, ObjectMemo> memoMap) {
		this.memoMap = memoMap;
	}
	public PackratMemo(long size) {
		this(new HashMap<Long, ObjectMemo>((int)size/5 + 1));
	}
	protected final void setMemo(long keypos, Peg keypeg, Pego generated, int consumed) {
		ObjectMemo m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
		m.next = this.memoMap.get(keypos);
		this.memoMap.put(keypos, m);
	}
	protected final ObjectMemo getMemo(Peg keypeg, long keypos) {
		ObjectMemo m = this.memoMap.get(keypos);
		while(m != null) {
			if(m.keypeg == keypeg) {
				this.memoHit += 1;
				return m;
			}
			m = m.next;
		}
		this.memoMiss += 1;
		return m;
	}
}


class DirectMemo extends Memo {
	protected Map<Long, ObjectMemo> memoMap;
	
	protected DirectMemo(Map<Long, ObjectMemo> memoMap) {
		this.memoMap = memoMap;
	}

	protected final long getpos(long keypos) {
		return keypos >> 24;
	}

	protected final void setMemo(long keypos, Peg keypeg, Pego generated, int consumed) {
		ObjectMemo m = null;
		m = newMemo();
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
		this.memoMap.put(Memo.makekey(keypos, keypeg), m);
	}

	protected final ObjectMemo getMemo(Peg keypeg, long keypos) {
		ObjectMemo m = this.memoMap.get(Memo.makekey(keypos, keypeg));
		if(m != null) {
			this.memoHit += 1;
		}
		else {
			this.memoMiss += 1;
		}
		return m;
	}

}

class FastFifoMemo extends Memo {
	
	private ObjectMemo[] memoArray;
	private long arrayOffset = 0;
	
	FastFifoMemo(int slotSize) {
		this.memoArray = new ObjectMemo[slotSize * 2];
	}
	
	private ObjectMemo getA(long keypos) {
		int index = (int)(keypos - arrayOffset);
		if(index >= 0 && index < memoArray.length) {
			return memoArray[index];
		}
		return null;
	}
	private void putA(long keypos, ObjectMemo m) {
		int index = (int)(keypos - arrayOffset);
		if(index >= 0) {
			if(!(index < memoArray.length)) {
				int half = memoArray.length / 2;
				this.unusedMemos(half);
				System.arraycopy(memoArray, half, memoArray, 0, half);
				Arrays.fill(memoArray, half, memoArray.length, null);
				this.arrayOffset += half;
				index -= half;
			}
			if(index < memoArray.length) {
				m.next = this.memoArray[index];
				this.memoArray[index] = m;
			}
		}
	}
	
	private void unusedMemos(int half) {
		ObjectMemo unusedHead = null;
		for(int i = 0; i < half; i++) {
			if(this.memoArray[i] != null) {
				ObjectMemo unusedTail = this.findTail(this.memoArray[i]);
				unusedTail.next = unusedHead;
				unusedHead = this.memoArray[i];
			}
		}
		if(unusedHead != null) {
			this.unusedMemo(unusedHead);
		}
	}
	
	protected final void setMemo(long keypos, Peg keypeg, Pego generated, int consumed) {
		ObjectMemo m = newMemo();
		m.keypeg = keypeg;
		m.generated = generated;
		m.consumed = consumed;
		this.putA(keypos, m);
	}

	protected final ObjectMemo getMemo(Peg keypeg, long keypos) {
		ObjectMemo m = this.getA(keypos);
		while(m != null) {
			if(m.keypeg == keypeg) {
				this.memoHit += 1;
				return m;
			}
			m = m.next;
		}
		this.memoMiss += 1;
		return m;
	}

}

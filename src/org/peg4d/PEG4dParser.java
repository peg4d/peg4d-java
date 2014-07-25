package org.peg4d;

import org.peg4d.MemoMap.ObjectMemo;

public class PEG4dParser extends RecursiveDecentParser {

	public PEG4dParser(Grammar peg, ParserSource source) {
		super(peg, source);
	}
	
	@Override
	public void initMemo() {
		if(Main.MemoFactor == -1) {  /* default */
			this.memoMap = new OpenHashMemo(256);
		}
		else if(Main.MemoFactor == 0) {
			this.memoMap = new NoMemo(); //new PackratMemo(this.source.length());
		}
		else {
			this.memoMap = new OpenHashMemo(Main.MemoFactor);
		}
	}
	
	@Override
	public Pego matchNewObject(Pego left, PegNewObject e) {
		long pos = this.getPosition();
		ObjectMemo m = this.memoMap.getMemo(e, pos);
		if(m != null) {
			if(m.generated == null) {
				return this.refoundFailure(e, pos+m.consumed);
			}
			setPosition(pos + m.consumed);
			return m.generated;
		}
		Pego generated = super.matchNewObject(left, e);
		if(generated.isFailure()) {
			this.memoMap.setMemo(pos, e, null, (int)(generated.getSourcePosition() - pos));
		}
		else {
			this.memoMap.setMemo(pos, e, generated, (int)(this.getPosition() - pos));
		}
		return generated;
	}


}

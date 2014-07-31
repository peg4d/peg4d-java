package org.peg4d;

import org.peg4d.MemoMap.ObjectMemo;

public class PackratParser extends RecursiveDecentParser {

	public PackratParser(Grammar peg, ParserSource source) {
		super(peg, source);
	}
	
//	@Override
//	public void initMemo() {
//		if(Main.MemoFactor == -1) {  /* default */
//			this.memoMap = new PackratMemo(this.source.length());
//		}
//		else if(Main.MemoFactor == 0) {
//			this.memoMap = new NoMemo();
//		}
//		else {
//			this.memoMap = new OpenHashMemo(Main.MemoFactor);
//		}
//	}
	
//	// Bugs existed -O0, but they disappeared !!
//	public Pego matchNonTerminal0(Pego left, PegNonTerminal label) {
//		Peg next = this.getRule(label.symbol);
//		long pos = this.getPosition();
//		ObjectMemo m = this.memoMap.getMemo(label, pos);
//		if(m != null) {
//			if(m.generated == null) {
//				return this.refoundFailure(label, pos+m.consumed);
//			}
//			setPosition(pos + m.consumed);
//			return m.generated;
//		}
////		if(Main.VerboseStatCall) {
////			next.countCall(this, label.symbol, pos);
////		}
//		Pego generated = next.simpleMatch(left, this);
//		if(generated.isFailure()) {
//			this.memoMap.setMemo(pos, label, null, (int)(generated.getSourcePosition() - pos));
//		}
//		else {
//			this.memoMap.setMemo(pos, label, generated, (int)(this.getPosition() - pos));
//		}
//		return generated;
//	}

	@Override
	public Pego matchNonTerminal(Pego left, PegNonTerminal label) {
		Peg next = this.getRule(label.symbol);
		long pos = this.getPosition();
		ObjectMemo m = this.memoMap.getMemo(next, pos);
		if(m != null) {
			if(m.generated == null) {
				return this.refoundFailure(label, pos+m.consumed);
			}
			setPosition(pos + m.consumed);
			return m.generated;
		}
		if(this.stat != null) {
			this.stat.countRepeatCall(next, pos);
		}
		Pego result = next.simpleMatch(left, this);
		if(result.isFailure()) {
			this.memoMap.setMemo(pos, next, null, (int)(result.getSourcePosition() - pos));
		}
		else {
			this.memoMap.setMemo(pos, next, result, (int)(this.getPosition() - pos));
		}
		return result;
	}

}

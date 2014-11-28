package org.peg4d.regex;

import org.peg4d.ParsingObject;


public class RegSeq extends RegexObject {

	public RegSeq() {
		super(null);
	}

	public RegSeq(ParsingObject po) {
		super(po);
		this.addQuantifier(po);
	}

	public boolean contains(RegexObject obj){
		String strThis = new String();
		String strObj = new String();
		String tmp = new String();

		for(RegexObject e: this.list){
			tmp = e.getLetter();
			strThis += tmp;
		}

		for(RegexObject e: obj.getList()){
			tmp = e.getLetter();
			strObj += tmp;
		}

		return strThis.equals(strObj);
	}

	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: list) {
			sb.append(e.getLetter());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(quantifier != null) {
			sb.append("(");
		}
		for(RegexObject e: list) {
			sb.append(e.toString());
			sb.append(" ");
		}
		if(quantifier != null) {
			sb.append(")");
			sb.append(quantifier.toString());
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()) return this.quantifier.repeatRule(sb.toString());
		else return sb.toString();
	}
}

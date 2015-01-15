package org.peg4d.regex;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public abstract class RegexObject {

	protected List<RegexObject> list;
	protected ParsingObject ref;
	protected Quantifier quantifier;
	protected RegexObject parent;
	public boolean beginWith;
	public boolean endWith;
	public boolean not;

	public RegexObject(ParsingObject po) {
		this(po, null);
	}

	public RegexObject(ParsingObject po, RegexObject parent) {
		this.parent = parent;
		this.beginWith = false;
		this.endWith = false;
		this.not = false;
		this.ref = po;
		this.quantifier = null;
		this.list = new ArrayList<RegexObject>();
	}

	public void addQuantifier(ParsingObject po) {
			if(is(po.get(0), "BeginWith")) {
				beginWith = true;
			}
			if(po.size() > 2) {
				quantifier = new Quantifier(po.get(2));
			}
			if(po.size() > 3 && is(po.get(3), "EndWith")) {
				endWith = true;
			}
	}

	public Quantifier getQuantifier(){
		return this.quantifier;
	}

	public void rmQuantifier(){
		this.quantifier = null;
	}

	public void add(RegexObject e) {
		this.list.add(e);
	}

	public RegexObject get(int i) {
		return this.list.get(i);
	}

	public List<RegexObject> getList(){
		return this.list;
	}

	public int size() {
		return this.list.size();
	}

	public ParsingObject getRef(){
		return this.ref;
	}

	public String getTag(){
		return this.quantifier.getLabel();
	}

	private boolean is(ParsingObject parsingObject, String string) {
		return parsingObject.getTag().toString().equals(string);
	}

	private RegexObject pop() {
		return this.list.remove(this.list.size() - 1);
	}

	public RegexObject popHead() {
		return this.list.remove(0);
	}

	public void pushHead(RegexObject that) {
		this.list.add(0, that);
	}

	public RegexObject popContinuation() {
		RegSeq rs = new RegSeq();
		if(this.size() < 1) {
			return rs;
		}
		rs.add(this.pop());
		return rs;
	}

	public void concat(RegexObject ro) {
		if(ro instanceof RegSeq) {
			for(int i = 0; i < ro.size(); i++) {
				this.add(ro.get(i));
			}
		} else {
			this.add(ro);
		}
	}

	abstract String getLetter();
}

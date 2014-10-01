package org.peg4d.model;
//package org.peg4d;
//
//
//
//class ParsingType {
//	
//	int log;
//	
//	
//	ParsingConstructor constructor = null;
//	ParsingTag tag = null;
//	ParsingType unionType = null;
//	
//	public boolean isEmpty() {
//		if(this.constructor == null || this.tag == null || this.unionType == null) {
//			return true;
//		}
//		return false;
//	}
//
//	public ParsingType dup() {
//		ParsingType t = this.dupThis();
//		if(this.unionType != null) {
//			t.unionType = this.unionType.dup();
//		}
//		return t;
//	}
//
//	private ParsingType dupThis() {
//		ParsingType t = new ParsingType();
//		t.constructor = this.constructor;
//		t.tag = this.tag;
//		return t;
//	}
//
//	@Override
//	public final String toString() {
//		if(this.isEmpty()) {
//			return "boolean";
//		}
//		if(this.isObjectType()) {
//			return "Object";
//		}
//		return "Mutable";
//	}
//
//	public final String toString2() {
//		String t = "";
//		if(this.unionType != null) {
//			t = "|" + this.unionType;
//		}
//		if(tag == null) {
//			if(this.isObjectType()) {
//				t = "#Empty" + t;
//			}
//			else {
//				t = "<bool>" + t;
//			}
//		}
//		else {
//			t = "#"+tag.toString() + t;
//		}
//		return t;
//	}
//	
//	public void setConstructor(ParsingConstructor e) {
//		this.constructor = e;
//	}
//
//	public boolean isObjectType() {
//		return this.constructor != null;
//	}
//
//	public final boolean hasTransition(ParsingType leftType) {
//		return this.constructor != leftType.constructor;
//	}
//	
//	public void addUnionType(ParsingType u) {
//		ParsingType t = this;
//		while(t.unionType != null) {
//			assert(t != u);
//			t = t.unionType;
//		}
//		t.unionType = u;
//		//System.out.println("added union: " + u.tag + " for " + this);
//	}
//
//	public boolean hasUnionTagging(ParsingTag tag) {
//		ParsingType t = this;
//		while(t != null) {
//			if(t.tag != null && t.tag.tagId == tag.tagId) {
//				return true;
//			}
//			t = t.unionType;
//		}
//		return false;
//	}
//	
//	private boolean unionTagging = false;
//	public void enableUnionTagging() {
//		if(this.tag == null) {
//			unionTagging = true;
//		}
//	}
//	public void disableUnionTagging() {
//		unionTagging = false;
//	}
//	
//	public void addTagging(ParsingTag tag) {
//		if(this.unionTagging) {
//			this.unionTagging = false;
//			if(!this.hasUnionTagging(tag)) {
//				ParsingType u = this.dupThis();
//				u.tag = tag;
//				this.addUnionType(u);
//			}
//		}
//		else {
//			//if(this.tag == null) {
//				this.tag = tag;
//				if(this.unionType != null) {
//					this.unionType.addTagging(tag);
//				}
//			//}
//		}
//	}
//
//	public void set(int i, ParsingType leftType) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public void set(int index, ParsingType rightType, ParsingConnector e) {
//		// TODO Auto-generated method stub
//		
//	}
//
//}

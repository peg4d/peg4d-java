package org.peg4d.model;

import java.util.HashMap;

import org.peg4d.ParsingTag;

public class ParsingModel {
	String name;
	HashMap<String, ParsingTag> map = new HashMap<String, ParsingTag>();
	
	public ParsingTag get(String tagName) {
		ParsingTag tag = this.map.get(tagName);
		if(tag == null) {
			tag = new ParsingTag(tagName);
			this.map.put(tagName, tag);
		}
		return tag;
	}
}



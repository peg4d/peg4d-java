package org.peg4d;

import java.util.HashMap;

public class ParsingTag {
	private static HashMap<String, Integer> idMap = new HashMap<String, Integer>();
	private static UList<String> tagNameList = new UList<String>(new String[16]);
	
	public static int tagId(String tagName) {
		Integer tagId = ParsingTag.idMap.get(tagName);
		if(tagId == null) {
			tagId = (ParsingTag.idMap.size());
			ParsingTag.idMap.put(tagName, tagId);
			tagNameList.add(tagName);
		}
		return tagId;
	}
	public static String tagName(int tagId) {
		return tagNameList.ArrayValues[tagId];
	}

	static {
		tagId("empty");
		tagId("error");
	}
	
	static final int Name         = tagId("Name");
	static final int List         = tagId("List");
	static final int Integer      = tagId("Integer");
	static final int String       = tagId("String");
	static final int Text         = tagId("Text");
	static final int CommonError  = tagId("error");

	public ParsingTag(String tagName) {
		this.tagId = ParsingTag.tagId(tagName);
	}

	protected int tagId;

	@Override
	public final String toString() {
		return tagName(this.tagId);
	}

	public final String key() {
		return this.toString();
	}

	public ParsingTag tagging() {
		return this;
	}
}

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
	
	protected int tagId;

	public ParsingTag(String tagName) {
		this.tagId = ParsingTag.tagId(tagName);
	}

	@Override
	public final String toString() {
		return tagName(this.tagId);
	}

	public ParsingTag tagging() {
		return this;
	}
}

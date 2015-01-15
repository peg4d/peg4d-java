package org.peg4d.writer;

import java.util.TreeMap;

import org.peg4d.ParsingObject;

public class TagWriter extends ParsingWriter {
	static {
		ParsingWriter.registerExtension("tag", TagWriter.class);
	}
	@Override
	protected void write(ParsingObject po) {
		TreeMap<String,Integer> m = new TreeMap<String,Integer>();
		this.tagCount(po, m);
		for(String k : m.keySet()) {
			this.out.print("#" + k + ":" + m.get(k));
		}
		this.out.println("");
	}
	private void tagCount(ParsingObject po, TreeMap<String,Integer> m) {
		for(int i = 0; i < po.size(); i++) {
			tagCount(po.get(i), m);
		}
		String key = po.getTag().toString();
		Integer n = m.get(key);
		if(n == null) {
			m.put(key, 1);
		}
		else {
			m.put(key, n+1);
		}
	}
}

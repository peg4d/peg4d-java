package org.peg4d.data;

import java.util.ArrayList;

import org.peg4d.ParsingObject;

public class NodeData {
	private String tag                = null;
	private String value              = null;
	private int freq                  = -1;
	ArrayList<ParsingObject> nodelist = null;
	
	public NodeData(String tag, String value, ParsingObject node) {
		this.tag   = tag;
		this.value = value;
		this.freq  = 1;
		this.nodelist = new ArrayList<ParsingObject>();
		this.nodelist.add(node);
	}
	public void increment() {
		this.freq++;
	}
	public void addNode(ParsingObject node) {
		this.nodelist.add(node);
	}
	public String getTag() {
		return this.tag;
	}
	public String getValue() {
		return this.value;
	}

	public int getFrequency() {
		return this.freq;
	}
	public ArrayList<ParsingObject> getNodeList() {
		return this.nodelist;
	}
}

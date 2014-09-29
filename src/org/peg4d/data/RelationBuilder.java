package org.peg4d.data;

import org.peg4d.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Queue;

public class RelationBuilder {
	
	private HashMap<String, HashMap<String, NodeData>> datamap = null;
	private ArrayList<ParsingObject> targetlist   = null;
	private ArrayList<String>        columnfield  = null;
	private int                      columnlength = -1;
	
	public RelationBuilder() {
		datamap     = new HashMap<String, HashMap<String, NodeData>>();
		targetlist  = new ArrayList<ParsingObject>();
		columnfield = new ArrayList<String>();
	}
	
	private void buildDataMap(ParsingObject node) {
		String tag  = node.getTag().toString();
		String key  = node.getText();
		if(!this.datamap.containsKey(tag)) {
			HashMap<String, NodeData> ndm = new HashMap<String, NodeData>();
			NodeData nodedata = new NodeData(tag, key, node);
			ndm.put(key, nodedata);
			this.datamap.put(tag, ndm);
		}
		else {
			HashMap<String, NodeData> ndm = this.datamap.get(tag);
			if(!ndm.containsKey(key)) {
				NodeData nodedata = new NodeData(tag, key, node);
				ndm.put(key, nodedata);
			}
			else {
				NodeData nodedata = ndm.get(key);
				nodedata.increment();
				nodedata.addNode(node);
			}
		}
	}
	
	private void analyzeFrequency(ParsingObject root) {
		if(root == null) return;
		Queue<ParsingObject> queue = new LinkedList<ParsingObject>();
		queue.offer(root);
		while(!queue.isEmpty()) {
			ParsingObject node = queue.poll();
			if(node.size() == 0) this.buildDataMap(node);
			for(int index = 0; index < node.size(); index++) {
				queue.offer(node.get(index));
			}
		}
	}

	private int culcFrequencyAverage() {
		int counter = 0, sum = 0;
		int a[] = new int[100000000];
		for(String tag : this.datamap.keySet()) {
			HashMap<String, NodeData> nodedatamap = this.datamap.get(tag);
			for(String value : nodedatamap.keySet()) {
				NodeData nodedata = nodedatamap.get(value);
				a[counter] = nodedata.getFrequency();
				counter++;
				sum += nodedata.getFrequency();
			}
		}
		return sum / counter;
	}
	private boolean isNumber(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}
	private boolean isKeyword(String str) {
		switch(str) {
		case "null": case "from": case "to":
			return true;
		}
		return false;
	}
	
	private void nominateColumnField() {
		int average = this.culcFrequencyAverage();
		for(String tag : this.datamap.keySet()) {
			HashMap<String, NodeData> nodedatamap = this.datamap.get(tag);
			for(String value : nodedatamap.keySet()) {
				NodeData nodedata = nodedatamap.get(value);
				//System.out.println("value: " + value + ", Frequency: " + nodedata.getFrequency());
				if(nodedata.getFrequency() > average) {
					String nodevalue = nodedata.getValue();
					if(!this.isNumber(nodevalue) && !this.isKeyword(nodevalue)) {
						this.columnfield.add(nodevalue);
						//System.out.println("value: " + nodevalue + ", Frequency: " + nodedata.getFrequency());
					}
				}
			}
		}
		this.columnlength = this.columnfield.size();
	}
	
	private void analyzeAverageDepthLevel(ParsingObject node, int depthlevel, int a[]) {
		if(node == null) return;
		if(node.size() == 0) a[depthlevel]++;
		for(int i = 0; i < node.size(); i++) {
			this.analyzeAverageDepthLevel(node.get(i), depthlevel+1, a);
		}
	}
	
	private int getTargetDepth(ParsingObject root) {
		int a[] = new int[16];
		this.analyzeAverageDepthLevel(root, 1, a);
		int max      = 0;
		int maxindex = 0;
		for(int i = 0; i < a.length; i++) {
			if(max < a[i]) {
				max = a[i];
				maxindex = i;
			}
		}
		return maxindex;
	}
	
	private void getTargetDepthNode(ParsingObject node, int depth, int target) {
		if(node == null) return;
		if(depth == target) this.targetlist.add(node);
		for(int i = 0; i < node.size(); i++) {
			this.getTargetDepthNode(node.get(i), depth + 1, target);
		}
	}
	private void generateSchema() {
		StringBuilder column = new StringBuilder();
		for(int i = 0; i < this.columnfield.size(); i++) {
			column.append(this.columnfield.get(i));
			if(i != this.columnfield.size() - 1) column.append(", ");
		}
		System.out.println(column.toString());
	}

	private void schemaMatcher(ParsingObject node, HashMap<String, ArrayList<String>> map) {
		if(node == null) return;
		if(node.size() == 0 && this.columnfield.contains(node.getText())) {
			ParsingObject parent   = node.getParent();
			ArrayList<String> data = map.get(node.getText());
			for(int i = 0; i < parent.size(); i++) {
				ParsingObject cur = parent.get(i);
				if(cur.equals(node) || data.contains(cur.getText()) 
						|| this.columnfield.contains(cur.getText())) continue;
				if(cur.size() == 0) data.add(cur.getText());
			}
		}
		for(int i = 0; i < node.size(); i++) {
			this.schemaMatcher(node.get(i), map);
		}
	}

	private LinkedHashMap<String, ArrayList<String>> initFieldData() {
		LinkedHashMap<String, ArrayList<String>> map = new LinkedHashMap<String, ArrayList<String>>();
		for(int i = 0; i < this.columnfield.size(); i++) {
			String field = this.columnfield.get(i);
			ArrayList<String> data = new ArrayList<String>();
			map.put(field, data);
		}
		return map;
	}
	
	private String concatList(ArrayList<String> value) {
		String ret = "";
		for(int i = 0; i < value.size(); i++) {
			ret += value.get(i);
			if(i != value.size() - 1) ret += ",";
		}
		return ret;
	}

	private void generateCSV(LinkedHashMap<String, ArrayList<String>> fielddatamap) {
		StringBuilder csv = new StringBuilder();
		int i = 0;
		int nullfieldcounter = 0;
		for(String key : fielddatamap.keySet()) {
			ArrayList<String> valuelist = fielddatamap.get(key);
			if(valuelist.size() == 1) {
				csv.append(valuelist.get(0));
			}
			else {
				String value = (valuelist.toString().length() > 128) ? "too long" : this.concatList(valuelist);
				csv.append(value);
				if(value.equals("")) nullfieldcounter++;
			}
			String delim = (i != fielddatamap.size() - 1) ? ", " : "";
			csv.append(delim);
			i++;
		}
		if(nullfieldcounter < this.columnlength - 3) System.out.println(csv.toString());
	}
	
	public void build(ParsingObject root) {
		int targetdepth = this.getTargetDepth(root);
		this.getTargetDepthNode(root, 1, targetdepth - 3);
		for(int i = 0; i < this.targetlist.size(); i++) {
			this.analyzeFrequency(this.targetlist.get(i));
		}
		this.nominateColumnField();
		this.generateSchema();
		for(int i = 0; i < this.targetlist.size(); i++) {
			LinkedHashMap<String, ArrayList<String>> fielddatamap = this.initFieldData();;
			this.schemaMatcher(this.targetlist.get(i), fielddatamap);
			this.generateCSV(fielddatamap);
		}
	}
}

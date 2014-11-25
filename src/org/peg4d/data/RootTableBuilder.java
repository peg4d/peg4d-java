package org.peg4d.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class RootTableBuilder {
	private ArrayList<String>   schema = null;
	private Map<String, String> table  = null;
	public RootTableBuilder() {
		this.schema = new ArrayList<String>();
		this.table  = new LinkedHashMap<String, String>();
		this.initSchema();
	}

	private void initSchema() {
		this.schema.add("OBJECTID");
		this.schema.add("COLUMN");
		this.schema.add("VALUE");
	}

	private void insertDelimiter(WrapperObject node, StringBuffer sbuf, int index) {
		if (index != node.size() - 1) {
			sbuf.append("|");
		}
	}

	private void getListData(WrapperObject sibling, StringBuffer sbuf) {
		for(int i = 0; i < sibling.size(); i++) {
			if(sibling.get(i).isTerminal()) {
				sibling.get(i).visited();
				sbuf.append(sibling.get(i).getText());
			}
			else {
				sbuf.append(sibling.get(i).getTag().toString());
				sbuf.append(":");
				sbuf.append(sibling.get(i).getObjectId());
			}
			this.insertDelimiter(sibling, sbuf, i);
		}
	}

	private void getTerminalData(WrapperObject sibling, StringBuffer sbuf) {
		sbuf.append(sibling.getText());
		sibling.visited();
	}

	private void getNonTerminalData(WrapperObject sibling, StringBuffer sbuf) {
		WrapperObject grandchild = sibling.get(0);
		if(grandchild.isTerminal()) {
			sbuf.append(grandchild.getText());
		}
		else {
			sbuf.append(sibling.getTag().toString());
		}
		sbuf.append(":");
		sbuf.append(sibling.getObjectId());
	}
	
	private void settingData(WrapperObject parent, StringBuffer sbuf) {
		for(int i = 1; i < parent.size(); i++) {
			WrapperObject sibling = parent.get(i);
			if(sibling.isTerminal()) {
				this.getTerminalData(sibling, sbuf);
			}
			else if(sibling.getTag().toString().equals("List")) {
				this.getListData(sibling, sbuf);
			}
			else {
				this.getNonTerminalData(sibling, sbuf);
			}
			this.insertDelimiter(parent, sbuf, i);
		}
	}
	
	private void setTableData(WrapperObject node) {
		WrapperObject parent = node.getParent();
		String        column = node.getText();
		String        key    = String.valueOf(parent.getObjectId());
		StringBuffer  sbuf   = new StringBuffer();
		sbuf.append(column);
		sbuf.append("\t");
		this.settingData(parent, sbuf);
		this.table.put(key, sbuf.toString());
	}

	private void buildRootTable(WrapperObject node) {
		if(node == null) {
			return;
		}
		if(node.isVisitedNode()) {
			return;
		}
		if(node.isTerminal() && !node.isVisitedNode()) {
			this.setTableData(node);
		}
		for(int i = 0; i < node.size(); i++) {
			this.buildRootTable(node.get(i));
		}
	}

	private void generateRootColumns() {
		for(int i = 0; i < this.schema.size(); i++) {
			System.out.print(this.schema.get(i) + "\t");
		}
		System.out.println();
	}

	public void build(WrapperObject node) {
		this.generateRootColumns();
		this.buildRootTable(node);
		for(String key : this.table.keySet()) {
			System.out.println(key + "\t" + this.table.get(key));
		}
		System.out.println("----------------------------------");
		System.out.println();
		System.out.println();
		System.out.println();
	}
}

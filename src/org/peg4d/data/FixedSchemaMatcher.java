package org.peg4d.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FixedSchemaMatcher extends Matcher {
	private Map<String, Set<String>>                  schema    = null;
	private Map<String, ArrayList<ArrayList<String>>> table     = null;
	private CSVGenerator                              generator = null;
	private RootTableBuilder                          builder   = null;
	public FixedSchemaMatcher(Map<String, Set<String>> schema) {
		this.schema    = schema;
		this.generator = new CSVGenerator();
		this.builder   = new RootTableBuilder();
		this.initTable();
	}

	private void initTable() {
		this.table = new HashMap<String, ArrayList<ArrayList<String>>>();
		for(String column : this.schema.keySet()) {
			this.table.put(column, new ArrayList<ArrayList<String>>());
		}
	}

	public Set<String> getSchema(String tablename) {
		return this.schema.get(tablename);
	}

	public Map<String, ArrayList<ArrayList<String>>> getTable() {
		return this.table;
	}
	public Map<String, Set<String>> getSchema() {
		return this.schema;
	}

	@Override
	public void insertDelimiter(WrapperObject node, StringBuffer sbuf, int index) {
		if (sbuf.length() > 0) {
			sbuf.append("|");
		}
	}

	public String getColumnData(WrapperObject subnode, String column) {
		StringBuffer sbuf = new StringBuffer();
		for(int i = 0; i < subnode.size(); i++) {
			WrapperObject child = subnode.get(i);
			if(!child.isTerminal() && child.getTag().toString().equals(column)) {
				this.insertDelimiter(child, sbuf, i);
				sbuf.append(child.getTag().toString() + ":" + child.getObjectId());
				continue;
			}
			if(child.getTag().toString().equals(column)) {
				child.visited();
				return child.getText();
			}
		}
		return sbuf.length() > 0 ? sbuf.toString() : null;
	}

	public void getTupleData(WrapperObject subnode, String tablename) {
		ArrayList<ArrayList<String>> tabledata = this.table.get(tablename);
		ArrayList<String> columndata = new ArrayList<String>();
		Set<String> columns = this.schema.get(tablename);
		for(String column : columns) {
			if(column.equals("OBJECTID")) {
				columndata.add(String.valueOf(subnode.getObjectId()));
				continue;
			}
			else {
				String data = this.getColumnData(subnode, column);
				columndata.add(data);
			}
		}
		tabledata.add(columndata);
	}

	@Override
	public boolean isTableName(String value) {
		return false;
	}

	private void checkTargetNode(WrapperObject node) {
		String tablename = node.getTag().toString();
		if(this.table.containsKey(tablename)) {
			this.getTupleData(node, tablename);
		}
	}

	@Override
	public void matching(WrapperObject node) {
		if(node == null) {
			return;
		}
		this.checkTargetNode(node);
		for(int i = 0; i < node.size(); i++) {
			this.matching(node.get(i));
		}
	}

	@Override
	public void match(WrapperObject root) {
		this.matching(root);
		this.builder.build(root);
		this.generator.generate(this);
	}
}

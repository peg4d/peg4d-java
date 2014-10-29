package org.peg4d.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SchemaMatcher {
	private Map<String, SubNodeDataSet>               schema    = null;
	private Map<String, ArrayList<ArrayList<String>>> table     = null;
	private CSVGenerator                              generator = null;
	private RootTableBuilder                          builder   = null;
	public SchemaMatcher(Map<String, SubNodeDataSet> schema) {
		this.schema = schema;
		this.initTable();
		this.generator = new CSVGenerator();
		this.builder   = new RootTableBuilder();
	}

	private void initTable() {
		this.table = new HashMap<String, ArrayList<ArrayList<String>>>();
		for(String column : this.schema.keySet()) {
			this.table.put(column, new ArrayList<ArrayList<String>>());
		}
	}

	public Map<String, ArrayList<ArrayList<String>>> getTable() {
		return this.table;
	}
	public Map<String, SubNodeDataSet> getSchema() {
		return this.schema;
	}

	private String getColumnData(LappingObject subnode, LappingObject tablenode, String column) {
		if(subnode == null) {
			return null;
		}
		Queue<LappingObject> queue = new LinkedList<LappingObject>();
		queue.offer(subnode);
		StringBuffer sbuf = new StringBuffer();
		while(!queue.isEmpty()) {
			LappingObject node = queue.poll();
			if(node.getText().toString().equals(column)) {
				node.visited();
				LappingObject parent = node.getParent();
				for(int i = 1; i < parent.size(); i++) {
					LappingObject sibling = parent.get(i);
					sibling.visited();
					if(sibling.size() == 0) {
						String data = sibling.getText().toString();
						sbuf.append(data.replace("\n", "\\n").replace("\t", "\\t"));
					}
					else {
						if(sibling.getTag().toString().equals("List")) { //FIXME
							for (int j = 0; j < sibling.size(); j++) {
								sibling.get(j).visited();
								sbuf.append(sibling.get(j).getText().toString());
								if (j != sibling.size() - 1) {
									sbuf.append("|");
								}
							}
						}
						else {
							sibling.get(0).visited();
							String data = "";
							if (sibling.get(0).size() == 0) {
								data = sibling.get(0).getText().toString();
								sbuf.append(data.replace("\n", "\\n").replace("\t", "\\t"));
								sbuf.append(":");
								sbuf.append(sibling.getObjectId());
							} else {
								for (int j = 0; j < sibling.size(); j++) {
									LappingObject grandchild = sibling.get(j);
									if (grandchild.get(0).size() == 0) {
										sbuf.append(grandchild.get(0).getText().toString());
										sbuf.append(":");
										sbuf.append(grandchild.getObjectId());
									}
									if (j != sibling.size() - 1) {
										sbuf.append("|");
									}
								}
							}
						}
					}
					if(i != parent.size() - 1) {
						sbuf.append("|");
					}
				}
			}
			for(int index = 0; index < node.size(); index++) {
				if(!node.equals(tablenode)) {
					queue.offer(node.get(index));
				}
			}
		}
		if(sbuf.length() > 0) {
			return "[" + sbuf.toString() + "]";
		}
		else {
			return null;
		}
	}

	private void getTupleData(LappingObject subnode, LappingObject tablenode, String tablename, SubNodeDataSet columns) {
		ArrayList<ArrayList<String>> tabledata = this.table.get(tablename);
		ArrayList<String> columndata = new ArrayList<String>();
		for(String column : columns.getFinalColumnSet()) {
			if(column.equals("OBJECTID")) {
				columndata.add(String.valueOf(subnode.getObjectId()));
				continue;
			}
			else {
				String data = this.getColumnData(subnode, tablenode, column);
				columndata.add(data);
			}
		}
		tabledata.add(columndata);
	}

	private void getTupleListData(LappingObject subnode, LappingObject tablenode, String tablename, SubNodeDataSet columns) {
		LappingObject listnode = subnode.get(1);
		for (int i = 0; i < listnode.size(); i++) {
			this.getTupleData(listnode.get(i), tablenode, tablename, columns);
		}
	}

	private boolean isTableName(String value) {
		return this.schema.containsKey(value) ? true : false;
	}

	private void matching(LappingObject root) {
		if(root == null) {
			return;
		}
		Queue<LappingObject> queue = new LinkedList<LappingObject>();
		queue.offer(root);
		while(!queue.isEmpty()) {
			LappingObject parent = queue.poll();
			if(parent.size() == 0) {
				continue;
			}
			LappingObject child = parent.get(0);
			if(child.size() == 0 && this.isTableName(child.getText().toString())) {
				child.visited();
				String tablename = child.getText().toString();
				if (parent.get(1).getTag().toString().equals("List")) {
					this.getTupleListData(parent, child, tablename, this.schema.get(tablename));
				} else {
					this.getTupleData(parent, child, tablename, this.schema.get(tablename));
				}
				parent.visited();
				continue;
			}
			for(int index = 0; index < parent.size(); index++) {
				queue.offer(parent.get(index));
			}
		}
	}

	public void match(LappingObject root) {
		this.matching(root);
		this.builder.build(root);
		this.generator.generate(this);
	}
}

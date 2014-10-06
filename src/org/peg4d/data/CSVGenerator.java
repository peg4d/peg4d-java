package org.peg4d.data;

import java.util.ArrayList;
import java.util.Map;

public class CSVGenerator {
	public CSVGenerator() {

	}

	private void generateData(String tablename, SchemaMatcher matcher) {
		ArrayList<ArrayList<String>> datalist = matcher.getTable().get(tablename);
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < datalist.size(); i++) {
			ArrayList<String> line = datalist.get(i);
			for(int j = 0; j < line.size(); j++) {
				buffer.append(line.get(j));
				buffer.append(",");
				//buffer.append("\t");
			}
			buffer.append("\n");
		}
		System.out.println(buffer.toString());
		System.out.println();
	}

	private void generateColumns(String tablename, SchemaMatcher matcher) {
		SubNodeDataSet columns  = matcher.getSchema().get(tablename);
		StringBuffer   buffer   = new StringBuffer();
		for(String column : columns.getFinalColumnSet()) {
			buffer.append(column);
			buffer.append(",");
			//buffer.append("\t");
		}
		System.out.println(buffer.toString());
		System.out.println("---------------------------------------");
	}

	public void generate(SchemaMatcher matcher) {
		Map<String, ArrayList<ArrayList<String>>> table = matcher.getTable();
		for(String tablename : table.keySet()) {
			System.out.println("tablename: " + tablename);
			System.out.println("-------------------------------");
			this.generateColumns(tablename, matcher);
			this.generateData(tablename, matcher);
		}
	}
}

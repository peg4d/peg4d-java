package org.peg4d.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SchemaDecider{
	private SchemaNominator   nominatedschema   = null;
	private SchemaTypeChecker schematypechecker = null;
	private WrapperObject     root              = null;

	public SchemaDecider(SchemaNominator nominatedschema, WrapperObject root) {
		this.nominatedschema   = nominatedschema;
		this.schematypechecker = new SchemaTypeChecker();
		this.root              = root;
	}

	private boolean isSubTree(SubNodeDataSet subnodedatasetX, SubNodeDataSet subnodedatasetY) {
		return this.schematypechecker.check(this.root, subnodedatasetX, subnodedatasetY);
	}

	private ArrayList<SubNodeDataSet> NominatedSchemaTable() {
		Map<String, SubNodeDataSet> schema = this.nominatedschema.getSchema();
		ArrayList<SubNodeDataSet>   list   = new ArrayList<SubNodeDataSet>();
		for(String tablename : schema.keySet()) {
			SubNodeDataSet subnodedataset = schema.get(tablename);
			list.add(subnodedataset);
		}
		return list;
	}

	private Map<String, SubNodeDataSet> buildMap(ArrayList<SubNodeDataSet> list) {
		Map<String, SubNodeDataSet> map = new LinkedHashMap<String, SubNodeDataSet>();
		for(int i = 0; i < list.size(); i++) {
			SubNodeDataSet set = list.get(i);
			String tablename = set.getAssumedTableName();
			map.put(tablename, set);
		}
		for(String key : map.keySet()) {
			SubNodeDataSet subnodeset = map.get(key);
			Set<String> preset = subnodeset.getAssumedColumnSet();
			subnodeset.setFinalColumnSet("OBJECTID");
			subnodeset.setFinalColumnSet(preset);
		}
		return map;
	}

	private Map<String, SubNodeDataSet> defineSchema() {
		ArrayList<SubNodeDataSet> schemalist = this.NominatedSchemaTable();
		return this.buildMap(schemalist);
	}

	public Map<String, SubNodeDataSet> define() {
		return this.defineSchema();
	}
}

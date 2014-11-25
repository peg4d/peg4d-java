package org.peg4d.data;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class TreeTypeChecker {
	private Map<String, Set<String>> schema = null;
	public TreeTypeChecker() {
		this.schema = new LinkedHashMap<String, Set<String>>();
	}

	private boolean isSchemaTypeTree(WrapperObject node, String parenttag) {
		return !node.isTerminal() && !this.schema.containsKey(parenttag);
	}

	private void collectColumnSet(WrapperObject node, String parenttag) {
		Set<String> schemaset = new LinkedHashSet<String>();
		schemaset.add("OBJECTID");
		for(int i = 0; i < node.size(); i++) {
			schemaset.add(node.get(i).getTag().toString());
		}
		this.schema.put(parenttag, schemaset);
	}

	private void setSchemaMap(WrapperObject node) {
		String parenttag = node.getTag().toString();
		if(this.isSchemaTypeTree(node, parenttag)) {
			this.collectColumnSet(node, parenttag);
		}
	}

	private void checking(WrapperObject node) {
		if(node == null) {
			return;
		}
		this.setSchemaMap(node);
		for(int i = 0; i < node.size(); i++) {
			this.checking(node.get(i));
		}
	}
	
	public Map<String, Set<String>> check(WrapperObject wrapperrootnode) {
		this.checking(wrapperrootnode);
		return this.schema;
	}
}

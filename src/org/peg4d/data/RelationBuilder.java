package org.peg4d.data;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RelationBuilder {
	private ParsingObject root         = null;
	private int           segmentidpos = 0;
	private ArrayList<SubNodeDataSet> allsubnodesetlist = null;
	public RelationBuilder(ParsingObject root) {
		this.root = root;
		this.segmentidpos++;
		this.allsubnodesetlist = new ArrayList<SubNodeDataSet>();
	}

	public ArrayList<SubNodeDataSet> getSubNodeDataSetList() {
		return this.allsubnodesetlist;
	}

	static public boolean isNumber(String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void setAllSubNodeSetList(WrapperObject node, String tablename, int id) {
		SubNodeDataSet subnodeset = new SubNodeDataSet(node, tablename, id);
		subnodeset.buildAssumedColumnSet();
		if (subnodeset.getAssumedColumnSet().size() > 1) {
			this.allsubnodesetlist.add(subnodeset);
		}
	}

	private void collectListSubNode(WrapperObject node) {
		WrapperObject assumedtablenode = node.getParent().get(0);
		String tablename = assumedtablenode.getText();
		for (int i = 0; i < node.size(); i++) {
			this.setAllSubNodeSetList(node.get(i), tablename, assumedtablenode.getObjectId());
		}
	}

	private void collectNormSubNode(WrapperObject node) {
		WrapperObject assumedtablenode = node.get(0);
		String tablename = assumedtablenode.getText();
		if (!RelationBuilder.isNumber(tablename)) {
			this.setAllSubNodeSetList(node, tablename, assumedtablenode.getObjectId());
		}
	}

	private void collectAllSubNode(WrapperObject node) {
		if (node == null) {
			return;
		}
		if (node.getTag().toString().equals("List")) {
			this.collectListSubNode(node);
		} else if (!node.isTerminal() && node.get(0).isTerminal()) {
			this.collectNormSubNode(node);
		}
		for (int i = 0; i < node.size(); i++) {
			this.collectAllSubNode(node.get(i));
		}
	}

	private void buildLappingTree(ParsingObject node, WrapperObject wrappernode) {
		if (node == null) {
			return;
		}
		wrappernode.getCoord().setLpos(this.segmentidpos++);
		int size = node.size();
		if (size > 0) {
			WrapperObject[] AST = new WrapperObject[size];
			for (int i = 0; i < node.size(); i++) {
				AST[i] = new WrapperObject(node.get(i));
				AST[i].setParent(wrappernode);
				this.buildLappingTree(node.get(i), AST[i]);
			}
			wrappernode.setAST(AST);
		}
		wrappernode.getCoord().setRpos(this.segmentidpos++);
	}

	private WrapperObject preprocessing() {
		WrapperObject wrapperrootnode = new WrapperObject(this.root);
		this.buildLappingTree(this.root, wrapperrootnode);
		this.collectAllSubNode(wrapperrootnode);
		return wrapperrootnode;
	}

	private void buildInferSchema(WrapperObject wrapperrootnode) {
		SchemaNominator preschema = new SchemaNominator(this);
		preschema.nominate();
		SchemaDecider defineschema = new SchemaDecider(preschema, wrapperrootnode);
		Map<String, SubNodeDataSet> definedschema = defineschema.define();
		Matcher matcher = new SchemaMatcher(definedschema);
		matcher.match(wrapperrootnode);
	}

	private void buildFixedSchema(WrapperObject wrapperrootnode) {
		TreeTypeChecker checker = new TreeTypeChecker();
		Map<String, Set<String>> definedschema = checker.check(wrapperrootnode);
		Matcher matcher = new FixedSchemaMatcher(definedschema);
		matcher.match(wrapperrootnode);
	}

	public void build(Boolean infer) {
		WrapperObject wrapperrootnode = this.preprocessing();
		if(infer) {
			this.buildInferSchema(wrapperrootnode);
		}
		else {
			this.buildFixedSchema(wrapperrootnode);
		}
	}
}

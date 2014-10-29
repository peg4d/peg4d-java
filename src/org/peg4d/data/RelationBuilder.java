package org.peg4d.data;

import java.util.ArrayList;
import java.util.Map;

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

	private void collectAllSubNode(LappingObject node) {
		if (node == null) {
			return;
		}
		if (node.getTag().toString().equals("List")) {
			LappingObject assumedtablenode = node.getParent().get(0);
			String tablename = assumedtablenode.getText();
			for (int i = 0; i < node.size(); i++) {
				SubNodeDataSet subnodeset = new SubNodeDataSet(node.get(i), tablename, assumedtablenode.getObjectId());
				subnodeset.buildAssumedColumnSet();
				if (subnodeset.getAssumedColumnSet().size() > 1) {
					this.allsubnodesetlist.add(subnodeset);
				}
			}
		} else if (node.size() != 0 && node.get(0).size() == 0) {
			LappingObject assumedtablenode = node.get(0);
			String value = assumedtablenode.getText();
			if (!RelationBuilder.isNumber(value)) {
				SubNodeDataSet subnodeset = new SubNodeDataSet(node, value, assumedtablenode.getObjectId());
				subnodeset.buildAssumedColumnSet();
				if (subnodeset.getAssumedColumnSet().size() > 1) {
					this.allsubnodesetlist.add(subnodeset);
				}
			}
		}
		for (int i = 0; i < node.size(); i++) {
			this.collectAllSubNode(node.get(i));
		}
	}

	private void buildLappingTree(ParsingObject node, LappingObject lappingnode) {
		if (node == null) {
			return;
		}
		lappingnode.getCoord().setLpos(this.segmentidpos++);
		int size = node.size();
		if (size > 0) {
			LappingObject[] AST = new LappingObject[size];
			for (int i = 0; i < node.size(); i++) {
				AST[i] = new LappingObject(node.get(i));
				AST[i].setParent(lappingnode);
				this.buildLappingTree(node.get(i), AST[i]);
			}
			lappingnode.setAST(AST);
		}
		lappingnode.getCoord().setRpos(this.segmentidpos++);
	}

	public void build() {
		LappingObject lappingrootnode = new LappingObject(this.root);
		this.buildLappingTree(this.root, lappingrootnode);
		this.collectAllSubNode(lappingrootnode);
		SchemaNominator preschema = new SchemaNominator(this);
		preschema.nominating();
		SchemaDecider defineschema = new SchemaDecider(preschema, lappingrootnode);
		Map<String, SubNodeDataSet> definedschema = defineschema.define();
		SchemaMatcher schemamatcher = new SchemaMatcher(definedschema);
		schemamatcher.match(lappingrootnode);
	}
}

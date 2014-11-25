package org.peg4d.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SchemaNominator {
	private RelationBuilder relationbuilder    = null;
	private Map<String, SubNodeDataSet> schema = null;
	public SchemaNominator(RelationBuilder relationbuilder) {
		this.relationbuilder = relationbuilder;
		this.schema = new LinkedHashMap<String, SubNodeDataSet>();
	}

	private Set<String> calcIntersection(Set<String> setX, Set<String> setY) {
		Set<String> intersection = new LinkedHashSet<String>(setX);
		intersection.retainAll(setY);
		return intersection;
	}

	private Set<String> calcUnion(Set<String> setX, Set<String> setY) {
		Set<String> union = new LinkedHashSet<String>(setX);
		union.addAll(setY);
		return union;
	}

	private double calculatingCoefficient(SubNodeDataSet datasetX, SubNodeDataSet datasetY) {
		Set<String> setX         = datasetX.getAssumedColumnSet();
		Set<String> setY         = datasetY.getAssumedColumnSet();
		Set<String> intersection = this.calcIntersection(setX, setY);
		Set<String> union        = this.calcUnion(setX, setY);
		return (double) intersection.size() / union.size(); // coefficient
	}

	private void nominateSchema(String tablename, SubNodeDataSet nodeX, SubNodeDataSet nodeY, double coefficient) {
		Set<String> setX = nodeX.getAssumedColumnSet();
		Set<String> setY = nodeY.getAssumedColumnSet();
		if(this.schema.containsKey(tablename)) {
			this.schema.get(tablename).getAssumedColumnSet().addAll(setX);
			this.schema.get(tablename).getAssumedColumnSet().addAll(setY);
		}
		else {
			nodeX.getAssumedColumnSet().addAll(setY);
			this.schema.put(tablename, nodeX);
		}
	}

	public Map<String, SubNodeDataSet> getSchema() {
		return this.schema;
	}

	private boolean isNominatableSet(SubNodeDataSet datasetX, SubNodeDataSet datasetY) {
		Set<String> setX  = datasetX.getAssumedColumnSet();
		Set<String> setY  = datasetY.getAssumedColumnSet();
		String tablenameX = datasetX.getAssumedTableName();
		String tablenameY = datasetY.getAssumedTableName();
		return tablenameX.equals(tablenameY) && setX.size() > 0 && setY.size() > 0;
	}

	private boolean checkThreshhold(double coefficient) {
		return coefficient > 0.5 && coefficient <= 1.0;
	}

	private boolean isSubNode(Coordinate parentcoord, Coordinate subnodecoord) {
		return Coordinate.checkLtpos(parentcoord, subnodecoord) && Coordinate.checkRtpos(parentcoord, subnodecoord);
	}

	private void removing(ArrayList<SubNodeDataSet> list, Coordinate parentcoord, Coordinate subnodecoord, int pos) {
		if (this.isSubNode(parentcoord, subnodecoord)) {
			list.remove(pos);
		}
	}

	private void removeSubNodeinRemoveList(ArrayList<SubNodeDataSet> list, ArrayList<SubNodeDataSet> removelist) {
		for (int i = 0; i < removelist.size(); i++) {
			Coordinate parentcoord = removelist.get(i).getSubNode().getCoord();
			for (int j = list.size() - 1; j >= 0; j--) {
				Coordinate subnodecoord = list.get(j).getSubNode().getCoord();
				this.removing(list, parentcoord, subnodecoord, j);
			}
		}
		removelist.clear();
	}

	private void removeSubNodeinList(ArrayList<SubNodeDataSet> list, int pos) {
		Coordinate parentcoord = list.get(pos).getSubNode().getCoord();
		for (int i = list.size() - 1; i >= pos; i--) {
			Coordinate subnodecoord = list.get(i).getSubNode().getCoord();
			this.removing(list, parentcoord, subnodecoord, i);
		}
	}
	
	private int removeList(ArrayList<SubNodeDataSet> list, ArrayList<SubNodeDataSet> removelist, int pos) {
		if (list.size() > 2) {
			removelist.add(list.get(pos));
			list.remove(pos);
			return pos - 1;
		}
		return pos;
	}

	private void calcSetRelation(ArrayList<SubNodeDataSet> list, SubNodeDataSet datasetX, SubNodeDataSet datasetY, int pos) {
		String setXname  = datasetX.getAssumedTableName();
		double coefficient = this.calculatingCoefficient(datasetX, datasetY);
		if (this.checkThreshhold(coefficient)) {
			this.nominateSchema(setXname, datasetX, datasetY, coefficient);
			this.removeSubNodeinList(list, pos);
		}
	}

	private ArrayList<SubNodeDataSet> collectRemoveList(ArrayList<SubNodeDataSet> list, int i) {
		ArrayList<SubNodeDataSet> removelist = new ArrayList<SubNodeDataSet>();
		for(int j = i + 1; j < list.size(); j++) {
			SubNodeDataSet datasetX = list.get(i);
			SubNodeDataSet datasetY = list.get(j);
			if (this.isNominatableSet(datasetX, datasetY) ) {
				this.calcSetRelation(list, datasetX, datasetY, i);
				j = this.removeList(list, removelist, j);
			}
		}
		return removelist;
	}

	private void filter(ArrayList<SubNodeDataSet> list) {
		for(int i = 0; i < list.size(); i++) {
			ArrayList<SubNodeDataSet> removelist = this.collectRemoveList(list, i);
			this.removeSubNodeinRemoveList(list, removelist);
		}
	}

	public void nominate() {
		ArrayList<SubNodeDataSet> list = this.relationbuilder.getSubNodeDataSetList();
		list.sort(new SubNodeDataSet());
		this.filter(list);
	}
}
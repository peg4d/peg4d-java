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

	private double calculatiingCoefficient(Set<String> setX, Set<String> setY) {
		Set<String> intersection  = this.calcIntersection(setX, setY);
		Set<String> union         = this.calcUnion(setX, setY);
		return (double) intersection.size() / union.size(); // coefficient
	}

	private void nominateSchema(String tablename, SubNodeDataSet nodeX, SubNodeDataSet nodeY, double coefficient) {
		Set<String> setX = nodeX.getAssumedColumnSet();
		Set<String> setY = nodeY.getAssumedColumnSet();
		nodeX.setCoefficient(coefficient);
		nodeY.setCoefficient(coefficient);
		if(this.schema.containsKey(tablename)) {
			this.schema.get(tablename).getAssumedColumnSet().addAll(setX);
			this.schema.get(tablename).getAssumedColumnSet().addAll(setY);
			return;
		}
		else {
			nodeX.getAssumedColumnSet().addAll(setY);
			this.schema.put(tablename, nodeX);
		}
	}

	public Map<String, SubNodeDataSet> getSchema() {
		return this.schema;
	}

	public void nominating() {
		ArrayList<SubNodeDataSet> list = this.relationbuilder.getSubNodeDataSetList();
		list.sort(new SubNodeDataSet());
		ArrayList<SubNodeDataSet> removelist = new ArrayList<SubNodeDataSet>();
		for(int i = 0; i < list.size(); i++) {
			boolean flag = true;
			for(int j = i + 1; j < list.size(); j++) {
				Set<String> setX = list.get(i).getAssumedColumnSet();
				Set<String> setY = list.get(j).getAssumedColumnSet();
				String setXname  = list.get(i).getAssumedTableName();
				String setYname  = list.get(j).getAssumedTableName();
				if (setXname.equals(setYname) && setX.size() > 0 && setY.size() > 0) {
					double coefficient = this.calculatiingCoefficient(setX, setY);
					if (coefficient > 0.5 && coefficient <= 1.0) {
						this.nominateSchema(setXname, list.get(i), list.get(j), coefficient);
						if (flag) {
							Coordinate parentpoint = list.get(i).getSubNode().getCoord();
							for (int k = list.size() - 1; k >= 0; k--) {
								Coordinate subnodepoint = list.get(k).getSubNode().getCoord();
								if (parentpoint.getLtpos() < subnodepoint.getLtpos() 
										&& subnodepoint.getRtpos() < parentpoint.getRtpos()) {
									list.remove(k);
								}
							}
							flag = false;
						}
					}
					if (list.size() > 2) {
						removelist.add(list.get(j));
						list.remove(j);
						j = j - 1;
					}
				}
			}
			for (int j = 0; j < removelist.size(); j++) {
				Coordinate parentpoint = removelist.get(j).getSubNode().getCoord();
				for (int k = list.size() - 1; k >= 0; k--) {
					Coordinate subnodepoint = list.get(k).getSubNode().getCoord();
					if (parentpoint.getLtpos() < subnodepoint.getLtpos()
							&& subnodepoint.getRtpos() < parentpoint.getRtpos()) {
						list.remove(k);
					}
				}
			}
			removelist.clear();
		}
	}
}
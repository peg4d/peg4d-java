package org.peg4d.data;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SubNodeDataSet implements Comparator<SubNodeDataSet> {
	private LappingObject   subNode            = null;
	private String          assumedTableName   = null;
	private Set<String>     assumedColumnSet   = null;
	private Set<String>     finalColumnSet     = null;
	private int             assumedTableNodeId = -1;
	private double          Coefficient        = -1;

	public SubNodeDataSet(LappingObject subNode, String assumedTableName, int assumedTableId) {
		this.subNode          = subNode;
		this.assumedTableName = assumedTableName;
		this.assumedColumnSet = new LinkedHashSet<String>();
		this.finalColumnSet   = new LinkedHashSet<String>();
		this.assumedTableNodeId   = assumedTableId;
	}
	public SubNodeDataSet() {

	}

	@Override
	public int compare(SubNodeDataSet o1, SubNodeDataSet o2) {
		Coordinate p1 = o1.subNode.getCoord();
		Coordinate p2 = o2.subNode.getCoord();
		return p2.getRange() - p1.getRange();
	}

	public void buildAssumedColumnSet() {
		if(this.subNode == null) {
			return;
		}
		Queue<LappingObject> queue = new LinkedList<LappingObject>();
		queue.offer(this.subNode);
		while(!queue.isEmpty()) {
			LappingObject node = queue.poll();
			if(node.size() != 0 && node.get(0).size() == 0
					&& node.get(0).getObjectId() != this.assumedTableNodeId) {
				String value = node.get(0).getText();
				if (!RelationBuilder.isNumber(value)) {
					this.assumedColumnSet.add(value);
				}
			}
			for(int index = 0; index < node.size(); index++) {
				queue.offer(node.get(index));
			}
		}
	}

	public LappingObject getSubNode() {
		return this.subNode;
	}
	public String getAssumedTableName() {
		return this.assumedTableName;
	}
	public Set<String> getAssumedColumnSet() {
		return this.assumedColumnSet;
	}

	public void setCoefficient(double coefficient) {
		this.Coefficient = coefficient;
	}

	public double getCoefficient() {
		return this.Coefficient;
	}
	public void setFinalColumnSet(String headcolumn) {
		this.finalColumnSet.add(headcolumn);
	}
	public void setFinalColumnSet(Set<String> set) {
		this.finalColumnSet.addAll(set);
	}
	public Set<String> getFinalColumnSet() {
		return this.finalColumnSet;
	}
}

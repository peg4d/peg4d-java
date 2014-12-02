package org.peg4d.data;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SubNodeDataSet implements Comparator<SubNodeDataSet> {
	private WrapperObject subNode            = null;
	private String        assumedTableName   = null;
	private Set<String>   assumedColumnSet   = null;
	private Set<String>   finalColumnSet     = null;
	private int           assumedTableNodeId = -1;
	private double        Coefficient        = -1;

	public SubNodeDataSet(WrapperObject subNode, String assumedTableName, int assumedTableId) {
		this.subNode            = subNode;
		this.assumedTableName   = assumedTableName;
		this.assumedColumnSet   = new LinkedHashSet<String>();
		this.finalColumnSet     = new LinkedHashSet<String>();
		this.assumedTableNodeId = assumedTableId;
	}
	public SubNodeDataSet() {
		this.assumedColumnSet   = new LinkedHashSet<String>();
		this.finalColumnSet     = new LinkedHashSet<String>();
	}

	@Override
	public int compare(SubNodeDataSet o1, SubNodeDataSet o2) {
		Coordinate p1 = o1.subNode.getCoord();
		Coordinate p2 = o2.subNode.getCoord();
		return p2.getRange() - p1.getRange();
	}

	private boolean isTableNode(WrapperObject node) {
		return node.get(0).getObjectId() == this.assumedTableNodeId;
	}

	private boolean checkNodeRel(WrapperObject node) {
		return !node.isTerminal() && node.get(0).isTerminal();
	}

	private boolean isAssumedColumn(WrapperObject node) {
		return this.checkNodeRel(node) && !this.isTableNode(node);
	}

	private void setAssumedColumnSet(WrapperObject node) {
		if(this.isAssumedColumn(node)) {
			this.assumedColumnSet.add(node.get(0).getText());
		}
	}

	public void buildAssumedColumnSet() {
		Queue<WrapperObject> queue = new LinkedList<WrapperObject>();
		queue.offer(this.subNode);
		while(!queue.isEmpty()) {
			WrapperObject node = queue.poll();
			this.setAssumedColumnSet(node);
			for(int index = 0; index < node.size(); index++) {
				queue.offer(node.get(index));
			}
		}
	}

	public WrapperObject getSubNode() {
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

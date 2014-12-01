package org.peg4d.data;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingTag;

public class WrapperObject {
	private ParsingObject node     = null;
	private static int    idCount  = 0;
	private int           objectId = -1;
	private Coordinate    coord    = null;
	private boolean       visited  = false;
	private WrapperObject AST[]    = null;
	private WrapperObject parent   = null;
	private String        value    = null;
	private int           size     = -1;

	public WrapperObject(ParsingObject node) {
		this.objectId = idCount++;
		this.node     = node;
		this.coord    = new Coordinate();
		this.size     = node.size();
	}

	public ParsingObject getParsingObject() {
		return this.node;
	}

	public int getObjectId() {
		return this.objectId;
	}

	public Coordinate getCoord() {
		return this.coord;
	}

	public void setParent(WrapperObject parent) {
		this.parent = parent;
	}

	public WrapperObject getParent() {
		return this.parent;
	}

	public WrapperObject[] getAST() {
		return this.AST;
	}

	public void setAST(WrapperObject[] AST) {
		this.AST = AST;

	}

	public WrapperObject get(int index) {
		return this.AST[index];
	}

	public int size() {
		return this.size;
	}

	public void visited() {
		this.visited = true;
	}

	public boolean isVisitedNode() {
		return this.visited;
	}

	public ParsingTag getTag() {
		return this.node.getTag();
	}

	public String getText() {
		if(this.value == null) {
			this.value = this.node.getText();
		}
		return this.value;
	}

	public boolean isTerminal() {
		return this.size == 0;
	}
}

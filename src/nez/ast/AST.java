package nez.ast;

import java.util.AbstractList;

import nez.SourceContext;
import nez.expr.Expression;

import org.peg4d.Utils;

public class AST extends AbstractList<AST> implements Node, SourcePosition {
	private SourceContext    source;
	private Tag       tag;
	private long      pos;
	private int       length;
	private Object    value  = null;
	AST               parent = null;
	private AST       subTree[] = null;

	public AST() {
		this.tag        = Tag.tag("Text");
		this.source     = null;
		this.pos        = 0;
		this.length     = 0;
	}

	AST(Tag tag, SourceContext source, long pos) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = 0;
	}

	@Override
	public Node newNode(SourceContext source, long pos, Expression e) {
		return new AST(this.tag, source, pos);
	}

	@Override
	public Tag getTag() {
		return this.tag;
	}

	@Override
	public void setTag(Tag tag) {
		this.tag = tag;
	}

	@Override
	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public void setEndingPosition(long pos) {
		this.length = (int)(pos - this.getSourcePosition());
	}

	@Override
	public final void expandAstToSize(int newSize) {
		if(newSize > this.size()) {
			this.resizeAst(newSize);
		}
	}

	@Override
	public void commitChild(int index, Node child) {
		this.set(index, (AST)child);
	}

//	/*
//	 * duplicate object
//	 */
//	
//	public AST dup() {
//		AST n = new AST();
//		n.tag    = this.tag;
//		n.source = this.source;
//		n.pos = this.pos;
//		n.length = this.length;
//		n.value  = this.value;
//		if(this.subTree != null) {
//			n.subTree = new AST[this.size()];
//			for (int i=0; i < this.subTree.length; i++) {
//				n.subTree[i] = this.subTree[i].dup();
//				n.subTree[i].parent = this;
//			}
//		}
//		return n;
//	}	
	
//	@Override
//	protected void finalize() {
//		System.out.println("gc " + this.getSourcePosition());
//	}

	public final AST getParent() {
		return this.parent;
	}

	public SourceContext getSource() {
		return this.source;
	}

	public long getSourcePosition() {
		return this.pos;
	}

	@Override
	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatPositionLine(type, this.getSourcePosition(), msg);
	}

//	public void setSourcePosition(long pos) {
//		this.pospeg = Utils.objectId(pos, Utils.getpegid(this.pospeg));
//		assert(pos == Utils.getpos(this.pospeg));
//	}

	public int getLength() {
		return this.length;
	}
	
	public final boolean is(Tag t) {
		return this.tag == t;
	}
	
	
	public final boolean isEmptyToken() {
		return this.length == 0;
	}
	
	public final String getText() {
		if(this.value != null) {
			return this.value.toString();
		}
		if(this.source != null) {
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.getLength());
			return this.value.toString();
		}
		return "";
	}

	// subTree[]
	
	@Override
	public final int size() {
		if(this.subTree == null) {
			return 0;
		}
		return this.subTree.length;
	}

	@Override
	public final AST get(int index) {
		return this.subTree[index];
	}

	public final AST get(int index, AST defaultValue) {
		if(index < this.size()) {
			return this.subTree[index];
		}
		return defaultValue;
	}

	@Override
	public final AST set(int index, AST node) {
		AST oldValue = null;
		if(!(index < this.size())){
			this.expandAstToSize(index+1);
		}
		oldValue = this.subTree[index];
		this.subTree[index] = node;
		node.parent = this;
		return oldValue;
	}
//	
//	public final void append(AST childNode) {
//		int size = this.size();
//		this.expandAstToSize(size+1);
//		this.subTree[size] = childNode;
//		childNode.parent = this;
//	}
//	
	private void resizeAst(int size) {
		if(this.subTree == null && size > 0) {
			this.subTree = new AST[size];
		}
		else if(size == 0){
			this.subTree = null;
		}
		else if(this.subTree.length != size) {
			AST[] newast = new AST[size];
			if(size > this.subTree.length) {
				System.arraycopy(this.subTree, 0, newast, 0, this.subTree.length);
			}
			else {
				System.arraycopy(this.subTree, 0, newast, 0, size);
			}
			this.subTree = newast;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	final void stringfy(String indent, StringBuilder sb) {
		sb.append("\n");
		sb.append(indent);
		sb.append("(#");
		sb.append(this.tag.name);
		if(this.subTree == null) {
			sb.append(" ");
			Utils.formatQuoteString(sb, '\'', this.getText(), '\'');
			sb.append(")");
		}
		else {
			String nindent = "   " + indent;
			for(int i = 0; i < this.size(); i++) {
				if(this.subTree[i] == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				}
				else {
					this.subTree[i].stringfy(nindent, sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append(")");
		}
	}
	
	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}

//	public final void insert(int index, AST childNode) {
//		int oldsize = this.size();
//		if(index < oldsize) {
//			AST[] newast = new AST[oldsize+1];
//			if(index > 0) {
//				System.arraycopy(this.subTree, 0, newast, 0, index);
//			}
//			newast[index] = childNode;
//			childNode.parent = this;
//			if(oldsize > index) {
//				System.arraycopy(this.subTree, index, newast, index+1, oldsize - index);
//			}
//			this.subTree = newast;
//		}
//		else {
//			this.append(childNode);
//		}
//	}
//
//	// To implement List<T> interface
//	@Override
//	public final void add(int index, AST element) {
//		this.insert(index, element);
//	}
//
//	// To implement List<T> interface
//	@Override
//	public final boolean add(AST element) {
//		this.append(element);
//		return true;
//	}
//
//	public final void removeAt(int index) {
//		int oldsize = this.size();
//		if(oldsize > 1) {
//			AST[] newast = new AST[oldsize-1];
//			if(index > 0) {
//				System.arraycopy(this.subTree, 0, newast, 0, index);
//			}
//			if(oldsize - index > 1) {
//				System.arraycopy(this.subTree, index+1, newast, index, oldsize - index - 1);
//			}
//			this.subTree = newast;
//		}
//		else {
//			this.subTree = null;
//		}
//	}
//
//	// To implement List<T> interface
//	@Override
//	public final AST remove(int index) {
//		AST removedObject = null;
//		if(index < this.size()){
//			removedObject = this.get(index);
//		}
//		this.removeAt(index);
//		return removedObject;
//	}
//
//	// To implement List<T> interface
//	@Override
//	protected final void removeRange(int fromIndex, int toIndex) {
//		if(fromIndex >= toIndex){
//			return;
//		}
//		final int oldSize = this.size();
//		if(fromIndex < 0){
//			fromIndex = 0;
//		}
//		if(toIndex > oldSize){
//			toIndex = oldSize;
//		}
//		if(fromIndex == 0 && toIndex == oldSize){
//			this.subTree = null;
//			return;
//		}
//		final int newSize = oldSize - (toIndex - fromIndex);
//		AST[] newast = new AST[newSize];
//		if(fromIndex > 0) {
//			System.arraycopy(this.subTree, 0, newast, 0, fromIndex);
//		}
//		if(oldSize - toIndex > 1) {
//			System.arraycopy(this.subTree, toIndex, newast, fromIndex, oldSize - toIndex - 1);
//		}
//		this.subTree = newast;
//	}
//
//	// To implement List<T> interface
//	@Override
//	public final void clear() {
//		this.subTree = null;
//	}
//
//	public void replace(AST oldone, AST newone) {
//		for(int i = 0; i < this.size(); i++) {
//			if(this.subTree[i] == oldone) {
//				this.subTree[i] = newone;
//				newone.parent = this;
//			}
//		}
//	}
//
//
//	public final AST findParentNode(int tagName) {
//		AST node = this;
//		while(node != null) {
//			if(node.is(tagName)) {
//				return node;
//			}
//			node = node.parent;
//		}
//		return null;
//	}
//
//	@Override
//	public Tag getTag() {
//		return this.tag;
//	}
//
//	public final AST getPath(String path) {
//		int loc = path.indexOf('#', 1);
//		if(loc == -1) {
//			return this.getPathByTag(path);
//		}
//		else {
//			String[] paths = path.split("#");
//			Main._Exit(1, "TODO: path = " + paths.length + ", " + paths[0]);
//			return null;
//		}
//	}
//	
//	private final AST getPathByTag(String tagName) {
//		int tagId = Tag.tagId(tagName);
//		for(int i = 0; i < this.size(); i++) {
//			AST p = this.get(i);
//			if(p.is(tagId)) {
//				return p;
//			}
//		}
//		return null;
//	}
	
}


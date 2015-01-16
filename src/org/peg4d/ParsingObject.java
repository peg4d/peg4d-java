package org.peg4d;

import java.util.AbstractList;

import org.peg4d.expression.ParsingExpression;

public class ParsingObject extends AbstractList<ParsingObject> implements ParsingTree {
	private ParsingSource    source = null;
	private long             pos = 0;
	private int              length = 0;
	private ParsingTag       tag;
	private Object           value  = null;
	ParsingObject            parent = null;
	private ParsingObject    AST[] = null;

	ParsingObject(ParsingTag tag, ParsingSource source, long pos) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = 0;
	}

//	ParsingObject(ParsingTag tag, ParsingSource source, long pos, ParsingExpression e) {
////		this.oid = idCount++;
//		this.tag        = tag;
//		this.source     = source;
//		this.pospeg     = ParsingUtils.objectId(pos, (short)e.uniqueId);
//		assert(pos == ParsingUtils.getpos(this.pospeg));
//		this.length     = 0;
//	}

	public ParsingObject() {
		this.tag        = new ParsingTag("#Text");
		this.source     = null;
		this.pos        = 0;
		this.length     = 0;
	}

	@Override
	public ParsingTree newParsingTree(ParsingSource source, long pos, ParsingExpression e) {
		return new ParsingObject(this.tag, source, pos);
	}


	@Override
	public void setTag(ParsingTag tag) {
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
	public void commitChild(int index, ParsingTree child) {
		this.set(index, (ParsingObject)child);
	}

	/*
	 * duplicate object
	 */
	
	public ParsingObject dup() {
		ParsingObject n = new ParsingObject();
		n.tag    = this.tag;
		n.source = this.source;
		n.pos = this.pos;
		n.length = this.length;
		n.value  = this.value;
		if(this.AST != null) {
			n.AST = new ParsingObject[this.size()];
			for (int i=0; i < this.AST.length; i++) {
				n.AST[i] = this.AST[i].dup();
				n.AST[i].parent = this;
			}
		}
		return n;
	}	
	
//	@Override
//	protected void finalize() {
//		System.out.println("gc " + this.getSourcePosition());
//	}

	public final ParsingObject getParent() {
		return this.parent;
	}

	public ParsingSource getSource() {
		return this.source;
	}

	public long getSourcePosition() {
		return this.pos;
	}
	
//	public void setSourcePosition(long pos) {
//		this.pospeg = ParsingUtils.objectId(pos, ParsingUtils.getpegid(this.pospeg));
//		assert(pos == ParsingUtils.getpos(this.pospeg));
//	}

	public int getLength() {
		return this.length;
	}

//	public void setLength(int length) {
//		this.length = length;
//	}
	
	public final boolean is(int tagId) {
		return this.tag.tagId == tagId;
	}
	
	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatPositionLine(type, this.getSourcePosition(), msg);
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

	// AST[]
	
	@Override
	public final int size() {
		if(this.AST == null) {
			return 0;
		}
		return this.AST.length;
	}

	@Override
	public final ParsingObject get(int index) {
		return this.AST[index];
	}

	public final ParsingObject get(int index, ParsingObject defaultValue) {
		if(index < this.size()) {
			return this.AST[index];
		}
		return defaultValue;
	}

	@Override
	public final ParsingObject set(int index, ParsingObject node) {
		ParsingObject oldValue = null;
		if(index == -1) {
			this.append(node);
		}
		else {
			if(!(index < this.size())){
				this.expandAstToSize(index+1);
			}
			oldValue = this.AST[index];
			this.AST[index] = node;
			node.parent = this;
		}
		return oldValue;
	}
	
	private void resizeAst(int size) {
		if(this.AST == null && size > 0) {
			this.AST = new ParsingObject[size];
		}
		else if(size == 0){
			this.AST = null;
		}
		else if(this.AST.length != size) {
			ParsingObject[] newast = new ParsingObject[size];
			if(size > this.AST.length) {
				System.arraycopy(this.AST, 0, newast, 0, this.AST.length);
			}
			else {
				System.arraycopy(this.AST, 0, newast, 0, size);
			}
			this.AST = newast;
		}
	}

	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}

	public final void append(ParsingObject childNode) {
		int size = this.size();
		this.expandAstToSize(size+1);
		this.AST[size] = childNode;
		childNode.parent = this;
	}

	public final void insert(int index, ParsingObject childNode) {
		int oldsize = this.size();
		if(index < oldsize) {
			ParsingObject[] newast = new ParsingObject[oldsize+1];
			if(index > 0) {
				System.arraycopy(this.AST, 0, newast, 0, index);
			}
			newast[index] = childNode;
			childNode.parent = this;
			if(oldsize > index) {
				System.arraycopy(this.AST, index, newast, index+1, oldsize - index);
			}
			this.AST = newast;
		}
		else {
			this.append(childNode);
		}
	}

	// To implement List<T> interface
	@Override
	public final void add(int index, ParsingObject element) {
		this.insert(index, element);
	}

	// To implement List<T> interface
	@Override
	public final boolean add(ParsingObject element) {
		this.append(element);
		return true;
	}

	public final void removeAt(int index) {
		int oldsize = this.size();
		if(oldsize > 1) {
			ParsingObject[] newast = new ParsingObject[oldsize-1];
			if(index > 0) {
				System.arraycopy(this.AST, 0, newast, 0, index);
			}
			if(oldsize - index > 1) {
				System.arraycopy(this.AST, index+1, newast, index, oldsize - index - 1);
			}
			this.AST = newast;
		}
		else {
			this.AST = null;
		}
	}

	// To implement List<T> interface
	@Override
	public final ParsingObject remove(int index) {
		ParsingObject removedObject = null;
		if(index < this.size()){
			removedObject = this.get(index);
		}
		this.removeAt(index);
		return removedObject;
	}

	// To implement List<T> interface
	@Override
	protected final void removeRange(int fromIndex, int toIndex) {
		if(fromIndex >= toIndex){
			return;
		}
		final int oldSize = this.size();
		if(fromIndex < 0){
			fromIndex = 0;
		}
		if(toIndex > oldSize){
			toIndex = oldSize;
		}
		if(fromIndex == 0 && toIndex == oldSize){
			this.AST = null;
			return;
		}
		final int newSize = oldSize - (toIndex - fromIndex);
		ParsingObject[] newast = new ParsingObject[newSize];
		if(fromIndex > 0) {
			System.arraycopy(this.AST, 0, newast, 0, fromIndex);
		}
		if(oldSize - toIndex > 1) {
			System.arraycopy(this.AST, toIndex, newast, fromIndex, oldSize - toIndex - 1);
		}
		this.AST = newast;
	}

	// To implement List<T> interface
	@Override
	public final void clear() {
		this.AST = null;
	}

	public void replace(ParsingObject oldone, ParsingObject newone) {
		for(int i = 0; i < this.size(); i++) {
			if(this.AST[i] == oldone) {
				this.AST[i] = newone;
				newone.parent = this;
			}
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
		sb.append(this.tag.toString());
		if(this.AST == null) {
			sb.append(" ");
			Utils.formatQuoteString(sb, '\'', this.getText(), '\'');
			sb.append(")");
		}
		else {
			String nindent = "   " + indent;
			for(int i = 0; i < this.size(); i++) {
				if(this.AST[i] == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				}
				else {
					this.AST[i].stringfy(nindent, sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append(")");
		}
	}

	public final ParsingObject findParentNode(int tagName) {
		ParsingObject node = this;
		while(node != null) {
			if(node.is(tagName)) {
				return node;
			}
			node = node.parent;
		}
		return null;
	}

	public ParsingTag getTag() {
		return this.tag;
	}

	public final ParsingObject getPath(String path) {
		int loc = path.indexOf('#', 1);
		if(loc == -1) {
			return this.getPathByTag(path);
		}
		else {
			String[] paths = path.split("#");
			Main._Exit(1, "TODO: path = " + paths.length + ", " + paths[0]);
			return null;
		}
	}
	
	private final ParsingObject getPathByTag(String tagName) {
		int tagId = ParsingTag.tagId(tagName);
		for(int i = 0; i < this.size(); i++) {
			ParsingObject p = this.get(i);
			if(p.is(tagId)) {
				return p;
			}
		}
		return null;
	}

//	public final SymbolTable getSymbolTable() {
//	PegObject node = this;
//	while(node.gamma == null) {
//		node = node.parent;
//	}
//	return node.gamma;
//}
//
//public final SymbolTable getLocalSymbolTable() {
//	if(this.gamma == null) {
//		SymbolTable gamma = this.getSymbolTable();
//		gamma = new SymbolTable(gamma.getNamespace(), this);
//		// NOTE: this.gamma was set in SymbolTable constructor
//		assert(this.gamma != null);
//	}
//	return this.gamma;
//}
//
//public final BunType getType(BunType defaultType) {
//	if(this.typed == null) {
//		if(this.matched != null) {
//			this.typed = this.matched.getReturnType(defaultType);
//		}
//		if(this.typed == null) {
//			return defaultType;
//		}
//	}
//	return this.typed;
//}

	
}


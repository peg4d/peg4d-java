package org.peg4d;

public class ParsingObject {
	private ParsingSource    source = null;
	private long             pospeg = 0;
	private int              length = 0;
	private ParsingTag       tag;
	private Object           value  = null;
	ParsingObject            parent = null;
	private ParsingObject    AST[] = null;

	ParsingObject(ParsingTag tag, ParsingSource source, long pospeg) {
		this.tag        = tag;
		this.source     = source;
		this.pospeg     = pospeg;
		this.length     = 0;
	}

	ParsingObject(ParsingTag tag, ParsingSource source, long pos, ParsingExpression e) {
		this.tag        = tag;
		this.source     = source;
		this.pospeg     = ParsingUtils.objectId(pos, e);
		assert(pos == ParsingUtils.getpos(this.pospeg));
		this.length     = 0;
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
		return ParsingUtils.getpos(this.pospeg);
	}

	void setSourcePosition(long pos) {
		this.pospeg = ParsingUtils.objectId(pos, ParsingUtils.getpegid(this.pospeg));
		assert(pos == ParsingUtils.getpos(this.pospeg));
	}

	void setEndPosition(long pos) {
		this.length = (int)(pos - this.getSourcePosition());
	}

	public int getLength() {
		return this.length;
	}

	void setLength(int length) {
		this.length = length;
	}
	
	void setTag(ParsingTag tag) {
		this.tag = tag;
	}

	void setValue(Object value) {
		this.value = value;
	}
	
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
			return this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.getLength());
		}
		return "";
	}

	// AST[]
	
	public ParsingExpression getSourceExpression() {
		short pegid = ParsingUtils.getpegid(pospeg);
		if(pegid > 0 && source.peg != null) {
			return source.peg.getDefinedExpression(pegid);
		}
		return null;
	}

	private final static ParsingObject[] LazyAST = new ParsingObject[0];

	private void checkLazyAST() {
//		if(this.AST == LazyAST) {
//			PConstructor e = (PConstructor)this.getSourceExpression();
//			this.AST = null;
//			long pos = this.getSourcePosition();
//			e.lazyMatch(this, new ParserContext(source.peg, source, pos, pos+this.getLength()), pos);
//		}
	}

	boolean compactAST() {
		if(this.AST != null) {
			ParsingExpression e = this.getSourceExpression();
			if(e instanceof ParsingConstructor && !((ParsingConstructor) e).leftJoin) {
				this.AST = LazyAST;
				return true;				
			}
		}
		return this.AST == LazyAST;
	}

	
	public final int size() {
		checkLazyAST();
		if(this.AST == null) {
			return 0;
		}
		return this.AST.length;
	}

	public final ParsingObject get(int index) {
		checkLazyAST();
		return this.AST[index];
	}

	public final ParsingObject get(int index, ParsingObject defaultValue) {
		if(index < this.size()) {
			return this.AST[index];
		}
		return defaultValue;
	}

	public final void set(int index, ParsingObject node) {
		if(index == -1) {
			this.append(node);
		}
		else {
			if(!(index < this.size())){
				this.expandAstToSize(index+1);
			}
			this.AST[index] = node;
			node.parent = this;
		}
	}
	
	private void resizeAst(int size) {
		if(this.AST == null && size > 0) {
			this.AST = new ParsingObject[size];
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

	final void expandAstToSize(int newSize) {
		if(newSize > this.size()) {
			this.resizeAst(newSize);
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
		sb.append("{#");
		sb.append(this.tag.toString());
		if(this.AST == null) {
			sb.append(" ");
			ParsingCharset.formatQuoteString(sb, '\'', this.getText(), '\'');
			sb.append("}");
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
			sb.append("}");
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

//	public final SymbolTable getSymbolTable() {
//		PegObject node = this;
//		while(node.gamma == null) {
//			node = node.parent;
//		}
//		return node.gamma;
//	}
//
//	public final SymbolTable getLocalSymbolTable() {
//		if(this.gamma == null) {
//			SymbolTable gamma = this.getSymbolTable();
//			gamma = new SymbolTable(gamma.getNamespace(), this);
//			// NOTE: this.gamma was set in SymbolTable constructor
//			assert(this.gamma != null);
//		}
//		return this.gamma;
//	}
//	
//	public final BunType getType(BunType defaultType) {
//		if(this.typed == null) {
//			if(this.matched != null) {
//				this.typed = this.matched.getReturnType(defaultType);
//			}
//			if(this.typed == null) {
//				return defaultType;
//			}
//		}
//		return this.typed;
//	}
//	
//	public boolean isVariable() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//	public void setVariable(boolean flag) {
//	}

//	public final int countUnmatched(int c) {
//		for(int i = 0; i < this.size(); i++) {
//			PegObject o = this.get(i);
//			c = o.countUnmatched(c);
//		}
//		if(this.matched == null) {
//			return c+1;
//		}
//		return c;
//	}



//
//	public static Pego newPego(PegInput source, long pos, int length, int size) {
//		Pego pego = new Pego("#new", source, pos);
//		pego.length = length;
//		if(size > 0) {
//			pego.expandAstToSize(size);
//		}
//		return pego;
//	}

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
}


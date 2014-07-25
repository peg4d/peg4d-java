package org.peg4d;

public class Pego {
	public ParserSource    source = null;
	public long            startIndex = 0;
	public int             length = 0;
	public String          tag = null;
	public String          message = null;
	public Pego            parent = null;
	public Pego            AST[] = null;

	public Pego(String tag) {
		this.tag = tag;
	}

	public Pego(String tag, ParserSource source, long startIndex) {
		this.tag        = tag;
		this.source     = source;
		this.startIndex = startIndex;
		this.length     = 0;
	}

	public final boolean isFailure() {
		return (this.tag == null);
	}

	public final boolean is(String tag) {
		return this.tag.equals(tag);
	}

	public final boolean equals2(Pego o) {
		if(this != o) {
			if(this.startIndex == o.startIndex && this.length == o.length) {
				if(this.tag == null) {
					if(o.tag == null) {
						return true;
					}
				}
				else {
					if(o.tag != null && this.tag.equals(o.tag)) {
						return true;
					}
				}
			}
//			if(Main.VerbosePeg) {
				System.out.println("@@diff: " + this.startIndex + "+" + this.length + this.tag + "  " + o.startIndex + "+" + o.length + o.tag);
//			}
			return false;
		}
		return true;
	}
	
	public void setEndPosition(long endIndex) {
		this.length     = (int)(endIndex - this.startIndex);
	}

	
	public final void setSource(Peg createdPeg, ParserSource source, long startIndex) {
		this.source     = source;
		this.startIndex = startIndex;
		this.length     = 0;
	}

	public final void setSource(long startIndex, long endIndex) {
		this.startIndex = startIndex;
		this.length     = (int)(endIndex - startIndex);
	}

	
	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatErrorMessage(type, this.startIndex, msg);
	}
	
	public final boolean isEmptyToken() {
		return this.length == 0;
	}
	
	public final String getText() {
		if(this.message != null) {
			return this.message;
		}
		if(this.source != null) {
			return this.source.substring(this.startIndex, this.startIndex + this.length);
		}
		return "";
	}

	// AST[]
	
	public final int size() {
		if(this.AST == null) {
			return 0;
		}
		return this.AST.length;
	}

	public final Pego get(int index) {
		return this.AST[index];
	}

	public final Pego get(int index, Pego defaultValue) {
		if(index < this.size()) {
			return this.AST[index];
		}
		return defaultValue;
	}

	public final void set(int index, Pego node) {
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
	
	public final void resizeAst(int size) {
		if(this.AST == null && size > 0) {
			this.AST = new Pego[size];
		}
		else if(this.AST.length != size) {
			Pego[] newast = new Pego[size];
			if(size > this.AST.length) {
				System.arraycopy(this.AST, 0, newast, 0, this.AST.length);
			}
			else {
				System.arraycopy(this.AST, 0, newast, 0, size);
			}
			this.AST = newast;
		}
	}

	public final void expandAstToSize(int newSize) {
		if(newSize > this.size()) {
			this.resizeAst(newSize);
		}
	}

	public final void append(Pego childNode) {
		int size = this.size();
		this.expandAstToSize(size+1);
		this.AST[size] = childNode;
		childNode.parent = this;
	}

	public final void insert(int index, Pego childNode) {
		int oldsize = this.size();
		if(index < oldsize) {
			Pego[] newast = new Pego[oldsize+1];
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
			Pego[] newast = new Pego[oldsize-1];
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
	
	public void replace(Pego oldone, Pego newone) {
		for(int i = 0; i < this.size(); i++) {
			if(this.AST[i] == oldone) {
				this.AST[i] = newone;
				newone.parent = this;
			}
		}
	}
	
	public final int count() {
		int count = 1;
		for(int i = 0; i < this.size(); i++) {
			count = count + this.get(i).count();
		}
		return count;
	}

	public final void checkNullEntry() {
		for(int i = 0; i < this.size(); i++) {
			if(this.get(i) == null) {
				this.set(i, new Pego("#empty", this.source, this.startIndex));
			}
		}
	}

	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}
	
//	public BunType typeAt(SymbolTable gamma, int index, BunType defaultType) {
//		if(index < this.size()) {
//			PegObject node = this.AST[index];
//			if(node.typed != null) {
//				return node.typed;
//			}
//			if(node.matched == null && gamma != null) {
//				node = gamma.tryMatch(node, true);
//			}
//			if(node.matched != null) {
//				return node.matched.getReturnType(defaultType);
//			}
//		}
//		return defaultType;
//	}

	@Override
	public String toString() {
		UStringBuilder sb = new UStringBuilder();
		this.stringfy(sb);
		return sb.toString();
	}

	final void stringfy(UStringBuilder sb) {
		if(this.AST == null) {
			sb.appendNewLine("{"+ this.tag+ " ", UCharset._QuoteString('\'', this.getText(), '\''), "}");
		}
		else {
			sb.appendNewLine("");
			sb.openIndent("{" + this.tag);
			for(int i = 0; i < this.size(); i++) {
				if(this.AST[i] == null) {
					sb.appendNewLine("null");
				}
				else {
					this.AST[i].stringfy(sb);
				}
			}
			sb.closeIndent("}");
		}
	}

//	private String info() {
//		if(this.matched == null) {
//			if(this.source != null && MainOption.VerbosePeg) {
//				return "         ## by peg : " + this.createdPeg;
//			}
//			return "";
//		}
//		else {
//			return "      :: " + this.getType(null) + " by " + this.matched;
//		}
//	}

	public final Pego findParentNode(String name) {
		Pego node = this;
		while(node != null) {
			if(node.is(name)) {
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

	public final Pego getParent() {
		return this.parent;
	}

	public void setTag(String tag) {
		this.tag = tag;
		// TODO Auto-generated method stub
		
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getSourcePosition() {
		return this.startIndex;
	}

	void setSourcePosition(long pos) {
		this.startIndex = pos;
	}
	public ParserSource getSource() {
		return this.source;
	}

	public int getLength() {
		return this.length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public static Pego newSource(String tag, ParserSource source, long pos) {
		Pego pego = new Pego(tag, source, pos);
		return pego;
	}

	public static Pego newAst(Pego pego, int size) {
		pego.expandAstToSize(size);
		return pego;
	}

	public static Pego newPego(ParserSource source, long pos, int length, int size) {
		Pego pego = new Pego("#new", source, pos);
		pego.length = length;
		if(size > 0) {
			pego.expandAstToSize(size);
		}
		return pego;
	}

	public String getTag() {
		return this.tag;
	}

	public final Pego getPath(String path) {
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
	
	private final Pego getPathByTag(String tag) {
		for(int i = 0; i < this.size(); i++) {
			Pego p = this.get(i);
			if(p.is(tag)) {
				return p;
			}
		}
		return null;
	}

}


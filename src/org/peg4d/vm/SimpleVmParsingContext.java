package org.peg4d.vm;


//public class SimpleVmParsingContext extends ParsingContext {
//	
//	public SimpleVmParsingContext(ParsingObject left, ParsingSource s, long pos) {
//		super(s);
//		this.left = left; // FIXME
//		this.pos = pos;
//	}
//	
//	public void opMatchCharset(byte[] bdata) {
//		try {
//			String text = new String(bdata, "UTF-8");
//			ParsingCharset u = ParsingCharset.newParsingCharset(text);
//			this.opMatchCharset(u);
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	@Override
//	public void opConnectObject(int index) {
//		ParsingObject parent = this.opop();
//		parent.setLength(parent.getLength() + this.left.getLength());
//		parent.set(parent.size(), this.left);
//		this.left = parent;
//	}
//	
//	public void opNewObject() {
//		if(this.canTransCapture()) {
//			ParsingModel model = new ParsingModel();
//			this.left = new ParsingObject(model.get("#empty"), this.source, this.pos);
//		}
//	}
//	
//	public void opTagging(Opcode op) {
//		if(this.canTransCapture()) {
//			String tagName;
//			try {
//				tagName = new String(op.bdata, "UTF-8");
//				ParsingTag tag = new ParsingTag(tagName);
//				this.left.setTag(tag.tagging());
//				this.left.setSourcePosition(lstack[lstacktop - 1]);
//				this.left.setLength((int) (this.pos - this.lstack[this.lstacktop - 1]));
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//}

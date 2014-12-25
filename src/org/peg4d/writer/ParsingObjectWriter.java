package org.peg4d.writer;

import org.peg4d.ParsingObject;
import org.peg4d.Utils;

public class ParsingObjectWriter extends ParsingWriter {
	
	@Override
	protected void write(ParsingObject po) {
		this.writePego(po, "");
		this.out.println();
	}

	private void writePego(ParsingObject po, String indent) {
		this.out.println();
		this.out.print(indent);
		this.out.print("(#" + po.getTag().toString()); 
		if(po.size() == 0) {
			this.out.print(" "); 
			this.out.print(Utils.quoteString('\'', po.getText(), '\''));
			this.out.print(")");
		}
		else {
			String nindent = "  " + indent;
			for(int i = 0; i < po.size(); i++) {
				this.writePego(po.get(i), nindent);
			}
			this.out.println();
			this.out.print(indent);
			this.out.print(")");
		}
	}
}

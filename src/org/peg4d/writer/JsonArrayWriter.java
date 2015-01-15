package org.peg4d.writer;

import org.peg4d.ParsingObject;
import org.peg4d.Utils;

public class JsonArrayWriter extends ParsingWriter {
	
	static {
		ParsingWriter.registerExtension("json", JsonArrayWriter.class);
	}

	@Override
	protected void write(ParsingObject po) {
		this.writePego(po);
		this.out.println();
	}

	private void writePego(ParsingObject po) {
		this.out.print("[\"#" + po.getTag().toString()+"\","); 
		if(po.size() == 0) {
			this.out.print(Utils.quoteString('\"', po.getText(), '\"'));
		}
		else {
			for(int i = 0; i < po.size(); i++) {
				this.writePego(po.get(i));
				if(i + 1 < po.size()) {
					this.out.print(",");
				}
			}
		}
		this.out.print("]");
	}
}

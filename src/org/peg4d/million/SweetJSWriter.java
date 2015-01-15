package org.peg4d.million;

import org.peg4d.ParsingObject;
import org.peg4d.writer.JsonArrayWriter;
import org.peg4d.writer.ParsingWriter;

public class SweetJSWriter extends ParsingWriter {
	
	static {
		ParsingWriter.registerExtension("js", SweetJSWriter.class);
	}
	
	@Override
	protected void write(ParsingObject po) {
		SourceGenerator generator = new SweetJSGenerator();
		generator.visit(po);
		this.out.println(generator.toString());

	}

}

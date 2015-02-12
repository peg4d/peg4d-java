package org.peg4d.fsharp;

import org.peg4d.ParsingObject;
import org.peg4d.million.SourceGenerator;
import org.peg4d.writer.JsonArrayWriter;
import org.peg4d.writer.ParsingWriter;

public class FSharpWriter extends ParsingWriter {
	
	static {
		ParsingWriter.registerExtension("fs", FSharpWriter.class);
	}
	
	@Override
	protected void write(ParsingObject po) {
		SourceGenerator generator = new FSharpGenerator();
		generator.visit(po);
		this.out.println(generator.toString());
	}

}

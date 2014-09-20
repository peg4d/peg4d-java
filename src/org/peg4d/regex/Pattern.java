package org.peg4d.regex;

import org.peg4d.Grammar;
import org.peg4d.GrammarFactory;

public class Pattern {
	
	/*
	 * $
	 * ^(ABC|ABC|ABC)
	 */
	
	public final static void main(String[] a) {
		GrammarFactory factory = new GrammarFactory();
		Grammar peg = factory.newGrammar("name", "filename");
//		peg.splitFile(fileName, 64KB);

//		
//		Grammar peg = factory.newGrammar("name");
//		peg.importGramamr("File");
//		peg.importGramamr("File");
//		peg.updateGrammar("Name = name");
//
//		s = ParsingSource.newInputstream(in);
//		Grammar.newInterator(fileName, f);
//		Grammar.newIterator(in, "Chunk");
//		peg.parseFile(fileName);
//		peg.parseFile(in);
//		peg.splitFile(file);
//		
//		Grammar.parse(in);
//		
//		Iterator<ParsingObject> itr = new ParsingObject<>
//		peg.parseFile(s);
//		peg.parseChunk(s);
//		
//		p = ParsingSource.newInputStream(in);
//		
//		Chunk data = p.parseChunk(p);
//		
//		Grammar.getRule();
//		
//		ParsingSource.match(s, Rule);
//		ParsingSource.indexOf(s, peg, Rule);
//		ParsingSource.split(s, Rule);
//		
//		ParsingObject p = peg.parseFile("string");
//		ParsingObject p = peg.parseChunk("string");
//
//		p.parse("string");
//
//		ParsingInputStream ins = new ParsingInputStream();
//
//		ParsingObject p = peg.parse("Name", "string");
//		boolean = peg.match("Name", "string");
//		p.parse("string");
//		ins.parse
	}

}

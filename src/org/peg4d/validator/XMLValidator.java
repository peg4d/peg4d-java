package org.peg4d.validator;

import org.peg4d.Grammar;
import org.peg4d.GrammarFactory;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class XMLValidator {
	private final String pegForDTD = "forValidation/xml_dtd.peg";
	private String DTDFile;
	private String inputXMLFile;
	private boolean result;

	public XMLValidator(String DTDFile, String inputXMLFile) {
		this.DTDFile = DTDFile;
		this.inputXMLFile = inputXMLFile;
		this.result = false;
	}

	public boolean run() {
		GrammarFactory dtdGrammarFactory = new GrammarFactory();
		Grammar peg4d = dtdGrammarFactory.newGrammar("DTD", pegForDTD);
		ParsingSource dtdSource = ParsingSource.loadSource(DTDFile);
		ParsingContext dtdContext = new ParsingContext(dtdSource);
		ParsingObject node = dtdContext.parse(peg4d, "File");
		XMLPegGenerater gen = new XMLPegGenerater(node);
		String genPegSource = gen.generatePegFile();
		GrammarFactory xmlGrammarFactory = new GrammarFactory();
		Grammar genPeg = xmlGrammarFactory.newGrammar("XML", genPegSource);
		ParsingSource xmlSource = ParsingSource.loadSource(inputXMLFile);
		ParsingContext xmlContext = new ParsingContext(xmlSource);
		ParsingObject xmlNode = xmlContext.parse(genPeg, "File");
		return !xmlContext.hasByteChar();
	}

	public boolean getResult() {
		return this.result;
	}

}

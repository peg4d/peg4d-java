package org.peg4d.regex;


public class RegNonTerminal extends RegexObject {

	private String label;
	public RegNonTerminal(String label) {
		super(null); //FIXME
		this.label = label;
	}

	public String getLetter() {
		return "";
	}

	@Override
	public String toString() {
		return label;
	}

}

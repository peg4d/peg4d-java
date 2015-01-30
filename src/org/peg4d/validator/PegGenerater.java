package org.peg4d.validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nez.util.UMap;

import org.peg4d.Main;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class PegGenerater {
	ParsingObject node;

	public PegGenerater(ParsingObject node) {
		this.node = node;
	}
	
	public String generatePegFile() {
		return null;
	}

	private ParsingSource generate(ParsingSource source, ParsingObject node, int index) {
		return null;
	}
}

class XMLPegGenerater extends PegGenerater {
	int arrayCount = 0;
	UMap<Integer> NameMap = new UMap<Integer>();
	UMap<Integer> AttMap = new UMap<Integer>();

	public XMLPegGenerater(ParsingObject node) {
		super(node);
	}

	@Override
	public String generatePegFile() {
		String generatedSource = loadSource("forValidation/rootXml.peg");
		this.getElementName(this.node);
		generatedSource = this.generate(generatedSource, this.node, 0);
		String generatedFilePath = "forValidation/generatedXml.peg";
		File newFile = new File(generatedFilePath);
		try {
			newFile.createNewFile();
			File file = new File(generatedFilePath);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(generatedSource);
			fileWriter.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		return generatedFilePath;
	}
	
	private String loadSource(String fileName) {
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				File f = new File(fileName);
//				if(f.length() > 128 * 1024) {
//					return new FileSource(fileName);
//				}
				Stream = new FileInputStream(fileName);
			} catch (IOException e) {
				Main._Exit(1, "file error: " + fileName);
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			return builder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}

	private final String generate(String source, ParsingObject node, int index) {
		int count = 0;
		for (int i = 0; i < node.size(); i++) {
			switch (node.get(i).getTag().toString()) {
			case "element":
				index = this.NameMap.get(node.get(i).get(0).getText());
				if (this.AttMap.hasKey(node.get(i).get(0).getText())) {
					if (node.get(i).size() == 3) {
						source += "Element" + index + " = { _* '<' @ElementName" + index + " _+ @Attribute" + index + " _* ( '/>' / '>' _* "
								+ "@Members"
								+ index + node.get(i).get(2).getText() + " _* '</' ELEMENTNAME"
								+ index + "'>' ) _* #element }\n\n";
					} else {
						source += "Element" + index + " = { _* '<' @ElementName" + index + " _+ @Attribute" + index + " _* ( '/>' / '>' _* "
								+ "@Members"
								+ index + " _* '</' ELEMENTNAME" + index
								+ "'>' ) _* #element }\n\n";
					}
				}
				else{
					if (node.get(i).size() == 3) {
						source += "Element" + index + " = { _* '<' @ElementName"
 + index
								+ " _* ( '/>' / '>' _* " + "(@Members" + index + ")"
								+ node.get(i).get(2).getText() + " _* '</' ELEMENTNAME"
								+ index
								+ "'>' ) _* #element }\n\n";
					} else {
						source += "Element" + index + " = { _* '<' @ElementName"
 + index
								+ " _* ( '/>' / '>' _* " + "@Members" + index
								+ " _* '</' ELEMENTNAME" + index + "'>' ) _* #element }\n\n";
					}
				}
				source = generate(source, node.get(i), index);
				break;

			case "elementName":
				source += "ElementName" + index + " = { \"" + node.get(i).getText()
						+ "\" #string }\n\n";
				source += "ELEMENTNAME" + index + " =  \"" + node.get(i).getText() + "\"\n\n";
				break;

			case "member":
				source = generate(source, node.get(i), index);
				source += "Members" + index + " = {";
				for (int j = 0; j < node.get(i).size() - 1; j++) {
					if (node.get(i).size() >= j + 2
							&& node.get(i).get(j + 1).getTag().toString().equals("or")) {
						source += " @Member" + index + "_" + j + " /";
						j++;
					}
 else {
						source += " @Member" + index + "_" + j;
					}
				}
				source += " @Member" + index + "_" + (node.get(i).size() - 1) + "}\n\n";
				break;

			case "others":
				source += "Members" + index + " =";
				if (node.get(i).getText().equals("EMPTY")) {
					source += " Empty \n\n";
				} else if (node.get(i).getText().equals("ANY")) {
					source += " Any \n\n";
				}
				break;

			case "docTypeName": // top of DTD
				source += "Element0 = _*  Member0 _* \n\n";
				source += "Member0 = { @Element1 #member }\n\n";
				break;

			case "memberName":
				if (node.get(i).size() == 1) {
					source += "Member" + index + "_" + count + " = { @Element"
							+ this.NameMap.get(node.get(i).get(0).getText())
							+ " #member}\n\n";
				} else if (node.get(i).size() == 2) {
					source += "Member" + index + "_" + count + " = { (@Element"
							+ this.NameMap.get(node.get(i).get(0).getText()) + ")"
							+ node.get(i).get(1).getText() + " #member}\n\n";
				}
				count++;
				break;

			case "data":
				if (node.size() > 1) {
					source += "Member" + index + "_" + count + " = { CHARDATA #data }\n\n";
				}
				else{
					source += "Member" + index + "_" + count + " = { CHARDATA? #data }\n\n";
				}
				count++;
				break;

			case "attlist":
				source = generate(source, node.get(i), index);
				source += "Attribute" + index + " ={";
				for (int j = 0; j <= node.get(i).size() - 2; j++) {
					if (node.get(i).get(j + 1).get(2).get(0).getText()
							.equals("#IMPLIED")) {
						source += "  (@AttParameter" + index + "_" + j + ")? _* #attribute";
					} else {
						source += "  @AttParameter" + index + "_" + j + " _* #attribute";
					}
				}
				source += "}\n\n";
				break;

			case "attParameter":
				if (node.get(i).get(2).get(0).getText().equals("#IMPLIED")) {
					source += "AttParameter" + index + "_" + count + " = { @AttName" + index + "_"
							+ count + " '=' (@String)? #attPara } \n\n";
				} else {
					source += "AttParameter" + index + "_" + count + " = { @AttName" + index + "_"
							+ count + " '=' @String #attPara } \n\n";
				}
				source += "AttName" + index + "_" + count + " = { '"
 + node.get(i).get(0).getText()
						+ "' #attName } \n\n";
				count++;
				break;

			case "or":
				count++;
				break;
			}
		}
		return source;
	}


	public final void getElementName(ParsingObject node) {
		for (int i = 0; i < node.size(); i++) {
			if (node.get(i).getTag().toString().equals("docTypeName")) {
				this.NameMap.put(node.get(i).getText(), 0);
			} else if (node.get(i).getTag().toString().equals("attlist")) {
				this.AttMap.put(node.get(i).get(0).getText(), i);
			} else if (node.get(i).getTag().toString().equals("element")) {
				this.NameMap.put(node.get(i).get(0).getText(), i);
			}
		}
	}
}

package org.peg4d.validator;

import org.peg4d.ParsingObject;



public class Main {
	private final static String defaultStartPoint = "Toplevel";
	private static String Command = null;
	private static String SchemaFile = null;
	private static String FileFormat = null;
	private static String InputFile = null;
	private static boolean XMLMode = false;
	private static boolean JSONMode = false;


	public final static void main(String[] args) {
		ParsingObject resultNode = null;
		parseCommandOption(args);
		if (XMLMode) {
			XMLValidator validator = new XMLValidator(SchemaFile, InputFile);
			boolean result = validator.run();
			if (result) {
				System.out.println("VALID XML FILE");
			} else {
				System.out.println("INVALID XML FILE");
			}
			System.exit(0);
		}
		else if (JSONMode) {
		}
	}

	final static void showUsage(String Message) {
		System.out.println(Message);
		System.out.println("nez-validator <command> optional files");
		System.out
				.println(" <InputFileFormat(XML/JSON)> <SchemaFile(.dtd/JsonSchemaFile)> <InputFile(.XML/.JSON)>      Validation Mode");
	}

	private static void parseCommandOption(String[] args) {
		int index = 0;
		while (index < args.length) {
			String argument = args[index];
			index++;
			if ((argument.equals("xml") || argument.equals("XML")) && (index < args.length)) {
				XMLMode = true;
				if (index < args.length)
					SchemaFile = args[index++];
				if (index <= args.length)
					InputFile = args[index++];
			}
 else {
				showUsage("Error!!");
				Main._Exit(0, "Unknown Option: " + argument);
			}
		}
	}

	public final static void _Exit(int status, String message) {
		System.out.println("EXIT " + message);
		System.exit(status);
	}
}

package org.peg4d;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Main {
	public final static String  ProgName  = "PEG4d";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 1;
	public final static int     MinerVersion = 0;
	public final static int     PatchLevel   = 0;
	public final static String  Version = "" + MajorVersion + "." + MinerVersion + "." + PatchLevel;
	public final static String  Copyright = "Copyright (c) 2014, Konoha4e project authors";
	public final static String  License = "BSD-Style Open Source";

	// -p konoha.peg
	private static String GrammarFile = null; // default

	// -s StartingPoint
	private static String StartingPoint = "TopLevel";  // default

	// -t output
	private static String OutputType = "pego";  // default

	// -f format
	private static String PegFormat = null;  // default

	// -i
	private static boolean ShellMode = false;

	// -c
	public static boolean RecognitionOnlyMode = false;

	//
	private static String InputFileName = null;
	
	// -o
	private static String OutputFileName = null;

	// --verbose
	public static boolean VerboseMode    = false;

	// --verbose:peg
	public static boolean VerbosePeg = false;

	// --test
	public static boolean TestMode = false;
	
	// --verbose:stat
	public static int     StatLevel = -1;

	// --static => false
	public static boolean TracingMemo = true;
	public static boolean AllExpressionMemo  = true;
	public static boolean PackratStyleMemo   = false;
	public static boolean ObjectFocusedMemo  = false;
	
	// -O
	public static int OptimizationLevel = 2;
	public static int MemoFactor = 256;
	public static String CSVFileName = "results.csv";

	public final static void main(String[] args) {
		parseCommandArguments(args);
		Grammar peg = GrammarFile == null ? Grammar.PEG4d : Grammar.load(GrammarFile);
		if(PegFormat != null) {
			Formatter fmt = loadFormatter(PegFormat);
			peg.show(StartingPoint, fmt);
			return;
		}
		if(InputFileName != null) {
			loadScript(peg, InputFileName);
		}
		else {
			ShellMode = true;
		}
		if(ShellMode) {
			performShell(peg);
		}
	}
	
	private static void parseCommandArguments(String[] args) {
		int index = 0;
		while (index < args.length) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			index = index + 1;
			if ((argument.equals("-p") || argument.equals("--peg")) && (index < args.length)) {
				GrammarFile = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-s") || argument.equals("--start")) && (index < args.length)) {
				StartingPoint = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-f") || argument.equals("--format")) && (index < args.length)) {
				PegFormat = args[index];
				index = index + 1;
			}
			else if(argument.startsWith("-M")) {
				Main.MemoFactor  = UCharset.parseInt(argument.substring(2), 256);
			}
			else if (argument.startsWith("-O")) {
				OptimizationLevel = UCharset.parseInt(argument.substring(2), 2);
			}
			else if (argument.equals("-i")) {
				ShellMode = true;
			}
			else if (argument.equals("-c")) {
				RecognitionOnlyMode = true;
			}
			else if(argument.startsWith("--test")) {
				TestMode = true;
			}
			else if(argument.startsWith("--stat")) {
				StatLevel = UCharset.parseInt(argument.substring(6), 1);
				OutputType = "none";
			}
			else if(argument.startsWith("--csv") && (index < args.length)) {
				CSVFileName = args[index];
				if(!CSVFileName.endsWith(".csv")) {
					Main._Exit(1, "invalid csv file: " + CSVFileName);
				}
				index = index + 1;
			}
			else if ((argument.equals("-t") || argument.equals("--target")) && (index < args.length)) {
				OutputType = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-o") || argument.equals("--out")) && (index < args.length)) {
				OutputFileName = args[index];
				if(OutputFileName.endsWith(".csv")) {
					OutputType = "csv";
				}
				if(OutputFileName.endsWith(".json")) {
					OutputType = "json";
				}
				index = index + 1;
			}
			else if(argument.startsWith("--memo")) {
				if(argument.equals("--memo:packrat")) {
					AllExpressionMemo = false;
					PackratStyleMemo = true;
					ObjectFocusedMemo = false;
				}
				else if(argument.equals("--memo:data")) {
					AllExpressionMemo = false;
					PackratStyleMemo = false;
					ObjectFocusedMemo = true;
				}
				else if(argument.equals("--memo:static")) {
					TracingMemo = false;
				}
				else {
					ShowUsage("unknown option: " + argument);
				}
			}
			else if(argument.startsWith("--verbose")) {
				VerboseMode = true;
			}
			else {
				ShowUsage("unknown option: " + argument);
			}
		}
		if (index < args.length) {
			InputFileName = args[index];
			index++;
		}
		else {
			ShellMode = true;
		}
	}

	public final static void ShowUsage(String Message) {
		System.out.println(ProgName + " :");
		System.out.println("  -p <FILE>                 Specify PEG file  default: PEG4d grammar");
		System.out.println("  -s | --start <NAME>       Specify Non-Terminal as the starting point. default: TopLevel");
		System.out.println("  -t <type>                 Specify output type. default: pego");
		System.out.println("     pego|none|json|csv");
		System.out.println("  -c                        Invoke as checker (without output generation). Exit 1 when failed");
		System.out.println("  -f | --format<type>       Specify PEG formatter");
		System.out.println("  --packrat                 Packrat Parser");
		System.out.println("  -M<num>                   Memo Factor -M0 => No Memo");
		System.out.println("  --verbose                 Printing Debug infomation");
		System.out.println("  --verbose:peg             Printing Peg/Debug infomation");
		Main._Exit(0, Message);
	}

	private final static UMap<Class<?>> driverMap = new UMap<Class<?>>();
	static {
		driverMap.put("peg4d", Formatter.class);
		//driverMap.put("pegjs", PegJSFormatter.class);
	}

	private static Formatter loadDriverImpl(String driverName) {
		try {
			return (Formatter) driverMap.get(driverName).newInstance();
		}
		catch(Exception e) {
		}
		return null;
	}
	
	private static Formatter loadFormatter(String driverName) {
		Formatter d = loadDriverImpl(driverName);
		if(d == null) {
			System.out.println("Supported formatter list:");
			UList<String> driverList = driverMap.keys();
			for(int i = 0; i < driverList.size(); i++) {
				String k = driverList.ArrayValues[i];
				d = loadDriverImpl(k);
				if(d != null) {
					System.out.println("\t" + k + " - " + d.getDesc());
				}
			}
			Main._Exit(1, "undefined formatter: " + driverName);
		}
		return d;
	}

	private static void loadScript(Grammar peg, String fileName) {
		String startPoint = StartingPoint;
		Main.printVerbose("FileName", fileName);
		Main.printVerbose("Grammar", peg.getName());
		Main.printVerbose("StartingPoint", StartingPoint);
		ParserContext p = peg.newParserContext(Main.loadSource(fileName));
		if(Main.StatLevel == 0) {
			long t = System.currentTimeMillis();
			while(System.currentTimeMillis()-t < 5000) {
				System.out.print(".");System.out.flush();
				p.parseNode(startPoint);
				p.sourcePosition = 0;
			}
			System.out.println();
		}
		//while(p.hasNode()) {
		p.beginPeformStat();
		Pego pego = p.parseNode(startPoint);
		p.endPerformStat(pego);
		//}
		if(p.hasChar()) {
			long pos = p.getPosition();
			if(pos > 0) {
				p.showPosition("consumed", pos-1);
			}
			p.showPosition("unconsumed", pos);
		}
		if(OutputType.equalsIgnoreCase("pego")) {
			System.out.println(pego);
		}
		else if(OutputType.equalsIgnoreCase("json")) {
			new Generator(OutputFileName).printJSON(pego);
		}
		else if(OutputType.equalsIgnoreCase("csv")) {
			new Generator(OutputFileName).printCSV(pego, 0.9);
		}
	}

//	private static void parseLine(Namespace gamma, BunDriver driver, String startPoint, ParserSource source) {
//		try {
//			Grammar peg = gamma.getGrammar("main");
//			ParserContext context = peg.newParserContext(source);
//			driver.startTransaction(OutputFileName);
//			while(context.hasNode()) {
//				context.beginStatInfo();
//				PegObject node = context.parseNode(startPoint);
//				context.endStatInfo(node);
////				if(ValidateJsonMode) {
////					parseAndValidateJson(node, gamma, driver, startPoint);
////				}
//				gamma.setNode(node);
//				if(!ParseOnlyMode && driver != null) {
//					if(!(driver instanceof PegDumpper)) {
//						node = gamma.tryMatch(node, true);
//					}
//					else {
//						node.matched = Functor.ErrorFunctor;
//					}
//					if(VerboseMode) {
//						System.out.println("Typed node: \n" + node + "\n:untyped: " + node.countUnmatched(0));
//					}
//					driver.startTopLevel();
//					node.matched.build(node, driver);
//					driver.endTopLevel();
//				}
//				if(node.is("#error")) {
//					if(OutputFileName != null) {
//						MainOption._Exit(1, "toplevel error was found");
//					}
//					break;
//				}
//			}
//			driver.generateMain();
//			driver.endTransaction();
//		}
//		catch (Exception e) {
//			PrintStackTrace(e, source.getLineNumber(0));
//		}
//	}
//	
//	private static boolean inParseAndValidateJson = false;
//	
//	private final static PegObject parseAndValidateJson(PegObject node, Namespace gamma, BunDriver driver, String startPoint) {
//		JsonPegGenerator generator = new JsonPegGenerator();
//		String language = generator.generateJsonPegFile(node);
//		gamma.loadPegFile("main", language);
//		driver.initTable(gamma);
//		if(InputJsonFile != null) {
//			inParseAndValidateJson = true;
//			ParserSource source = Main.loadSource(InputJsonFile);
//			ParserContext context = Main.newParserContext(source);
//			gamma.guessGrammar(context, "main");
//			driver.startTransaction(OutputFileName);
//			while(context.hasNode()) {
//				node = context.parseNode(startPoint);
//					if(context.hasChar()) {
//						//System.out.println(ValidParserContext.InvalidLine);
//						//System.out.println("** uncosumed: '" + context + "' **");
//						break;
//					}
//					else {
//						System.out.println("parsed:\n" + node.toString());
//						System.out.println("\n\nVALID");
//					}
//			}
//		}
//		return node;
//	}

	public final static void performShell(Grammar peg) {
		Main._PrintLine(ProgName + Version + " (" + CodeName + ") on " + Main._GetPlatform());
		Main._PrintLine(Copyright);
		Main._PrintLine("Tips: \\Name to switch the starting point to Name");
		int linenum = 1;
		String line = null;
		String startPoint = StartingPoint;
		while ((line = readMultiLine(startPoint + ">>> ", "    ")) != null) {
			if(line.startsWith("\\")) {
				startPoint = switchStaringPoint(peg, line.substring(1), startPoint);
				continue;
			}
			ParserSource source = new StringSource("(stdin)", linenum, line);
			ParserContext p = peg.newParserContext(source);
			Pego pego = p.parseNode(startPoint);
			System.out.println("Parsed: " + pego);
			linenum = linenum + 1;
		}
		System.out.println("");
	}
	
	private static String switchStaringPoint(Grammar peg, String ruleName, String startPoint) {
		if(peg.hasRule(ruleName)) {
			peg.show(ruleName);
			return ruleName;
		}
		UList<String> list = peg.pegMap.keys();
		System.out.print("Choose:");
		for(int i = 0; i < list.size(); i++) {
			System.out.print(" " + list.ArrayValues[i]);
		}
		System.out.println("");
		return startPoint;
	}

	private static jline.ConsoleReader ConsoleReader = null;

	private final static String readMultiLine(String prompt, String prompt2) {
		if(ConsoleReader == null) {
			try {
				ConsoleReader = new jline.ConsoleReader();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		String line = readSingleLine(prompt);
		if(line == null) {
			System.exit(0);
		}
		if(prompt2 != null) {
			int level = 0;
			while((level = CheckBraceLevel(line)) > 0) {
				String line2 = readSingleLine(prompt2);
				line += "\n" + line2;
			}
			if(level < 0) {
				line = "";
				Main._PrintLine(" .. canceled");
			}
		}
		ConsoleReader.getHistory().addToHistory(line);
		return line;
	}

	private final static String readSingleLine(String prompt) {
		try {
			return ConsoleReader.readLine(prompt);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}


	private static void PrintStackTrace(Exception e, long linenum) {
		StackTraceElement[] elements = e.getStackTrace();
		int size = elements.length + 1;
		StackTraceElement[] newElements = new StackTraceElement[size];
		int i = 0;
		for(; i < size; i++) {
			if(i == size - 1) {
				newElements[i] = new StackTraceElement("<TopLevel>", "TopLevelEval", "stdin", (int)linenum);
				break;
			}
			newElements[i] = elements[i];
		}
		e.setStackTrace(newElements);
		e.printStackTrace();
	}

	private final static int CheckBraceLevel(String Text) {
		int level = 0;
		for(int i = 0; i < Text.length(); i++) {
			char ch = Text.charAt(i);
			if(ch == '{') {
				level++;
			}
			if(ch == '}') {
				level--;
			}
		}
		return level;
	}

	// file

	public final static ParserSource loadSource(String fileName) {
		//ZLogger.VerboseLog(ZLogger.VerboseFile, "loading " + FileName);
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				File f = new File(fileName);
				if(f.length() > 128 * 1024) {
					return new FileSource(fileName);
				}
				Stream = new FileInputStream(fileName);
			} catch (FileNotFoundException e) {
				Main._Exit(1, "file not found: " + fileName);
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();
			while(line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			StringSource s = new StringSource(fileName, 1, builder.toString());
//			StringSource s2 = new StringSource(fileName);
//			System.out.println("@@@@ " + s.length() + ", " + s2.length());
			return s;
		}
		catch(IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}

	public final static String _GetPlatform() {
		return "Java JVM-" + System.getProperty("java.version");
	}

//	public final static String _GetEnv(String Name) {
//		return System.getenv(Name);
//	}
//
//	public final static void _Print(Object msg) {
//		System.err.print(msg);
//	}

	public final static void _PrintLine(Object message) {
		System.err.println(message);
	}

	public final static void printVerbose(String head, Object message) {
		if(Main.VerboseMode) {
			System.out.println(head + ": " + message);
		}
	}

	
	
	public final static void _Exit(int status, String message) {
		if(Main.VerboseMode) {
			System.err.println("EXIT " + Main._GetStackInfo(3) + " " + message);
		}
		else {
			System.err.println("EXIT " + message);
		}
		System.exit(status);
	}

	public static boolean DebugMode = false;

	public final static void _PrintDebug(String msg) {
		if(Main.DebugMode) {
			_PrintLine("DEBUG " + Main._GetStackInfo(3) + ": " + msg);
		}
	}

	public final static String _GetStackInfo(int depth) {
		String LineNumber = " ";
		Exception e =  new Exception();
		StackTraceElement[] Elements = e.getStackTrace();
		if(depth < Elements.length) {
			StackTraceElement elem = Elements[depth];
			LineNumber += elem;
		}
		return LineNumber;
	}

	public final static boolean _IsFlag(int flag, int flag2) {
		return ((flag & flag2) == flag2);
	}

	public final static int _UnsetFlag(int flag, int flag2) {
		return (flag & (~flag2));
	}

	public final static char _GetChar(String Text, int Pos) {
		return Text.charAt(Pos);
	}

	public final static String _CharToString(int ch) {
			return String.format("%c", ch);
	}

	public static String _SourceBuilderToString(UStringBuilder sb) {
		return Main._SourceBuilderToString(sb, 0, sb.slist.size());
	}

	public static String _SourceBuilderToString(UStringBuilder sb, int beginIndex, int endIndex) {
		StringBuilder jsb = new StringBuilder();
		for(int i = beginIndex; i < endIndex; i = i + 1) {
			jsb.append(sb.slist.ArrayValues[i]);
		}
		return jsb.toString();
	}

	public final static void _WriteFile(String fileName, UList<UStringBuilder> list) {
		if(fileName == null) {
			for(int i = 0; i < list.size(); i++) {
				UStringBuilder sb = list.ArrayValues[i];
				System.out.println(sb.toString());
				sb.clear();
			}
		}
		else {
			try {
				BufferedWriter w = new BufferedWriter(new FileWriter(fileName));
				for(int i = 0; i < list.size(); i++) {
					UStringBuilder sb = list.ArrayValues[i];
					w.write(sb.toString());
					w.write("\n\n");
					sb.clear();
				}
				w.close();
			}
			catch(IOException e) {
				_Exit(1, "cannot to write: " + e);
			}
		}
	}

}

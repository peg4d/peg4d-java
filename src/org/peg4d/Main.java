package org.peg4d;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

import org.peg4d.data.RelationBuilder;
import org.peg4d.jvm.JavaByteCodeGenerator;
import org.peg4d.pegcode.PegVMByteCodeGenerator;
import org.peg4d.pegcode.GrammarFormatter;
import org.peg4d.writer.ParsingObjectWriter;
import org.peg4d.writer.ParsingWriter;
import org.peg4d.writer.TagWriter;
import org.peg4d.regex.RegexObject;
import org.peg4d.regex.RegexObjectConverter;
import org.peg4d.regex.RegexPegGenerator;

public class Main {
	public final static String  ProgName  = "Nez";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 0;
	public final static int     MinerVersion = 9;
	public final static int     PatchLevel   = 1;
	public final static String  Version = "" + MajorVersion + "." + MinerVersion + "." + PatchLevel;
	public final static String  Copyright = "Copyright (c) 2014, Nez project authors";
	public final static String  License = "BSD-Style Open Source";

	public final static void main(String[] args) {
		parseCommandOption(args);
		if(Command == null) {
			showUsage("unspecified command");
		}
		try {
			Method m = Main.class.getMethod(Command);
			m.invoke(null);
		} catch (NoSuchMethodException e) {
			showUsage("unknown command: " + Command);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// -X specified
	private static Class<?> OutputWriterClass = null;

	private static String Command = null;
	// -p konoha.p4d
	private static String GrammarFile = null; // default
	// -s, --string
	private static String InputString = null;
	// -i, --input
	private static String InputFileName = null;

	// -o, --output
	private static String OutputFileName = null;

	// -t, --type
	private static String OutputType = null;

	// --start
	private static String StartingPoint = "File";  // default
	// -W
	public static int WarningLevel = 1;
	// -g
	public static int DebugLevel = 0;
	// --verbose
	public static boolean VerboseMode    = false;
	// --test
	public static boolean TestMode = true;

	// --a
	public static boolean DiskMode = false;

	//--infer
	public static boolean InferRelation = false;

	// --jvm
	public static boolean JavaByteCodeGeneration = false;
	
	// --pegvm
	public static boolean PegVMByteCodeGeneration = false;

	// -O
	public static int OptimizationLevel = 2;

	// --log
	public static NezLogger  Logger = null;
	public static String CSVFileName = "results.csv";

	private static String[] FileList = null;

	private static void parseCommandOption(String[] args) {
		int index = 0;
		if(args.length > 0) {
			if(!args[0].startsWith("-")) {
				Command = args[0];
				index = 1;
			}
		}
		while (index < args.length) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			index = index + 1;
			if (argument.equals("-X") && (index < args.length)) {
				try {
					Class<?> c = Class.forName(args[index]);
					if(ParsingWriter.class.isAssignableFrom(c)) {
						OutputWriterClass = c;
					}
				} catch (ClassNotFoundException e) {
					Main._Exit(1, "-X specified class is not found: " + args[index]);
				}
				index = index + 1;
			}
			else if ((argument.equals("-p") || argument.equals("--peg")) && (index < args.length)) {
				GrammarFile = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-s") || argument.equals("--string")) && (index < args.length)) {
				InputString = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-i") || argument.equals("--input")) && (index < args.length)) {
				InputFileName = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-o") || argument.equals("--output")) && (index < args.length)) {
				OutputFileName = args[index];
//				if(OutputType == null && OutputFileName.lastIndexOf('.') > 0) {
//					OutputType = OutputFileName.substring(OutputFileName.lastIndexOf('.')+1);
//				}
				index = index + 1;
			}
//			else if ((argument.equals("-t") || argument.equals("--type")) && (index < args.length)) {
//				OutputType = args[index];
//				index = index + 1;
//			}
			else if (argument.equals("--start") && (index < args.length)) {
				StartingPoint = args[index];
				index = index + 1;
			}
			else if (argument.startsWith("-O")) {
				OptimizationLevel = Utils.parseInt(argument.substring(2), 2);
			}
			else if (argument.startsWith("-W")) {
				WarningLevel = Utils.parseInt(argument.substring(2), 2);
			}
			else if (argument.startsWith("-g")) {
				DebugLevel = Utils.parseInt(argument.substring(2), 1);
			}
			else if(argument.startsWith("--memo")) {
				if(argument.equals("--memo:none")) {
					MemoizationManager.NoMemo = true;
				}
				else if(argument.equals("--memo:packrat")) {
					MemoizationManager.PackratParsing = true;
				}
				else if(argument.equals("--memo:window")) {
					MemoizationManager.SlidingWindowParsing = true;
				}
				else if(argument.equals("--memo:slide")) {
					MemoizationManager.SlidingLinkedParsing = true;
				}
				else if(argument.equals("--memo:notrace")) {
					MemoizationManager.Tracing = false;
				}
				else {
					int distance = Utils.parseInt(argument.substring(7), -1);
					if(distance >= 0) {
						MemoizationManager.BacktrackBufferSize  = distance;
					}
					else {
						showUsage("unknown option: " + argument);
					}
				}
			}
			else if(argument.startsWith("--log")) {
				String logFile = "nezlog.csv";
				if(argument.endsWith(".csv")) {
					logFile = argument.substring(6);
				}
				Logger = new NezLogger(logFile);
				if(OutputWriterClass == null) {
					OutputWriterClass = TagWriter.class;
				}
			}
			else if(argument.startsWith("--verbose")) {
				if(argument.equals("--verbose:memo")) {
					MemoizationManager.VerboseMemo = true;
				}
				else {
					VerboseMode = true;
				}
			}
			else if (argument.equals("--infer")) {
				InferRelation = true;
			}
			else if(argument.equals("--jvm")) {
				JavaByteCodeGeneration = true;
			}
			else if(argument.equals("--pegvm")) {
				PegVMByteCodeGeneration = true;
			}
			else {
				showUsage("unknown option: " + argument);
			}
		}
		if(index < args.length) {
			FileList = new String[args.length - index];
			System.arraycopy(args, index, FileList, 0, FileList.length);
		}
		if(GrammarFile == null) {
			if(InputFileName != null) {
				GrammarFile = guessGrammarFile(InputFileName);
			}
		}
		if(InputFileName == null && InputString == null && !PegVMByteCodeGeneration) {
			System.out.println("unspecified inputs: invoking interactive shell");
			Command = "shell";
		}
		if(OutputWriterClass == null) {
			OutputWriterClass = ParsingObjectWriter.class;
		}
	}

	final static void showUsage(String Message) {
		System.out.println("nez <command> optional files");
		System.out.println("  -p | --peg <filename>      Specify an PEGs grammar file");
		System.out.println("  -i | --input <filename>    Specify an input file");
		System.out.println("  -s | --string <string>     Specify an input string");
		System.out.println("  -o | --output <filename>   Specify an output file");
//		System.out.println("  -t | --type <filename>     Specify an output type");
		System.out.println("  --start <NAME>             Specify Non-Terminal as the starting point");
		System.out.println("  -W<num>                    Warning Level (default:1)");
		System.out.println("  -O<num>                    Optimization Level (default:2)");
		System.out.println("  -g                         Debug Level");
		System.out.println("  --memo:x                   Memo configuration");
		System.out.println("     none|packrat|window|slide|notrace");
		System.out.println("  --memo:<num>               Expected backtrack distance (default: 256)");
		System.out.println("  --verbose                  Printing Debug infomation");
		System.out.println("  --verbose:memo             Printing Memoization information");
		System.out.println("  --infer                    Specify an inference schema for rel command");
		System.out.println("  --jvm                      Generate java byte code at runtime");
		System.out.println("  -X <class>                 Specify an extension class");
		System.out.println("");
		System.out.println("The most commonly used nez commands are:");
		System.out.println("  parse        Parse -i input or -s string to -o output");
		System.out.println("  check        Parse -i input or -s string");
		System.out.println("  shell        Try parsing in an interactive way");
		System.out.println("  rel          Convert -f file to relations (csv file)");
		System.out.println("  nezex        Convert -i regex to peg");
		System.out.println("  conv         Convert PEG4d rules to the specified format in -o");
		System.out.println("  find         Search nonterminals that can match inputs");
		Main._Exit(0, Message);
	}

	private final static UMap<Class<?>> driverMap = new UMap<Class<?>>();
	static {
		driverMap.put("p4d", org.peg4d.pegcode.PEG4dFormatter.class);
		driverMap.put("peg", org.peg4d.pegcode.PEG4dFormatter.class);
		driverMap.put("c2", org.peg4d.pegcode.CGenerator2.class);
		driverMap.put("pegjs", org.peg4d.pegcode.PEGjsFormatter.class);
		driverMap.put("py", org.peg4d.pegcode.PythonGenerator.class);
		driverMap.put("vm", org.peg4d.pegcode.PegVMByteCodeGenerator.class);
	}

	private static GrammarFormatter loadDriverImpl(String driverName) {
		try {
			return (GrammarFormatter) driverMap.get(driverName).newInstance();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public final static String guessGrammarFile(String fileName) {
		int loc = fileName.lastIndexOf('.');
		if(loc > 0) {
			String ext = fileName.substring(loc+1);
			String grammarFile = "org/peg4d/lib/" + ext + ".p4d";
			InputStream stream = Main.class.getResourceAsStream("/" + grammarFile);
			//System.out.println("grammar: " + grammarFile + ", stream: " + stream);
			if(stream != null) {
				return grammarFile;
			}
		}
		return null;
	}

	static Grammar newGrammar() {
		Grammar grammar = GrammarFile == null ? GrammarFactory.Grammar : new GrammarFactory().newGrammar("main", GrammarFile, Logger);
		if(JavaByteCodeGeneration) {
			JavaByteCodeGenerator g = new JavaByteCodeGenerator();
			g.formatGrammar(grammar, null);
		}
		return grammar;
	}

	static ParsingSource newParsingSource(Grammar peg) {
		if(InputFileName != null) {
			return ParsingSource.loadSource(InputFileName);
		}
		if(InputString == null) {
			showUsage("unspecfied input; expected -i or -s option");
		}
		return new StringSource(InputString);
	}

	private static int StatTimes = 10;

	public static void check() {
		Grammar peg = newGrammar();
		if(Logger == null) {
			ParsingContext context = new ParsingContext(newParsingSource(peg));
			boolean res = context.match(peg, StartingPoint, new MemoizationManager());
			System.exit(res ? 0 : 1);
		}
		else {
			ParsingContext context = null;
			ParsingObject po = null;
			long bestTime = Long.MAX_VALUE;
			long t0 = System.currentTimeMillis();
			for(int i = 0; i < StatTimes; i++) {
				ParsingSource source = newParsingSource(peg);
				context = new ParsingContext(source);
				long t1 = System.currentTimeMillis();
				context.setLogger(Logger);
				context.match(peg, StartingPoint, new MemoizationManager());
				long t2 = System.currentTimeMillis();
				long t = t2 - t1;
				Main.printVerbose("ErapsedTime", "" + t + "ms");
				if(t < bestTime) {
					bestTime = t;
				}
				if(t2 - t0 > 200000) {
					break;
				}
			}
			Logger.dump(bestTime, context, null);
			//ParsingWriter.writeAs(OutputWriterClass, OutputFileName, po);
		}
	}

	public static void parse() {
		Grammar peg = newGrammar();
		if(Logger == null) {
			ParsingContext context = new ParsingContext(newParsingSource(peg));
			ParsingObject po = context.parse(peg, StartingPoint, new MemoizationManager());
			if(context.isFailure()) {
				System.out.println(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
				System.out.println(context.source.formatPositionLine("maximum matched", context.head_pos, ""));
				if(Main.DebugLevel > 0) {
					System.out.println(context.maximumFailureTrace);
				}
				return;
			}
			if(context.hasByteChar()) {
				System.out.println(context.source.formatPositionLine("unconsumed", context.pos, ""));
				System.out.println(context.source.formatPositionLine("maximum matched", context.head_pos, ""));
				if(Main.DebugLevel > 0) {
					System.out.println(context.maximumFailureTrace);
				}
			}
			ParsingWriter.writeAs(OutputWriterClass, OutputFileName, po);
		}
		else {
			ParsingContext context = null;
			ParsingObject po = null;
			long bestTime = Long.MAX_VALUE;
			long t0 = System.currentTimeMillis();
			for(int i = 0; i < StatTimes; i++) {
				ParsingSource source = newParsingSource(peg);
				context = new ParsingContext(source);
				long t1 = System.currentTimeMillis();
				context.setLogger(Logger);
				po = context.parse(peg, StartingPoint, new MemoizationManager());
				long t2 = System.currentTimeMillis();
				long t = t2 - t1;
				Main.printVerbose("ErapsedTime", "" + t + "ms");
				if(t < bestTime) {
					bestTime = t;
				}
				if(t2 - t0 > 200000) {
					break;
				}
			}
			Logger.dump(bestTime, context, po);
			ParsingWriter.writeAs(OutputWriterClass, OutputFileName, po);
		}
	}
	
	public static void conv() {
		Grammar peg = newGrammar();
		if (PegVMByteCodeGeneration) {
			PegVMByteCodeGenerator g = new PegVMByteCodeGenerator();
			g.formatGrammar(peg, null);
			g.writeByteCode(GrammarFile, OutputFileName, peg);
		}
	}

	public static void rel() {
		Grammar peg = newGrammar();
		ParsingContext context = new ParsingContext(newParsingSource(peg));
		ParsingObject pego = context.parse(peg, StartingPoint, new MemoizationManager());
		RelationBuilder RBuilder = new RelationBuilder(pego);
		RBuilder.build(InferRelation);
	}

	public static void nezex() {
		Grammar peg = new GrammarFactory().newGrammar("main", "src/resource/regex.p4d");
		Main.printVerbose("Grammar", peg.getName());
		Main.printVerbose("StartingPoint", StartingPoint);
		ParsingContext context = new ParsingContext(newParsingSource(peg));
		ParsingObject pego = context.parse(peg, StartingPoint, new MemoizationManager());
		if(context.isFailure()) {
			System.out.println(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
			System.out.println(context.source.formatPositionLine("maximum matched", context.head_pos, ""));
			if(Main.DebugLevel > 0) {
				System.out.println(context.maximumFailureTrace);
			}
			return;
		}
		if(context.hasByteChar()) {
			System.out.println(context.source.formatPositionLine("unconsumed", context.pos, ""));
			System.out.println(context.source.formatPositionLine("maximum matched", context.head_pos, ""));
			if(Main.DebugLevel > 0) {
				System.out.println(context.maximumFailureTrace);
			}
		}

		System.out.println("Parsed: " + pego);

		Map<String, RegexObject> ro = new RegexObjectConverter(pego).convert();
		RegexPegGenerator pegfile = new RegexPegGenerator(OutputFileName, ro);

		System.out.println();
		pegfile.writePeg();
		pegfile.close();

		return;
	}

	private final static void displayShellVersion(Grammar peg) {
		Main._PrintLine(ProgName + "-" + Version + " (" + CodeName + ") on " + Main._GetPlatform());
		Main._PrintLine(Copyright);
	}

	public final static void shell() {
		Grammar peg = newGrammar();
		displayShellVersion(peg);
		Main._PrintLine("Tips: \\Name to switch the starting point to Name");
		int linenum = 1;
		String line = null;
		String startPoint = "Chunk";
		while ((line = readMultiLine(startPoint + ">>> ", "    ")) != null) {
			if(line.startsWith("\\")) {
				startPoint = switchStaringPoint(peg, line.substring(1), startPoint);
				continue;
			}
			ParsingSource source = new StringSource("(stdin)", linenum, line);
			ParsingContext context = new ParsingContext(source);
			ParsingObject po = context.parse(peg, startPoint);
			if(context.isFailure()) {
				System.out.println(context.source.formatPositionLine("error", context.fpos, context.getErrorMessage()));
			}
			else {
				System.out.println("Parsed: " + po);
			}
			linenum = linenum + 1;
		}
		System.out.println("");
	}

	private static String switchStaringPoint(Grammar peg, String ruleName, String startPoint) {
		if(peg.hasRule(ruleName)) {
			peg.show(ruleName);
			return ruleName;
		}
		UList<String> list = peg.ruleMap.keys();
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

	public final static String _GetPlatform() {
		return "Java JVM-" + System.getProperty("java.version");
	}

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
			System.out.println("EXIT " + Main._GetStackInfo(3) + " " + message);
		}
		else {
			System.out.println("EXIT " + message);
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

	public final static void dumpStack() {
		Exception e =  new Exception();
		StackTraceElement[] Elements = e.getStackTrace();
		for(int i = 0; i < Elements.length; i++) {
			StackTraceElement elem = Elements[i];
			System.out.println(elem.getMethodName());
		}
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

	public final static void reportException(Exception e) {
		e.printStackTrace();
		Main._Exit(1, e.getMessage());
	}

}

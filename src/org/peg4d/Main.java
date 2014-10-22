package org.peg4d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;

import org.peg4d.data.RelationBuilder;
import org.peg4d.ext.Generator;
import org.peg4d.pegcode.GrammarFormatter;

public class Main {
	public final static String  ProgName  = "PEG4d";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 0;
	public final static int     MinerVersion = 2;
	public final static int     PatchLevel   = 7;
	public final static String  Version = "" + MajorVersion + "." + MinerVersion + "." + PatchLevel;
	public final static String  Copyright = "Copyright (c) 2014, Konoha project authors";
	public final static String  License = "BSD-Style Open Source";

	// -p konoha.peg
	private static String GrammarFile = null; // default

	// -s StartingPoint
	private static String StartingPoint = "File";  // default

	// -t output
	private static String OutputType = "pego";  // default

	// -f format
	private static String PEGFormatter = null;  // default

	// -i
	private static boolean ShellMode = false;

	// -c
	public static boolean RecognitionOnlyMode = false;
	
	// -W
	public static int WarningLevel = 1;

	// -g
	public static int DebugLevel = 0;
	
	// --find
	private static int FindFileIndex = -1;
	
	//
	private static String InputFileName = null;
	
	// -o
	private static String OutputFileName = null;

	// --verbose
	public static boolean VerboseMode    = false;

	// --verbose:peg
	public static boolean VerbosePeg = false;

	// --test
	public static boolean TestMode = true;

	// --a
	public static boolean DiskMode = false;
	
	// --verbose:stat
	public static int     StatLevel = -1;
	
	// --relation
	public static boolean Relation = false;

	// --memo:no
//	public static String  ParserName = null;
//	public static boolean TracingMemo = true;
//	public static boolean UseFifo = false;
//	public static boolean AllExpressionMemo  = false;
//	public static boolean PackratStyleMemo   = true;
//	public static boolean ObjectFocusedMemo  = false;
	
	// -O
	public static int OptimizationLevel = 2;
	public static String CSVFileName = "results.csv";

	public final static void main(String[] args) {
		//new PEG4dGrammar2();
		parseCommandArguments(args);
//		if(FindFileIndex != -1) {
//			Grammar peg = new GrammarFactory().newGrammar("main");
//			for(int i = FindFileIndex; i < args.length; i++) {
//				peg.importGrammar(args[i]);
//			}
//			peg.verifyRules();
//			performShell2(peg);
//			return;
//		}
		Grammar peg = GrammarFile == null ? GrammarFactory.Grammar : new GrammarFactory().newGrammar("main", GrammarFile);
		if(PEGFormatter != null) {
			GrammarFormatter fmt = loadGrammarFormatter(PEGFormatter);
			StringBuilder sb = new StringBuilder();
			fmt.formatGrammar(peg, sb);
			return ;
		}
		if(InputFileName != null) {
			loadInputFile(peg, InputFileName);
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
				PEGFormatter = args[index];
				index = index + 1;
			}
			else if (argument.startsWith("-O")) {
				OptimizationLevel = ParsingCharset.parseInt(argument.substring(2), 2);
			}
			else if (argument.startsWith("-W")) {
				WarningLevel = ParsingCharset.parseInt(argument.substring(2), 2);
			}
			else if (argument.startsWith("-g")) {
				DebugLevel = ParsingCharset.parseInt(argument.substring(2), 1);
			}
			else if (argument.equals("-i")) {
				Main.OptimizationLevel = 0;
				ShellMode = true;
			}
			else if (argument.equals("-c")) {
				RecognitionOnlyMode = true;
			}
			else if (argument.equals("-a")) {
				DiskMode = true;
			}
			else if ((argument.equals("--find")) && (index < args.length)) {
				FindFileIndex = index;
				return;
			}
			else if (argument.equals("--relation")) {
				Relation = true;
			}
			else if(argument.startsWith("--test")) {
				TestMode = true;
			}
			else if(argument.startsWith("--stat")) {
				StatLevel = ParsingCharset.parseInt(argument.substring(6), 1);
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
//			else if (argument.equals("--name")) {
//				ParserName = args[index];
//				index = index + 1;
//			}
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
					int distance = ParsingCharset.parseInt(argument.substring(7), -1);
					if(distance >= 0) {
						MemoizationManager.BacktrackBufferSize  = distance;
					}
					else {
						ShowUsage("unknown option: " + argument);
					}
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
			else {
				ShowUsage("unknown option: " + argument);
			}
		}
		if (index < args.length) {
			InputFileName = args[index];
			index++;
		}
		else {
			Main.OptimizationLevel = 0;
			ShellMode = true;
		}
	}

	public final static void ShowUsage(String Message) {
		System.out.println(ProgName + " :");
		System.out.println("  -p <FILE>                 Specify PEG file  default: PEG4d grammar");
		System.out.println("  -s | --start <NAME>       Specify Non-Terminal as the starting point. default: TopLevel");
		System.out.println("  -t <type>                 Specify output type. default: pego");
		System.out.println("     tag|pego|none|json|csv");
		System.out.println("  -c                        Invoke as checker (without output generation). Exit 1 when failed");
		System.out.println("  -f | --format<type>       Specify PEG formatter");
		System.out.println("  -W<num>                   Warning Level (default:1)");
		System.out.println("  -O<num>                   Optimization Level (default:2)");
		System.out.println("  -g                        Debug Level");
		System.out.println("  --memo:x                  Memo configuration");
		System.out.println("     none|packrat|window|slide|notrace");
		System.out.println("  --memo:<num>              Expected backtrack distance (default: 256)");
		System.out.println("  --verbose                 Printing Debug infomation");
		System.out.println("  --verbose:memo            Printing Memoization information");
		Main._Exit(0, Message);
	}
	
	private final static UMap<Class<?>> driverMap = new UMap<Class<?>>();
	static {
		driverMap.put("p4d", org.peg4d.pegcode.PEG4dFormatter.class);
		driverMap.put("peg", org.peg4d.pegcode.PEG4dFormatter.class);
		driverMap.put("c2", org.peg4d.pegcode.CGenerator2.class);
		driverMap.put("pegjs", org.peg4d.pegcode.PEGjsFormatter.class);
		driverMap.put("py", org.peg4d.pegcode.PythonGenerator.class);
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
	
	private static GrammarFormatter loadGrammarFormatter(String driverName) {
		GrammarFormatter d = loadDriverImpl(driverName);
		if(d == null) {
			System.out.println("Supported formatter list:");
			UList<String> driverList = driverMap.keys();
			for(int i = 0; i < driverList.size(); i++) {
				String k = driverList.ArrayValues[i];
				d = loadDriverImpl(k);
				if(d != null) {
					System.out.println("\t" + k + " - " + d.getDesc());
				}
				else {
					System.out.println("\t" + k + " - " + d);
				}
			}
			Main._Exit(1, "undefined formatter: " + driverName);
		}
		return d;
	}
	
	private synchronized static void loadStat(Grammar peg, String fileName) {
		String startPoint = StartingPoint;
		ParsingSource source = null;
		ParsingContext context = null;
		ParsingObject po = null;
		long bestTime = Long.MAX_VALUE;
		ParsingStatistics stat = null;
		for(int i = 0; i < 20; i++) {
			source = Main.loadSource(peg, fileName);
			context = new ParsingContext(Main.loadSource(peg, fileName));
			stat = new ParsingStatistics(peg, source);
			context.initStat(stat);
			if(Main.RecognitionOnlyMode) {
				context.match(peg, startPoint, new MemoizationManager());
			}
			else {
				po = context.parse(peg, startPoint, new MemoizationManager());
			}
			long t = stat.end();
			System.out.println("ErapsedTime: " + t);
			if(t < bestTime) {
				bestTime = t;
			}
			if(t > 60000 * 5) {
				break;
			}
		}
		stat.ErapsedTime = bestTime;
		stat.end(po, context);
	}
	
	private synchronized static void loadInputFile(Grammar peg, String fileName) {
		String startPoint = StartingPoint;
		Main.printVerbose("FileName", fileName);
		Main.printVerbose("Grammar", peg.getName());
		Main.printVerbose("StartingPoint", StartingPoint);
		if(OutputType.equalsIgnoreCase("stat")) {
			loadStat(peg, fileName);
			return;
		}
		ParsingSource source = Main.loadSource(peg, fileName);
		ParsingContext context = new ParsingContext(Main.loadSource(peg, fileName));
		if(Main.StatLevel == 0) {
			long t = System.currentTimeMillis();
			while(System.currentTimeMillis()-t < 4000) {
				System.out.print(".");System.out.flush();
				context.parseChunk(peg, startPoint);
				context.pos = 0;
			}
			context.initMemo(null);
			System.gc();
			try{
				Thread.sleep(500);
			}catch(InterruptedException e){
			}
			System.out.println(" GO!!");
		}
		source = Main.loadSource(peg, fileName);
		context = new ParsingContext(source);
		if(OutputType.equalsIgnoreCase("stat")) {
			context.initStat(new ParsingStatistics(peg, source));
		}
		if(Main.RecognitionOnlyMode) {
			boolean res = context.match(peg, startPoint, new MemoizationManager());
			if(OutputType.equalsIgnoreCase("stat")) {
				context.recordStat(null);
				return;
			}
			System.exit(res ? 0 : 1);
		}
		ParsingObject pego = context.parse(peg, startPoint, new MemoizationManager());
		if(Relation) {
			RelationBuilder RBuilder = new RelationBuilder();
			RBuilder.build(pego);
		}
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
		if(OutputType.equalsIgnoreCase("stat")) {
			context.recordStat(pego);
			return;
		}
		if(OutputType.equalsIgnoreCase("tag")) {
			outputMap(pego);
			return;
		}
		if(OutputType.equalsIgnoreCase("pego")) {
			new Generator(OutputFileName).writePego(pego);
		}
		else if(OutputType.equalsIgnoreCase("json")) {
			new Generator(OutputFileName).writeJSON(pego);
		}
		else if(OutputType.equalsIgnoreCase("csv")) {
			new Generator(OutputFileName).writeCommaSeparateValue(pego, 0.9);
		}
	}

	private static void outputMap(ParsingObject po) {
		TreeMap<String,Integer> m = new TreeMap<String,Integer>();
		tagCount(po, m);
		for(String k : m.keySet()) {
			System.out.print("#" + k + ":" + m.get(k));
		}
		System.out.println("");
	}

	private static void tagCount(ParsingObject po, TreeMap<String,Integer> m) {
		for(int i = 0; i < po.size(); i++) {
			tagCount(po.get(i), m);
		}
		String key = po.getTag().toString();
		Integer n = m.get(key);
		if(n == null) {
			m.put(key, 1);
		}
		else {
			m.put(key, n+1);
		}
	}
	
	private final static void displayShellVersion(Grammar peg) {
		Main._PrintLine(ProgName + "-" + Version + " (" + CodeName + ") on " + Main._GetPlatform());
		Main._PrintLine(Copyright);
	}

	public final static void performShell(Grammar peg) {
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
			ParsingSource source = new StringSource(peg, "(stdin)", linenum, line);
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

	public final static void performShell2(Grammar peg) {
		displayShellVersion(peg);
		UList<ParsingRule> ruleList = peg.getRuleList();
		UList<String> seq = new UList<String>(new String[16]);
		int linenum = 1;
		String line = null;
		while ((line = readMultiLine("?>>> ", "    ")) != null) {
			ParsingSource source = new StringSource(peg, "(stdin)", linenum, line);
			ParsingContext context = new ParsingContext(source);
			for(int i = 0; i < ruleList.size(); i++) {
				ParsingRule rule = ruleList.ArrayValues[i];
				if(rule.isObjectType()) {
					context.resetSource(source, 0);
					context.parse(peg, rule.ruleName);
					if(context.isFailure()) {
						continue;
					}
					seq.add(rule.ruleName);
					infer(ruleList, context, seq, peg);
					seq.pop();
				}
			}
			linenum = linenum + 1;
		}
		System.out.println("");
	}
	
	static void infer(UList<ParsingRule> ruleList, ParsingContext context, UList<String> seq, Grammar peg) {
		if(!context.hasByteChar()) {
			printSequence(seq);
			return;
		}
		boolean foundRule = false;
		long pos = context.getPosition();
		for(int i = 0; i < ruleList.size(); i++) {
			ParsingRule rule = ruleList.ArrayValues[i];
			//if(rule.objectType) {
				context.setPosition(pos);
				context.parse(peg, rule.ruleName);
				if(context.isFailure()) {
					continue;
				}
				seq.add(rule.ruleName);
				foundRule = true;
				infer(ruleList, context, seq, peg);
				seq.pop();
			//}
		}
		if(!foundRule) {
			context.setPosition(pos);
			int ch = context.getByteChar();
			seq.add("'" + (char)ch + "'");
			context.consume(1);
			infer(ruleList, context, seq, peg);
			seq.pop();
		}
	}

	static void printSequence(UList<String> seq) {
		StringBuilder sb = new StringBuilder();
		sb.append("matched: ");
		for(int i = 0; i < seq.size(); i++) {
			sb.append(" ");
			sb.append(seq.ArrayValues[i]);
		}
		System.out.println(sb.toString());
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

	public final static ParsingSource loadSource(Grammar peg, String fileName) {
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				File f = new File(fileName);
				if(f.length() > 128 * 1024) {
					return new FileSource(peg, fileName);
				}
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
			while(line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			return new StringSource(peg, fileName, 1, builder.toString());
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

}

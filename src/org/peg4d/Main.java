package org.peg4d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.peg4d.ext.Generator;

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
	private static String StartingPoint = "File";  // default

	// -t output
	private static String OutputType = "pego";  // default

	// -f format
	private static String PEGFormatter = null;  // default

	// -i
	private static boolean ShellMode = false;

	// -c
	public static boolean RecognitionOnlyMode = false;

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
	public static boolean TestMode = false;

	// --a
	public static boolean DiskMode = false;
	
	// --verbose:stat
	public static int     StatLevel = -1;

	// --static => false
	public static String  ParserName = null;
	public static boolean TracingMemo = true;
	public static boolean UseFifo = false;
	public static boolean AllExpressionMemo  = false;
	public static boolean PackratStyleMemo   = true;
	public static boolean ObjectFocusedMemo  = false;
	
	// -O
	public static int OptimizationLevel = 2;
	public static int MemoFactor = 256;
	public static String CSVFileName = "results.csv";

	public final static void main(String[] args) {
		parseCommandArguments(args);
		if(FindFileIndex != -1) {
			Grammar peg = new GrammarFactory().newGrammar("main");
			for(int i = FindFileIndex; i < args.length; i++) {
				peg.importGrammar(args[i]);
			}
			peg.verify();
			performShell2(peg);
			return;
		}
		Grammar peg = GrammarFile == null ? Grammar.PEG4d : new GrammarFactory().newGrammar("main", GrammarFile);
		if(PEGFormatter != null) {
			GrammarFormatter fmt = loadFormatter(PEGFormatter);
			peg.formatAll(fmt);
			return;
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
			else if ((argument.equals("--find")) && (index < args.length)) {
				FindFileIndex = index;
				return;
			}
			else if ((argument.equals("-s") || argument.equals("--start")) && (index < args.length)) {
				StartingPoint = args[index];
				index = index + 1;
			}
			else if ((argument.equals("-f") || argument.equals("--format")) && (index < args.length)) {
				PEGFormatter = args[index];
				index = index + 1;
			}
			else if(argument.startsWith("-M")) {
				Main.MemoFactor  = ParsingCharset.parseInt(argument.substring(2), 256);
			}
			else if (argument.startsWith("-O")) {
				OptimizationLevel = ParsingCharset.parseInt(argument.substring(2), 2);
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
			else if (argument.equals("--name")) {
				ParserName = args[index];
				index = index + 1;
			}
			else if(argument.startsWith("--memo")) {
				if(argument.equals("--memo:data")) {
					AllExpressionMemo = false;
					PackratStyleMemo = false;
					ObjectFocusedMemo = true;
				}
				else if(argument.equals("--memo:none")) {
					AllExpressionMemo = false;
					PackratStyleMemo = false;
					ObjectFocusedMemo = false;
					TracingMemo = false;
				}
				else if(argument.equals("--memo:all")) {
					AllExpressionMemo = true;
					PackratStyleMemo = false;
					ObjectFocusedMemo = false;
				}
				else if(argument.equals("--memo:static")) {
					TracingMemo = false;
				}
				else if(argument.equals("--memo:fifo")) {
					UseFifo = true;
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
			Main.OptimizationLevel = 0;
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
		driverMap.put("p4d", GrammarFormatter.class);
		driverMap.put("peg", GrammarFormatter.class);
		driverMap.put("vm", CodeGenerator.class);
	}

	private static GrammarFormatter loadDriverImpl(String driverName) {
		try {
			return (GrammarFormatter) driverMap.get(driverName).newInstance();
		}
		catch(Exception e) {
		}
		return null;
	}
	
	private static GrammarFormatter loadFormatter(String driverName) {
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
			}
			Main._Exit(1, "undefined formatter: " + driverName);
		}
		return d;
	}

	private synchronized static void loadInputFile(Grammar peg, String fileName) {
		String startPoint = StartingPoint;
		Main.printVerbose("FileName", fileName);
		Main.printVerbose("Grammar", peg.getName());
		Main.printVerbose("StartingPoint", StartingPoint);
		ParsingStream p = peg.newParserContext(Main.loadSource(peg, fileName));
		if(Main.StatLevel == 0) {
			long t = System.currentTimeMillis();
			p.setRecognitionMode(true);
			while(System.currentTimeMillis()-t < 4000) {
				System.out.print(".");System.out.flush();
				p.parseChunk(startPoint);
				p.pos = 0;
			}
			p.setRecognitionMode(false);
			p.initMemo();
			System.gc();
			try{
				Thread.sleep(500);
			}catch(InterruptedException e){
			}
			System.out.println(" GO!!");
		}
		p.beginPeformStat();
		ParsingObject pego = p.parse(startPoint);
		if(p.isFailure()) {
			p.showPosition("syntax error", p.fpos);
			return;
		}
		p.endPerformStat(pego);
		if(p.hasByteChar()) {
			p.showPosition("unconsumed", p.pos);
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
			ParsingStream p = peg.newParserContext(source);
			ParsingObject pego = p.parseChunk(startPoint);
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
		UList<PegRule> ruleList = peg.getRuleList();
		UList<String> seq = new UList<String>(new String[16]);
		int linenum = 1;
		String line = null;
		while ((line = readMultiLine("?>>> ", "    ")) != null) {
			ParsingStream p = peg.newParserContext();
			ParsingSource source = new StringSource(peg, "(stdin)", linenum, line);
			for(int i = 0; i < ruleList.size(); i++) {
				PegRule rule = ruleList.ArrayValues[i];
				if(rule.objectType) {
					p.resetSource(source);
					ParsingObject pego = p.parse(rule.ruleName);
					if(p.isFailure()) {
						continue;
					}
					seq.add(rule.ruleName);
					infer(ruleList, p, seq);
					seq.pop();
				}
			}
			linenum = linenum + 1;
		}
		System.out.println("");
	}
	
	static void infer(UList<PegRule> ruleList, ParsingStream p, UList<String> seq) {
		if(!p.hasByteChar()) {
			printSequence(seq);
			return;
		}
		boolean foundRule = false;
		long pos = p.getPosition();
		for(int i = 0; i < ruleList.size(); i++) {
			PegRule rule = ruleList.ArrayValues[i];
			//if(rule.objectType) {
				p.setPosition(pos);
				ParsingObject pego = p.parse(rule.ruleName);
				if(p.isFailure()) {
					continue;
				}
				seq.add(rule.ruleName);
				foundRule = true;
				infer(ruleList, p, seq);
				seq.pop();
			//}
		}
		if(!foundRule) {
			p.setPosition(pos);
			int ch = p.getByteChar();
			seq.add("'" + (char)ch + "'");
			p.consume(1);
			infer(ruleList, p, seq);
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

}

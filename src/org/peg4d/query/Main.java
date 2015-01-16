package org.peg4d.query;

import java.util.Optional;

import org.peg4d.Grammar;
import org.peg4d.GrammarFactory;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;
import org.peg4d.query.Executor.QueryExecutionException;

// import "hogehoge" as ns;
// Name[Chunk as Expr]

public class Main {	//TODO: pipe line mode
	private final static String defaultStartPoint = "TopLevel";
	private static Optional<String> pegDef4Data = Optional.empty();
	private static Optional<String> targetData = Optional.empty();
	private final static String pegDef4Query = "query.peg";
	private static Optional<String> queryScript = Optional.empty();

	private static boolean verboseQuery = false;
	private static boolean verboseData = false;

	private static boolean time = false;
	private static TimeUnit unit = TimeUnit.MilliSecond;

	private static enum TimeUnit {
		Second     ("s",  1),
		MilliSecond("ms", 0);

		private final String unitSymbol;
		private final int count;
		private TimeUnit(String unitSymbol, int count) {
			this.unitSymbol = unitSymbol;
			this.count = count;
		}

		public String getUnitSymbol() {
			return this.unitSymbol;
		}

		public double convertUnit(long num) {
			int d = 1;
			for(int i = 0; i < this.count; i++) {
				d *= 1000;
			}
			return (double) num / d;
		}

		public static TimeUnit toTimeUnit(String unitSymbol) {
			for(TimeUnit unit : TimeUnit.values()) {
				if(unit.getUnitSymbol().equals(unitSymbol)) {
					return unit;
				}
			}
			return TimeUnit.MilliSecond;
		}
	}

	public static void main(String[] args) {
		/**
		 * parse argument
		 */
		final ArgumentsParser argsParser = new ArgumentsParser();
		argsParser.addDefaultAction(s -> argsParser.printHelpBeforeExit(System.err, 1))
		.addHelp("h", "help", false, "show this help message", 
				s -> argsParser.printHelpBeforeExit(System.out, 0))
		.addOption("g", "grammar", true, "peg definition of target data format", true,
				s -> pegDef4Data = Optional.of(s.get()))
		.addOption("t", "target", true, "target data file", 
				s -> targetData = Optional.of(s.get()))
		.addOption("q", "query", true, "query script", 
				s -> queryScript = Optional.of(s.get()))
		.addOption("vd", "verbose:data", false, "display parsed data", 
				s -> verboseData = true)
		.addOption("vq", "verbose:query", false, "display parsed query", 
				s -> verboseQuery = true)
		.addOption(null, "time", false, "display query execution time", 
				s -> time = true)
		.addOption(null, "time-unit", true, "set time unit (s, ms)", 
				s -> unit = TimeUnit.toTimeUnit(s.get()));


		try {
			argsParser.parseAndInvokeAction(args);
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
			argsParser.printHelpBeforeExit(System.err, 1);
		}

		/**
		 * parse data file
		 */
		
		Grammar peg = pegDef4Data.get() == null ? GrammarFactory.Grammar : new GrammarFactory().newGrammar("data", pegDef4Data.get());
		
		String dataFilename = targetData.get();
		ParsingSource dataSource = org.peg4d.ParsingSource.loadSource(dataFilename);
		ParsingContext context = new ParsingContext(dataSource);
		ParsingObject parsedObject = context.parse2(peg, defaultStartPoint, new ParsingObject(), null);

		if(verboseData) {
			System.err.println("parsed data:");
			System.err.println(parsedObject);
			System.err.println("");
		}
		if(context.hasByteChar()) {
			System.out.println("uncosumed");
			System.exit(1);
		}
		if(context.isFailure()) {
			System.err.println(parsedObject);
			System.exit(1);
		}

		/**
		 * exec query
		 */
		Grammar queryPeg = new GrammarFactory().newGrammar("data", pegDef4Query);
		
		if(queryScript.isPresent()) {
			boolean result = parseAndRunQuery(new Executor(), queryPeg, 
					org.peg4d.ParsingSource.loadSource(queryScript.get()), parsedObject);
			System.exit(result ? 0 : 1);
		}
		else {
			Shell shell = new Shell();
			Pair<String, Integer> resultPair = null;
			Executor executor = new Executor();
			while((resultPair = shell.readLine()) != null) {	//TODO:
				if(resultPair.getLeft().equals("")) {
					continue;
				}
				ParsingSource source = Helper.loadLine(queryPeg, "(stdin)", resultPair.getRight(), resultPair.getLeft());
				parseAndRunQuery(executor, queryPeg, source, parsedObject);
			}
			System.out.println("");
		}
	}

	private static boolean parseAndRunQuery(Executor executor, Grammar queryPeg, ParsingSource source, ParsingObject target) {
		ParsingContext queryParserContext = new ParsingContext(source);
		ParsingObject queryTree = queryParserContext.parse2(queryPeg, defaultStartPoint, new ParsingObject(), null);
		if(verboseQuery) {
			System.err.println("parsed query:");
			System.err.println(queryTree);
		}
		if(queryParserContext.hasByteChar()) {
			System.out.println("uncosumed");
			return false;
		}
		try {
			long start = 0;
			long stop = 0;
			if(time) {
				start = System.currentTimeMillis();
			}

			RList resultList = executor.execQuery(queryTree, target);

			if(time) {
				stop = System.currentTimeMillis();
			}
			resultList.stream().forEach(t -> System.out.println(stringify(t)));

			if(time) {
				System.err.println("query execution time: " + unit.convertUnit(stop - start) + unit.getUnitSymbol());
			}
			return true;
		}
		catch(QueryExecutionException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}

	private static String stringify(Object value) {
		if(value instanceof ParsingObject) {
			return ((ParsingObject) value).getText();
		}
		return value.toString();
	}
}

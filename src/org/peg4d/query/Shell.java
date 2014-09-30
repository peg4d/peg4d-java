package org.peg4d.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import jline.ConsoleReader;

public class Shell {
	private final static String startMessage = "Run PegQuery Shell";
	private final String prompt1 = "pegquery> ";
	private final String prompt2 = "          ";
	
	protected int lineNumber;

	protected final ConsoleReader consoleReader;

	public Shell() {
		try {
			this.consoleReader = new ConsoleReader();
			this.lineNumber = 1;
			System.out.println(startMessage);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	protected void incrementLineNumber(List<String> lineList) {
		this.lineNumber += lineList.size();
	}

	protected Optional<String> readSingleLine(String prompt) {
		try {
			return Optional.ofNullable(this.consoleReader.readLine(prompt));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @return
	 * return null if receive Ctrl-D
	 */
	public Pair<String, Integer> readLine() {
		List<String> lineList = new ArrayList<>();
		try {
			String line = this.readSingleLine(this.prompt1).get();
			lineList.add(line);
			while(this.checkLineContinuation(line)) {
				line = this.readSingleLine(this.prompt2).get();
				lineList.add(line);
			}
		}
		catch(NoSuchElementException e) {
			return null;
		}
		StringBuilder sBuilder = new StringBuilder();
		for(String readLine : lineList) {
			sBuilder.append(this.escapeBackSlash(readLine));
			sBuilder.append(System.lineSeparator());
		}
		final int curLineNum = this.lineNumber;
		String joinedLine = sBuilder.toString().trim();
		if(!joinedLine.equals("")) {
			this.incrementLineNumber(lineList);
		}
		return new Pair<String, Integer>(joinedLine, curLineNum);
	}

	/**
	 * 
	 * @param line
	 * not null
	 * @return
	 */
	protected boolean checkLineContinuation(String line) {
		final int size = line.length();
		for(int i = 0; i < size; i++) {
			char ch = line.charAt(i);
			if(ch == '\\' && i == size - 1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * escape back slash just before new line
	 * @param line
	 * @return
	 */
	protected String escapeBackSlash(String line) {
		if(line.endsWith("\\")) {
			return line.substring(0, line.length() - 1);
		}
		return line;
	}
}

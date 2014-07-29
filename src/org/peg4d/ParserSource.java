package org.peg4d;

public abstract class ParserSource {
	public String fileName;
	public Stat stat = null;
	
	public ParserSource(String fileName, long linenum) {
		this.fileName = fileName;
		this.pushLineMemo(0, linenum);
	}

	public abstract long length();
	public abstract long getFileLength();
	public abstract char charAt(long n);
	public abstract String substring(long startIndex, long endIndex);
	public abstract ParserSource trim(long startIndex, long endIndex);

	private class LineMemo {
		long pos;
		long linenum;
		LineMemo prev;
		LineMemo(long pos, long linenum, LineMemo prev) {
			this.pos = pos;
			this.linenum = linenum;
			this.prev = prev;
		}
	}

	private LineMemo lineMemo;
	protected void pushLineMemo(long pos, long linenum) {
		if(lineMemo == null) {
			this.lineMemo = new LineMemo(pos, linenum, null);
		}
		else {
			if(pos - lineMemo.pos > 1000) {
				this.lineMemo = new LineMemo(pos, linenum, this.lineMemo);
			}
		}
	}

	private final LineMemo searchLineMemo(long pos) {
		LineMemo cur = this.lineMemo;
		LineMemo found = null;
		while(cur != null) {
			if(cur.pos <= pos) {
				if(found == null || found.pos < cur.pos) {
					found = cur; 
				}
			}
			cur = cur.prev;
		}
		return found;
	}
	
	public final long getLineNumber(long pos) {
		LineMemo found = this.searchLineMemo(pos);
		long LineNumber = found.linenum;
		long i = found.pos;
		while(i < pos) {
			char ch = this.charAt(i);
			if(ch == '\n') {
				LineNumber = LineNumber + 1;
			}
			i = i + 1;
		}
		this.pushLineMemo(pos, LineNumber);
		return LineNumber;
	}
//	public final int getIndentSize(int fromPosition) {
//		int startPosition = this.getLineStartPosition(fromPosition);
//		int length = 0;
//		for(;startPosition < this.length(); startPosition = startPosition+1) {
//			char ch = charAt(startPosition);
//			if(ch == '\t') {
//				length = length + 8;
//			}
//			else if(ch == ' ') {
//				length = length + 1;
//			}
//			else {
//				break;
//			}
//		}
//		return length;
//
//	}
	public final String getIndentText(long fromPosition) {
		long startPosition = this.getLineStartPosition(fromPosition);
		long i = startPosition;
		for(; i < fromPosition; i++) {
			char ch = this.charAt(i);
			if(ch != ' ' && ch != '\t') {
				break;
			}
		}
		return this.substring(startPosition, i);
	}

	public final long getLineStartPosition(long fromPostion) {
		long startIndex = fromPostion;
		if(!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			char ch = charAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getLineTextAt(long pos) {
		long startIndex = this.getLineStartPosition(pos);
		long endIndex = startIndex;
		while(endIndex < this.length()) {
			char ch = charAt(endIndex);
			if(ch == '\n') {
				break;
			}
			endIndex = endIndex + 1;
		}
		return this.substring(startIndex, endIndex);
	}

	public final String getMarker(long pos) {
		long startIndex = this.getLineStartPosition(pos);
		String markerLine = "";
		long i = startIndex;
		while(i < pos) {
			char ch = charAt(i);
			if(ch == '\n') {
				break;
			}
			if(ch == '\t') {
				markerLine = markerLine + "\t";
			}
			else {
				markerLine = markerLine + " ";
			}
			i = i + 1;
		}
		return markerLine + "^";
	}

	public final String formatErrorHeader(String error, long pos, String message) {
		return "(" + this.fileName + ":" + this.getLineNumber(pos) + ") [" + error +"] " + message;
	}

	public final String formatErrorMessage(String errorType, long pos, String msg) {
		String line = this.getLineTextAt(pos);
		String delim = "\n\t";
		if(line.startsWith("\t") || line.startsWith(" ")) {
			delim = "\n";
		}
		String header = this.formatErrorHeader(errorType, pos, msg);
		String marker = this.getMarker(pos);
		msg = header + delim + line + delim + marker;
		return msg;
	}

	public final String checkFileName(String fileName) {
		int loc = this.fileName.lastIndexOf("/");
		if(loc > 0) {
			return this.fileName.substring(0, loc+1) + fileName; 
		}
		return fileName;
	}


}

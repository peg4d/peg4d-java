package org.peg4d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ParsingSource {
	Grammar peg;
	String fileName;
	Stat stat = null;
	
	public ParsingSource(Grammar peg, String fileName, long linenum) {
		this.peg = peg;
		this.fileName = fileName;
		this.pushLineMemo(0, linenum);
	}

	public abstract long length();
	public abstract int  charAt(long n);
	public abstract boolean match(long pos, byte[] text);
	public abstract String substring(long startIndex, long endIndex);

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
			int ch = this.charAt(i);
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
			int ch = this.charAt(i);
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
			int ch = charAt(startIndex);
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
			int ch = charAt(endIndex);
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
			int ch = charAt(i);
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

	public final String getFilePath(String fileName) {
		int loc = this.fileName.lastIndexOf("/");
		if(loc > 0) {
			return this.fileName.substring(0, loc+1) + fileName; 
		}
		return fileName;
	}
}

class FileSource extends ParsingSource {
	final static int PageSize = 4096;
	
	private RandomAccessFile file;
	private long fileLength = 0;
	private long buffer_offset;
	private byte[] buffer;
	
	private StringSource debug = null;
	private final int FifoSize = 8; 
	private LinkedHashMap<Long, byte[]> fifoMap = null;
	
	FileSource(Grammar peg, String fileName) throws FileNotFoundException {
		super(peg, fileName, 1);
		this.file = new RandomAccessFile(fileName, "r");
		try {
			this.fileLength = this.file.length();
		} catch (IOException e) {
			e.printStackTrace();
			this.fileLength = 0;
		}
		this.buffer_offset = 0;
		if(this.FifoSize > 0) {
			this.fifoMap = new LinkedHashMap<Long, byte[]>(FifoSize) {  //FIFO
				private static final long serialVersionUID = 6725894996600788028L;
				@Override
				protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest)  {
					if(this.size() > FifoSize) {
						return true;			
					}
					return false;
				}
			};
			this.buffer = null;
		}
		else {
			this.fifoMap = null;
			this.buffer = new byte[PageSize];
		}
		this.readMainBuffer(this.buffer_offset);
	}
	@Override
	public final long length() {
		return this.fileLength;
	}

	private long buffer_alignment(long pos) {
		return (pos / PageSize) * PageSize;
	}

	@Override
	public final int charAt(long n) {
		int buffer_pos = (int)(n - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(n);
			this.readMainBuffer(this.buffer_offset);
			buffer_pos = (int)(n - this.buffer_offset);
		}
		return this.buffer[buffer_pos] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		int offset = (int)(pos - this.buffer_offset);
		if(offset >= 0 && offset + text.length <= PageSize) {
			switch(text.length) {
			case 0:
				break;
			case 1:
				if(text[0] == this.buffer[offset]) {
					return true;
				}
				return false;
			case 2:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1]) {
					return true;
				}
				return false;
			case 3:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1] && text[2] == this.buffer[offset+2]) {
					return true;
				}
				return false;
			case 4:
				if(text[0] == this.buffer[offset] && text[1] == this.buffer[offset+1] && text[2] == this.buffer[offset+2] && text[3] == this.buffer[offset+3]) {
					return true;
				}
				return false;
			default:
				for(int i = 0; i < text.length; i++) {
					if(text[i] != this.buffer[offset+i]) {
						return false;
					}
				}
			}
			return true;
		}
		for(int i = 0; i < text.length; i++) {
			if((text[i] & 0xff) != this.charAt(pos + i)) {
				return false;
			}
		}
		return true;
	}
	
	public final int charAtDebug(long n) {
		//assert(n < this.fileLength);
		int c = this.charAt(n);
		if(this.debug != null) {
			int c2 = this.debug.charAt(n);
			if(c != c2) {
				Main._Exit(1, "different " + this.fileName + " pos=" + n + "c='"+c+"', c2='"+c2+"'");
			}
		}
		return c;
	}
	
	@Override
	public final String substring(long startIndex, long endIndex) {
		if(endIndex > startIndex) {
			try {
				long off_s = buffer_alignment(startIndex);
				long off_e = buffer_alignment(endIndex);
				if(off_s == off_e) {
					if(this.buffer_offset != off_s) {
						this.buffer_offset = off_s;
						this.readMainBuffer(this.buffer_offset);
					}
					return new String(this.buffer, (int)(startIndex - this.buffer_offset), (int)(endIndex - startIndex), "UTF8");
				}
				else {
					byte[] b = new byte[(int)(endIndex - startIndex)];
					this.readStringBuffer(startIndex, b);
					return new String(b, "UTF8");
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	public final String substringDebug(long startIndex, long endIndex) {
		String s= this.substring(startIndex, endIndex);
		if(this.debug != null) {
			String s2= this.debug.substring(startIndex, endIndex);
			if(!s.equals(s2)) {
				System.out.println("s1: " + s);
				System.out.println("s2: " + s2);
				Main._Exit(1, "different " + this.fileName + " pos=" + startIndex + " end=" + endIndex);
			}
		}
		return s;
	}

	private void readBuffer(long pos, byte[] b) {
		try {
			//System.out.println("read buffer: " + pos + ", size = " + b.length);
			this.file.seek(pos);
			int readsize = this.file.read(b);
			for(int i = readsize; i < b.length; i++) {
				b[i] = 0;
			}
			if(this.stat != null) {
				stat.readFile(b.length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readMainBuffer(long pos) {
		if(this.fifoMap != null) {
			Long key = pos;
			byte[] buf = this.fifoMap.get(key);
			if(buf == null) {
				buf = new byte[PageSize];
				this.readBuffer(pos, buf);
				this.fifoMap.put(key, buf);
				this.buffer = buf;
			}
			else {
				this.buffer = buf;
			}
		}
		else {
			this.readBuffer(pos, this.buffer);
		}
	}

	private void readStringBuffer(long pos, byte[] buf) {
		if(this.fifoMap != null) {
			int copied = 0;
			long start = pos;
			long end = pos + buf.length;
			while(start < end) {
				long offset = this.buffer_alignment(start);
				if(this.buffer_offset != offset) {
					this.buffer_offset = offset;
					this.readMainBuffer(offset);
				}
				int start_off = (int)(start - offset);
				int end_off = (int)(end - offset);				
				if(end_off <= PageSize) {
					int len = end_off - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					assert(copied == buf.length);
					return;
				}
				else {
					int len = PageSize - start_off;
					System.arraycopy(this.buffer, start_off, buf, copied, len);
					copied += len;
					start += len;
				}
			}
		}
		else {
			this.readBuffer(pos, buf);
		}
	}
}

class StringSource extends ParsingSource {
	private byte[] textBuffer;
	StringSource(Grammar peg, String fileName, long linenum, String sourceText) {
		super(peg, fileName, linenum);
		this.textBuffer = UCharset.toUtf8(sourceText);
	}

//	StringSource(Grammar peg, String fileName) {
//		super(peg, fileName, 1);
//		try {
//			RandomAccessFile f = new RandomAccessFile(fileName, "r");
//			this.textBuffer = new byte[(int)f.length()];
//			f.read(this.textBuffer);
//			f.close();
//		}
//		catch(IOException e) {
//		}
//	}
	
	@Override
	public final long length() {
		return this.textBuffer.length;
	}

	@Override
	public final int charAt(long n) {
		return this.textBuffer[(int)n] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.textBuffer[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.textBuffer, (int)(startIndex), (int)(endIndex - startIndex), "UTF8");
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
}



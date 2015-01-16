package org.peg4d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ParsingSource {
	public final static int EOF = 256; 
	public final static String DefaultEncoding = "UTF8";

	private String     fileName;
	protected long     startLineNum = 1;
	NezLogger  stat = null;
	
	public ParsingSource(String fileName, long linenum) {
		this.fileName = fileName;
		this.startLineNum = linenum;
	}

	public abstract int     byteAt(long pos);
	public abstract int     fastByteAt(long pos);
	public abstract long    length();

	private final static int E = 1;
	final static int[] utf8LengthMatrix = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        E, E, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, E, E,
        0 /* EOF */
	};

	public final static byte[] toUtf8(String text) {
		try {
			return text.getBytes(DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
			Main._Exit(1, "unsupported character: " + e);
		}
		return text.getBytes();
	}

	public final static int lengthOfUtf8(int ch) {
		return ParsingSource.utf8LengthMatrix[ch];
	}

	public final static int lengthOfUtf8(byte ch) {
		return ParsingSource.utf8LengthMatrix[ch & 0xff];
	}

	public int charAt(long pos) {
		int c = byteAt(pos), c2, c3, c4;
		int len = ParsingSource.lengthOfUtf8(c);
		switch(len) {
		case 1:
			return c;
		case 2:
			// 0b11111 = 31
			// 0b111111 = 63
			c2 = byteAt(pos + 1) & 63;
			return ((c & 31) << 6) | c2;
		case 3:
			c2 = byteAt(pos + 1) & 63;
			c3 = byteAt(pos + 2) & 63;
			return ((c & 15) << 12) | c2 << 6 | c3;
		case 4:
			c2 = byteAt(pos + 1) & 63;
			c3 = byteAt(pos + 2) & 63;
			c4 = byteAt(pos + 3) & 63;
			return ((c & 7) << 18) | c2 << 12 | c3 << 6 | c4;
		}
		return -1;
	}

	public final int charLength(long pos) {
		int c = byteAt(pos);
		return ParsingSource.lengthOfUtf8(c);
	}
	
	public abstract boolean match(long pos, byte[] text);
	public abstract String  substring(long startIndex, long endIndex);
	public abstract long    linenum(long pos);
	
	public final String getResourceName() {
		return fileName;
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
			int ch = byteAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getIndentText(long fromPosition) {
		long startPosition = this.getLineStartPosition(fromPosition);
		long i = startPosition;
		String indent = "";
		for(; i < fromPosition; i++) {
			int ch = this.byteAt(i);
			if(ch != ' ' && ch != '\t') {
				if(i + 1 != fromPosition) {
					for(long j = i;j < fromPosition; j++) {
						indent = indent + " ";
					}
				}
				break;
			}
		}
		indent = this.substring(startPosition, i) + indent;
		return indent;
	}

	public final String formatPositionMessage(String messageType, long pos, String message) {
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ") [" + messageType +"] " + message;
	}

	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}
	
	public final String getTextAround(long pos, String delim) {
		int ch = 0;
		if(pos < 0) {
			pos = 0;
		}
		while(this.byteAt(pos) == ParsingSource.EOF && pos > 0) {
			pos -= 1;
		}
		long startIndex = pos;
		while(startIndex > 0) {
			ch = byteAt(startIndex);
			if(ch == '\n' && pos - startIndex > 0) {
				startIndex = startIndex + 1;
				break;
			}
			if(pos - startIndex > 60 && ch < 128) {
				break;
			}
			startIndex = startIndex - 1;
		}
		long endIndex = pos + 1;
		while((ch = byteAt(endIndex)) != ParsingSource.EOF) {
			if(ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
				break;
			}
			endIndex = endIndex + 1;
		}
		StringBuilder source = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for(long i = startIndex; i < endIndex; i++) {
			ch = byteAt(i);
			if(ch == '\n') {
				source.append("\\N");
				if(i == pos) {
					marker.append("^^");
				}
				else {
					marker.append("\\N");
				}
			}
			else if(ch == '\t') {
				source.append("    ");
				if(i == pos) {
					marker.append("^^^^");
				}
				else {
					marker.append("    ");
				}
			}
			else {
				source.append((char)ch);
				if(i == pos) {
					marker.append("^");
				}
				else {
					marker.append(" ");
				}
			}
		}
		return delim + source.toString() + delim + marker.toString();
	}

	public final String getFilePath(String fileName) {
		int loc = this.getResourceName().lastIndexOf("/");
		if(loc > 0) {
			return this.getResourceName().substring(0, loc+1) + fileName; 
		}
		return fileName;
	}

	public final static ParsingSource newStringSource(ParsingObject po) {
		ParsingSource s = po.getSource();
		return new StringSource(s.getResourceName(), s.linenum(po.getSourcePosition()), po.getText());
	}

	public final static ParsingSource loadSource(String fileName) {
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				File f = new File(fileName);
				if(f.length() > 128 * 1024) {
					return new FileSource(fileName);
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
			return new StringSource(fileName, 1, builder.toString());
		}
		catch(IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}
}

class StringSource extends ParsingSource {
	private byte[] utf8;
	long textLength;
	
	StringSource(String sourceText) {
		super("(string)", 1);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length-1;
	}
	
	StringSource(String fileName, long linenum, String sourceText) {
		super(fileName, linenum);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length-1;
	}
	
	private final byte[] toZeroTerminalByteSequence(String s) {
		byte[] b = ParsingSource.toUtf8(s);
		byte[] b2 = new byte[b.length+1];
		System.arraycopy(b, 0, b2, 0, b.length);
		return b2;
	}
	
	@Override
	public final long length() {
		return this.textLength;
	}

	@Override
	public final int byteAt(long pos) {
		if(pos < this.textLength) {
			return this.utf8[(int)pos] & 0xff;
		}
		return ParsingSource.EOF;
	}

	@Override
	public final int fastByteAt(long pos) {
		return this.utf8[(int)pos] & 0xff;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if(pos + text.length > this.textLength) {
			return false;
		}
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.utf8[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.utf8, (int)(startIndex), (int)(endIndex - startIndex), ParsingSource.DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		int end = (int)pos;
		if(end >= this.utf8.length) {
			end = this.utf8.length;
		}
		for(int i = 0; i < end; i++) {
			if(this.utf8[i] == '\n') {
				count++;
			}
		}
		return count;
	}
}

class FileSource extends ParsingSource {
	public final static int PageSize = 4096;
	
	private RandomAccessFile file;
	private long fileLength = 0;
	private long buffer_offset;
	private byte[] buffer;
	private long lines[];
	
	private final int FifoSize = 8; 
	private LinkedHashMap<Long, byte[]> fifoMap = null;
	
	FileSource(String fileName) throws IOException {
		super(fileName, 1);
		this.file = new RandomAccessFile(fileName, "r");
		this.fileLength = this.file.length();
		this.buffer_offset = 0;
		lines = new long[((int)this.fileLength / PageSize) + 1];
		lines[0] = 1;
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
	public final int byteAt(long pos) {
		if(pos < this.fileLength) {
			return fastByteAt(pos);
		}
		return ParsingSource.EOF;
	}
	
	@Override
	public final int fastByteAt(long pos) {
		int buffer_pos = (int)(pos - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(pos);
			this.readMainBuffer(this.buffer_offset);
			buffer_pos = (int)(pos - this.buffer_offset);
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
			if((text[i] & 0xff) != this.byteAt(pos + i)) {
				return false;
			}
		}
		return true;
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
					return new String(this.buffer, (int)(startIndex - this.buffer_offset), (int)(endIndex - startIndex), ParsingSource.DefaultEncoding);
				}
				else {
					byte[] b = new byte[(int)(endIndex - startIndex)];
					this.readStringBuffer(startIndex, b);
					return new String(b, ParsingSource.DefaultEncoding);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	private int lineIndex(long pos) {
		return (int)(pos / this.PageSize);
	}

	private long startLineNum(long pos) {
		int index = lineIndex(pos);
		return this.lines[index];
	}
	
	@Override
	public final long linenum(long pos) {
		long count = startLineNum(pos);
		byteAt(pos); // restore buffer at pos
		int offset = (int)(pos - this.buffer_offset);
		for(int i = 0; i < offset; i++) {
			if(this.buffer[i] == '\n') {
				count++;
			}
		}
		return count;
	}
	
	private void readMainBuffer(long pos) {
		int index = lineIndex(pos);
		if(this.lines[index] == 0) {
			long count = this.lines[index-1];
			for(int i = 0; i < this.buffer.length; i++) {
				if(this.buffer[i] == '\n') {
					count++;
				}
			}
			this.lines[index] = count;
		}
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
	
	private void readBuffer(long pos, byte[] b) {
		try {
			this.file.seek(pos);
			int readsize = this.file.read(b);
			for(int i = readsize; i < b.length; i++) {
				b[i] = 0;
			}
//			if(this.stat != null) {
//				stat.readFile(b.length);
//			}
		} catch (IOException e) {
			e.printStackTrace();
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
	
//	public final String substringDebug(long startIndex, long endIndex) {
//		String s= this.substring(startIndex, endIndex);
//		if(this.debug != null) {
//			String s2= this.debug.substring(startIndex, endIndex);
//			if(!s.equals(s2)) {
//				System.out.println("s1: " + s);
//				System.out.println("s2: " + s2);
//				Main._Exit(1, "different " + this.fileName + " pos=" + startIndex + " end=" + endIndex);
//			}
//		}
//		return s;
//	}
//	
//	public final int charAtDebug(long n) {
//		//assert(n < this.fileLength);
//		int c = this.charAt(n);
//		if(this.debug != null) {
//			int c2 = this.debug.charAt(n);
//			if(c != c2) {
//				Main._Exit(1, "different " + this.fileName + " pos=" + n + "c='"+c+"', c2='"+c2+"'");
//			}
//		}
//		return c;
//	}

}

//class InputStreamSource extends ParsingSource {
//	public InputStreamSource(Grammar peg, String fileName, InputStream inStream) {
//		super(peg, fileName, 1);
//	}
//
//	@Override
//	public long length() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int byteAt(long n) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int fastByteAt(long pos) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public boolean match(long pos, byte[] text) {
//		return false;
//	}
//
//	@Override
//	public String substring(long startIndex, long endIndex) {
//		return null;
//	}
//
//	@Override
//	public long linenum(long pos) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//
//}

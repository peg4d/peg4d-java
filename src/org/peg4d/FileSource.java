package org.peg4d;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileSource extends ParserSource {
	final static int PageSize = 4096;
	
	private RandomAccessFile file;
	private long fileLength = 0;
	private long buffer_offset;
	private byte[] buffer;
	
	private StringSource debug = null;
	private final int FifoSize = 8; 
	private LinkedHashMap<Long, byte[]> fifoMap = null;
	
	public FileSource(String fileName) throws FileNotFoundException {
		super(fileName, 1);
		this.file = new RandomAccessFile(fileName, "r");
		try {
			this.fileLength = this.file.length();
		} catch (IOException e) {
			e.printStackTrace();
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
		//this.debug = new StringSource(fileName);
	}
	public final long length() {
		return this.fileLength;
	}
	public final long getFileLength() {
		return this.fileLength;
	}
	private long buffer_alignment(long pos) {
		return (pos / PageSize) * PageSize;
	}

	public final char charAt(long n) {
		int buffer_pos = (int)(n - this.buffer_offset);
		if(!(buffer_pos >= 0 && buffer_pos < PageSize)) {
			this.buffer_offset = buffer_alignment(n);
			this.readMainBuffer(this.buffer_offset);
			buffer_pos = (int)(n - this.buffer_offset);
		}
		return (char)this.buffer[buffer_pos];
	}
	
	public final char charAtDebug(long n) {
		//assert(n < this.fileLength);
		char c = this.charAt(n);
		if(this.debug != null) {
			char c2 = this.debug.charAt(n);
			if(c != c2) {
				Main._Exit(1, "different " + this.fileName + " pos=" + n + "c='"+(int)c+"', c2='"+(int)c2+"'");
			}
		}
		return c;
	}
	
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
			this.statIOCount += 1;
			this.statReadLength += b.length;
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

	@Override
	public final ParserSource trim(long startIndex, long endIndex) {
		//long pos = this.getLineStartPosition(startIndex);
		long linenum = this.getLineNumber(startIndex);
		String s = this.substring(startIndex, endIndex);
		//System.out.println("trim: " + linenum + " : " + s);
		return new StringSource(this.fileName, linenum, s);
	}

}

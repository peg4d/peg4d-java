package org.peg4d;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

class ParsingStatistics {

	ParsingStatistics(Grammar peg, ParsingSource source) {
		this.PegSize = peg.getRuleSize();
		this.setText("Peg", peg.getName());
		this.setCount("PegSize", peg.getRuleSize());
		source.stat = this;
	}
	
	int PegSize;
	int statOptimizedPeg = 0;

	long statFileLength = 0;  /* [bytes] */
	long statIOCount    = 0;
	long statReadLength = 0;  /* [bytes] */
	
	final void readFile(int length) {
		this.statIOCount += 1;
		this.statReadLength += length;
	}

	long UnconsumedLength = 0; /* [chars] */
	long ConsumedLength = 0;   /* [chars] */

	long BacktrackCount = 0;
	long BacktrackSize = 0;
	
	long MostProccedPostion = 0;
	long WorstBacktrackSize = 0;
	long WorstBacktrackPosition = 0;

	public final boolean statBacktrack(long backed_pos, long current_pos) {
		boolean maximumConsumed = false;
		this.BacktrackCount = this.BacktrackCount + 1;
		this.BacktrackSize  = this.BacktrackSize + current_pos - backed_pos;
		if(this.MostProccedPostion < current_pos) {
			this.MostProccedPostion = current_pos;
			maximumConsumed = true;
		}
		long len = this.MostProccedPostion - backed_pos;
		this.countBacktrackLength(len);
		if(len > this.WorstBacktrackSize) {
			this.WorstBacktrackSize = len;
			this.WorstBacktrackPosition = backed_pos;
		}
		return maximumConsumed;
	}

	int backtrackCount[] = new int[32];
	private void countBacktrackLength(long len) {
		int n = (int)(Math.log(len) / Math.log(2.0));
		backtrackCount[n] += 1;
		//if(backtrackCount[n] % 100 == 0) System.out.print(".");
	}
	
//	int BacktrackHistgram[] = new int[4096];
//	
//	private void setHistgram(int pos, int len) {
//		int end = pos + len < BacktrackHistgram.length ? pos + len : BacktrackHistgram.length;
//		for(int i = pos; i < end; i++) {
//			this.BacktrackHistgram[i] += 1;
//		}
//	}
//
//	private void showHistgram() {
//		int max = 1;
//		for(int i = 0; i < this.BacktrackHistgram.length; i++) {
//			if(max < this.BacktrackHistgram[i]) {
//				max = this.BacktrackHistgram[i];
//			}
//		}
//		for(int i = 0; i < this.BacktrackHistgram.length/10; i++) {
//			System.out.println(""+i + "\t: " + this.BacktrackHistgram[i] + "\t" + star(this.BacktrackHistgram[i], max));
//		}
//	}
//
//	private String star(int n, int m) {
//		n = 30 * n / m;
//		String s = "";
//		for(int i = 0; i < n; i++) {
//			s += "*";
//		}
//		return s;
//	}
	
	int  NewObjectCount = 0;
	int  ObjectCount    = 0;
	int  UsedObjectCount = 0;
	int  DisposalObjectCount = 0;
	int  NodeCount = 0;
	int  EdgeCount = 0;
	int  ObjectMaxDepth = 0;
	long HeapSize = 0;
	long UsedHeapSize = 0;
	
	final void countObjectCreation() {
		this.ObjectCount += 1;
	}
	
	final void statObject(ParsingObject pego) {
		UMap<ObjectCounter> m = new UMap<ObjectCounter>();
		this.UsedObjectCount = 0;
		this.EdgeCount = 0;
		this.NodeCount = 0;
		this.ObjectMaxDepth = 0;
		
		this.statObjectStructure(pego, 1, m);
		
		this.setCount("CreatedObject", this.ObjectCount);
		this.setCount("UsedObject", this.UsedObjectCount);
//		this.setCount("DisposedObject", this.ObjectCount - this.UsedObjectCount);
//		this.setRatio("Disposal/Used", this.ObjectCount - this.UsedObjectCount, this.UsedObjectCount);
//		this.setCount("NewObject", this.NewObjectCount);
//		this.setRatio("New/Creation", this.NewObjectCount - this.ObjectCount, this.ObjectCount);
		this.setCount("ObjectEdge", this.EdgeCount);
		this.setCount("ObjectNode", this.NodeCount);
		this.setCount("ObjectDepth", this.ObjectMaxDepth);
	}

	private void statObjectStructure(ParsingObject pego, int depth, UMap<ObjectCounter> m) {
		if(depth > this.ObjectMaxDepth) {
			this.ObjectMaxDepth = depth;
		}
		this.UsedObjectCount += 1;
		if(pego.size() == 0) {
			this.EdgeCount += 1;
		}
		else {
			this.NodeCount += 1;
			for(int i = 0; i < pego.size(); i++) {
				this.statObjectStructure(pego.get(i), depth+1, m);
			}
		}
		String tag = pego.getTag().toString();
		ObjectCounter c = m.get(tag);
		if(c == null) {
			c = new ObjectCounter();
			c.tag = tag;
			m.put(tag, c);
		}
		c.count += 1;
		c.length += pego.getLength();
		c.size += pego.size();
	}
	
	class ObjectCounter {
		String tag;
		int count = 0;
		long length = 0;
		int size = 0;
	}
	

	long InitTotalHeap = 0;
	long StartTime = 0;
	long ErapsedTime = 0;
	
	double Latency  = 0;

	public void start() {
		System.gc(); // meaningless ?
		this.BacktrackCount = 0;
		this.BacktrackSize = 0;
		this.WorstBacktrackPosition = 0;
		this.WorstBacktrackSize = 0;
		
		this.InitTotalHeap = Runtime.getRuntime().totalMemory();
		long free =  Runtime.getRuntime().freeMemory();
		this.UsedHeapSize =  InitTotalHeap - free;
		this.StartTime = System.currentTimeMillis();
	}
	
	public long end() {
		this.ErapsedTime = (System.currentTimeMillis() - StartTime);
		return this.ErapsedTime;
	}
	
	public void end(ParsingObject pego, ParsingContext p) {
		if(this.ErapsedTime == 0) {
			this.ErapsedTime = (System.currentTimeMillis() - StartTime);
		}

		System.gc(); // meaningless ?
		this.ConsumedLength = p.getPosition();
		this.UnconsumedLength = p.source.length() - p.getPosition();
		this.statFileLength = p.source.length();

		long total = Runtime.getRuntime().totalMemory();
		long free =  Runtime.getRuntime().freeMemory();
		this.HeapSize = total - free;
		this.UsedHeapSize =  this.HeapSize - this.UsedHeapSize;

		
		this.set(new vText("Parser", p.getName()));
		this.setCount("Optimization", Main.OptimizationLevel);
		this.setCount("BacktrackBufferSize", MemoizationManager.BacktrackBufferSize);

		String fileName = p.source.getResourceName();
		if(fileName.lastIndexOf('/') > 0) {
			fileName = fileName.substring(fileName.lastIndexOf('/')+1);
		}
		String id = fileName;
		if(fileName.lastIndexOf('.') > 0) {
			id = fileName.substring(0, fileName.lastIndexOf('.'));
		}
		id += "#" ;//+ p.peg.DefinedExpressionSize;

		this.set(new vText("FileName", fileName));
		this.setCount1("FileSize", this.statFileLength);
		this.setCount1("DiskIO", this.statIOCount);
		this.setCount1("ReadFileSize", this.statReadLength);
		this.setRatio1("IO Ratio", this.statReadLength, this.statFileLength);

		this.setCount("Memory", this.InitTotalHeap);
		id = id + ((Main.RecognitionOnlyMode) ? 'C' : 'O');
		id = id + Main.OptimizationLevel + "." + MemoizationManager.BacktrackBufferSize + "."  + (this.InitTotalHeap / (1024*1024)) +"M";
		this.setCount("HeapSize", this.HeapSize);
		this.setRatio1("Heap/File", this.HeapSize, this.statFileLength);		
		this.setCount("UsedHeapSize", this.UsedHeapSize);
		
		this.setCount("ConsumedLength", this.ConsumedLength);
		this.setCount1("UnconsumedLength", this.UnconsumedLength);
		
		this.setCount("ParsingLength", this.ConsumedLength + this.BacktrackSize);
		this.setRatio("Parsing/Consumed", this.ConsumedLength + this.BacktrackSize, this.ConsumedLength);
		
		this.setCount("Backtrack", this.BacktrackCount);
		this.setCount("WorstBacktrack", this.WorstBacktrackSize);
		
		this.setRatio("BacktrackAverage", this.BacktrackSize, this.BacktrackCount);
//		this.setRatio("Backtrack1", this.backtrackCount[0], this.BacktrackCount);
//		this.setRatio("Backtrack2", this.backtrackCount[1], this.BacktrackCount);
//		this.setRatio("Backtrack4", this.backtrackCount[2], this.BacktrackCount);
//		this.setRatio("Backtrack8", this.backtrackCount[3], this.BacktrackCount);
//		this.setRatio("Backtrack16", this.backtrackCount[4], this.BacktrackCount);
//		this.setRatio("Backtrack32", this.backtrackCount[5], this.BacktrackCount);
//		this.setRatio("Backtrack64", this.backtrackCount[6], this.BacktrackCount);
//		this.setRatio("Backtrack128", this.backtrackCount[7], this.BacktrackCount);
//		this.setRatio1("Backtrack256", this.backtrackCount[8], this.BacktrackCount);
//		this.setRatio1("Backtrack512", this.backtrackCount[9], this.BacktrackCount);
//		this.setRatio1("Backtrack1024", this.backtrackCount[10], this.BacktrackCount);
		
		p.memoMap.stat(this);
//		ckeckRepeatCounter();

		if(pego != null) {
			this.statObject(pego);
		}
		//p.peg.updateStat(this);
		this.setText("StatId", id);
		this.setCount("Latency", this.ErapsedTime);  // ms
		this.setRatio("Throughput", this.ConsumedLength, this.ErapsedTime);
		
		this.writeCSV();
		//System.out.println("WorstBacktrack: " + p.source.substring(this.WorstBacktrackPosition, this.WorstBacktrackPosition + this.WorstBacktrackSize));
		double cf = 0;
		for(int i = 0; i < 16; i++) {
			int n = 1 << i;
			double f = (double)this.backtrackCount[i] / this.BacktrackCount;
			cf += this.backtrackCount[i];
			System.out.println(String.format("%d\t%d\t%2.3f\t%2.3f", n, this.backtrackCount[i], f, (cf / this.BacktrackCount)));
			if(n > this.WorstBacktrackSize) break;
		}
	}
	
	private UMap<vData> csvMap = new UMap<vData>();
	
	private void set (vData data) {
		if(Main.VerboseMode) {
			StringBuilder sb = new StringBuilder();
			data.stringfy(sb, false);
			System.out.println(sb.toString());
		}
		this.csvMap.put(data.key, data);
	}
	
	private abstract class vData {
		String key;
		vData(String key) {
			this.key = key;
		}
		abstract void stringfy(StringBuilder sb, boolean raw);
	}
	
	private class vText extends vData {
		String value;
		vText(String key, String value) {
			super(key);
			this.value = value;
		}
		@Override
		void stringfy(StringBuilder sb, boolean raw) {
			if(!raw) {
				sb.append(this.key);
				sb.append(": ");
			}
			sb.append(this.value);
		}
	}

	private class vCount extends vData {
		long value;
		vCount(String key, long value) {
			super(key);
			this.value = value;
		}
		@Override
		void stringfy(StringBuilder sb, boolean raw) {
			if(!raw) {
				sb.append(this.key);
				sb.append(": ");
			}
			sb.append(""+this.value);
		}
	}

	private class vRatio extends vData {
		double value;
		vRatio(String key, long v1, long v2) {
			super(key);
			this.value = (double)v1 / v2;
		}
		@Override
		void stringfy(StringBuilder sb, boolean raw) {
			if(!raw) {
				sb.append(this.key);
				sb.append(": ");
				sb.append(String.format("%.4f", this.value));
			}
			else {
				if(Double.isInfinite(this.value) || Double.isNaN(this.value)) {
					sb.append("");
				}
				else {
					sb.append(String.format("%.5f", this.value));
				}
			}
		}
	}

	public final void setText(String key, String v) {
		this.set(new vText(key, v));
	}

	public final void setCount(String key, long v) {
		this.set(new vCount(key, v));
	}

	public final void setCount1(String key, long v) {
		if(v > 0) {
			this.set(new vCount(key, v));
		}
	}

	public final void setRatio(String key, long v, long v2) {
		this.set(new vRatio(key, v, v2));
	}

	public final void setRatio1(String key, long v, long v2) {
		if(v > 0) {
			this.set(new vRatio(key, v, v2));
		}
	}

	public final void write(String text) {
		System.out.print(text);
	}
	
	private final void CSV(StringBuilder sb, String key) {
		vData d = this.csvMap.get(key);
		if(d != null) {
			sb.append(key);
			sb.append(",");
			d.stringfy(sb, true);
			sb.append(",");
		}
	}

	public final void writeCSV() {
		StringBuilder sb = new StringBuilder();
		sb.append("v5,");
		this.CSV(sb, "Id");
		this.CSV(sb, "Memory");
		this.CSV(sb, "Optimization");
		this.CSV(sb, "BacktrackBufferSize");
		/** **/
		this.CSV(sb, "Peg");
		this.CSV(sb, "PegSize");		
		/** **/
		this.CSV(sb, "FileName");
		this.CSV(sb, "FileSize");
		this.CSV(sb, "Latency");
		this.CSV(sb, "Throughput");
		this.CSV(sb, "HeapSize");
		this.CSV(sb, "Heap/File");
//		this.CSV(sb, "UsedObject");

		/** **/
		this.CSV(sb, "Backtrack");
		this.CSV(sb, "WorstBacktrack");
		this.CSV(sb, "ParsingLength");
		this.CSV(sb, "Parsing/Consumed");
		
		/** **/
		this.CSV(sb, "Memo");
		this.CSV(sb, "MemoStored");
		this.CSV(sb, "MemoUsed");
		this.CSV(sb, "Used/Stored");
		this.CSV(sb, "UnusedNonTerminal");
		this.CSV(sb, "DeactivatedNonTerminal");

		
//		/** **/
//		this.CSV(sb, "ObjectEdge");
//		this.CSV(sb, "ObjectNode");
//		this.CSV(sb, "ObjectDepth");

		/** **/
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");
		sb.append("" + sdf1.format(new Date()));
		String csv = sb.toString();
		System.out.println(csv);
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Main.CSVFileName, true)))) {
		    out.println(csv);
		}catch (IOException e) {
			Main._Exit(1, "Can't write csv log: " + Main.CSVFileName);
		}
	}
	
	
}

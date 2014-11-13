package org.peg4d.jvm;

public class OptimizedByteCodeGenerator extends JavaByteCodeGenerator {
	public OptimizedByteCodeGenerator() {
		this(false);
	}

	protected OptimizedByteCodeGenerator(boolean enableDump) {
		super(true, enableDump);
	}
}

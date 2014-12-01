package org.peg4d.jvm;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.peg4d.ParsingContext;

public class JvmRuntime {
	/**
	 * 
	 * @param lookup
	 * @param methodName
	 * dummy
	 * @param type
	 * must be int[]
	 * @param encodedUtf8Codes
	 * @return
	 */
	public final static CallSite bsmUtf8Codes(MethodHandles.Lookup lookup, String methodName, MethodType type, String codeString) {
		String[] codeStrings = codeString.split(",");
		final int size = codeStrings.length;
		int[] utf8Codes = new int[size];
		for(int i = 0; i < size; i++) {
			utf8Codes[i] = Integer.parseInt(codeStrings[i], 16);
		}
		return new ConstantCallSite(MethodHandles.constant(int[].class, utf8Codes));
	}

	public final static boolean matchUtf8(ParsingContext context, int[] utf8Codes) {
		long pos = context.getPosition();
		int mark = context.markLogStack();
		int size = utf8Codes.length;
		for(int i = 0; i < size; i++) {
			if(context.source.byteAt(context.pos) != utf8Codes[i]) {
				context.failure(null);
				context.abortLog(mark);
				context.rollback(pos);
				return false;
			}
			context.consume(1);
		}
		return true;
	}

	public final static boolean matchUtf8NoRollback(ParsingContext context, int[] utf8Codes) {
		int size = utf8Codes.length;
		for(int i = 0; i < size; i++) {
			if(context.source.byteAt(context.pos) != utf8Codes[i]) {
				context.failure(null);
				return false;
			}
			context.consume(1);
		}
		return true;
	}
}

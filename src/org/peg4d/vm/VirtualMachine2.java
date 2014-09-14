package org.peg4d.vm;

import org.peg4d.ParsingCharset;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class VirtualMachine2 {
	
	public static ParsingObject run(ParsingSource s, ParsingObject left, long pos, int pc, Opcode2[] code) {
		long fpos = -1;
		long[] lstack = new long[4096];
		ParsingObject[] ostack = new ParsingObject[4096];
		int top = 0;

		lstack[0] = 0;
		Opcode2 op = code[pc];
		while(true) {
			switch(op.opcode) {
			case NOP:
				break;
			case EXIT:
				return left;
			case JUMP:
				pc = op.n;
				break;
			case CALL:
				lstack[top] = pc;
				top++;
				pc = op.n;
				break;
			case RET:
				pc = (int)lstack[top];
				top--;
				break;
			case IFSUCC:
				if(left != null) {
					pc = op.n;
				}
				break;
			case IFFAIL:
				if(left == null) {
					pc = op.n;
				}
				break;
			case IFp:
				if(lstack[top + op.n] == pos) {
					pc = op.n;
				}
				break;
			case IFZERO:
				if(lstack[top + op.n] == 0) {
					pc = op.n;
				}
				break;
			case PUSHo:
				ostack[top] = left;
				top++;
				break;
			case PUSHp:
				lstack[top] = pos;
				top++;
				break;
			case PUSHf:
				lstack[top] = fpos;
				top++;
				break;
			case LOADo:
				ostack[top + op.n] = left;
				break;
			case LOADp:
				lstack[top + op.n] = pos;
				break;
			case LOADf:
				lstack[top + op.n ] = fpos;
				break;
			case STOREo:
				left = ostack[top + op.n] = left;
				break;
			case STOREp:
				pos = lstack[top + op.n] = pos;
				break;
			case STOREf:
				fpos = lstack[top + op.n ] = fpos;
				break;
			case POP:
				top--;
				break;
			case POPn:
				top -= op.n;
				break;
			case CONSUME:
				pos += lstack[top];
				break;
			case FAIL:
				if(pos > fpos) {  // adding error location
					fpos = pos;
					//perror(fpos, null);
				}
				left = null;
				break;
			case BYTE:
				lstack[top] = ((ParsingCharset)op.v).consume(s, pos);  // consumed
				break;
			case CHAR:
				lstack[top] = ((ParsingCharset)op.v).consume(s, pos); // consumed
				break;
			case BYTE1:
				lstack[top] = (s.byteAt(pos) == op.n) ? 1: 0;  // consumed
				break;
			case CHAR2:
				lstack[top] = (s.charAt(pos) == op.n) ? 2: 0;  // consumed
				break;
			case CHAR3:
				lstack[top] = (s.charAt(pos) == op.n) ? 3: 0;  // consumed
				break;
			case CHAR4:
				lstack[top] = (s.charAt(pos) == op.n) ? 4: 0;
				break;
			case NEW:
				//left = new ParsingObject(this.emptyTag, s, pos, null);
				//lstack[top] = this.objectMark();
				top++;
				break;
			case COMMIT:
				//this.commit(lstack[top]);
				top--;
				break;
			case ABORT:
				//this.abort(lstack[top]);
				top--;
				break;
			case LINK:
				break;
			}
			pc = pc + 1;
		}
	}
		
	boolean enableTransCapturing = true;
	
	final boolean canTransCapture() {
		return this.enableTransCapturing;
	}
	
	final boolean setRecognitionMode(boolean recognitionMode) {
		boolean b = this.enableTransCapturing;
		this.enableTransCapturing = recognitionMode;
		return b;
	}
	
}








package org.peg4d.vm;

import org.peg4d.ParsingContext;

public class VirtualMachine {
	public static void run(ParsingContext c, int pc, Opcode[] code) {
		Opcode op = code[pc];
		while(true) {
			switch(op.opcode) {
			case EXIT:
				return;
			case JUMP:
				pc = op.ndata;
				break;
			case CALL:
				c.lpush(pc);
				pc = op.ndata;
				break;
			case RET:
			    pc = c.lpop2i();
				break;
			case IFSUCC:
				if(!c.isFailure()) {
					pc = op.ndata;
				}
				break;
			case IFFAIL:
				if(c.isFailure()) {
					pc = op.ndata;
				}
				break;
			case opFailure:
				c.opFailure();
				break;
			case opMatchText:
				c.opMatchText(op.bdata);
				break;
			case opMatchByteChar:
				c.opMatchByteChar(op.ndata);
				break;
			case opMatchCharset:
				
				break;
			case opMatchAnyChar:
				c.opMatchAnyChar();
				break;
			case opMatchTextNot:
				c.opMatchTextNot(op.bdata);
				break;
			case opMatchByteCharNot:
				c.opMatchByteCharNot(op.ndata);
				break;
			case opMatchCharsetNot:
				
			case opMatchOptionalText:
				c.opMatchOptionalText(op.bdata);
				break;
			case opMatchOptionalByteChar:
				c.opMatchOptionalByteChar(op.ndata);
				break;
			case opMatchOptionalCharset:

				break;
			case opRememberPosition:
				c.opRememberPosition();
				break;
			case opCommitPosition:
				c.opCommitPosition();
				break;
			case opBacktrackPosition:
				c.opBacktrackPosition();
				break;
			case opRememberSequencePosition:
				c.opRememberSequencePosition();
				break;
			case opComitSequencePosition:
				c.opComitSequencePosition();
				break;
			case opBackTrackSequencePosition:
				c.opBackTrackSequencePosition();
				break;
			case opRememberFailurePosition:
				c.opRememberFailurePosition();
				break;
			case opUpdateFailurePosition:
				c.opUpdateFailurePosition();
				break;
			case opForgetFailurePosition:
				c.opForgetFailurePosition();
				break;
			case opStoreObject:
				c.opStoreObject();
				break;
			case opDropStoredObject:
				c.opDropStoredObject();
				break;
			case opRestoreObject:
				c.opRestoreObject();
				break;
			case opRestoreObjectIfFailure:
				c.opRestoreObjectIfFailure();
				break;
			case opRestoreNegativeObject:
				c.opRestoreNegativeObject();
				break;
			case opConnectObject:
				c.opConnectObject(op.ndata);
				break;
			case opDisableTransCapture:
				c.opDisableTransCapture();
				break;
			case opEnableTransCapture:
				c.opEnableTransCapture();
				break;				
			case opNewObject:
				//c.opNewObject(e);
				break;
			case opLeftJoinObject:
				//c.opLeftJoinObject(e);
				break;
			case opCommitObject:
				c.opCommitObject();
				break;
			case opRefreshStoredObject:
				c.opRefreshStoredObject();
				break;
			case opTagging:
				//c.opTagging(tag);
				break;
			case opValue:
				c.opValue("");
				break;
			case opIndent:
				break;
			}
			pc = pc + 1;
		}
	}
	
}








package org.peg4d.vm;

//import org.peg4d.SimpleVmParsingContext;
//
//public class SimpleVirtualMachine {
//	public static void run(SimpleVmParsingContext c, int pc, Opcode[] code) {
//		boolean exit = true;
//		while(exit == true) {
//			Opcode op = code[pc];
//			switch(op.opcode) {
//			case EXIT:
//				exit = false;
//				break;
//			case JUMP:
//				pc = op.ndata;
//				break;
//			case CALL:
//				c.lpush(pc);
//				pc = op.ndata;
//				break;
//			case RET:
//			    pc = c.lpop2i();
//				break;
//			case IFSUCC:
//				if(!c.isFailure()) {
//					pc = op.ndata;
//				}
//				break;
//			case IFFAIL:
//				if(c.isFailure()) {
//					pc = op.ndata;
//				}
//				break;
//			case opMatchText:
//				c.opMatchText(op.bdata);
//				break;
//			case opMatchCharset:
//				c.opMatchCharset(op.bdata);
//				break;
//			case opMatchAnyChar:
//				c.opMatchAnyChar();
//				break;
//			case opRememberPosition:
//				c.opRememberPosition();
//				break;
//			case opCommitPosition:
//				c.opCommitPosition();
//				break;
//			case opBacktrackPosition:
//				c.opBacktrackPosition();
//				break;
//			case opRememberFailurePosition:
//				c.opRememberFailurePosition();
//				break;
//			case opUpdateFailurePosition:
//				c.opUpdateFailurePosition();
//				break;
//			case opForgetFailurePosition:
//				c.opForgetFailurePosition();
//				break;
//			case opStoreObject:
//				c.opStoreObject();
//				break;
//			case opDropStoredObject:
//				c.opDropStoredObject();
//				break;
//			case opRestoreObject:
//				c.opRestoreObject();
//				break;
//			case opRestoreNegativeObject:
//				c.opRestoreNegativeObject();
//				break;
//			case opConnectObject:
//				c.opConnectObject(op.ndata);
//				break;			
//			case opNewObject:
//				c.opNewObject();
//				break;
//			case opCommitObject:
//				c.opCommitObject();
//				break;
//			case opTagging:
//				c.opTagging(op);
//				break;
//			}
//			pc = pc + 1;
//		}
//	}
//}

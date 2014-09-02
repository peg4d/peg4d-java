package org.peg4d.vm;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class Machine {

	private static ParsingObject failure(MachineContext c) {
		return null;
	}
	
	private static void link(ParsingObject parent, ParsingObject child, int index) {
		
	}

	public final static void PUSH_POS(MachineContext c) {
	    c.lstack[c.lstacktop] = c.pos;
	    c.lstacktop++;
	}

	public final static void POP_POS(MachineContext c) {
	    c.lstacktop--;
	}

	public final static void POP_POS_BACK(MachineContext c) {
	    c.lstacktop--;
	    c.pos = c.lstack[c.lstacktop];
	}

	public final static void PUSH_BUFPOS(MachineContext c) {
	    c.lstack[c.lstacktop] = c.bstacktop;
	    c.lstacktop++;
	}

	public final static void POP_BUFPOS(MachineContext c) {
	    c.lstacktop--;
	}

	public final static void POP_BUFPOS_BACK(MachineContext c) {
	    c.lstacktop--;
	    c.bstacktop = (int)c.lstack[c.lstacktop];
	}

	public final static void PUSH_FPOS(MachineContext c) {
	    c.lstack[c.lstacktop] = c.fpos;
	    c.lstacktop++;
	}

	public final static void POP_FPOS(MachineContext c) {
	    c.lstacktop--;
	}

	public final static void POP_FPOS_FORGET(MachineContext c) {
	    c.lstacktop--;
	    c.fpos = c.lstack[c.lstacktop];
	}
	
	public final static void PUSH_LEFT(MachineContext c) {
	    c.ostack[c.ostacktop] = c.left;
	    c.ostacktop++;
	}

	public final static void POP_LEFT(MachineContext c)
	{
	    c.ostacktop--;
	    c.left = c.ostack[c.ostacktop];
	}

	public final static void POP_LEFT_IFFAIL(MachineContext c)
	{
	    c.ostacktop--;
	    if(c.left == null) {
	        c.left = c.ostack[c.ostacktop];
	    }
	}

	public final static void POP_LEFT_NOT(MachineContext c)
	{
	    c.ostacktop--;
	    if(c.left == null) {
	        c.left = c.ostack[c.ostacktop];
	    }
	    else {
	        c.left = failure(c);
	    }
	}

	public final static void POP_LEFT_CONNECT(MachineContext c, Opcode op)
	{
	    c.ostacktop--;
	    if(c.left != null) {
	        ParsingObject left = c.ostack[c.ostacktop];
	        if(c.left != left) {
	            link(left, c.left, op.ndata);
	            c.left = left;
	        }
	    }
	}

	public final static void TMATCH(MachineContext c, Opcode op)
	{
		if(c.source.match(c.pos, op.bdata)) {
			c.pos += op.ndata;
		}
		else {
			c.left = failure(c);
		}
	}

	
	
	
	
	
	ParsingObject run(ParsingObject left, ParsingSource s, long pos, int pc, Opcode[] code) {
		Opcode op = code[pc];
		MachineContext c = new MachineContext(left, s, pos);
		while(true) {
			switch(op.opcode) {
			case EXIT:
				return c.left;
			case JUMP:
				pc = op.ndata;
				break;
			case CALL:
			    c.lstack[c.lstacktop] = pc;
			    c.lstacktop++;
				pc = op.ndata;
				break;
			case RET:
			    c.lstacktop--;
			    pc = (int)c.lstack[c.lstacktop];
				break;
			case IFSUCC:
				if(c.left != null) {
					pc = op.ndata;
				}
				break;
			case IFFAIL:
				if(c.left == null) {
					pc = op.ndata;
				}
				break;
			case PUSH_POS:
				PUSH_POS(c);
				break;
			case POP_POS:
				POP_POS(c);
				break;
			case POP_POS_BACK:
				POP_POS_BACK(c);
				break;
			case PUSH_BUFPOS:
				PUSH_BUFPOS(c);
				break;
			case POP_BUFPOS:
				POP_BUFPOS(c);
				break;
			case POP_BUFPOS_BACK:
				POP_BUFPOS_BACK(c);
				break;
			case PUSH_FPOS:
				PUSH_FPOS(c);
				break;
			case POP_FPOS:
				POP_FPOS(c);
				break;
			case POP_FPOS_FORGET:
				POP_FPOS_FORGET(c);
				break;

			case PUSH_LEFT:
				PUSH_LEFT(c);
				break;
			case POP_LEFT:
				POP_LEFT(c);
				break;
			case POP_LEFT_IFFAIL:
				POP_LEFT_IFFAIL(c);
				break;
			case POP_LEFT_NOT:
				POP_LEFT_NOT(c);
				break;
			case POP_LEFT_CONNECT:
				POP_LEFT_CONNECT(c, op);
				break;
				
			case TMATCH:
				TMATCH(c, op);
				break;
			}
			pc = pc + 1;
		}
	}
	
}








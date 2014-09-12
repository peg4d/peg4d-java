package org.peg4d.vm;

import org.peg4d.PExpression;

public final class Opcode {
	public MachineInstruction opcode;
    public int ndata = 0;          
    public byte[] bdata = null;
    public PExpression generated; // this is 
    
    public Opcode(MachineInstruction mi) {
    	this.opcode = mi;
    }
    public Opcode(MachineInstruction mi, int ndata) {
    	this.opcode = mi;
    	this.ndata = ndata;
    }
    public Opcode(MachineInstruction mi, PExpression p) {
    	this.opcode = mi;
    	this.generated = p;
    }
    @Override
	public String toString() {
    	if(this.bdata == null) {
    		return opcode.toString() + " " + ndata; 
    	}
    	else {
    		return opcode.toString() + " " + ndata; 
    	}
    }
    public final boolean isJumpCode() {
    	return this.opcode.compareTo(MachineInstruction.IFFAIL) <= 0;
    }
}
package org.peg4d.vm;

import org.peg4d.expression.ParsingExpression;

public final class SimpleOpcode {
	public MachineInstruction opcode;
    public int ndata = 0;          
    public byte[] bdata = null;
    public ParsingExpression generated; // this is 
    
    public SimpleOpcode(MachineInstruction mi) {
    	this.opcode = mi;
    }
    public SimpleOpcode(MachineInstruction mi, int ndata) {
    	this.opcode = mi;
    	this.ndata = ndata;
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
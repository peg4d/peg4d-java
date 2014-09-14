package org.peg4d.vm;


public final class Opcode2 {
	public int index = 0;
	public Instruction2 opcode;
    public int      n = 0;          
    public Object   v = null;
    
    public Opcode2(int index, Instruction2 mi) {
    	this.index = index;
    	this.opcode = mi;
    }
    @Override
	public String toString() {
    	if(this.v == null) {
    		return opcode.toString() + " " + v; 
    	}
    	else {
    		return opcode.toString() + " " + n; 
    	}
    }
    public final boolean isJumpCode() {
    	return this.opcode.compareTo(Instruction2.IFFAIL) <= 0;
    }
}

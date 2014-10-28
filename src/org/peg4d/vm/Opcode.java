package org.peg4d.vm;

public class Opcode {
	public Instruction inst;
	public int ndata = 0;
	public String name = null;
	
	public Opcode(Instruction inst) {
		this.inst = inst;
	}
	
	public Opcode(Instruction inst, int ndata) {
		this.inst = inst;
		this.ndata = ndata;
	}
	
	public Opcode(Instruction inst, String name) {
		this.inst = inst;
		this.name = name;
	}
	
	public String toString() {
		if (this.name == null) {
			return inst.toString() + " " + ndata;
		}
		return inst.toString() + " " + name;
	}
	
	public final boolean isJumpCode() {
    	return this.inst.compareTo(Instruction.IFFAIL) <= 0;
    }
}

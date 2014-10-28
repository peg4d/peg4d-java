package org.peg4d.vm;

public class Opcode {
	public Instruction inst;
	public int[] ndata;
	public String name = null;
	
	public Opcode(Instruction inst) {
		this.inst = inst;
		this.ndata = new int[1];
	}
	
	public Opcode(Instruction inst, int ndata) {
		this.inst = inst;
		this.ndata = new int[1];
		this.ndata[0] = ndata;
	}
	
	public Opcode(Instruction inst, int ndata1, int ndata2) {
		this.inst = inst;
		this.ndata = new int[2];
		this.ndata[0] = ndata1;
		this.ndata[1] = ndata2;
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

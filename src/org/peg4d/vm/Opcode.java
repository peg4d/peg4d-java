package org.peg4d.vm;

import java.util.ArrayList;
import java.util.List;

public class Opcode {
	public Instruction inst;
	public List<Integer> ndata;
	public int jump = 0;
	public String name = null;
	
	public Opcode(Instruction inst) {
		this.inst = inst;
		this.ndata = new ArrayList<Integer>();
	}
	
	public Opcode(Instruction inst, String name) {
		this.inst = inst;
		this.ndata = new ArrayList<Integer>();
		this.name = name;
	}
	
	public Opcode append(int ndata) {
		this.ndata.add(ndata);
		return this;
	}
	
	public void add(int index, int ndata) {
		this.ndata.add(index, ndata);
	}
	
	public int get(int index) {
		return this.ndata.get(index);
	}
	
	public int remove(int index) {
		return this.ndata.remove(index);
	}
	
	public int size() {
		return this.ndata.size();
	}
	
	public String toString() {
		if (this.name == null) {
			if (!this.ndata.isEmpty()) {
				String str = inst.toString();
				for (int i = 0; i < this.ndata.size(); i++) {
					str = str +  " " + this.ndata.get(i); 
				}
				if (this.jump != 0) {
					str = str + " " + this.jump;
				}
				return str;
			}
			else {
				if (this.jump != 0) {
					return inst.toString() + " " + this.jump;
				}
				return inst.toString();
			}
		}
		return inst.toString() + " " + name;
	}
	
	public final boolean isJumpCode() {
    	return this.inst.compareTo(Instruction.ANY) <= 0 || (this.inst.compareTo(Instruction.NOTBYTE) >= 0 && this.inst.compareTo(Instruction.ANDSTRING) <= 0);
    }
}

package org.peg4d.pegcode;

public class Optimizer {
	
	public void optimize(Module module) {
		for(int i = 0; i < module.size(); i++) {
			this.optimizeFunction(module.get(i));
		}
	}
	
	public void optimizeFunction(Function func) {
		for(int i = 0; i < func.size(); i++) {
			BasicBlock bb = func.get(i);
			
			for(int j = 0; j < bb.size(); j++) {
				Instruction inst = bb.get(j);
				if (inst instanceof JumpInstruction) {
					JumpInstruction jinst = (JumpInstruction)inst;
					optimizeJump(func, bb, jinst.jump, jinst, j);
				}
				else if (inst instanceof JumpMatchingInstruction) {
					JumpMatchingInstruction jinst = (JumpMatchingInstruction)inst;
					optimizeJumpMatching(func, bb, jinst.jump, jinst, j);
				}
			}
		}
	}
	
	public void optimizeJump(Function func, BasicBlock bb, BasicBlock jump, JumpInstruction jinst, int index) {
		if (jump.size() == 0) {
			jump = func.get(func.indexOf(jump)+1);
			optimizeJump(func, bb, jump, jinst, index);
			return;
		}
		int currentIndex = func.indexOf(bb)+1;
		while (func.get(currentIndex).size() == 0) {
			currentIndex++;
		}
		if (func.indexOf(jump) == currentIndex && index == bb.size()-1) {
			bb.remove(index);
		}
		else if (jump.get(0) instanceof JUMP) {
			JUMP tmp = (JUMP)jump.get(0);
			jinst.jump = tmp.jump;
			optimizeJump(func, bb, tmp.jump, jinst, currentIndex);
		}
		else if (jump.get(0) instanceof RET && jinst instanceof JUMP) {
			Instruction ret = jump.get(0);
			bb.remove(index);
			bb.add(index, ret);
		}
	}
	
	public void optimizeJumpMatching(Function func, BasicBlock bb, BasicBlock jump, JumpMatchingInstruction jinst, int index) {
		if (jump.size() == 0) {
			jump = func.get(func.indexOf(jump)+1);
			optimizeJumpMatching(func, bb, jump, jinst, index);
			return;
		}
		if (jump.get(0) instanceof JUMP) {
			JUMP tmp = (JUMP)jump.get(0);
			jinst.jump = tmp.jump;
			optimizeJumpMatching(func, bb, tmp.jump, jinst, index);
		}
	}
}

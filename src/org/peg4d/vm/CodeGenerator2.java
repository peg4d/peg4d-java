package org.peg4d.vm;

import java.util.HashMap;

import org.peg4d.Grammar;
import org.peg4d.UList;

public class CodeGenerator2 {
	
	int opIndex = 0;
	
	
	UList<Opcode2> opList = new UList<Opcode2>(new Opcode2[256]);	
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();	
	HashMap<String,Integer> ruleMap = new HashMap<String,Integer>();	
	
	public final void makeRule(Grammar peg, String ruleName) {
		this.ruleMap.put(ruleName, opIndex);
	}
	
	private Opcode2 newOpcode(Instruction2 mi) {
		Opcode2 c = new Opcode2(opIndex, mi);
		opIndex++;
		opList.add(c);
		return c;
	}

	public final void writeCode(Instruction2 mi) {
		newOpcode(mi);
	}
	
	int labelId = 0;
	public final int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	
	public final void writeLabel(int label) {
		Opcode2 c = newOpcode(Instruction2.NOP);
		c.n = label;
	}
	
	public final void writeJumpCode(Instruction2 mi, int labelId) {
		Opcode2 c = newOpcode(mi);
		c.n = labelId;
	}

	private void writeCode(Instruction2 mi, String op) {
		Opcode2 c = newOpcode(mi);

	}

	public void writeCallNonTerminal(Grammar base, String ruleName) {
		// TODO Auto-generated method stub
		
	}

	public void writePush(int position) {
		// TODO Auto-generated method stub
		
	}

	public void writeBack(int position, int index) {
		// TODO Auto-generated method stub
		
	}

	public void writePop(int position) {
		// TODO Auto-generated method stub
		
	}

	public void begin() {
		// TODO Auto-generated method stub
		
	}

	public void end() {
		// TODO Auto-generated method stub
		
	}

	public void writeStore(int parsedresult, int i) {
		// TODO Auto-generated method stub
		
	}

}

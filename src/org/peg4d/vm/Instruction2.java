package org.peg4d.vm;

public enum Instruction2 {
	NOP,   // (n = int labelId)
	EXIT,
	JUMP,  // (n = pc)
	CALL,  // (n = pc)
	RET,   // 
	IFSUCC,  // 
	IFFAIL,
	IFp,
	IFZERO,  // stack[top] == 0
	PUSHo,
	PUSHp,
	PUSHf,
	LOADo,
	LOADp,
	LOADf,
	STOREo,
	STOREp,
	STOREf,
	POP,
	POPn,
	CONSUME,
	FAIL,
	BYTE,
	CHAR,
	BYTE1,
	CHAR2,
	CHAR3,
	CHAR4,
	TEXT,  /* optional */
	ANY,   /* optional */
	NEW,
	NEWJOIN,
	COMMIT,
	ABORT,
	LINK,
	TAG,
	VALUE,
}
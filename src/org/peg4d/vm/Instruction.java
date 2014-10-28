package org.peg4d.vm;

public enum Instruction {
	EXIT,
	JUMP,
	CALL,
	RET, 
	IFSUCC, 
	IFFAIL,
	PUSHo,
	PUSHp,
	PUSHf,
	POP,
	FAIL,
	BYTE,
	CHAR,
	ANY,
	NEW,
	NEWJOIN,
	COMMIT,
	ABORT,
	LINK,
	TAG,
	VALUE,
	REMEMBER,
	BACK
}

package org.peg4d.pegcode;

public enum Opcode {
	EXIT,
	JUMP,
	CALL,
	RET,
	CONDBRANCH,
	REPCOND,
	CHARRANGE,
	CHARSET,
	STRING,
	ANY,
	PUSHo,
	PUSHconnect,
	PUSHp,
	LOADp1,
	LOADp2,
	LOADp3,
	POPp,
	POPo,
	STOREo,
	STOREp,
	STOREp1,
	STOREp2,
	STOREp3,
	STOREflag,
	NEW,
	NEWJOIN,
	COMMIT,
	ABORT,
	LINK,
	SETendp,
	TAG,
	VALUE,
	MAPPEDCHOICE,
	SCAN,
	CHECKEND,
	DEF,
	IS,
	ISA,
	BLOCKSTART,
	BLOCKEND,
	INDENT,
	NOTBYTE,
	NOTANY,
	NOTCHARSET,
	NOTBYTERANGE,
	NOTSTRING,
	OPTIONALBYTE,
	OPTIONALCHARSET,
	OPTIONALBYTERANGE,
	OPTIONALSTRING,
	ZEROMOREBYTERANGE,
	ZEROMORECHARSET,
	ZEROMOREWS,
	REPEATANY,
	NOTCHARANY
}

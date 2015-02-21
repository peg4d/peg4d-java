#include <stdint.h>

#ifndef PEGVM_H
#define PEGVM_H

#define PEGVM_DEBUG 0
#define PEGVM_PROFILE 0
#define PEGVM_OP_MAX 46
#define PEGVM_PROFILE_MAX 46

typedef struct byteCodeInfo {
  int pos;
  uint8_t version0;
  uint8_t version1;
  uint32_t filename_length;
  uint8_t *filename;
  uint32_t pool_size_info;
  uint64_t bytecode_length;
} byteCodeInfo;

typedef struct PegVMInstruction {
  union {
    long opcode;
    const void *ptr;
  };
  //  union {
  struct PegVMInstruction *jump;
  int *ndata;
  char *chardata;
  //  };
} PegVMInstruction;

#define PEGVM_OP_EACH(OP)                                                      \
  OP(EXIT) OP(JUMP) OP(CALL) OP(RET) OP(CONDBRANCH) OP(REPCOND) OP(CHARRANGE)  \
      OP(CHARSET) OP(STRING) OP(ANY) OP(PUSHo) OP(PUSHconnect) OP(PUSHp1)      \
      OP(LOADp1) OP(LOADp2) OP(LOADp3) OP(POPp) OP(POPo) OP(STOREo) OP(STOREp) \
      OP(STOREp1) OP(STOREp2) OP(STOREp3) OP(STOREflag) OP(NEW) OP(NEWJOIN)    \
      OP(COMMIT) OP(ABORT) OP(LINK) OP(SETendp) OP(TAG) OP(VALUE)              \
      OP(MAPPEDCHOICE) OP(SCAN) OP(CHECKEND) OP(DEF) OP(IS) OP(ISA)            \
      OP(BLOCKSTART) OP(BLOCKEND) OP(INDENT) OP(NOTBYTE) OP(NOTANY)            \
      OP(NOTCHARSET) OP(NOTBYTERANGE) OP(NOTSTRING) OP(OPTIONALBYTE)           \
      OP(OPTIONALCHARSET) OP(OPTIONALBYTERANGE) OP(OPTIONALSTRING)             \
      OP(ZEROMOREBYTERANGE) OP(ZEROMORECHARSET) OP(ZEROMOREWS) OP(REPEATANY)
// OP(DTABLE)

enum pegvm_opcode {
#define DEFINE_ENUM(NAME) PEGVM_OP_##NAME,
  PEGVM_OP_EACH(DEFINE_ENUM)
#undef DEFINE_ENUM
  PEGVM_OP_ERROR = -1
};

void nez_PrintErrorInfo(const char *errmsg);
void dump_pego(ParsingObject *pego, char *source, int level);
PegVMInstruction *nez_LoadMachineCode(ParsingContext context,
                                      const char *fileName,
                                      const char *nonTerminalName);
void nez_DisposeInstruction(PegVMInstruction *inst, long length);
int ParserContext_Execute(ParsingContext context, PegVMInstruction *inst);
extern ParsingObject nez_Parse(ParsingContext context, PegVMInstruction *inst);
extern void nez_ParseStat(ParsingContext context, PegVMInstruction *inst);
extern void nez_Match(ParsingContext context, PegVMInstruction *inst);
extern PegVMInstruction *nez_VM_Prepare(ParsingContext context, PegVMInstruction *inst);
extern void nez_VM_PrintProfile(const char *file_type);

#endif

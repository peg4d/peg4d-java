#include <stdint.h>

#ifndef PEGVM_H
#define PEGVM_H
#define PEGVM_DEBUG 0
#define PEGVM_PROFILE 0
#define PEGVM_OP_MAX 74

typedef struct byteCodeInfo {
    int pos;
    uint8_t version0;
    uint8_t version1;
    uint32_t filename_length;
    uint8_t *filename;
    uint32_t pool_size_info;
    uint64_t bytecode_length;
} byteCodeInfo;

typedef struct Instruction {
    long opcode;
    int *ndata;
    char *name;
    const void *ptr;
    struct Instruction *jump;
} PegVMInstruction, Instruction;

#define PEGVM_OP_EACH(OP)      \
    OP(EXIT)                   \
    OP(JUMP)                   \
    OP(CALL)                   \
    OP(RET)                    \
    OP(IFSUCC)                 \
    OP(IFFAIL)                 \
    OP(REPCOND)                \
    OP(BYTE)                   \
    OP(STRING)                 \
    OP(CHAR)                   \
    OP(CHARSET)                \
    OP(ANY)                    \
    OP(NOTBYTE)                \
    OP(NOTANY)                 \
    OP(NOTCHARSET)             \
    OP(NOTBYTERANGE)           \
    OP(NOTSTRING)              \
    OP(ANDBYTE)                \
    OP(ANDCHARSET)             \
    OP(ANDBYTERANGE)           \
    OP(ANDSTRING)              \
    OP(OPTIONALBYTE)           \
    OP(OPTIONALCHARSET)        \
    OP(OPTIONALBYTERANGE)      \
    OP(OPTIONALSTRING)         \
    OP(ZEROMOREBYTERANGE)      \
    OP(ZEROMORECHARSET)        \
    OP(PUSHo)                  \
    OP(PUSHconnect)            \
    OP(PUSHp)                  \
    OP(PUSHf)                  \
    OP(PUSHm)                  \
    OP(POP)                    \
    OP(POPo)                   \
    OP(STOREo)                 \
    OP(STOREp)                 \
    OP(STOREf)                 \
    OP(STOREm)                 \
    OP(FAIL)                   \
    OP(SUCC)                   \
    OP(NEW)                    \
    OP(NEWJOIN)                \
    OP(COMMIT)                 \
    OP(ABORT)                  \
    OP(LINK)                   \
    OP(SETendp)                \
    OP(TAG)                    \
    OP(VALUE)                  \
    OP(MAPPEDCHOICE)           \
    OP(PUSHconnect_CALL)       \
    OP(PUSHp_CALL)             \
    OP(PUSHp_STRING)           \
    OP(PUSHp_CHAR)             \
    OP(PUSHp_ZEROMORECHARSET)  \
    OP(PUSHp_PUSHp)            \
    OP(POP_JUMP)               \
    OP(POP_REPCOND)            \
    OP(STOREo_JUMP)            \
    OP(STOREp_JUMP)            \
    OP(STOREp_ZEROMORECHARSET) \
    OP(STOREp_PUSHp)           \
    OP(STOREp_POP)             \
    OP(STOREp_TAG)             \
    OP(FAIL_JUMP)              \
    OP(SUCC_STOREp)            \
    OP(NEW_BYTE)               \
    OP(NEW_STRING)             \
    OP(NEW_PUSHp)              \
    OP(COMMIT_JUMP)            \
    OP(ABORT_JUMP)             \
    OP(ABORT_STOREo)           \
    OP(SETendp_POP)            \
    OP(TAG_JUMP)               \
    OP(TAG_SETendp)
// OP(DTABLE)

enum pegvm_opcode {
#define DEFINE_ENUM(NAME) PEGVM_OP_##NAME,
    PEGVM_OP_EACH(DEFINE_ENUM)
#undef DEFINE_ENUM
        PEGVM_OP_ERROR = -1
};

PegVMInstruction *loadByteCodeFile(ParsingContext context,
                                   PegVMInstruction *inst,
                                   const char *fileName);
int ParserContext_Execute(ParsingContext context, PegVMInstruction *inst);
extern long PegVM_Execute(ParsingContext context, Instruction *inst,
                          MemoryPool pool);
extern Instruction *PegVM_Prepare(ParsingContext context, Instruction *inst,
                                  MemoryPool pool);
#endif

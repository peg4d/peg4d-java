#include "parsing.h"
#ifndef testGenerateC_pegvm_h
#define testGenerateC_pegvm_h
#define PEGVM_DEBUG 0
#define PEGVM_PROFILE 1;
#define PEGVM_OP_MAX 74

#define PEGVM_OP_EACH(OP)\
    OP(EXIT)\
    OP(JUMP)\
    OP(CALL)\
    OP(RET)\
    OP(IFSUCC)\
    OP(IFFAIL)\
    OP(REPCOND)\
    OP(BYTE)\
    OP(STRING)\
    OP(CHAR)\
    OP(CHARSET)\
    OP(ANY)\
    OP(NOTBYTE)\
    OP(NOTANY)\
    OP(NOTCHARSET)\
    OP(NOTBYTERANGE)\
    OP(NOTSTRING)\
    OP(ANDBYTE)\
    OP(ANDCHARSET)\
    OP(ANDBYTERANGE)\
    OP(ANDSTRING)\
    OP(OPTIONALBYTE)\
    OP(OPTIONALCHARSET)\
    OP(OPTIONALBYTERANGE)\
    OP(OPTIONALSTRING)\
    OP(ZEROMOREBYTERANGE)\
    OP(ZEROMORECHARSET)\
    OP(PUSHo)\
    OP(PUSHconnect)\
    OP(PUSHp)\
    OP(PUSHf)\
    OP(PUSHm)\
    OP(POP)\
    OP(POPo)\
    OP(STOREo)\
    OP(STOREp)\
    OP(STOREf)\
    OP(STOREm)\
    OP(FAIL)\
    OP(SUCC)\
    OP(NEW)\
    OP(NEWJOIN)\
    OP(COMMIT)\
    OP(ABORT)\
    OP(LINK)\
    OP(SETendp)\
    OP(TAG)\
    OP(VALUE)\
    OP(MAPPEDCHOICE)\
    OP(PUSHconnect_CALL)\
    OP(PUSHp_CALL)\
    OP(PUSHp_STRING)\
    OP(PUSHp_CHAR)\
    OP(PUSHp_ZEROMORECHARSET)\
    OP(PUSHp_PUSHp)\
    OP(POP_JUMP)\
    OP(POP_REPCOND)\
    OP(STOREo_JUMP)\
    OP(STOREp_JUMP)\
    OP(STOREp_ZEROMORECHARSET)\
    OP(STOREp_PUSHp)\
    OP(STOREp_POP)\
    OP(STOREp_TAG)\
    OP(FAIL_JUMP)\
    OP(SUCC_STOREp)\
    OP(NEW_BYTE)\
    OP(NEW_STRING)\
    OP(NEW_PUSHp)\
    OP(COMMIT_JUMP)\
    OP(ABORT_JUMP)\
    OP(ABORT_STOREo)\
    OP(SETendp_POP)\
    OP(TAG_JUMP)\
    OP(TAG_SETendp)
    //OP(DTABLE)

enum pegvm_opcode {
#define DEFINE_ENUM(NAME) PEGVM_OP_##NAME,
    PEGVM_OP_EACH(DEFINE_ENUM)
#undef DEFINE_ENUM
    PEGVM_OP_ERROR = -1
};

static const char *get_opname(uint8_t opcode)
{
    switch (opcode) {
#define OP_DUMPCASE(OP) case PEGVM_OP_##OP : return "" #OP;
    PEGVM_OP_EACH(OP_DUMPCASE);
    default:
        assert(0 && "UNREACHABLE");
        break;
#undef OP_DUMPCASE
    }
    return "";
}

static void dump_PegVMInstructions(Instruction *inst, uint64_t size) {
    uint64_t i;
    int j;
    for (i = 0; i < size; i++) {
        j = 0;
        fprintf(stderr, "[%llu] %s ", i, get_opname(inst[i].opcode));
        if (inst[i].ndata) {
            switch (inst->opcode) {
#define OP_DUMPCASE(OP) case PEGVM_OP_##OP:
            OP_DUMPCASE(CHAR) {
                fprintf(stderr, "[%d-", inst[i].ndata[1]);
                fprintf(stderr, "%d] ", inst[i].ndata[2]);
                //fprintf(stderr, "%d ", inst[i].jump);
            }
            OP_DUMPCASE(CHARSET) {
                //fprintf(stderr, "%d ", inst[i].jump);
            }
            default:
            //fprintf(stderr, "%d ", inst[i].jump);
            break;
            }
        }
        if (inst[i].name) {
            fprintf(stderr, "%s", inst[i].name);
        }
        fprintf(stderr, "\n");
    }
}

typedef struct byteCodeInfo {
    int pos;
    uint8_t version0;
    uint8_t version1;
    uint32_t filename_length;
    uint8_t *filename;
    uint32_t pool_size_info;
    uint64_t bytecode_length;
} byteCodeInfo;

PegVMInstruction *loadByteCodeFile(ParsingContext context, PegVMInstruction *inst, const char *fileName);
int ParserContext_Execute(ParsingContext context, PegVMInstruction *inst);
#endif

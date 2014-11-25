#include "parsing.h"
#ifndef testGenerateC_pegvm_h
#define testGenerateC_pegvm_h
#define PEGVM_DEBUG 0

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
OP(READAHEAD)\
OP(NEXTCHOICE)\
OP(ENDCHOICE)
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
                        fprintf(stderr, "%d ", inst[i].jump);
                    }
                    OP_DUMPCASE(CHARSET) {
                        fprintf(stderr, "%d ", inst[i].jump);
                    }
                    default:
                        fprintf(stderr, "%d ", inst[i].jump);
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
    uint32_t readAheadCount;
    uint64_t bytecode_length;
} byteCodeInfo;

static void dump_byteCodeInfo(byteCodeInfo *info)
{
    fprintf(stderr, "ByteCodeVersion:%u.%u\n", info->version0, info->version1);
    fprintf(stderr, "PEGFile:%s\n", info->filename);
    fprintf(stderr, "LengthOfByteCode:%llu\n", info->bytecode_length);
    fprintf(stderr, "\n");
}

static uint32_t read32(char *inputs, byteCodeInfo *info)
{
    uint32_t value = 0;
    value = (uint8_t)inputs[info->pos++];
    value = (value) | ((uint8_t)inputs[info->pos++] << 8);
    value = (value) | ((uint8_t)inputs[info->pos++] << 16);
    value = (value) | ((uint8_t)inputs[info->pos++] << 24);
    return value;
}

static uint64_t read64(char *inputs, byteCodeInfo *info)
{
    uint64_t value1 = read32(inputs, info);
    uint64_t value2 = read32(inputs, info);
    return value2 << 32 | value1;
}

PegVMInstruction* loadByteCodeFile(ParsingContext context, PegVMInstruction *inst, const char *fileName) {
    size_t len;
    char *buf = loadFile(fileName, &len);
    int j = 0;
    byteCodeInfo info;
    info.pos = 0;
    
    info.version0 = buf[info.pos++];
    info.version1 = buf[info.pos++];
    info.filename_length = read32(buf, &info);
    info.filename = malloc(sizeof(uint8_t) * info.filename_length);
    for (uint32_t i = 0; i < info.filename_length; i++) {
        info.filename[i] = buf[info.pos++];
    }
    info.readAheadCount = read32(buf, &info);
    info.bytecode_length = read64(buf, &info);
    
    // dump byte code infomation
    dump_byteCodeInfo(&info);
    
    free(info.filename);
    
    //memset(inst, 0, sizeof(*inst) * info.bytecode_length);
    inst = malloc(sizeof(*inst) * info.bytecode_length);
    context->matchCase = malloc(sizeof(struct MatchCase) * info.readAheadCount);
    
    for (uint64_t i = 0; i < info.bytecode_length; i++) {
        int code_length;
        inst[i].opcode = buf[info.pos++];
        code_length = buf[info.pos++];
        if (code_length != 0) {
            inst[i].ndata = malloc(sizeof(int) * (code_length + 1));
            inst[i].ndata[0] = code_length;
            while (j < code_length) {
                inst[i].ndata[j+1] = read32(buf, &info);
                j++;
            }
        }
        j = 0;
        inst[i].jump = read32(buf, &info);
        code_length = buf[info.pos++];
        if (code_length != 0) {
            inst[i].name = malloc(sizeof(int) * code_length);
            while (j < code_length) {
                inst[i].name[j] = buf[info.pos++];
                j++;
            }
        }
        j = 0;
        
    }
    
    if (PEGVM_DEBUG) {
        dump_PegVMInstructions(inst, info.bytecode_length);
    }
    
    context->bytecode_length = info.bytecode_length;
    
    //free(buf);
    return inst;
}


int ParserContext_Execute(ParsingContext context, PegVMInstruction *inst);


#endif

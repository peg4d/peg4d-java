#include <stdio.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include "parsing.h"
#include "pegvm.h"

static char *loadFile(const char *filename, size_t *length)
{
    size_t len = 0;
    FILE *fp = fopen(filename, "rb");
    char *source;
    if (!fp) {
        return NULL;
    }
    fseek(fp, 0, SEEK_END);
    len = (size_t)ftell(fp);
    fseek(fp, 0, SEEK_SET);
    source = (char *)malloc(len + 1);
    if (len != fread(source, 1, len, fp)) {
        fprintf(stderr, "fread error\n");
        exit(EXIT_FAILURE);
    }
    source[len] = '\0';
    fclose(fp);
    *length = len;
    return source;
}

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

PegVMInstruction *loadByteCodeFile(ParsingContext context, PegVMInstruction *inst, const char *fileName) {
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
    info.pool_size_info = read32(buf, &info);
    info.bytecode_length = read64(buf, &info);

    // dump byte code infomation
    dump_byteCodeInfo(&info);

    free(info.filename);

    //memset(inst, 0, sizeof(*inst) * info.bytecode_length);
    inst = malloc(sizeof(*inst) * info.bytecode_length);

    for (uint64_t i = 0; i < info.bytecode_length; i++) {
        int code_length;
        inst[i].opcode = buf[info.pos++];
        code_length = (uint8_t)buf[info.pos++];
        code_length = (code_length) | ((uint8_t)buf[info.pos++] << 8);
        if (code_length != 0) {
            inst[i].ndata = malloc(sizeof(int) * (code_length + 1));
            inst[i].ndata[0] = code_length;
            while (j < code_length) {
                inst[i].ndata[j+1] = read32(buf, &info);
                j++;
            }
        }
        j = 0;
        inst[i].jump = inst+read32(buf, &info);
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
    context->pool_size = info.pool_size_info;

    //    for (long i = 0; i < context->bytecode_length; i++) {
    //        if((inst+i)->opcode < PEGVM_OP_ANDSTRING) {
    //            (inst+i)->jump_inst = inst+(inst+i)->jump;
    //        }
    //    }

    //free(buf);
    return inst;
}

void ParsingContext_Init(ParsingContext this, const char *filename)
{
    memset(this, 0, sizeof(*this));
    this->pos = this->input_size = 0;
    this->inputs = loadFile(filename, &this->input_size);
    //P4D_setObject(this, &this->left, P4D_newObject(this, this->pos));
    this->stack_pointer_base = (long *) malloc(sizeof(long) * PARSING_CONTEXT_MAX_STACK_LENGTH);
    this->object_stack_pointer_base = (ParsingObject *) malloc(sizeof(ParsingObject) * PARSING_CONTEXT_MAX_STACK_LENGTH);
    this->call_stack_pointer_base = (Instruction **) malloc(sizeof(Instruction *) * PARSING_CONTEXT_MAX_STACK_LENGTH);
    this->stack_pointer = &this->stack_pointer_base[0];
    this->object_stack_pointer = &this->object_stack_pointer_base[0];
    this->call_stack_pointer = &this->call_stack_pointer_base[0];
}

void dispose_pego(ParsingObject *pego)
{
    if (pego[0] != NULL) {
        if (pego[0]->child_size != 0) {
            for (int i = 0; i < pego[0]->child_size; i++) {
                dispose_pego(&pego[0]->child[i]);
            }
            free(pego[0]->child);
            pego[0]->child = NULL;
        }
        //free(pego[0]);
        //pego[0] = NULL;
    }
}

void ParsingContext_Dispose(ParsingContext this)
{
    free(this->inputs);
    this->inputs = NULL;
    free(this->call_stack_pointer_base);
    this->call_stack_pointer_base = NULL;
    free(this->stack_pointer_base);
    this->stack_pointer_base = NULL;
    free(this->object_stack_pointer_base);
    this->object_stack_pointer_base = NULL;
    //dispose_pego(&this->unusedObject);
}

int ParserContext_IsFailure(ParsingContext context)
{
    return context->left == NULL;
}

void ParserContext_RecordFailurePos(ParsingContext context, size_t consumed)
{
    context->left = NULL;
    context->pos -= consumed;
}

// #define INC_SP(N) (context->stack_pointer += (N))
// #define DEC_SP(N) (context->stack_pointer -= (N))
static inline long INC_SP(ParsingContext context, int N)
{
    context->stack_pointer += (N);
    assert(context->stack_pointer >= context->stack_pointer_base &&
           context->stack_pointer < &context->stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
    return *context->stack_pointer;
}

static inline long DEC_SP(ParsingContext context, int N)
{
    context->stack_pointer -= N;
    assert(context->stack_pointer >= context->stack_pointer_base &&
           context->stack_pointer < &context->stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
    return *context->stack_pointer;
}

static inline ParsingObject INC_OSP(ParsingContext context, int N)
{
    context->object_stack_pointer += (N);
    assert(context->object_stack_pointer >= context->object_stack_pointer_base &&
           context->object_stack_pointer < &context->object_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
    return *context->object_stack_pointer;
}

static inline ParsingObject DEC_OSP(ParsingContext context, int N)
{
    context->object_stack_pointer -= N;
    assert(context->object_stack_pointer >= context->object_stack_pointer_base &&
           context->object_stack_pointer < &context->object_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
    return *context->object_stack_pointer;
}

static inline void PUSH_IP(ParsingContext context, Instruction *pc)
{
    *context->call_stack_pointer++ = pc;
    assert(context->call_stack_pointer >= context->call_stack_pointer_base &&
           context->call_stack_pointer < &context->call_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
}

static inline Instruction **POP_IP(ParsingContext context)
{
    --context->call_stack_pointer;
    assert(context->call_stack_pointer >= context->call_stack_pointer_base &&
           context->call_stack_pointer < &context->call_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
    return context->call_stack_pointer;
}

#define PUSH_SP(INST) (*context->stack_pointer = (INST), INC_SP(context, 1))
#define POP_SP(INST) (DEC_SP(context, 1))
#define PUSH_OSP(INST) (*context->object_stack_pointer = (INST), INC_OSP(context, 1))
#define POP_OSP(INST) (DEC_OSP(context, 1))
//#define TOP_SP() (*context->stack_pointer)
#define JUMP pc = (pc)->jump; goto *(pc)->ptr;
#define RET pc = *POP_IP(context); goto *(pc)->ptr;

#ifdef PEGVM_PROFILE
static uint64_t count[PEGVM_OP_MAX];
static uint64_t count_all;
#define OP(OP) PEGVM_OP_##OP: count[PEGVM_OP_##OP]++; count_all++;
#else
#define OP(OP) PEGVM_OP_##OP:
#endif

void PegVM_PrintProfile()
{
#ifdef PEGVM_PROFILE
    for (int i = 0; i < PEGVM_OP_MAX; i++) {
        fprintf(stderr, "%llu %s\n", count[i], get_opname(i));
        //fprintf(stderr, "%s: %llu (%0.2f%%)\n", get_opname(i), count[i], (double)count[i]*100/(double)count_all);
    }
#endif
}

long execute(ParsingContext context, Instruction *inst, MemoryPool pool)
{
    static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
        PEGVM_OP_EACH(DEFINE_TABLE)
#undef DEFINE_TABLE
    };

    long i;
    int failflag = 0;

    PUSH_IP(context, inst);
    P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));

    for (i = 0; i < context->bytecode_length; i++) {
        (inst+i)->ptr = table[(inst+i)->opcode];
    }

    Instruction *pc = inst + 1;
    goto *(pc)->ptr;

#define DISPATCH_NEXT ++pc; goto *(pc)->ptr;
    OP(EXIT) {
        P4D_commitLog(context, 0, context->left, pool);
        return failflag;
    }
    OP(JUMP) {
        JUMP;
    }
    OP(CALL) {
        PUSH_IP(context, pc+1);
        JUMP;
        //pc = inst[pc].jump;
        //goto *inst[pc].ptr;
    }
    OP(RET) {
        RET;
    }
    OP(IFSUCC) {
        if (!failflag) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(IFFAIL){
        if (failflag) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(REPCOND) {
        if (context->pos != POP_SP()) {
            DISPATCH_NEXT;
        }
        JUMP;
    }
    OP(PUSHo){
        ParsingObject left = P4D_newObject(context, context->pos, pool);
        *left = *context->left;
        left->refc = 1;
        PUSH_OSP(left);
        DISPATCH_NEXT;
    }
    OP(PUSHconnect) {
        ParsingObject left = context->left;
        context->left->refc++;
        PUSH_OSP(left);
        PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(PUSHp) {
        PUSH_SP(context->pos);
        DISPATCH_NEXT;
    }
    OP(PUSHf) {
        assert(0 && "Not implemented");
    }
    OP(PUSHm){
        PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(POP){
        POP_SP();
        DISPATCH_NEXT;
    }
    OP(POPo){
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &left, NULL);
        DISPATCH_NEXT;
    }
    OP(STOREo){
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &context->left, left);
        DISPATCH_NEXT;}
    OP(STOREp){
        context->pos = POP_SP();
        DISPATCH_NEXT;
    }
    OP(STOREf){
        assert(0 && "Not implemented");
    }
    OP(STOREm){
        DISPATCH_NEXT;
    }
    OP(FAIL){
        failflag = 1;
        DISPATCH_NEXT;
    }
    OP(SUCC){
        failflag = 0;
        DISPATCH_NEXT;
    }
    OP(BYTE) {
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            failflag = 1;
            JUMP;
        }
        context->pos++;
        DISPATCH_NEXT;
    }
    OP(STRING) {
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] != (pc)->ndata[j]) {
                failflag = 1;
                JUMP;
            }
            context->pos++;
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(CHAR){
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            failflag = 1;
            JUMP;
        }
        context->pos++;
        DISPATCH_NEXT;
    }
    OP(CHARSET){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j]) {
                context->pos++;
                DISPATCH_NEXT;
            }
            j++;
        }
        failflag = 1;
        JUMP;
    }
    OP(ANY){
        if(context->inputs[context->pos] != 0) {
            context->pos++;
            DISPATCH_NEXT;
        }
        failflag = 1;
        JUMP;
    }
    OP(NOTBYTE){
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            DISPATCH_NEXT;
        }
        failflag = 1;
        JUMP;
    }
    OP(NOTANY){
        if(context->inputs[context->pos] != 0) {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(NOTCHARSET){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j]) {
                failflag = 1;
                JUMP;
            }
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(NOTBYTERANGE){
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            DISPATCH_NEXT;
        }
        failflag = 1;
        JUMP;
    }
    OP(NOTSTRING){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        long pos = context->pos;
        while (j < len) {
            if (context->inputs[context->pos] != (pc)->ndata[j]) {
                context->pos = pos;
                DISPATCH_NEXT;
            }
            context->pos++;
            j++;
        }
        context->pos = pos;
        failflag = 1;
        JUMP;
    }
    OP(ANDBYTE){
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(ANDCHARSET){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j]) {
                DISPATCH_NEXT;
            }
            j++;
        }
        failflag = 1;
        JUMP;
    }
    OP(ANDBYTERANGE){
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(ANDSTRING){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        long pos = context->pos;
        while (j < len) {
            if (context->inputs[context->pos] != (pc)->ndata[j]) {
                failflag = 1;
                context->pos = pos;
                JUMP;
            }
            context->pos++;
            j++;
        }
        context->pos = pos;
        DISPATCH_NEXT;
    }
    OP(OPTIONALBYTE){
        if (context->inputs[context->pos] == (pc)->ndata[1]) {
            context->pos++;
        }
        DISPATCH_NEXT;
    }
    OP(OPTIONALCHARSET){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j]) {
                context->pos++;
                DISPATCH_NEXT;
            }
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(OPTIONALBYTERANGE){
        if (context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2]) {
            context->pos++;
        }
        DISPATCH_NEXT;
    }
    OP(OPTIONALSTRING){
        int j = 1;
        int len = (pc)->ndata[0]+1;
        long pos = context->pos;
        while (j < len) {
            if (context->inputs[context->pos] != (pc)->ndata[j]) {
                context->pos = pos;
                DISPATCH_NEXT;
            }
            context->pos++;
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(ZEROMOREBYTERANGE){
        while (1) {
            if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
                DISPATCH_NEXT;
            }
            context->pos++;
        }
    }
    OP(ZEROMORECHARSET){
        int j;
        int len = (pc)->ndata[0];
    ZEROMORECHARSET_LABEL:
        j = 0;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j+1]) {
                context->pos++;
                goto ZEROMORECHARSET_LABEL;
            }
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(NEW){
        PUSH_SP(P4D_markLogStack(context));
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        //PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(NEWJOIN){
        ParsingObject left = NULL;
        P4D_setObject(context, &left, context->left);
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        //PUSH_SP(P4D_markLogStack(context));
        P4D_lazyJoin(context, left, pool);
        P4D_lazyLink(context, context->left, (pc)->ndata[1], left, pool);
        DISPATCH_NEXT;
    }
    OP(COMMIT){
        P4D_commitLog(context, (int)POP_SP(), context->left, pool);
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (pc)->ndata[1], context->left, pool);
        P4D_setObject(context, &context->left, parent);
        DISPATCH_NEXT;
    }
    OP(ABORT){
        P4D_abortLog(context, (int)POP_SP());
        DISPATCH_NEXT;
    }
    OP(LINK){
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (pc)->ndata[1], context->left, pool);
        //P4D_setObject(context, &context->left, parent);
        PUSH_OSP(parent);
        DISPATCH_NEXT;
    }
    OP(SETendp){
        context->left->end_pos = context->pos;
        DISPATCH_NEXT;
    }
    OP(TAG){
        context->left->tag = (pc)->name;
        DISPATCH_NEXT;
    }
    OP(VALUE){
        context->left->value = (pc)->name;
        DISPATCH_NEXT;
    }
    OP(MAPPEDCHOICE) {
        pc = inst+(pc)->ndata[context->inputs[context->pos] + 1];
        goto *(pc)->ptr;
    }
    OP(PUSHconnect_CALL){
        ParsingObject left = context->left;
        context->left->refc++;
        PUSH_OSP(left);
        PUSH_SP(P4D_markLogStack(context));
        PUSH_IP(context, pc+1);
        JUMP;
    }
    OP(PUSHp_CALL){
        PUSH_SP(context->pos);
        PUSH_IP(context, pc+1);
        JUMP;
    }
    OP(PUSHp_STRING){
        PUSH_SP(context->pos);
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] != (pc)->ndata[j]) {
                failflag = 1;
                JUMP;
            }
            context->pos++;
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(PUSHp_CHAR){
        PUSH_SP(context->pos);
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            failflag = 1;
            JUMP;
        }
        context->pos++;
        DISPATCH_NEXT;
    }
    OP(PUSHp_ZEROMORECHARSET){
        PUSH_SP(context->pos);
        int j;
        int len = (pc)->ndata[0];
    PUSHp_ZEROMORECHARSET_LABEL:
        j = 0;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j+1]) {
                context->pos++;
                goto PUSHp_ZEROMORECHARSET_LABEL;
            }
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(PUSHp_PUSHp){
        PUSH_SP(context->pos);
        PUSH_SP(context->pos);
        DISPATCH_NEXT;
    }
    OP(POP_JUMP){
        POP_SP();
        JUMP;
    }
    OP(POP_REPCOND){
        POP_SP();
        if (context->pos != POP_SP()) {
            DISPATCH_NEXT;
        }
        JUMP;
    }
    OP(STOREo_JUMP){
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &context->left, left);
        JUMP;
    }
    OP(STOREp_JUMP){
        context->pos = POP_SP();
        JUMP;
    }
    OP(STOREp_ZEROMORECHARSET){
        context->pos = POP_SP();
        int j;
        int len = (pc)->ndata[0];
    STOREp_ZEROMORECHARSET_LABEL:
        j = 0;
        while (j < len) {
            if (context->inputs[context->pos] == (pc)->ndata[j+1]) {
                context->pos++;
                goto STOREp_ZEROMORECHARSET_LABEL;
            }
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(STOREp_PUSHp){
        context->pos = POP_SP();
        PUSH_SP(context->pos);
        DISPATCH_NEXT;
    }
    OP(STOREp_POP){
        context->pos = POP_SP();
        POP_SP();
        DISPATCH_NEXT;
    }
    OP(STOREp_TAG){
        context->pos = POP_SP();
        context->left->tag = (pc)->name;
        DISPATCH_NEXT;
    }
    OP(FAIL_JUMP){
        failflag = 1;
        JUMP;
    }
    OP(SUCC_STOREp){
        failflag = 0;
        context->pos = POP_SP();
        DISPATCH_NEXT;
    }
    OP(NEW_BYTE){
        PUSH_SP(P4D_markLogStack(context));
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            failflag = 1;
            JUMP;
        }
        context->pos++;
        DISPATCH_NEXT;
    }
    OP(NEW_STRING){
        PUSH_SP(P4D_markLogStack(context));
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        int j = 1;
        int len = (pc)->ndata[0]+1;
        while (j < len) {
            if (context->inputs[context->pos] != (pc)->ndata[j]) {
                failflag = 1;
                JUMP;
            }
            context->pos++;
            j++;
        }
        DISPATCH_NEXT;
    }
    OP(NEW_PUSHp){
        PUSH_SP(P4D_markLogStack(context));
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        PUSH_SP(context->pos);
        DISPATCH_NEXT;
    }
    OP(COMMIT_JUMP){
        P4D_commitLog(context, (int)POP_SP(), context->left, pool);
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (pc)->ndata[1], context->left, pool);
        P4D_setObject(context, &context->left, parent);
        JUMP;
    }
    OP(ABORT_JUMP){
        P4D_abortLog(context, (int)POP_SP());
        JUMP;
    }
    OP(ABORT_STOREo){
        P4D_abortLog(context, (int)POP_SP());
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &context->left, left);
        DISPATCH_NEXT;
    }
    OP(SETendp_POP){
        context->left->end_pos = context->pos;
        POP_SP();
        DISPATCH_NEXT;
    }
    OP(TAG_JUMP){
        context->left->tag = (pc)->name;
        JUMP;
    }
    OP(TAG_SETendp){
        context->left->tag = (pc)->name;
        context->left->end_pos = context->pos;
        DISPATCH_NEXT;
    }
//    OP(DTHREAD) {
//        return (long)table;
//    }

    return failflag;
}
#include <stdio.h>
#include <time.h>
#include <getopt.h>
#include "pegvm.h"


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
#define COUNT(OP) count[PEGVM_OP_##OP]++; count_all++;
#define OP(OP) PEGVM_OP_##OP:
#else
#define OP(OP) PEGVM_OP_##OP:
#endif

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
#ifdef PEGVM_PROFILE
        COUNT(EXIT)
        for (int i = 0; i < PEGVM_OP_MAX; i++) {
            fprintf(stderr, "%s: %llu (%0.2f%%)\n", get_opname(i), count[i], (double)count[i]*100/(double)count_all);
        }
#endif
        P4D_commitLog(context, 0, context->left, pool);
        return failflag;
    }
    OP(JUMP) {
#ifdef PEGVM_PROFILE
        COUNT(JUMP)
#endif
        JUMP;
    }
    OP(CALL) {
#ifdef PEGVM_PROFILE
        COUNT(CALL)
#endif
        PUSH_IP(context, pc+1);
        JUMP;
        //pc = inst[pc].jump;
        //goto *inst[pc].ptr;
    }
    OP(RET) {
#ifdef PEGVM_PROFILE
        COUNT(RET)
#endif
        RET;
    }
    OP(IFSUCC) {
#ifdef PEGVM_PROFILE
        COUNT(IFSUCC)
#endif
        if (!failflag) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(IFFAIL){
#ifdef PEGVM_PROFILE
        COUNT(IFFAIL)
#endif
        if (failflag) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(REPCOND) {
#ifdef PEGVM_PROFILE
        COUNT(REPCOND)
#endif
        if (context->pos != POP_SP()) {
            DISPATCH_NEXT;
        }
        JUMP;
    }
    OP(PUSHo){
#ifdef PEGVM_PROFILE
        COUNT(PUSHo)
#endif
        ParsingObject left = P4D_newObject(context, context->pos, pool);
        *left = *context->left;
        left->refc = 1;
        PUSH_OSP(left);
        DISPATCH_NEXT;
    }
    OP(PUSHconnect) {
#ifdef PEGVM_PROFILE
        COUNT(PUSHconnect)
#endif
        ParsingObject left = context->left;
        context->left->refc++;
        PUSH_OSP(left);
        PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(PUSHp) {
#ifdef PEGVM_PROFILE
        COUNT(PUSHp)
#endif
        PUSH_SP(context->pos);
        DISPATCH_NEXT;
    }
    OP(PUSHf) {
        assert(0 && "Not implemented");
    }
    OP(PUSHm){
#ifdef PEGVM_PROFILE
        COUNT(PUSHm)
#endif
        PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(POP){
#ifdef PEGVM_PROFILE
        COUNT(POP)
#endif
        POP_SP();
        DISPATCH_NEXT;
    }
    OP(POPo){
#ifdef PEGVM_PROFILE
        COUNT(POPo)
#endif
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &left, NULL);
        DISPATCH_NEXT;
    }
    OP(STOREo){
#ifdef PEGVM_PROFILE
        COUNT(STOREo)
#endif
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &context->left, left);
        DISPATCH_NEXT;}
    OP(STOREp){
#ifdef PEGVM_PROFILE
        COUNT(STOREp)
#endif
        context->pos = POP_SP();
        DISPATCH_NEXT;
    }
    OP(STOREf){
        assert(0 && "Not implemented");
    }
    OP(STOREm){
#ifdef PEGVM_PROFILE
        COUNT(STOREm)
#endif
        DISPATCH_NEXT;
    }
    OP(FAIL){
#ifdef PEGVM_PROFILE
        COUNT(FAIL)
#endif
        failflag = 1;
        DISPATCH_NEXT;
    }
    OP(SUCC){
#ifdef PEGVM_PROFILE
        COUNT(SUCC)
#endif
        failflag = 0;
        DISPATCH_NEXT;
    }
    OP(BYTE) {
#ifdef PEGVM_PROFILE
        COUNT(BYTE)
#endif
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            failflag = 1;
            JUMP;
        }
        context->pos++;
        DISPATCH_NEXT;
    }
    OP(STRING) {
#ifdef PEGVM_PROFILE
        COUNT(STRING)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(CHAR)
#endif
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            failflag = 1;
            JUMP;
        }
        context->pos++;
        DISPATCH_NEXT;
    }
    OP(CHARSET){
#ifdef PEGVM_PROFILE
        COUNT(CHARSET)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(ANY)
#endif
        if(context->inputs[context->pos] != 0) {
            context->pos++;
            DISPATCH_NEXT;
        }
        failflag = 1;
        JUMP;
    }
    OP(NOTBYTE){
#ifdef PEGVM_PROFILE
        COUNT(NOTBYTE)
#endif
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            DISPATCH_NEXT;
        }
        failflag = 1;
        JUMP;
    }
    OP(NOTANY){
#ifdef PEGVM_PROFILE
        COUNT(NOTANY)
#endif
        if(context->inputs[context->pos] != 0) {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(NOTCHARSET){
#ifdef PEGVM_PROFILE
        COUNT(NOTCHARSET)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(NOTBYTERANGE)
#endif
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            DISPATCH_NEXT;
        }
        failflag = 1;
        JUMP;
    }
    OP(NOTSTRING){
#ifdef PEGVM_PROFILE
        COUNT(NOTSTRING)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(ANDBYTE)
#endif
        if (context->inputs[context->pos] != (pc)->ndata[1]) {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(ANDCHARSET){
#ifdef PEGVM_PROFILE
        COUNT(ANDCHARSET)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(ANDBYTERANGE)
#endif
        if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(ANDSTRING){
#ifdef PEGVM_PROFILE
        COUNT(ANDSTRING)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(OPTIONALBYTE)
#endif
        if (context->inputs[context->pos] == (pc)->ndata[1]) {
            context->pos++;
        }
        DISPATCH_NEXT;
    }
    OP(OPTIONALCHARSET){
#ifdef PEGVM_PROFILE
        COUNT(OPTIONALCHARSET)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(OPTIONALBYTERANGE)
#endif
        if (context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2]) {
            context->pos++;
        }
        DISPATCH_NEXT;
    }
    OP(OPTIONALSTRING){
#ifdef PEGVM_PROFILE
        COUNT(OPTIONALSTRING)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(ZEROMOREBYTERANGE)
#endif
        while (1) {
            if (!(context->inputs[context->pos] >= (pc)->ndata[1] && context->inputs[context->pos] <= (pc)->ndata[2])) {
                DISPATCH_NEXT;
            }
            context->pos++;
        }
    }
    OP(ZEROMORECHARSET){
#ifdef PEGVM_PROFILE
        COUNT(ZEROMORECHARSET)
#endif
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
#ifdef PEGVM_PROFILE
        COUNT(NEW)
#endif
        PUSH_SP(P4D_markLogStack(context));
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        //PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(NEWJOIN){
#ifdef PEGVM_PROFILE
        COUNT(NEWJOIN)
#endif
        ParsingObject left = NULL;
        P4D_setObject(context, &left, context->left);
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        //PUSH_SP(P4D_markLogStack(context));
        P4D_lazyJoin(context, left, pool);
        P4D_lazyLink(context, context->left, (pc)->ndata[1], left, pool);
        DISPATCH_NEXT;
    }
    OP(COMMIT){
#ifdef PEGVM_PROFILE
        COUNT(COMMIT)
#endif
        P4D_commitLog(context, (int)POP_SP(), context->left, pool);
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (pc)->ndata[1], context->left, pool);
        P4D_setObject(context, &context->left, parent);
        DISPATCH_NEXT;
    }
    OP(ABORT){
#ifdef PEGVM_PROFILE
        COUNT(ABORT)
#endif
        P4D_abortLog(context, (int)POP_SP());
        DISPATCH_NEXT;
    }
    OP(LINK){
#ifdef PEGVM_PROFILE
        COUNT(LINK)
#endif
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (pc)->ndata[1], context->left, pool);
        //P4D_setObject(context, &context->left, parent);
        PUSH_OSP(parent);
        DISPATCH_NEXT;
    }
    OP(SETendp){
#ifdef PEGVM_PROFILE
        COUNT(SETendp)
#endif
        context->left->end_pos = context->pos;
        DISPATCH_NEXT;
    }
    OP(TAG){
#ifdef PEGVM_PROFILE
        COUNT(TAG)
#endif
        context->left->tag = (pc)->name;
        DISPATCH_NEXT;
    }
    OP(VALUE){
#ifdef PEGVM_PROFILE
        COUNT(VALUE)
#endif
        context->left->value = (pc)->name;
        DISPATCH_NEXT;
    }
    OP(MAPPEDCHOICE) {
#ifdef PEGVM_PROFILE
        COUNT(MAPPEDCHOICE)
#endif
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

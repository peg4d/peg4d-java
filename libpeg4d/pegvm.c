#include <stdio.h>
#include <time.h>
#include <getopt.h>
#include "pegvm.h"

long execute(ParsingContext context, Instruction *inst, MemoryPool pool);

int main(int argc, char * const argv[])
{
    struct ParsingContext context;
    PegVMInstruction *inst = NULL;
    struct MemoryPool pool;
    const char *syntax_file = NULL;
    const char *output_type = NULL;
    const char *input_file = NULL;
    const char *orig_argv0 = argv[0];
    int opt;
    while ((opt = getopt(argc, argv, "p:t:")) != -1) {
        switch (opt) {
            case 'p':
                syntax_file = optarg;
                break;
            case 't':
                output_type = optarg;
                break;
            default: /* '?' */
                peg_usage(orig_argv0);
        }
    }
    argc -= optind;
    argv += optind;
    if (argc == 0) {
        peg_usage(orig_argv0);
    }
    if (syntax_file == NULL) {
        peg_error("not input syntaxfile");
    }
    input_file = argv[0];
    ParsingContext_Init(&context, input_file);
    inst = loadByteCodeFile(&context, inst, syntax_file);
    uint64_t bytecode_length = context.bytecode_length;
    pool.pool_size = context.input_size * bytecode_length / 1000;
    createMemoryPool(&pool);
    if(output_type == NULL || !strcmp(output_type, "pego")) {
        context.bytecode_length = bytecode_length;
        clock_t start = clock();
        if(execute(&context, inst, &pool)) {
            peg_error("parse error");
        }
        clock_t end = clock();
        dump_pego(&context.left, context.inputs, 0);
        fprintf(stderr, "EraspedTime: %lf\n", (double)(end - start) / CLOCKS_PER_SEC);
    }
    else if(!strcmp(output_type, "stat")) {
        for (int i = 0; i < 20; i++) {
            init_pool(&pool);
            clock_t start = clock();
            if(execute(&context, inst, &pool)) {
                peg_error("parse error");
            }
            clock_t end = clock();
            fprintf(stderr, "EraspedTime: %lf\n", (double)(end - start) / CLOCKS_PER_SEC);
            dispose_pego(&context.left);
            context.pos = 0;
        }
    }
    else if (!strcmp(output_type, "file")) {
        context.bytecode_length = bytecode_length;
        char output_file[256] = "dump/dump_parsed_";
        char fileName[256];
        size_t input_fileName_len = strlen(input_file);
        size_t start = 0;
        size_t index = 0;
        while (input_fileName_len > 0) {
            input_fileName_len--;
            if (input_file[input_fileName_len] == '/') {
                start = input_fileName_len + 1;
                break;
            }
            if (input_file[input_fileName_len] == '.') {
                index = input_fileName_len;
            }

        }
        strncpy(fileName, input_file + start, index-start);
        strcat(output_file, fileName);
        strcat(output_file, ".txt");
        if(execute(&context, inst, &pool)) {
            peg_error("parse error");
        }
        FILE *file;
        file = fopen(output_file, "w");
        if (file == NULL) {
            assert(0 && "can not open file");
        }
        dump_pego_file(file, &context.left, context.inputs, 0);
        fclose(file);
    }
    destroyMemoryPool(&pool);
    ParsingContext_Dispose(&context);
    free(inst);
    inst = NULL;
    return 0;
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

static inline void PUSH_IP(ParsingContext context, int pc)
{
    *context->call_stack_pointer++ = pc;
    assert(context->call_stack_pointer >= context->call_stack_pointer_base &&
           context->call_stack_pointer < &context->call_stack_pointer_base[PARSING_CONTEXT_MAX_STACK_LENGTH]);
}

static inline int *POP_IP(ParsingContext context)
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
#define JUMP pc = (inst+pc)->jump; goto *(inst+pc)->ptr;
#define RET pc = *POP_IP(context) + 1; goto *(inst+pc)->ptr;


long execute(ParsingContext context, Instruction *inst, MemoryPool pool)
{
    static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
        PEGVM_OP_EACH(DEFINE_TABLE)
#undef DEFINE_TABLE
    };

    int failflag = 0;

#if PEGVM_DEBUG
    int pushcount[1024];
    int popcount[1024];
    int startpc[1024];
    int endpc[1024];
    int count = 0;
    int pushOcount[1024];
    int popOcount[1024];
    
    pushcount[0] = 0;
    popcount[0] = 0;
    pushOcount[count] = 0;
    popOcount[count] = 0;
    startpc[0] = 1;
#endif
    
    PUSH_IP(context, -1);
    P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));

    for (uint64_t i = 0; i < context->bytecode_length; i++) {
        (inst+i)->ptr = table[(inst+i)->opcode];
    }
    
    int pc = 1;
    goto *(inst+pc)->ptr;
                
#define OP(OP) PEGVM_OP_##OP:
#define DISPATCH_NEXT ++pc; goto *(inst+pc)->ptr;
    OP(EXIT) {
        P4D_commitLog(context, 0, context->left, pool);
        return failflag;
    }
    OP(JUMP) {
        JUMP;
    }
    OP(CALL) {
        PUSH_IP(context, pc);
        JUMP;
        //pc = inst[pc].jump;
#if PEGVM_DEBUG
            count++;
            pushcount[count] = 0;
            popcount[count] = 0;
            pushOcount[count] = 0;
            popOcount[count] = 0;
            startpc[count] = pc;
#endif
        //goto *inst[pc].ptr;
    }
    OP(RET) {
#if PEGVM_DEBUG
        endpc[count] = pc;
        if (pushcount[count] != popcount[count]) {
            fprintf(stderr, "(start:%d ~ end:%d) ", startpc[count], endpc[count]);
            assert(0 && "pushcount != popcount");
        }
        if (pushOcount[count] != popOcount[count]) {
            fprintf(stderr, "(push:%d ~ pop:%d) \n", pushOcount[count], popOcount[count]);
            fprintf(stderr, "(start:%d ~ end:%d) ", startpc[count], endpc[count]);
            assert(0 && "pushOcount != popOcount");
        }
        count--;
#endif
        RET;
    }
    OP(IFSUCC) {
        if (!failflag) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(IFFAIL){
#if PEGVM_DEBUG
        if (pc == 910) {
            pc = 910;
        }
#endif
        if (failflag) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(REPCOND) {
#if PEGVM_DEBUG
        popcount[count]++;
#endif
        if (context->pos == POP_SP()) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(PUSHo){
#if PEGVM_DEBUG
        pushOcount[count]++;
#endif
        ParsingObject left = P4D_newObject(context, context->pos, pool);
        *left = *context->left;
        left->refc = 1;
        PUSH_OSP(left);
        DISPATCH_NEXT;
    }
    OP(PUSHconnect) {
#if PEGVM_DEBUG
        pushOcount[count]++;
#endif
        ParsingObject left = context->left;
        context->left->refc++;
        PUSH_OSP(left);
#if PEGVM_DEBUG
        pushcount[count]++;
#endif
        PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(PUSHp) {
#if PEGVM_DEBUG
        pushcount[count]++;
#endif
        PUSH_SP(context->pos);
        DISPATCH_NEXT;
    }
    OP(PUSHf) {
        assert(0 && "Not implemented");
    }
    OP(PUSHm){
#if PEGVM_DEBUG
        pushcount[count]++;
#endif
        PUSH_SP(P4D_markLogStack(context));
        DISPATCH_NEXT;
    }
    OP(POP){
#if PEGVM_DEBUG
        popcount[count]++;
#endif
        POP_SP();
        DISPATCH_NEXT;
    }
    OP(POPo){
#if PEGVM_DEBUG
        popOcount[count]++;
#endif
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &left, NULL);
        DISPATCH_NEXT;
    }
    OP(STOREo){
#if PEGVM_DEBUG
        popOcount[count]++;
#endif
        ParsingObject left = POP_OSP();
        P4D_setObject(context, &context->left, left);
        DISPATCH_NEXT;}
    OP(STOREp){
#if PEGVM_DEBUG
        popcount[count]++;
#endif
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
        if (context->inputs[context->pos] == (inst+pc)->ndata[1]) {
            context->pos++;
        }
        else {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(STRING) {
        int j = 0;
        while (j < (inst+pc)->ndata[0]) {
            if (context->inputs[context->pos] == (inst+pc)->ndata[j+1]) {
                context->pos++;
                j++;
            }
            else {
                failflag = 1;
                JUMP;
            }
        }
        DISPATCH_NEXT;
    }
    OP(CHAR){
        if (context->inputs[context->pos] >= (inst+pc)->ndata[1] && context->inputs[context->pos] <= (inst+pc)->ndata[2]) {
            context->pos++;
        }
        else {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(CHARSET){
        int j = 0;
        while (j < (inst+pc)->ndata[0]) {
            if (context->inputs[context->pos] == (inst+pc)->ndata[j+1]) {
                context->pos++;
                goto CHARSET_CONSUME;
            }
            j++;
        }
        failflag = 1;
        JUMP;
    CHARSET_CONSUME:
        DISPATCH_NEXT
    }
    OP(ANY){
        if(context->inputs[context->pos] != 0) {
            context->pos++;
        }
        else {
            failflag = 1;
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(NEW){
#if PEGVM_DEBUG
        if (pc == 977) {
            pc = 977;
        }
        pushcount[count]++;
#endif
        PUSH_SP(P4D_markLogStack(context));
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        DISPATCH_NEXT;
    }
    OP(NEWJOIN){
        //popOcount[count]++;
        ParsingObject left;
        P4D_setObject(context, &left, context->left);
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos, pool));
        P4D_lazyJoin(context, left, pool);
        P4D_lazyLink(context, context->left, (inst+pc)->ndata[1], left, pool);
        DISPATCH_NEXT;
    }
    OP(COMMIT){
#if PEGVM_DEBUG
        popcount[count]++;
#endif
        P4D_commitLog(context, (int)POP_SP(), context->left, pool);
#if PEGVM_DEBUG
        popOcount[count]++;
#endif
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (inst+pc)->ndata[1], context->left, pool);
        P4D_setObject(context, &context->left, parent);
        DISPATCH_NEXT;
    }
    OP(ABORT){
#if PEGVM_DEBUG
        popcount[count]++;
#endif
        P4D_abortLog(context, (int)POP_SP());
        DISPATCH_NEXT;
    }
    OP(LINK){
#if PEGVM_DEBUG
        popOcount[count]++;
#endif
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, (inst+pc)->ndata[1], context->left, pool);
        //P4D_setObject(context, &context->left, parent);
        PUSH_OSP(parent);
#if PEGVM_DEBUG
        pushOcount[count]++;
#endif
        DISPATCH_NEXT;
    }
    OP(SETendp){
        context->left->end_pos = context->pos;
        DISPATCH_NEXT;
    }
    OP(TAG){
        context->left->tag = (inst+pc)->name;
        DISPATCH_NEXT;
    }
    OP(VALUE){
        context->left->value = (inst+pc)->name;
        DISPATCH_NEXT;
    }
//    OP(DTHREAD) {
//        return (long)table;
//    }

    return failflag;
}

#include <stdio.h>
#include <time.h>
#include "pegvm.h"

int execute(ParsingContext context, Instruction *inst);

int main(int argc, char * const argv[])
{
    //chdir("/Users/hondashun/Documents/workspace/peg4d-java/libpeg4d");
    struct ParsingContext context;
    PegVMInstruction *inst;
    const char *syntax_file = NULL;
    const char *output_type = NULL;
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
    ParsingContext_Init(&context, argv[0]);
    inst = loadByteCodeFile(&context, inst, syntax_file);
    if(output_type == NULL || !strcmp(output_type, "pego")) {
        clock_t start = clock();
        if(execute(&context, inst)) {
            peg_error("parse error");
        }
        clock_t end = clock();
        dump_pego(&context.left, context.inputs, 0);
        fprintf(stderr, "EraspedTime: %lf\n", (double)(end - start) / CLOCKS_PER_SEC);
    }
    else if(!strcmp(output_type, "stat")) {
        clock_t start = clock();
        if(execute(&context, inst)) {
            peg_error("parse error");
        }
        clock_t end = clock();
        fprintf(stderr, "EraspedTime: %lf\n", (double)(end - start) / CLOCKS_PER_SEC);
        dispose_pego(&context.left);
    }
    ParsingContext_Dispose(&context);
    free(inst);
    inst = NULL;
    return 0;
}


int ParserContext_IsFailure(ParsingContext context)
{
    return context->left == NULL;
}

#define MAX(A, B) ((A)>(B)?(A):(B))
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
#define TOP_SP() (*context->stack_pointer)
#define JUMP pc = *inst[pc].ndata; goto *inst[pc].ptr;
#define RET pc = *POP_IP(context) + 1; goto *inst[pc].ptr;

int execute(ParsingContext context, Instruction *inst)
{
    PUSH_IP(context, -1);
    static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
        PEGVM_OP_EACH(DEFINE_TABLE)
#undef DEFINE_TABLE
    };
    
    int failflag = 0;
    
    for (int i = 0; i < context->bytecode_length; i++) {
        inst[i].ptr = table[inst[i].opcode];
    }
    
    int pc = 1;
    goto *inst[pc].ptr;
                
#define OP(OP) PEGVM_OP_##OP:
#define DISPATCH_NEXT pc++; goto *inst[pc].ptr;
    OP(EXIT) {
        P4D_commitLog(context, 0, context->left);
        return failflag;
    }
    OP(JUMP) {
        JUMP;
    }
    OP(CALL) {
        PUSH_IP(context, pc);
        JUMP;
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
    OP(PUSHo){
        ParsingObject left = P4D_newObject(context, context->pos);
        *left = *context->left;
        left->refc = 1;
        PUSH_OSP(left);
        DISPATCH_NEXT;
    }
    OP(PUSHconnect) {
        ParsingObject left = context->left;
        context->left->refc++;
        PUSH_OSP(left);
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
        P4D_setObject(context, &left, NULL);
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
        if (context->inputs[context->pos] == inst[pc].ndata[0]) {
            context->pos++;
        }
        else {
            failflag = 1;
        }
        DISPATCH_NEXT;
    }
    OP(CHAR){
        if (context->inputs[context->pos] >= inst[pc].ndata[0] && context->inputs[context->pos] <= inst[pc].ndata[1]) {
            P4D_consume(&context->pos, 1);
        }
        else {
            failflag = 1;
        }
        DISPATCH_NEXT;
    }
    OP(ANY){
        if(context->inputs[context->pos] != 0) {
            P4D_consume(&context->pos, 1);
        }
        else {
            failflag = 1;
        }
        DISPATCH_NEXT;
    }
    OP(REPCOND) {
        if (context->pos == POP_SP()) {
            JUMP;
        }
        DISPATCH_NEXT;
    }
    OP(NEW){
        P4D_setObject(context, &context->left, P4D_newObject(context, context->pos));
        DISPATCH_NEXT;
    }
    OP(NEWJOIN){
        P4D_lazyJoin(context, (ParsingObject)POP_OSP());
        DISPATCH_NEXT;
    }
    OP(COMMIT){
        P4D_commitLog(context, (int)POP_SP(), context->left);
        DISPATCH_NEXT;
    }
    OP(ABORT){
        P4D_abortLog(context, (int)POP_SP());
        DISPATCH_NEXT;
    }
    OP(LINK){
        ParsingObject parent = (ParsingObject)POP_OSP();
        P4D_lazyLink(context, parent, inst[pc].ndata[0], context->left);
        P4D_setObject(context, &context->left, parent);
        DISPATCH_NEXT;
    }
    OP(SETendp){
        context->left->end_pos = context->pos;
        DISPATCH_NEXT;
    }
    OP(TAG){
        context->left->tag = inst[pc].name;
        DISPATCH_NEXT;
    }
    OP(VALUE){
        context->left->value = inst[pc].name;
        DISPATCH_NEXT;
    }
    OP(REMEMBER){
        assert(0 && "Not implemented");
    }
    OP(BACK){
        assert(0 && "Not implemented");
    }

    return failflag;
}

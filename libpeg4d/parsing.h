#include <stdint.h>
#include <stdlib.h>

#ifndef PEGVM_PARSING_H
#define PEGVM_PARSING_H

struct ParsingObject {
    int  refc;  // referencing counting gc
    long oid;
    long start_pos;
    long end_pos;
    const char *tag;
    const char *value;
    struct ParsingObject *parent;
    int child_size;
    struct ParsingObject **child;
};

typedef struct Instruction {
    long opcode;
    int *ndata;
    char *name;
    const void *ptr;
    struct Instruction *jump;
} PegVMInstruction, Instruction;

struct ParsingLog {
    struct ParsingLog *next;
    int    index;
    struct ParsingObject *childNode;
};

struct ParsingContext {
    char *inputs;
    size_t input_size;
    long pos;
    struct ParsingObject *left;
    struct ParsingObject *unusedObject;

    int    logStackSize;
    struct ParsingLog *logStack;

    long bytecode_length;
    size_t pool_size;

    long *stack_pointer;
    struct ParsingObject **object_stack_pointer;
    struct Instruction **call_stack_pointer;
    long *stack_pointer_base;
    struct ParsingObject **object_stack_pointer_base;
    struct Instruction **call_stack_pointer_base;
};

struct MemoryPool {
    size_t pool_size;
    struct ParsingObject *object_pool;
    struct ParsingLog *log_pool;
    long object_pool_index;
    long log_pool_index;
};

typedef struct ParsingObject* ParsingObject;
typedef struct ParsingContext* ParsingContext;
typedef struct ParsingLog* ParsingLog;
typedef struct MemoryPool* MemoryPool;

ParsingObject P4D_newObject(ParsingContext this, long start, MemoryPool pool);
void P4D_setObject(ParsingContext this, ParsingObject *var, ParsingObject o);
void dispose_pego(ParsingObject *pego);

#define PARSING_CONTEXT_MAX_ERROR_LENGTH 256
#define PARSING_CONTEXT_MAX_STACK_LENGTH 1024

void ParsingContext_Init(ParsingContext this, const char *filename);
void ParsingContext_Dispose(ParsingContext this);

// static void P4D_consume(long *pos, long length)
// {
//     *pos += length;
// }
// static inline void P4D_disposeObject(ParsingObject o)
// {
//     free(o);
// }


#endif /* end of include guard */

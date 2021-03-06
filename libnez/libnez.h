#include <stdint.h>
#include <stdlib.h>
#include <assert.h>

#ifndef PEGVM_PARSING_H
#define PEGVM_PARSING_H

struct ParsingObject {
  int refc; // referencing counting gc
  int child_size;
  struct ParsingObject **child;
  struct ParsingObject *parent;
  long start_pos;
  long end_pos;
  const char *tag;
  const char *value;
};

struct ParsingLog {
  struct ParsingLog *next;
  struct ParsingObject *childNode;
  int index;
} __attribute__((packed));

struct MemoryPool {
  struct ParsingObject *object_pool;
  struct ParsingLog *log_pool;
  size_t oidx;
  size_t lidx;
  size_t init_size;
};

struct SymbolTableEntry {
  int tableType; // T in <def T e>
  int utf8_length;
  char *utf8;
};

struct ParsingContext {
  char *inputs;
  size_t input_size;
  long pos;
  struct ParsingObject *left;
  struct ParsingObject *unusedObject;

  int logStackSize;
  struct ParsingLog *logStack;

  size_t pool_size;
  struct MemoryPool *mpool;

  int symbolTableSize;
  int stateValue;
  int stateCount;
  struct SymbolTableEntry *stackedSymbolTable;

  long bytecode_length;
  long startPoint;

  long *stack_pointer;
  struct ParsingObject **object_stack_pointer;
  union PegVMInstruction **call_stack_pointer;
  long *stack_pointer_base;
  struct ParsingObject **object_stack_pointer_base;
  union PegVMInstruction **call_stack_pointer_base;
  int repeat_table[256];
};

typedef struct ParsingObject *ParsingObject;
typedef struct ParsingContext *ParsingContext;
typedef struct ParsingLog *ParsingLog;
typedef struct SymbolTableEntry *SymbolTableEntry;

typedef struct MemoryPool *MemoryPool;

extern MemoryPool nez_CreateMemoryPool(MemoryPool mpool, size_t init_size);
extern void MemoryPool_Reset(MemoryPool mpool);
extern void nez_DisposeMemoryPool(MemoryPool mpool);

static inline ParsingObject MemoryPool_AllocParsingObject(MemoryPool mpool) {
  assert(mpool->oidx < mpool->init_size);
  return &mpool->object_pool[mpool->oidx++];
}

static inline ParsingLog MemoryPool_AllocParsingLog(MemoryPool mpool) {
  assert(mpool->lidx < mpool->init_size);
  return &mpool->log_pool[mpool->lidx++];
}

ParsingObject P4D_newObject(ParsingContext ctx, const char *start, MemoryPool pool);
ParsingObject P4D_setObject_(ParsingContext, ParsingObject, ParsingObject);
void P4D_setObject(ParsingContext ctx, ParsingObject *var, ParsingObject o);
void nez_DisposeObject(ParsingObject pego);

#define PARSING_CONTEXT_MAX_ERROR_LENGTH 256
#define PARSING_CONTEXT_MAX_STACK_LENGTH 1024

ParsingContext nez_CreateParsingContext(const char *filename);
void nez_DisposeParsingContext(ParsingContext ctx);

ParsingLog P4D_newLog(ParsingContext ctx, MemoryPool pool);
void P4D_unuseLog(ParsingContext ctx, ParsingLog log);
int P4D_markLogStack(ParsingContext ctx);
void P4D_commitLog(ParsingContext ctx, int mark, ParsingObject newnode,
                   MemoryPool pool);
void P4D_abortLog(ParsingContext ctx, int mark);
void P4D_lazyLink(ParsingContext ctx, ParsingObject parent, int index,
                  ParsingObject child, MemoryPool pool);
void P4D_lazyJoin(ParsingContext ctx, ParsingObject left, MemoryPool pool);
void P4D_unusedObject(ParsingContext ctx, ParsingObject o);
void P4D_setObject(ParsingContext ctx, ParsingObject *var, ParsingObject o);

void nez_newSymbolTableEntry(SymbolTableEntry ste, int tableType, int len,
                             char **indent);
int pushSymbolTable(ParsingContext ctx, int tableType, int len, char *s);
void popSymbolTable(ParsingContext ctx, int stackTop);
long matchSymbolTableTop(ParsingContext ctx, const char *cur, int tableType);
long matchSymbolTable(ParsingContext ctx, const char *cur, int tableType);

char *getIndentText(ParsingContext ctx, const char *cur, long *len);
long getLineStartPosition(ParsingContext ctx, const char *cur);
// static void P4D_consume(long *pos, long length)
// {
//     *pos += length;
// }
// static inline void P4D_disposeObject(ParsingObject o)
// {
//     free(o);
// }

#endif /* end of include guard */

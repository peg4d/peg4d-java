#include "libnez.h"

ParsingLog P4D_newLog(ParsingContext ctx, MemoryPool pool) {
  ParsingLog l;
#ifdef USE_MALLOC
  l = (ParsingLog)malloc(sizeof(struct ParsingLog));
#else
  l = MemoryPool_AllocParsingLog(pool);
#endif
  l->next = NULL;
  l->childNode = NULL;
  return l;
}

void P4D_unuseLog(ParsingContext ctx, ParsingLog log) {
  P4D_setObject(ctx, &log->childNode, NULL);
#ifdef USE_MALLOC
  free(log);
#endif
}

int P4D_markLogStack(ParsingContext ctx) { return ctx->logStackSize; }

void P4D_lazyLink(ParsingContext ctx, ParsingObject parent, int index,
                  ParsingObject child, MemoryPool pool) {
  ParsingLog l = P4D_newLog(ctx, pool);
  P4D_setObject(ctx, &l->childNode, child);
  child->parent = parent;
  l->index = index;
  l->next = ctx->logStack;
  ctx->logStack = l;
  ctx->logStackSize += 1;
}

void P4D_lazyJoin(ParsingContext ctx, ParsingObject left, MemoryPool pool) {
  ParsingLog l = P4D_newLog(ctx, pool);
  P4D_setObject(ctx, &l->childNode, left);
  l->index = -9;
  l->next = ctx->logStack;
  ctx->logStack = l;
  ctx->logStackSize += 1;
}

void P4D_commitLog(ParsingContext ctx, int mark, ParsingObject newnode,
                   MemoryPool pool) {
  ParsingLog first = NULL;
  int objectSize = 0;
  while (mark < ctx->logStackSize) {
    ParsingLog cur = ctx->logStack;
    ctx->logStack = ctx->logStack->next;
    ctx->logStackSize--;
    if (cur->index == -9) { // lazyCommit
      P4D_commitLog(ctx, mark, cur->childNode, pool);
      P4D_unuseLog(ctx, cur);
      break;
    }
    if (cur->childNode->parent == newnode) {
      cur->next = first;
      first = cur;
      objectSize += 1;
    } else {
      P4D_unuseLog(ctx, cur);
    }
  }
  if (objectSize > 0) {
    newnode->child = (ParsingObject *)calloc(sizeof(ParsingObject), objectSize);
    newnode->child_size = objectSize;
    for (int i = 0; i < objectSize; i++) {
      ParsingLog cur = first;
      first = first->next;
      if (cur->index == -1) {
        cur->index = i;
      }
      P4D_setObject(ctx, &newnode->child[cur->index], cur->childNode);
      P4D_unuseLog(ctx, cur);
    }
    for (int i = 0; i < objectSize; i++) {
      if (newnode->child[i] == NULL) {
        P4D_setObject(ctx, &newnode->child[i], P4D_newObject(ctx, ctx->inputs, pool));
      }
    }
  }
}

void P4D_abortLog(ParsingContext ctx, int mark) {
  while (mark < ctx->logStackSize) {
    ParsingLog l = ctx->logStack;
    ctx->logStack = ctx->logStack->next;
    ctx->logStackSize--;
    P4D_unuseLog(ctx, l);
  }
}

ParsingObject P4D_newObject(ParsingContext ctx, const char *start, MemoryPool pool) {
  ParsingObject o = MemoryPool_AllocParsingObject(pool);
  o->refc = 0;
  o->start_pos = start - ctx->inputs;
  o->end_pos = o->start_pos;
  o->tag = "#empty"; // default
  o->value = NULL;
  o->parent = NULL;
  o->child = NULL;
  o->child_size = 0;
  return o;
}

void P4D_unusedObject(ParsingContext ctx, ParsingObject o) {
  o->parent = ctx->unusedObject;
  ctx->unusedObject = o;
  if (o->child_size > 0) {
    for (int i = 0; i < o->child_size; i++) {
      P4D_setObject(ctx, &(o->child[i]), NULL);
    }
    free(o->child);
    o->child = NULL;
  }
}

ParsingObject P4D_setObject_(ParsingContext ctx, ParsingObject var, ParsingObject o) {
  if (var != NULL) {
    var->refc -= 1;
    if (var->refc == 0) {
      P4D_unusedObject(ctx, var);
    }
  }
  if (o != NULL) {
    o->refc += 1;
  }
  return o;
}

void P4D_setObject(ParsingContext ctx, ParsingObject *var, ParsingObject o) {
  P4D_setObject_(ctx, *var, o);
  *var = o;
}

void nez_newSymbolTableEntry(SymbolTableEntry ste, int tableType, int len,
                             char **indent) {
  ste->tableType = tableType;
  ste->utf8_length = len;
  ste->utf8 = indent[0];
}

int pushSymbolTable(ParsingContext ctx, int tableType, int len, char *s) {
  int stackTop = ctx->symbolTableSize;
  nez_newSymbolTableEntry(&ctx->stackedSymbolTable[stackTop], tableType, len,
                          &s);
  ctx->symbolTableSize++;
  ctx->stateCount += 1;
  ctx->stateValue = ctx->stateCount;
  return stackTop;
}

void popSymbolTable(ParsingContext ctx, int stackTop) {
  ctx->symbolTableSize = stackTop;
}

static int match(ParsingContext ctx, const char *cur, char *utf8, int utf8_length) {
  const char *inputs = cur;
  const char *p = (const char *)utf8;
  const char *pend = p + utf8_length;
  while (p < pend) {
    if (*inputs++ != *p++) {
      return 0;
    }
  }
  return 1;
}

long matchSymbolTableTop(ParsingContext ctx, const char *cur, int tableType) {
  for (int i = ctx->symbolTableSize - 1; i >= 0; i--) {
    struct SymbolTableEntry s = ctx->stackedSymbolTable[i];
    if (s.tableType == tableType) {
      if (match(ctx, cur, s.utf8, s.utf8_length)) {
        return s.utf8_length;
        return 0;
      }
      break;
    }
  }
  return 1;
}

long matchSymbolTable(ParsingContext ctx, const char *cur, int tableType) {
  for (int i = ctx->symbolTableSize - 1; i >= 0; i--) {
    struct SymbolTableEntry s = ctx->stackedSymbolTable[i];
    if (s.tableType == tableType) {
      if (match(ctx, cur, s.utf8, s.utf8_length)) {
        return s.utf8_length;
      }
    }
  }
  return 1;
}

long getLineStartPosition(ParsingContext ctx, const char *cur) {
  long startIndex = cur - ctx->inputs;
  if (!(startIndex < (long)ctx->input_size)) {
    startIndex = ctx->input_size - 1;
  }
  if (startIndex < 0) {
    startIndex = 0;
  }
  while (startIndex > 0) {
    char ch = cur[startIndex];
    if (ch == '\n') {
      startIndex = startIndex + 1;
      break;
    }
    startIndex = startIndex - 1;
  }
  return startIndex;
}

char *getIndentText(ParsingContext ctx, const char *cur, long *len) {
  long from = cur - ctx->inputs;
  long start = getLineStartPosition(ctx, cur);
  long i = start;
  int pos = 0;
  *len = from - start;
  char *indent = (char *)malloc(*len);
  for (; i < from; i++) {
    char ch = cur[i];
    if (ch != ' ' && ch != '\t') {
      if (i + 1 != from) {
        for (long j = i; j < from; j++) {
          indent[pos++] = ' ';
        }
        break;
      }
    } else {
      indent[pos++] = cur[i];
    }
  }
  return indent;
}

#include "parsing.h"

ParsingLog P4D_newLog(ParsingContext ctx, MemoryPool pool)
{
    ParsingLog l;
#ifdef USE_MALLOC
    l = (ParsingLog)malloc(sizeof (struct ParsingLog));
#else
    l = MemoryPool_AllocParsingLog(pool);
#endif
    l->next = NULL;
    l->childNode = NULL;
    return l;
}

void P4D_unuseLog(ParsingContext ctx, ParsingLog log)
{
    P4D_setObject(ctx, &log->childNode, NULL);
#ifdef USE_MALLOC
    free(log);
#endif
}

int P4D_markLogStack(ParsingContext ctx)
{
    return ctx->logStackSize;
}

void P4D_lazyLink(ParsingContext ctx, ParsingObject parent, int index, ParsingObject child, MemoryPool pool)
{
    ParsingLog l = P4D_newLog(ctx, pool);
    P4D_setObject(ctx, &l->childNode, child);
    child->parent = parent;
    l->index = index;
    l->next = ctx->logStack;
    ctx->logStack = l;
    ctx->logStackSize += 1;
}

void P4D_lazyJoin(ParsingContext ctx, ParsingObject left, MemoryPool pool)
{
    ParsingLog l = P4D_newLog(ctx, pool);
    P4D_setObject(ctx, &l->childNode, left);
    l->index = -9;
    l->next = ctx->logStack;
    ctx->logStack = l;
    ctx->logStackSize += 1;
}

void P4D_commitLog(ParsingContext ctx, int mark, ParsingObject newnode, MemoryPool pool)
{
    ParsingLog first = NULL;
    int objectSize = 0;
    while(mark < ctx->logStackSize) {
        ParsingLog cur = ctx->logStack;
        ctx->logStack = ctx->logStack->next;
        ctx->logStackSize--;
        if(cur->index == -9) { // lazyCommit
            P4D_commitLog(ctx, mark, cur->childNode, pool);
            P4D_unuseLog(ctx, cur);
            break;
        }
        if(cur->childNode->parent == newnode) {
            cur->next = first;
            first = cur;
            objectSize += 1;
        }
        else {
            P4D_unuseLog(ctx, cur);
        }
    }
    if(objectSize > 0) {
        newnode->child = (ParsingObject*)calloc(sizeof(ParsingObject), objectSize);
        newnode->child_size = objectSize;
        for(int i = 0; i < objectSize; i++) {
            ParsingLog cur = first;
            first = first->next;
            if(cur->index == -1) {
                cur->index = i;
            }
            P4D_setObject(ctx, &newnode->child[cur->index], cur->childNode);
            P4D_unuseLog(ctx, cur);
        }
        for(int i = 0; i < objectSize; i++) {
            if(newnode->child[i] == NULL) {
                P4D_setObject(ctx, &newnode->child[i], P4D_newObject(ctx, 0, pool));
            }
        }
    }
}

void P4D_abortLog(ParsingContext ctx, int mark)
{
    while(mark < ctx->logStackSize) {
        ParsingLog l = ctx->logStack;
        ctx->logStack = ctx->logStack->next;
        ctx->logStackSize--;
        P4D_unuseLog(ctx, l);
    }
}

ParsingObject P4D_newObject(ParsingContext ctx, long start, MemoryPool pool)
{
    ParsingObject o = MemoryPool_AllocParsingObject(pool);
    o->refc       = 0;
    o->start_pos  = start;
    o->end_pos    = start;
    o->tag        = "#empty";  // default
    o->value      = NULL;
    o->parent     = NULL;
    o->child      = NULL;
    o->child_size = 0;
    return o;
}

void P4D_unusedObject(ParsingContext ctx, ParsingObject o)
{
    o->parent = ctx->unusedObject;
    ctx->unusedObject = o;
    if(o->child_size > 0) {
        for(int i = 0; i < o->child_size; i++) {
            P4D_setObject(ctx, &(o->child[i]), NULL);
        }
        free(o->child);
        o->child = NULL;
    }
}

void P4D_setObject(ParsingContext ctx, ParsingObject *var, ParsingObject o)
{
    if (var[0] != NULL) {
        var[0]->refc -= 1;
        if(var[0]->refc == 0) {
            P4D_unusedObject(ctx, var[0]);
        }
    }
    var[0] = o;
    if (o != NULL) {
        o->refc += 1;
    }
}

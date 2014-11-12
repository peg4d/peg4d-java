#include<stdio.h>
#include<stdlib.h>
#include <string.h>
#include<assert.h>
#include <unistd.h>


struct ParsingObject
{
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
    int jump;
} PegVMInstruction, Instruction;

struct ParsingLog {
    struct ParsingLog *next;
    int    index;
    struct ParsingObject *childNode;
};

struct ParsingContext
{
    char *inputs;
	size_t input_size;
	long pos;
	struct ParsingObject *left;

    struct ParsingObject *unusedObject;
    
	int    logStackSize;
    struct ParsingLog *logStack;
    
    uint64_t bytecode_length;
    
    long *stack_pointer;
    struct ParsingObject **object_stack_pointer;
    int *call_stack_pointer;
    long *stack_pointer_base;
    struct ParsingObject **object_stack_pointer_base;
    int *call_stack_pointer_base;
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

void P4D_disposeObject(ParsingObject o);

void dump_pego(ParsingObject *pego, char* source, int level)
{
    int i;
    long j;
    if (pego[0]) {
        for (i = 0; i < level; i++) {
            fprintf(stderr, "  ");
        }
        fprintf(stderr, "{%s ", pego[0]->tag);
        if (pego[0]->child_size == 0) {
            fprintf(stderr, "'");
            if (pego[0]->value == NULL) {
                for (j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
                    fprintf(stderr, "%c", source[j]);
                }
            }
            else {
                fprintf(stderr, "%s", pego[0]->value);
            }
            fprintf(stderr, "'");
        }
        else {
            fprintf(stderr, "\n");
            for (j = 0; j < pego[0]->child_size; j++) {
                dump_pego(&pego[0]->child[j], source, level + 1);
            }
            for (i = 0; i < level; i++) {
                fprintf(stderr, "  ");
            }
            free(pego[0]->child);
            pego[0]->child = NULL;
        }
        fprintf(stderr, "}\n");
        //free(pego[0]);
        //pego[0] = NULL;
    }
    else {
        fprintf(stderr, "%p tag:null\n", pego);
    }
}

void dispose_pego(ParsingObject *pego) {
    if (pego[0]) {
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

void dump_pego_file(FILE *file, ParsingObject *pego, char* source, int level)
{
    int i;
    long j;
    if (pego[0]) {
        for (i = 0; i < level; i++) {
            fprintf(file, " ");
        }
        fprintf(file, "{%s ", pego[0]->tag);
        if (pego[0]->child_size == 0) {
            fprintf(file, "'");
            if (pego[0]->value == NULL) {
                for (j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
                    fprintf(file, "%c", source[j]);
                }
            }
            else {
                fprintf(file, "%s", pego[0]->value);
            }
            fprintf(file, "'");
        }
        else {
            fprintf(file, "\n");
            for (j = 0; j < pego[0]->child_size; j++) {
                dump_pego_file(file, &pego[0]->child[j], source, level + 1);
            }
            for (i = 0; i < level; i++) {
                fprintf(file, " ");
            }
            free(pego[0]->child);
            pego[0]->child = NULL;
        }
        fprintf(file, "}\n");
        //free(pego[0]);
        //pego[0] = NULL;
    }
    else {
        fprintf(file, "%p tag:null\n", pego);
    }
}

ParsingObject P4D_newObject(ParsingContext this, long start, MemoryPool pool);
void P4D_setObject(ParsingContext this, ParsingObject *var, ParsingObject o);

#define PARSING_CONTEXT_MAX_ERROR_LENGTH 256
#define PARSING_CONTEXT_MAX_STACK_LENGTH 1024

void ParsingContext_Init(ParsingContext this, const char *filename)
{
    memset(this, 0, sizeof(*this));
    this->pos = this->input_size = 0;
    this->inputs = loadFile(filename, &this->input_size);
    //P4D_setObject(this, &this->left, P4D_newObject(this, this->pos));
    this->stack_pointer_base = (long *) malloc(sizeof(long) * PARSING_CONTEXT_MAX_STACK_LENGTH);
    this->object_stack_pointer_base = (ParsingObject *) malloc(sizeof(ParsingObject) * PARSING_CONTEXT_MAX_STACK_LENGTH);
    this->call_stack_pointer_base = (int*) malloc(sizeof(int) * PARSING_CONTEXT_MAX_STACK_LENGTH);
    this->stack_pointer = &this->stack_pointer_base[0];
    this->object_stack_pointer = &this->object_stack_pointer_base[0];
    this->call_stack_pointer = &this->call_stack_pointer_base[0];
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
    dispose_pego(&this->unusedObject);
}

void createMemoryPool(MemoryPool pool) {
    pool->object_pool = malloc(sizeof(ParsingObject) * pool->pool_size);
    pool->log_pool = malloc(sizeof(ParsingLog) * pool->pool_size);
    pool->object_pool_index = 0;
    pool->log_pool_index = 0;
}

void init_pool(MemoryPool pool) {
    pool->object_pool_index = 0;
    pool->log_pool_index = 0;
}

void destroyMemoryPool(MemoryPool pool) {
    free(pool->object_pool);
    pool->object_pool = NULL;
    free(pool->log_pool);
    pool->log_pool = NULL;
}

void P4D_consume(long *pos, long length)
{
    *pos += length;
}

ParsingObject P4D_newObject(ParsingContext this, long start, MemoryPool pool)
{
    ParsingObject o;
    if (this->unusedObject == NULL) {
        o = (ParsingObject)malloc(sizeof (struct ParsingObject));
        //o = &pool->object_pool[pool->object_pool_index];
        //pool->object_pool_index++;
    }
    else {
        o = this->unusedObject;
        this->unusedObject = o->parent;
    }
    o->refc       = 0;
    o->oid        = 0;
    o->start_pos  = start;
    o->end_pos    = start;
    o->tag        = "#empty";  // default
    o->value      = NULL;
    o->parent     = NULL;
    o->child      = NULL;
    o->child_size = 0;
    return o;
}

void P4D_unusedObject(ParsingContext this, ParsingObject o)
{
    o->parent = this->unusedObject;
    this->unusedObject = o;
    if(o->child_size > 0) {
        for(int i = 0; i < o->child_size; i++) {
            P4D_setObject(this, &(o->child[i]), NULL);
        }
        free(o->child);
        o->child = NULL;
    }
}

void P4D_setObject(ParsingContext this, ParsingObject *var, ParsingObject o)
{
    if (var[0] != NULL) {
        if(var[0] == o) {
            return;
        }
        var[0]->refc -= 1;
        if(var[0]->refc == 0) {
            P4D_unusedObject(this, var[0]);
        }
    }
    var[0] = o;
    if (o != NULL) {
        o->refc += 1;
    }
}

void P4D_disposeObject(ParsingObject o)
{
    free(o);
    o = NULL;
}

ParsingLog P4D_newLog(ParsingContext this) {
    ParsingLog l = (ParsingLog)malloc(sizeof (struct ParsingLog));
    l->next = NULL;
    l->childNode = NULL;
    return l;
}

void P4D_unuseLog(ParsingContext this, ParsingLog log) {
    P4D_setObject(this, &log->childNode, NULL);
    free(log);
    log = NULL;
}

int P4D_markLogStack(ParsingContext this) {
    return this->logStackSize;
}

void P4D_lazyLink(ParsingContext this, ParsingObject parent, int index, ParsingObject child) {
    ParsingLog l = P4D_newLog(this);
    P4D_setObject(this, &l->childNode, child);
    child->parent = parent;
    l->index = index;
    l->next = this->logStack;
    this->logStack = l;
    this->logStackSize += 1;
}

void P4D_lazyJoin(ParsingContext this, ParsingObject left) {
    ParsingLog l = P4D_newLog(this);
    P4D_setObject(this, &l->childNode, left);
    l->index = -9;
    l->next = this->logStack;
    this->logStack = l;
    this->logStackSize += 1;
}

void P4D_commitLog(ParsingContext this, int mark, ParsingObject newnode, MemoryPool pool) {
    ParsingLog first = NULL;
    int objectSize = 0;
    while(mark < this->logStackSize) {
        ParsingLog cur = this->logStack;
        this->logStack = this->logStack->next;
        this->logStackSize--;
        if(cur->index == -9) { // lazyCommit
            P4D_commitLog(this, mark, cur->childNode, pool);
            P4D_unuseLog(this, cur);
            break;
        }
        if(cur->childNode->parent == newnode) {
            cur->next = first;
            first = cur;
            objectSize += 1;
        }
        else {
            P4D_unuseLog(this, cur);
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
            P4D_setObject(this, &newnode->child[cur->index], cur->childNode);
            P4D_unuseLog(this, cur);
        }
        for(int i = 0; i < objectSize; i++) {
            if(newnode->child[i] == NULL) {
                P4D_setObject(this, &newnode->child[i], P4D_newObject(this, 0, pool));
            }
        }
    }
}

void P4D_abortLog(ParsingContext this, int mark) {
    while(mark < this->logStackSize) {
        ParsingLog l = this->logStack;
        this->logStack = this->logStack->next;
        this->logStackSize--;
        P4D_unuseLog(this, l);
    }
}

static void peg_usage(const char *file)
{
    fprintf(stderr, "Usage: %s -f peg_bytecode target_file\n", file);
    exit(EXIT_FAILURE);
}

static void peg_error(const char *errmsg)
{
    fprintf(stderr, "%s\n", errmsg);
    exit(EXIT_FAILURE);
}



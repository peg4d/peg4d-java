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
    struct ParsingLog *unusedLog;
};

typedef struct ParsingObject* ParsingObject;
typedef struct ParsingContext* ParsingContext;
typedef struct ParsingLog* ParsingLog;

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

void dump_pego(ParsingObject pego, char* source, int level)
{
    int i;
    long j;
    if (pego) {
        for (i = 0; i < level; i++) {
            fprintf(stderr, "  ");
        }
        fprintf(stderr, "{%s ", pego->tag);
        if (pego->child_size == 0) {
            fprintf(stderr, "'");
            for (j = pego->start_pos; j < pego->end_pos; j++) {
                fprintf(stderr, "%c", source[j]);
            }
            fprintf(stderr, "'");
        }
        else {
            fprintf(stderr, "\n");
            for (j = 0; j < pego->child_size; j++) {
                dump_pego(pego->child[j], source, level + 1);
            }
            for (i = 0; i < level; i++) {
                fprintf(stderr, "  ");
            }
        }
        fprintf(stderr, "}\n");
        P4D_disposeObject(pego);
    }
    else {
        fprintf(stderr, "%p tag:null\n", pego);
    }
}

ParsingObject P4D_newObject(ParsingContext this, long start);
void P4D_setObject(ParsingContext this, ParsingObject *var, ParsingObject o);

void ParsingContext_Init(ParsingContext this, const char *filename)
{
    memset(this, 0, sizeof(*this));
    this->pos = this->input_size = 0;
    this->inputs = loadFile(filename, &this->input_size);
    P4D_setObject(this, &this->left, P4D_newObject(this, this->pos));
}

void ParsingContext_Dispose(ParsingContext this)
{
    free(this->inputs);
    this->inputs = NULL;
}

void P4D_consume(long *pos, long length)
{
    *pos += length;
}

ParsingObject P4D_newObject(ParsingContext this, long start)
{
    ParsingObject o;
    if (this->unusedObject == NULL) {
        o = (ParsingObject)malloc(sizeof (struct ParsingObject));
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
    if(this->unusedLog == NULL) {
        ParsingLog l = (ParsingLog)malloc(sizeof (struct ParsingLog));
        l->next = NULL;
        l->childNode = NULL;
        return l;
    }
    ParsingLog l = this->unusedLog;
    this->unusedLog = l->next;
    l->next = NULL;
    assert(l->childNode == NULL);
    return l;
}

void P4D_unuseLog(ParsingContext this, ParsingLog log) {
    P4D_setObject(this, &log->childNode, NULL);
    log->next = this->unusedLog;
    this->unusedLog = log;
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

void P4D_commitLog(ParsingContext this, int mark, ParsingObject newnode) {
    ParsingLog first = NULL;
    int objectSize = 0;
    while(mark < this->logStackSize) {
        ParsingLog cur = this->logStack;
        this->logStack = this->logStack->next;
        this->logStackSize--;
        if(cur->index == -9) { // lazyCommit
            P4D_commitLog(this, mark, cur->childNode);
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
                P4D_setObject(this, &newnode->child[i], P4D_newObject(this, 0));
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



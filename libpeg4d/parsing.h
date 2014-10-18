#include<stdio.h>
#include<stdlib.h>
#include<assert.h>

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
    unsigned char *inputs;
	long input_size;
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

void P4D_setObject(ParsingContext this, ParsingObject *var, ParsingObject o);

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
    o->refc += 1;
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


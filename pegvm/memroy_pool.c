#include <stdio.h>
#include "parsing.h"

struct MemoryObjectHeader {
    struct MemoryObjectHeader *prev;
    struct MemoryObjectHeader *next;
};

MemoryPool MemoryPool_Init(MemoryPool mpool, size_t init_size) {
    mpool->init_size = init_size;
    mpool->object_pool =
        (ParsingObject)malloc(sizeof(struct ParsingObject) * init_size);
    mpool->log_pool = (ParsingLog)malloc(sizeof(struct ParsingLog) * init_size);
    mpool->oidx = mpool->lidx = 0;
    return mpool;
}

void MemoryPool_Reset(MemoryPool mpool) {
    mpool->oidx = mpool->lidx = 0;
}

void MemoryPool_Dispose(MemoryPool mpool) {
    MemoryPool_Reset(mpool);
    free(mpool->object_pool);
    free(mpool->log_pool);
    mpool->object_pool = NULL;
    mpool->log_pool = NULL;
}

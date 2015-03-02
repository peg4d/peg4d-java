#include <stdio.h>
#include "libnez.h"

struct MemoryObjectHeader {
  struct MemoryObjectHeader *prev;
  struct MemoryObjectHeader *next;
};

MemoryPool nez_CreateMemoryPool(MemoryPool mpool, size_t init_size) {
  mpool->init_size = init_size;
  mpool->object_pool = (ParsingObject)malloc(init_size);
  mpool->log_pool = (ParsingLog)malloc(init_size);
  mpool->oidx = mpool->lidx = 0;
  assert(mpool->object_pool != NULL);
  return mpool;
}

void MemoryPool_Reset(MemoryPool mpool) { mpool->oidx = mpool->lidx = 0; }

void nez_DisposeMemoryPool(MemoryPool mpool) {
  MemoryPool_Reset(mpool);
  free(mpool->object_pool);
  free(mpool->log_pool);
  mpool->object_pool = NULL;
  mpool->log_pool = NULL;
}

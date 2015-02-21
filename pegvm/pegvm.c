#include <stdio.h>
#include <sys/time.h> // gettimeofday
#include <assert.h>
#include <string.h>
#include "libnez.h"
#include "pegvm.h"

#ifdef DHAVE_CONFIG_H
#include "config.h"
#endif

static uint64_t timer() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

void nez_PrintErrorInfo(const char *errmsg) {
  fprintf(stderr, "%s\n", errmsg);
  exit(EXIT_FAILURE);
}

static char *loadFile(const char *filename, size_t *length) {
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

static const char *get_opname(uint8_t opcode) {
  switch (opcode) {
#define OP_DUMPCASE(OP) \
  case PEGVM_OP_##OP:   \
    return "" #OP;
    PEGVM_OP_EACH(OP_DUMPCASE);
  default:
    assert(0 && "UNREACHABLE");
    break;
#undef OP_DUMPCASE
  }
  return "";
}

static void dump_PegVMInstructions(PegVMInstruction *inst, uint64_t size) {
  uint64_t i;
  for (i = 0; i < size; i++) {
    fprintf(stderr, "[%llu] %s ", i, get_opname(inst[i].opcode));
    if (inst[i].ndata) {
      switch (inst->opcode) {
#define OP_DUMPCASE(OP) case PEGVM_OP_##OP:
        OP_DUMPCASE(CHARRANGE) {
          fprintf(stderr, "[%d-", inst[i].ndata[1]);
          fprintf(stderr, "%d] ", inst[i].ndata[2]);
          // fprintf(stderr, "%d ", inst[i].jump);
        }
        OP_DUMPCASE(CHARSET) {
          // fprintf(stderr, "%d ", inst[i].jump);
        }
      default:
        // fprintf(stderr, "%d ", inst[i].jump);
        break;
      }
    }
    if (inst[i].chardata) {
      fprintf(stderr, "%s", inst[i].chardata);
    }
    fprintf(stderr, "\n");
  }
}

static void dump_byteCodeInfo(byteCodeInfo *info) {
  fprintf(stderr, "ByteCodeVersion:%u.%u\n", info->version0, info->version1);
  fprintf(stderr, "PEGFile:%s\n", info->filename);
  fprintf(stderr, "LengthOfByteCode:%llu\n", info->bytecode_length);
  fprintf(stderr, "\n");
}

static uint32_t read32(char *inputs, byteCodeInfo *info) {
  uint32_t value = 0;
  value = (uint8_t)inputs[info->pos++];
  value = (value) | ((uint8_t)inputs[info->pos++] << 8);
  value = (value) | ((uint8_t)inputs[info->pos++] << 16);
  value = (value) | ((uint8_t)inputs[info->pos++] << 24);
  return value;
}

static uint64_t read64(char *inputs, byteCodeInfo *info) {
  uint64_t value1 = read32(inputs, info);
  uint64_t value2 = read32(inputs, info);
  return value2 << 32 | value1;
}

PegVMInstruction *nez_LoadMachineCode(ParsingContext context,
                                      const char *fileName,
                                      const char *nonTerminalName) {
  PegVMInstruction *inst = NULL;
  size_t len;
  char *buf = loadFile(fileName, &len);
  byteCodeInfo info;
  info.pos = 0;

  info.version0 = buf[info.pos++];
  info.version1 = buf[info.pos++];
  info.filename_length = read32(buf, &info);
  info.filename = malloc(sizeof(uint8_t) * info.filename_length + 1);
  for (uint32_t i = 0; i < info.filename_length; i++) {
    info.filename[i] = buf[info.pos++];
  }
  info.filename[info.filename_length] = 0;
  info.pool_size_info = read32(buf, &info);

  int ruleSize = read32(buf, &info);
  char **ruleTable = (char **)malloc(sizeof(char *) * ruleSize);
  for (int i = 0; i < ruleSize; i++) {
    int ruleNameLen = read32(buf, &info);
    ruleTable[i] = (char *)malloc(ruleNameLen);
    for (int j = 0; j < ruleNameLen; j++) {
      ruleTable[i][j] = buf[info.pos++];
    }
    long index = read64(buf, &info);
    if (nonTerminalName != NULL) {
      if (!strcmp(ruleTable[i], nonTerminalName)) {
        context->startPoint = index;
      }
    }
  }

  if (context->startPoint == 0) {
    context->startPoint = 1;
  }

  info.bytecode_length = read64(buf, &info);

  // dump byte code infomation
  dump_byteCodeInfo(&info);

  // free bytecode info
  free(info.filename);
  for (int i = 0; i < ruleSize; i++) {
    free(ruleTable[i]);
  }
  free(ruleTable);

  // memset(inst, 0, sizeof(*inst) * info.bytecode_length);
  inst = malloc(sizeof(*inst) * info.bytecode_length);

  for (uint64_t i = 0; i < info.bytecode_length; i++) {
    int code_length;
    inst[i].opcode = buf[info.pos++];
    code_length = (uint8_t)buf[info.pos++];
    code_length = (code_length) | ((uint8_t)buf[info.pos++] << 8);
    if (code_length == 0) {
    }
    else if (code_length == 1) {
        inst[i].ndata = malloc(sizeof(int));
        inst[i].ndata[0] = read32(buf, &info);
    } else if (inst[i].opcode == PEGVM_OP_MAPPEDCHOICE) {
        inst[i].ndata = malloc(sizeof(int) * code_length);
        for (int j = 0; j < code_length; j++) {
            inst[i].ndata[j] = read32(buf, &info);
        }
    } else if (inst[i].opcode == PEGVM_OP_SCAN) {
        inst[i].ndata = malloc(sizeof(int) * 2);
        inst[i].ndata[0] = read32(buf, &info);
        inst[i].ndata[1] = read32(buf, &info);
    } else {
        inst[i].ndata = malloc(sizeof(int));
        inst[i].ndata[0] = code_length;
        inst[i].chardata = malloc(sizeof(int) * code_length);
        for (int j = 0; j < code_length; j++) {
            inst[i].chardata[j] = read32(buf, &info);
        }
    }
    inst[i].jump = inst + read32(buf, &info);
    code_length = buf[info.pos++];
    if (code_length != 0) {
      inst[i].chardata = malloc(sizeof(char) * code_length + 1);
      for (int j = 0; j < code_length; j++) {
        inst[i].chardata[j] = buf[info.pos++];
      }
      inst[i].chardata[code_length] = 0;
    }
  }

  if (PEGVM_DEBUG) {
    dump_PegVMInstructions(inst, info.bytecode_length);
  }

  context->bytecode_length = info.bytecode_length;
  context->pool_size = info.pool_size_info;
  return inst;
}

void nez_DisposeInstruction(PegVMInstruction *inst, long length) {
  for (long i = 0; i < length; i++) {
    if (inst[i].ndata != NULL) {
      free(inst[i].ndata);
      inst[i].ndata = NULL;
    }
    if (inst[i].chardata != NULL) {
      free(inst[i].chardata);
      inst[i].chardata = NULL;
    }
  }
  free(inst);
  inst = NULL;
}

ParsingContext nez_CreateParsingContext(ParsingContext ctx,
                                        const char *filename) {
  ctx = (ParsingContext)malloc(sizeof(struct ParsingContext));
  ctx->pos = ctx->input_size = 0;
  ctx->startPoint = 0;
  ctx->mpool = (MemoryPool)malloc(sizeof(struct MemoryPool));
  ctx->inputs = loadFile(filename, &ctx->input_size);
  ctx->stackedSymbolTable =
      (SymbolTableEntry)malloc(sizeof(struct SymbolTableEntry) * 256);
  // P4D_setObject(ctx, &ctx->left, P4D_newObject(ctx, ctx->pos));
  ctx->stack_pointer_base =
      (long *)malloc(sizeof(long) * PARSING_CONTEXT_MAX_STACK_LENGTH);
  ctx->object_stack_pointer_base = (ParsingObject *)malloc(
      sizeof(ParsingObject) * PARSING_CONTEXT_MAX_STACK_LENGTH);
  ctx->call_stack_pointer_base = (PegVMInstruction **)malloc(
      sizeof(PegVMInstruction *) * PARSING_CONTEXT_MAX_STACK_LENGTH);
  ctx->stack_pointer = &ctx->stack_pointer_base[0];
  ctx->object_stack_pointer = &ctx->object_stack_pointer_base[0];
  ctx->call_stack_pointer = &ctx->call_stack_pointer_base[0];
  return ctx;
}

void nez_DisposeObject(ParsingObject *pego) {
  if (pego[0] != NULL) {
    if (pego[0]->child_size != 0) {
      for (int i = 0; i < pego[0]->child_size; i++) {
        nez_DisposeObject(&pego[0]->child[i]);
      }
      free(pego[0]->child);
      pego[0]->child = NULL;
    }
    // free(pego[0]);
    // pego[0] = NULL;
  }
}

void nez_DisposeParsingContext(ParsingContext ctx) {
  free(ctx->inputs);
  ctx->inputs = NULL;
  free(ctx->mpool);
  ctx->mpool = NULL;
  free(ctx->stackedSymbolTable);
  ctx->stackedSymbolTable = NULL;
  free(ctx->call_stack_pointer_base);
  ctx->call_stack_pointer_base = NULL;
  free(ctx->stack_pointer_base);
  ctx->stack_pointer_base = NULL;
  free(ctx->object_stack_pointer_base);
  ctx->object_stack_pointer_base = NULL;
  free(ctx);
  ctx = NULL;
  // dispose_pego(&ctx->unusedObject);
}

void nez_VM_PrintProfile(const char *file_type) {
#if PEGVM_PROFILE
  fprintf(stderr, "\ninstruction count \n");
  for (int i = 0; i < PEGVM_PROFILE_MAX; i++) {
    fprintf(stderr, "%llu %s\n", count[i], get_opname(i));
    // fprintf(stderr, "%s: %llu (%0.2f%%)\n", get_opname(i), count[i],
    // (double)count[i]*100/(double)count_all);
  }
  FILE *file;
  file = fopen("pegvm_profile.csv", "w");
  if (file == NULL) {
    assert(0 && "can not open file");
  }
  fprintf(file, ",");
  for (int i = 0; i < PEGVM_PROFILE_MAX; i++) {
    fprintf(file, "%s", get_opname(i));
    if (i != PEGVM_PROFILE_MAX - 1) {
      fprintf(file, ",");
    }
  }
  for (int i = 0; i < PEGVM_PROFILE_MAX; i++) {
    fprintf(file, "%s,", get_opname(i));
    for (int j = 0; j < PEGVM_PROFILE_MAX; j++) {
      fprintf(file, "%llu", conbination_count[i][j]);
      if (j != PEGVM_PROFILE_MAX - 1) {
        fprintf(file, ",");
      }
    }
    fprintf(file, "\n");
  }
  fclose(file);
#if 0
  if (file_type) {
    fprintf(stderr, "\nrule_count\n");
    if (!strcmp(file_type, "json")) {
      for (int i = 0; i < PEGVM_json_RULE_MAX; i++) {
        fprintf(stderr, "%llu %s\n", rule_count[i], get_json_rule(i));
      }
    } else if (!strcmp(file_type, "xml")) {
      for (int i = 0; i < PEGVM_xml_RULE_MAX; i++) {
        fprintf(stderr, "%llu %s\n", rule_count[i], get_xml_rule(i));
      }
    } else if (!strcmp(file_type, "c99")) {
      for (int i = 0; i < PEGVM_c99_RULE_MAX; i++) {
        fprintf(stderr, "%llu %s\n", rule_count[i], get_c99_rule(i));
      }
    }
  }
#endif
#endif
}

#include "pegvm_core.c"

ParsingObject nez_Parse(ParsingContext context, PegVMInstruction *inst) {
  if (nez_VM_Execute(context, inst)) {
    nez_PrintErrorInfo("parse error");
  }
  dump_pego(&context->left, context->inputs, 0);
  return context->left;
}

void nez_ParseStat(ParsingContext context, PegVMInstruction *inst) {
  for (int i = 0; i < 5; i++) {
    uint64_t start, end;
    MemoryPool_Reset(context->mpool);
    start = timer();
    if (nez_VM_Execute(context, inst)) {
      nez_PrintErrorInfo("parse error");
    }
    end = timer();
    fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
    nez_DisposeObject(&context->left);
    context->pos = 0;
  }
}

void nez_Match(ParsingContext context, PegVMInstruction *inst) {
  uint64_t start, end;
  start = timer();
  if (nez_VM_Execute(context, inst)) {
    nez_PrintErrorInfo("parse error");
  }
  end = timer();
  fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
  fprintf(stdout, "match\n\n");
  nez_DisposeObject(&context->left);
}

PegVMInstruction *nez_VM_Prepare(ParsingContext context, PegVMInstruction *inst) {
  long i;
  const void **table = (const void **)nez_VM_Execute(context, NULL);
  PegVMInstruction *ip = inst;
  for (i = 0; i < context->bytecode_length; i++) {
    ip->ptr = table[ip->opcode];
    ++ip;
  }
  return inst;
}

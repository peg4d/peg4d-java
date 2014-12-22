#include <stdio.h>
#include <sys/time.h> // gettimeofday
#include <string.h>
#include <assert.h>
#include <unistd.h>

#include "parsing.h"
#include "pegvm.h"

extern void PegVM_PrintProfile(const char *file_type);
struct Instruction *loadByteCodeFile(ParsingContext context,
                                     struct Instruction *inst,
                                     const char *fileName);

void dump_pego(ParsingObject *pego, char *source, int level);
void dump_json_file(FILE *file, ParsingObject *pego, char *source, int level);
void dump_pego_file(FILE *file, ParsingObject *pego, char *source, int level);

static uint64_t timer() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

static void peg_usage(const char *file) {
  fprintf(stderr, "Usage: %s -f peg_bytecode target_file\n", file);
  exit(EXIT_FAILURE);
}

static void peg_error(const char *errmsg) {
  fprintf(stderr, "%s\n", errmsg);
  exit(EXIT_FAILURE);
}

int main(int argc, char *const argv[]) {
  struct ParsingContext context;
  struct Instruction *inst = NULL;
  struct MemoryPool pool;
  const char *syntax_file = NULL;
  const char *output_type = NULL;
  const char *input_file = NULL;
  const char *orig_argv0 = argv[0];
  const char *file_type = NULL;
  int opt;
  while ((opt = getopt(argc, argv, "p:t:c:")) != -1) {
    switch (opt) {
    case 'p':
      syntax_file = optarg;
      break;
    case 't':
      output_type = optarg;
      break;
    case 'c':
      file_type = optarg;
      break;
    default: /* '?' */
      peg_usage(orig_argv0);
    }
  }
  argc -= optind;
  argv += optind;
  if (argc == 0) {
    peg_usage(orig_argv0);
  }
  if (syntax_file == NULL) {
    peg_error("not input syntaxfile");
  }
  input_file = argv[0];
  ParsingContext_Init(&context, input_file);
  inst = loadByteCodeFile(&context, inst, syntax_file);
  uint64_t bytecode_length = context.bytecode_length;
  MemoryPool_Init(&pool, context.pool_size * context.input_size / 100);
  inst = PegVM_Prepare(&context, inst, &pool);
  if (output_type == NULL || !strcmp(output_type, "pego")) {
    uint64_t start, end;
    context.bytecode_length = bytecode_length;
    start = timer();
    if (PegVM_Execute(&context, inst, &pool)) {
      peg_error("parse error");
    }
    end = timer();
    dump_pego(&context.left, context.inputs, 0);
    fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
  } else if (!strcmp(output_type, "stat")) {
    for (int i = 0; i < 20; i++) {
      uint64_t start, end;
      MemoryPool_Reset(&pool);
      start = timer();
      if (PegVM_Execute(&context, inst, &pool)) {
        peg_error("parse error");
      }
      end = timer();
      fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
      dispose_pego(&context.left);
      context.pos = 0;
    }
  } else if (!strcmp(output_type, "file")) {
    context.bytecode_length = bytecode_length;
    char output_file[256] = "dump_parsed_";
    char fileName[256];
    size_t input_fileName_len = strlen(input_file);
    size_t start = 0;
    size_t index = 0;
    while (input_fileName_len > 0) {
      input_fileName_len--;
      if (input_file[input_fileName_len] == '/') {
        start = input_fileName_len + 1;
        break;
      }
      if (input_file[input_fileName_len] == '.') {
        index = input_fileName_len;
      }
    }
    strncpy(fileName, input_file + start, index - start);
    strcat(output_file, fileName);
    strcat(output_file, ".txt");
    if (PegVM_Execute(&context, inst, &pool)) {
      peg_error("parse error");
    }
    FILE *file;
    file = fopen(output_file, "w");
    if (file == NULL) {
      assert(0 && "can not open file");
    }
    dump_pego_file(file, &context.left, context.inputs, 0);
    fclose(file);
  } else if (!strcmp(output_type, "json")) {
    context.bytecode_length = bytecode_length;
    char output_file[256] = "dump_parsed_";
    char fileName[256];
    size_t input_fileName_len = strlen(input_file);
    size_t start = 0;
    size_t index = 0;
    while (input_fileName_len > 0) {
      input_fileName_len--;
      if (input_file[input_fileName_len] == '/') {
        start = input_fileName_len + 1;
        break;
      }
      if (input_file[input_fileName_len] == '.') {
        index = input_fileName_len;
      }
    }
    strncpy(fileName, input_file + start, index - start);
    strcat(output_file, fileName);
    strcat(output_file, ".json");
    if (PegVM_Execute(&context, inst, &pool)) {
      peg_error("parse error");
    }
    FILE *file;
    file = fopen(output_file, "w");
    if (file == NULL) {
      assert(0 && "can not open file");
    }
    dump_json_file(file, &context.left, context.inputs, 0);
    fclose(file);
  }
  PegVM_PrintProfile(file_type);
  MemoryPool_Dispose(&pool);
  ParsingContext_Dispose(&context);
  free(inst);
  inst = NULL;
  return 0;
}

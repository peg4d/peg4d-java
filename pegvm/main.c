#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>

#include "libnez.h"
#include "pegvm.h"

extern void PegVM_PrintProfile(const char *file_type);
struct Instruction *nez_LoadMachineCode(ParsingContext context,
                                        struct Instruction *inst,
                                        const char *fileName,
                                        const char *nonTerminalName);

void dump_json_file(FILE *file, ParsingObject *pego, char *source, int level);
void dump_pego_file(FILE *file, ParsingObject *pego, char *source, int level);

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
  nez_CreateParsingContext(&context, input_file);
  inst = nez_LoadMachineCode(&context, inst, syntax_file, "String");
  uint64_t bytecode_length = context.bytecode_length;
  nez_CreateMemoryPool(&pool, context.pool_size * context.input_size / 100);
  inst = PegVM_Prepare(&context, inst, &pool);
  if (output_type == NULL || !strcmp(output_type, "pego")) {
    ParsingObject po = nez_Parse(&context, inst, &pool);
    nez_DisposeObject(&po);
  } else if (!strcmp(output_type, "match")) {
    nez_Match(&context, inst, &pool);
  } else if (!strcmp(output_type, "stat")) {
    nez_ParseStat(&context, inst, &pool);
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
    nez_DisposeObject(&context.left);
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
    nez_DisposeObject(&context.left);
    fclose(file);
  }
  PegVM_PrintProfile(file_type);
  nez_DisposeMemoryPool(&pool);
  nez_DisposeParsingContext(&context);
  free(inst);
  inst = NULL;
  return 0;
}

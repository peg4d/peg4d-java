#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <getopt.h>

#include "libnez.h"
#include "pegvm.h"

void dump_json_file(FILE *file, ParsingObject *pego, char *source, int level);
void dump_pego_file(FILE *file, ParsingObject *pego, char *source, int level);

static void nez_ShowUsage(const char *file) {
  // fprintf(stderr, "Usage: %s -f peg_bytecode target_file\n", file);
  fprintf(stderr, "\npegvm <command> optional files\n");
  fprintf(stderr, "  -p <filename> Specify an PEGs grammar bytecode file\n");
  fprintf(stderr, "  -i <filename> Specify an input file\n");
  fprintf(stderr, "  -o <filename> Specify an output file\n");
  fprintf(stderr, "  -t <type>     Specify an output type\n");
  fprintf(stderr, "  -h            Display this help and exit\n\n");
  exit(EXIT_FAILURE);
}

int main(int argc, char *const argv[]) {
  ParsingContext context = NULL;
  struct PegVMInstruction *inst = NULL;
  const char *syntax_file = NULL;
  const char *input_file = NULL;
  const char *output_type = NULL;
  const char *output_file = NULL;
  const char *file_type = NULL;
  const char *orig_argv0 = argv[0];
  int opt;
  while ((opt = getopt(argc, argv, "p:i:t:o:c:h:")) != -1) {
    switch (opt) {
    case 'p':
      syntax_file = optarg;
      break;
    case 'i':
      input_file = optarg;
      break;
    case 't':
      output_type = optarg;
      break;
    case 'o':
      output_file = optarg;
      break;
    case 'c':
      file_type = optarg;
      break;
    case 'h':
      nez_ShowUsage(orig_argv0);
    default: /* '?' */
      nez_ShowUsage(orig_argv0);
    }
  }
  if (syntax_file == NULL) {
    nez_PrintErrorInfo("not input syntaxfile");
  }
  context = nez_CreateParsingContext(input_file);
  inst = nez_LoadMachineCode(context, syntax_file, "File");
  nez_CreateMemoryPool(context->mpool, 128*1024*1024);
  if (output_type == NULL || !strcmp(output_type, "pego")) {
    ParsingObject po = nez_Parse(context, inst);
    nez_DisposeObject(po);
  } else if (!strcmp(output_type, "match")) {
    nez_Match(context, inst);
  } else if (!strcmp(output_type, "stat")) {
    nez_ParseStat(context, inst);
  } else if (!strcmp(output_type, "file")) {
    if (output_file == NULL) {
      nez_PrintErrorInfo("not input outoutfile");
    }
    nez_Parse(context, inst);
    FILE *file;
    file = fopen(output_file, "w");
    if (file == NULL) {
      assert(0 && "can not open file");
    }
    dump_pego_file(file, &context->left, context->inputs, 0);
    nez_DisposeObject(context->left);
    fclose(file);
  } else if (!strcmp(output_type, "json")) {
    if (output_file == NULL) {
      nez_PrintErrorInfo("not input outoutfile");
    }
    nez_Parse(context, inst);
    FILE *file;
    file = fopen(output_file, "w");
    if (file == NULL) {
      assert(0 && "can not open file");
    }
    dump_json_file(file, &context->left, context->inputs, 0);
    nez_DisposeObject(context->left);
    fclose(file);
  }
  nez_VM_PrintProfile(file_type);
  nez_DisposeInstruction(inst, context->bytecode_length);
  nez_DisposeMemoryPool(context->mpool);
  nez_DisposeParsingContext(context);
  return 0;
}

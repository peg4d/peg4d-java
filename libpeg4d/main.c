#include <stdio.h>
#include <sys/time.h> // gettimeofday
#include <string.h>
#include <assert.h>
#include <unistd.h>

#include "parsing.h"

long execute(ParsingContext context, struct Instruction *inst, MemoryPool pool);
struct Instruction *loadByteCodeFile(ParsingContext context, struct Instruction *inst, const char *fileName);

static void dump_pego(ParsingObject *pego, char *source, int level)
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

static void write_json(FILE *file, ParsingObject *pego, char* source, int level);
static int isJsonArray(ParsingObject pego);
static void write_json_array(FILE *file, ParsingObject *pego, char* source, int level);
static void write_json_indent(FILE *file, int level);
static void write_json_object(FILE *file, ParsingObject *pego, char* source, int level);

static void dump_json_file(FILE *file, ParsingObject *pego, char* source, int level)
{
    fprintf(file, "{\n" );
    fprintf(file, " \"tag\": \"%s\", \"value\": ", pego[0]->tag);
    write_json(file, pego, source, level+1);
    fprintf(file, "\n}");
}

static void write_json(FILE *file, ParsingObject *pego, char* source, int level)
{
    if (pego[0]) {
        if (pego[0]->child_size > 0) {
            if (isJsonArray(pego[0])) {
                write_json_array(file, pego, source, level);
            }
            else {
                write_json_object(file, pego, source, level);
            }
        }
        else {
            fprintf(file, "\"");
            if (pego[0]->value == NULL) {
                for (long j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
                    fprintf(file, "%c", source[j]);
                }
            }
            else {
                fprintf(file, "%s", pego[0]->value);
            }
            fprintf(file, "\"");
        }
    }
    else {
        fprintf(stderr, "%p tag:null\n", pego);
    }
}

static int isJsonArray(ParsingObject pego) {
    return pego->child_size > 1;
}

static void write_json_array(FILE *file, ParsingObject *pego, char* source, int level)
{
    fprintf(file, "[");
    for (int i = 0; i < pego[0]->child_size; i++) {
        fprintf(file, "\n");
        write_json_indent(file, level + 1);
        fprintf(file, "{");
        fprintf(file, "\n");
        write_json_indent(file, level + 2);
        fprintf(file, "\"tag\": \"%s\", \"value\": ", pego[0]->child[i]->tag);
        write_json(file, &pego[0]->child[i], source, level + 3);
        fprintf(file, "\n");
        write_json_indent(file, level + 1);
        fprintf(file, "}");
        if (i+1 < pego[0]->child_size) {
            fprintf(file, ",");
        }
    }
    fprintf(file, "\n");
    write_json_indent(file, level);
    fprintf(file, "]");
}

static void write_json_object(FILE *file, ParsingObject *pego, char* source, int level)
{
    if (pego[0]) {
        if (pego[0]->child_size > 0) {
            fprintf(file, "{");
            for (int i = 0; i < pego[0]->child_size; i++) {
                fprintf(file, "\n");
                write_json_indent(file, level + 1);
                fprintf(file, "\"%s\": ", pego[0]->child[i]->tag);
                write_json(file, &pego[0]->child[i], source, level + 2);
                if (i+1 < pego[0]->child_size) {
                    fprintf(file, ",");
                }
            }
            fprintf(file, "\n");
            write_json_indent(file, level + 1);
            fprintf(file, "}");
        }
        else {
            if (pego[0]->value == NULL) {
                for (long j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
                    fprintf(file, "%c", source[j]);
                }
            }
            else {
                fprintf(file, "%s", pego[0]->value);
            }
        }
    }
}

static void write_json_indent(FILE *file, int level)
{
    for (int i = 0; i < level; i++) {
        fprintf(file, " ");
    }
}

static void dump_pego_file(FILE *file, ParsingObject *pego, char* source, int level)
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


static uint64_t timer()
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
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

int main(int argc, char * const argv[])
{
    struct ParsingContext context;
    struct Instruction *inst = NULL;
    struct MemoryPool pool;
    const char *syntax_file = NULL;
    const char *output_type = NULL;
    const char *input_file = NULL;
    const char *orig_argv0 = argv[0];
    int opt;
    while ((opt = getopt(argc, argv, "p:t:")) != -1) {
        switch (opt) {
        case 'p':
            syntax_file = optarg;
            break;
        case 't':
            output_type = optarg;
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
    if(output_type == NULL || !strcmp(output_type, "pego")) {
        uint64_t start, end;
        context.bytecode_length = bytecode_length;
        start = timer();
        if(execute(&context, inst, &pool)) {
            peg_error("parse error");
        }
        end = timer();
        dump_pego(&context.left, context.inputs, 0);
        fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
    }
    else if(!strcmp(output_type, "stat")) {
        for (int i = 0; i < 20; i++) {
            uint64_t start, end;
            MemoryPool_Reset(&pool);
            start = timer();
            if(execute(&context, inst, &pool)) {
                peg_error("parse error");
            }
            end = timer();
            fprintf(stderr, "ErapsedTime: %llu msec\n", end - start);
            dispose_pego(&context.left);
            context.pos = 0;
        }
    }
    else if (!strcmp(output_type, "file")) {
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
        strncpy(fileName, input_file + start, index-start);
        strcat(output_file, fileName);
        strcat(output_file, ".txt");
        if(execute(&context, inst, &pool)) {
            peg_error("parse error");
        }
        FILE *file;
        file = fopen(output_file, "w");
        if (file == NULL) {
            assert(0 && "can not open file");
        }
        dump_pego_file(file, &context.left, context.inputs, 0);
        fclose(file);
    }
    else if(!strcmp(output_type, "json")) {
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
        strncpy(fileName, input_file + start, index-start);
        strcat(output_file, fileName);
        strcat(output_file, ".json");
        if(execute(&context, inst, &pool)) {
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
    MemoryPool_Dispose(&pool);
    ParsingContext_Dispose(&context);
    free(inst);
    inst = NULL;
    return 0;
}

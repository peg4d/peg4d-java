long execute(ParsingContext context, Instruction *inst, MemoryPool pool);

int main(int argc, char * const argv[])
{
    struct ParsingContext context;
    PegVMInstruction *inst = NULL;
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
    pool.pool_size = context.pool_size * context.input_size / 100;
    createMemoryPool(&pool);
    if(output_type == NULL || !strcmp(output_type, "pego")) {
        context.bytecode_length = bytecode_length;
        clock_t start = clock();
        if(execute(&context, inst, &pool)) {
            peg_error("parse error");
        }
        clock_t end = clock();
        dump_pego(&context.left, context.inputs, 0);
        fprintf(stderr, "ErapsedTime: %lf\n", (double)(end - start) / CLOCKS_PER_SEC);
    }
    else if(!strcmp(output_type, "stat")) {
        for (int i = 0; i < 20; i++) {
            init_pool(&pool);
            clock_t start = clock();
            if(execute(&context, inst, &pool)) {
                peg_error("parse error");
            }
            clock_t end = clock();
            fprintf(stderr, "ErapsedTime: %lf\n", (double)(end - start) / CLOCKS_PER_SEC);
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
    destroy_pool(&pool);
    ParsingContext_Dispose(&context);
    free(inst);
    inst = NULL;
    return 0;
}



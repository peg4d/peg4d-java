#include <stdio.h>
#include "libnez.h"

void dump_pego(ParsingObject *pego, char *source, int level) {
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
      } else {
        fprintf(stderr, "%s", pego[0]->value);
      }
      fprintf(stderr, "'");
    } else {
      fprintf(stderr, "\n");
      for (j = 0; j < pego[0]->child_size; j++) {
        dump_pego(&pego[0]->child[j], source, level + 1);
      }
      for (i = 0; i < level; i++) {
        fprintf(stderr, "  ");
      }
    }
    fprintf(stderr, "}\n");
  } else {
    fprintf(stderr, "%p tag:null\n", pego);
  }
}

void dump_pego_file(FILE *file, ParsingObject *pego, char *source, int level) {
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
      } else {
        fprintf(file, "%s", pego[0]->value);
      }
      fprintf(file, "'");
    } else {
      fprintf(file, "\n");
      for (j = 0; j < pego[0]->child_size; j++) {
        dump_pego_file(file, &pego[0]->child[j], source, level + 1);
      }
      for (i = 0; i < level; i++) {
        fprintf(file, " ");
      }
    }
    fprintf(file, "}\n");
  } else {
    fprintf(file, "%p tag:null\n", pego);
  }
}

static void write_json(FILE *file, ParsingObject *pego, char *source,
                       int level);
static int isJsonArray(ParsingObject pego);
static void write_json_array(FILE *file, ParsingObject *pego, char *source,
                             int level);
static void write_json_indent(FILE *file, int level);
static void write_json_object(FILE *file, ParsingObject *pego, char *source,
                              int level);

void dump_json_file(FILE *file, ParsingObject *pego, char *source, int level) {
  fprintf(file, "{\n");
  fprintf(file, " \"tag\": \"%s\", \"value\": ", pego[0]->tag);
  write_json(file, pego, source, level + 1);
  fprintf(file, "\n}");
}

static void write_json(FILE *file, ParsingObject *pego, char *source,
                       int level) {
  if (pego[0]) {
    if (pego[0]->child_size > 0) {
      if (isJsonArray(pego[0])) {
        write_json_array(file, pego, source, level);
      } else {
        write_json_object(file, pego, source, level);
      }
    } else {
      fprintf(file, "\"");
      if (pego[0]->value == NULL) {
        for (long j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
          fprintf(file, "%c", source[j]);
        }
      } else {
        fprintf(file, "%s", pego[0]->value);
      }
      fprintf(file, "\"");
    }
  } else {
    fprintf(stderr, "%p tag:null\n", pego);
  }
}

static int isJsonArray(ParsingObject pego) { return pego->child_size > 1; }

static void write_json_array(FILE *file, ParsingObject *pego, char *source,
                             int level) {
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
    if (i + 1 < pego[0]->child_size) {
      fprintf(file, ",");
    }
  }
  fprintf(file, "\n");
  write_json_indent(file, level);
  fprintf(file, "]");
}

static void write_json_object(FILE *file, ParsingObject *pego, char *source,
                              int level) {
  if (pego[0]) {
    if (pego[0]->child_size > 0) {
      fprintf(file, "{");
      for (int i = 0; i < pego[0]->child_size; i++) {
        fprintf(file, "\n");
        write_json_indent(file, level + 1);
        fprintf(file, "\"%s\": ", pego[0]->child[i]->tag);
        write_json(file, &pego[0]->child[i], source, level + 2);
        if (i + 1 < pego[0]->child_size) {
          fprintf(file, ",");
        }
      }
      fprintf(file, "\n");
      write_json_indent(file, level + 1);
      fprintf(file, "}");
    } else {
      if (pego[0]->value == NULL) {
        for (long j = pego[0]->start_pos; j < pego[0]->end_pos; j++) {
          fprintf(file, "%c", source[j]);
        }
      } else {
        fprintf(file, "%s", pego[0]->value);
      }
    }
  }
}

static void write_json_indent(FILE *file, int level) {
  for (int i = 0; i < level; i++) {
    fprintf(file, " ");
  }
}

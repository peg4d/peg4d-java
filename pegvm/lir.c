#include <stdbool.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <limits.h>

typedef struct ByteCodeLoader {
  char *input;
  byteCodeInfo *info;
  PegVMInstruction *head;
} ByteCodeLoader;

typedef struct charpair {
  char c1;
  char c2;
} __attribute__((packed)) charpair;

typedef struct pegvm_string {
  unsigned len;
  char text[1];
} *pegvm_string_ptr_t;

typedef struct inst_table_t {
  PegVMInstruction *table[256];
} *inst_table_t;

typedef struct int_table_t {
  int table[256];
} *int_table_t;

typedef struct short_table_t {
  short table[256];
} *short_table_t;

typedef struct char_table_t {
  char table[256];
} *char_table_t;

typedef struct bit_table_t {
  char table[32];
} *bit_table_t;

typedef union PegVMInstructionBase {
  int opcode;
  const void *addr;
} PegVMInstructionBase;

static uint16_t read16(char *inputs, byteCodeInfo *info) {
  uint16_t value = (uint8_t)inputs[info->pos++];
  value = (value) | ((uint8_t)inputs[info->pos++] << 8);
  return value;
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

static uint32_t Loader_Read32(ByteCodeLoader *loader) {
  return read32(loader->input, loader->info);
}

static uint16_t Loader_Read16(ByteCodeLoader *loader) {
  return read16(loader->input, loader->info);
}

static PegVMInstruction *GetJumpAddr(PegVMInstruction *inst, int offset);

static PegVMInstruction *Loader_GetJumpAddr(ByteCodeLoader *loader, PegVMInstruction *inst) {
    int dst = Loader_Read32(loader);
#if 0
    return GetJumpAddr(loader->head, dst);
#else
    return GetJumpAddr(inst, dst);
#endif
}

static pegvm_string_ptr_t Loader_ReadString(ByteCodeLoader *loader) {
  uint32_t len = Loader_Read16(loader);
  pegvm_string_ptr_t str = (pegvm_string_ptr_t)__malloc(sizeof(*str) - 1 + len);
  str->len = len;
  for (uint32_t i = 0; i < len; i++) {
    str->text[i] = Loader_Read32(loader);
  }
  return str;
}

static pegvm_string_ptr_t Loader_ReadName(ByteCodeLoader *loader) {
  uint32_t len = Loader_Read16(loader);
  pegvm_string_ptr_t str = (pegvm_string_ptr_t)__malloc(sizeof(*str) - 1 + len);
  str->len = len;
  for (uint32_t i = 0; i < len; i++) {
    str->text[i] = loader->input[loader->info->pos++];
  }
  return str;
}

#define OPCODE_IEXIT 0
typedef struct IEXIT {
  PegVMInstructionBase base;
} IEXIT;

static void Emit_EXIT(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IEXIT *ir = (IEXIT *)inst;
  ir->base.opcode = OPCODE_IEXIT;
}
#define OPCODE_IJUMP 1
typedef struct IJUMP {
  PegVMInstructionBase base;
  PegVMInstruction * jump;
} IJUMP;

static void Emit_JUMP(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IJUMP *ir = (IJUMP *)inst;
  ir->base.opcode = OPCODE_IJUMP;
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_ICALL 2
typedef struct ICALL {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
} ICALL;

static void Emit_CALL(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICALL *ir = (ICALL *)inst;
  ir->base.opcode = OPCODE_ICALL;
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_IRET 3
typedef struct IRET {
  PegVMInstructionBase base;
} IRET;

static void Emit_RET(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IRET *ir = (IRET *)inst;
  ir->base.opcode = OPCODE_IRET;
}
#define OPCODE_ICONDBRANCH 4
typedef struct ICONDBRANCH {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  int val;
} ICONDBRANCH;

static void Emit_CONDTRUE(PegVMInstruction *inst, ByteCodeLoader *loader);
static void Emit_CONDFALSE(PegVMInstruction *inst, ByteCodeLoader *loader);
static void Emit_CONDBRANCH(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICONDBRANCH *ir = (ICONDBRANCH *)inst;
  ir->base.opcode = OPCODE_ICONDBRANCH;
  ir->val = Loader_Read32(loader);
  if (ir->val) {
    Emit_CONDTRUE(inst, loader);
  } else {
    Emit_CONDFALSE(inst, loader);
  }
  // ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_ICONDTRUE 5
typedef struct ICONDTRUE {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  int val;
} ICONDTRUE;

static void Emit_CONDTRUE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICONDTRUE *ir = (ICONDTRUE *)inst;
  ir->base.opcode = OPCODE_ICONDTRUE;
  // ir->val = Loader_Read32(loader);
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_ICONDFALSE 6
typedef struct ICONDFALSE {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  int val;
} ICONDFALSE;

static void Emit_CONDFALSE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICONDFALSE *ir = (ICONDFALSE *)inst;
  ir->base.opcode = OPCODE_ICONDFALSE;
  // ir->val = Loader_Read32(loader);
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_IREPCOND 7
typedef struct IREPCOND {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
} IREPCOND;

static void Emit_REPCOND(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IREPCOND *ir = (IREPCOND *)inst;
  ir->base.opcode = OPCODE_IREPCOND;
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_ICHARRANGE 8
typedef struct ICHARRANGE {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  charpair cdata;
} ICHARRANGE;

static void Emit_CHARRANGE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICHARRANGE *ir = (ICHARRANGE *)inst;
  ir->base.opcode = OPCODE_ICHARRANGE;
  ir->jump = Loader_GetJumpAddr(loader, inst);
  ir->cdata.c1 = Loader_Read32(loader);
  ir->cdata.c2 = Loader_Read32(loader);
}
#define OPCODE_ICHARSET 9
typedef struct ICHARSET {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  bit_table_t set;
} ICHARSET;

static void Emit_CHARSET(PegVMInstruction *inst, ByteCodeLoader *loader) {
  int len = Loader_Read16(loader);
  ICHARSET *ir = (ICHARSET *)inst;
  ir->base.opcode = OPCODE_ICHARSET;
  ir->set = (bit_table_t)__malloc(sizeof(struct bit_table_t));
  for (int i = 0; i < len; i++) {
    char c = Loader_Read32(loader);
    ir->set->table[c / 8] |= 1 << (c % 8);
  }
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_ISTRING 10
typedef struct ISTRING {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  pegvm_string_ptr_t chardata;
} ISTRING;

static void Emit_STRING(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTRING *ir = (ISTRING *)inst;
  ir->base.opcode = OPCODE_ISTRING;
  ir->jump = Loader_GetJumpAddr(loader, inst);
  ir->chardata = Loader_ReadString(loader);
}
#define OPCODE_ISTRING1 11
/* optimized STRING(chardata.len == 1) */
typedef struct ISTRING1 {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  pegvm_string_ptr_t chardata;
} ISTRING1;

// static void Emit_STRING1(PegVMInstruction *inst, ByteCodeLoader *loader)
// {
//   ISTRING1 *ir = (ISTRING1 *) inst;
//   ir->base.opcode = OPCODE_ISTRING1;
//   ir->jump = Loader_GetJumpAddr(loader, inst);
//   ir->chardata = Loader_ReadString(loader);
// }
#define OPCODE_ISTRING2 12
/* optimized STRING(chardata.len == 2) */
typedef struct ISTRING2 {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  pegvm_string_ptr_t chardata;
} ISTRING2;

// static void Emit_STRING2(PegVMInstruction *inst, ByteCodeLoader *loader)
// {
//   ISTRING2 *ir = (ISTRING2 *) inst;
//   ir->base.opcode = OPCODE_ISTRING2;
//   ir->jump = Loader_GetJumpAddr(loader, inst);
//   ir->chardata = Loader_ReadString(loader);
// }
#define OPCODE_ISTRING4 13
/* optimized STRING(chardata.len == 4) */
typedef struct ISTRING4 {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  pegvm_string_ptr_t chardata;
} ISTRING4;

// static void Emit_STRING4(PegVMInstruction *inst, ByteCodeLoader *loader)
// {
//   ISTRING4 *ir = (ISTRING4 *) inst;
//   ir->base.opcode = OPCODE_ISTRING4;
//   ir->jump = Loader_GetJumpAddr(loader, inst);
//   ir->chardata = Loader_ReadString(loader);
// }
#define OPCODE_IANY 14
typedef struct IANY {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
} IANY;

static void Emit_ANY(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IANY *ir = (IANY *)inst;
  ir->base.opcode = OPCODE_IANY;
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_IPUSHo 15
typedef struct IPUSHo {
  PegVMInstructionBase base;
} IPUSHo;

static void Emit_PUSHo(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IPUSHo *ir = (IPUSHo *)inst;
  ir->base.opcode = OPCODE_IPUSHo;
}
#define OPCODE_IPUSHconnect 16
typedef struct IPUSHconnect {
  PegVMInstructionBase base;
} IPUSHconnect;

static void Emit_PUSHconnect(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IPUSHconnect *ir = (IPUSHconnect *)inst;
  ir->base.opcode = OPCODE_IPUSHconnect;
}
#define OPCODE_IPUSHp1 17
typedef struct IPUSHp1 {
  PegVMInstructionBase base;
} IPUSHp1;

static void Emit_PUSHp1(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IPUSHp1 *ir = (IPUSHp1 *)inst;
  ir->base.opcode = OPCODE_IPUSHp1;
}
#define OPCODE_ILOADp1 18
typedef struct ILOADp1 {
  PegVMInstructionBase base;
} ILOADp1;

static void Emit_LOADp1(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ILOADp1 *ir = (ILOADp1 *)inst;
  ir->base.opcode = OPCODE_ILOADp1;
}
#define OPCODE_ILOADp2 19
typedef struct ILOADp2 {
  PegVMInstructionBase base;
} ILOADp2;

static void Emit_LOADp2(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ILOADp2 *ir = (ILOADp2 *)inst;
  ir->base.opcode = OPCODE_ILOADp2;
}
#define OPCODE_ILOADp3 20
typedef struct ILOADp3 {
  PegVMInstructionBase base;
} ILOADp3;

static void Emit_LOADp3(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ILOADp3 *ir = (ILOADp3 *)inst;
  ir->base.opcode = OPCODE_ILOADp3;
}
#define OPCODE_IPOPp 21
typedef struct IPOPp {
  PegVMInstructionBase base;
} IPOPp;

static void Emit_POPp(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IPOPp *ir = (IPOPp *)inst;
  ir->base.opcode = OPCODE_IPOPp;
}
#define OPCODE_IPOPo 22
typedef struct IPOPo {
  PegVMInstructionBase base;
} IPOPo;

static void Emit_POPo(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IPOPo *ir = (IPOPo *)inst;
  ir->base.opcode = OPCODE_IPOPo;
}
#define OPCODE_ISTOREo 23
typedef struct ISTOREo {
  PegVMInstructionBase base;
} ISTOREo;

static void Emit_STOREo(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTOREo *ir = (ISTOREo *)inst;
  ir->base.opcode = OPCODE_ISTOREo;
}
#define OPCODE_ISTOREp 24
typedef struct ISTOREp {
  PegVMInstructionBase base;
} ISTOREp;

static void Emit_STOREp(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTOREp *ir = (ISTOREp *)inst;
  ir->base.opcode = OPCODE_ISTOREp;
}
#define OPCODE_ISTOREp1 25
typedef struct ISTOREp1 {
  PegVMInstructionBase base;
} ISTOREp1;

static void Emit_STOREp1(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTOREp1 *ir = (ISTOREp1 *)inst;
  ir->base.opcode = OPCODE_ISTOREp1;
}
#define OPCODE_ISTOREp2 26
typedef struct ISTOREp2 {
  PegVMInstructionBase base;
} ISTOREp2;

static void Emit_STOREp2(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTOREp2 *ir = (ISTOREp2 *)inst;
  ir->base.opcode = OPCODE_ISTOREp2;
}
#define OPCODE_ISTOREp3 27
typedef struct ISTOREp3 {
  PegVMInstructionBase base;
} ISTOREp3;

static void Emit_STOREp3(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTOREp3 *ir = (ISTOREp3 *)inst;
  ir->base.opcode = OPCODE_ISTOREp3;
}
#define OPCODE_ISTOREflag 28
typedef struct ISTOREflag {
  PegVMInstructionBase base;
  int val;
} ISTOREflag;

static void Emit_STOREflag(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISTOREflag *ir = (ISTOREflag *)inst;
  ir->base.opcode = OPCODE_ISTOREflag;
  ir->val = Loader_Read32(loader);
}
#define OPCODE_INEW 29
typedef struct INEW {
  PegVMInstructionBase base;
} INEW;

static void Emit_NEW(PegVMInstruction *inst, ByteCodeLoader *loader) {
  INEW *ir = (INEW *)inst;
  ir->base.opcode = OPCODE_INEW;
}
#define OPCODE_INEWJOIN 30
typedef struct INEWJOIN {
  PegVMInstructionBase base;
  int ndata;
} INEWJOIN;

static void Emit_NEWJOIN(PegVMInstruction *inst, ByteCodeLoader *loader) {
  INEWJOIN *ir = (INEWJOIN *)inst;
  ir->base.opcode = OPCODE_INEWJOIN;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_ICOMMIT 31
typedef struct ICOMMIT {
  PegVMInstructionBase base;
  int ndata;
} ICOMMIT;

static void Emit_COMMIT(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICOMMIT *ir = (ICOMMIT *)inst;
  ir->base.opcode = OPCODE_ICOMMIT;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_IABORT 32
typedef struct IABORT {
  PegVMInstructionBase base;
} IABORT;

static void Emit_ABORT(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IABORT *ir = (IABORT *)inst;
  ir->base.opcode = OPCODE_IABORT;
}
#define OPCODE_ILINK 33
typedef struct ILINK {
  PegVMInstructionBase base;
  int ndata;
} ILINK;

static void Emit_LINK(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ILINK *ir = (ILINK *)inst;
  ir->base.opcode = OPCODE_ILINK;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_ISETendp 34
typedef struct ISETendp {
  PegVMInstructionBase base;
} ISETendp;

static void Emit_SETendp(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISETendp *ir = (ISETendp *)inst;
  ir->base.opcode = OPCODE_ISETendp;
}
#define OPCODE_ITAG 35
typedef struct ITAG {
  PegVMInstructionBase base;
  pegvm_string_ptr_t chardata;
} ITAG;

static void Emit_TAG(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ITAG *ir = (ITAG *)inst;
  ir->base.opcode = OPCODE_ITAG;
  ir->chardata = Loader_ReadName(loader);
}
#define OPCODE_IVALUE 36
typedef struct IVALUE {
  PegVMInstructionBase base;
  pegvm_string_ptr_t chardata;
} IVALUE;

static void Emit_VALUE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IVALUE *ir = (IVALUE *)inst;
  ir->base.opcode = OPCODE_IVALUE;
  ir->chardata = Loader_ReadName(loader);
}
#define OPCODE_IMAPPEDCHOICE 37
typedef struct IMAPPEDCHOICE {
  PegVMInstructionBase base;
  int_table_t ndata;
} IMAPPEDCHOICE;

#define OPCODE_IMAPPEDCHOICE_8 38
typedef struct IMAPPEDCHOICE_8 {
  PegVMInstructionBase base;
  char_table_t ndata;
} IMAPPEDCHOICE_8;

// static void Emit_MAPPEDCHOICE_8(PegVMInstruction *inst, ByteCodeLoader*loader)
// {
//   IMAPPEDCHOICE_8 *ir = (IMAPPEDCHOICE_8 *) inst;
//   ir->base.opcode = OPCODE_IMAPPEDCHOICE_8;
//   ir->ndata =  (char_table_t) __malloc(sizeof(struct char_table_t));
//   for (int i = 0; i < 256; i++) {
//       ir->ndata->table[i] = (char)Loader_Read32(loader);
//   }
// }
#define OPCODE_IMAPPEDCHOICE_16 39
typedef struct IMAPPEDCHOICE_16 {
  PegVMInstructionBase base;
  short_table_t ndata;
} IMAPPEDCHOICE_16;

// static void Emit_MAPPEDCHOICE_16(PegVMInstruction *inst, ByteCodeLoader*loader)
// {
//   IMAPPEDCHOICE_16 *ir = (IMAPPEDCHOICE_16 *) inst;
//   ir->base.opcode = OPCODE_IMAPPEDCHOICE_16;
//   ir->ndata =  (short_table_t) __malloc(sizeof(struct short_table_t));
//   for (int i = 0; i < 256; i++) {
//       ir->ndata->table[i] = (short)Loader_Read32(loader);
//   }
// }

#define MAX(a, b) ((a) < (b) ? (b) : (a))
static void Emit_MAPPEDCHOICE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  struct int_table_t table = {};
  int max_offset = 0;
  IMAPPEDCHOICE *ir = (IMAPPEDCHOICE *)inst;
  ir->base.opcode = OPCODE_IMAPPEDCHOICE;
  for (int i = 0; i < 256; i++) {
    int offset = Loader_Read32(loader);
    max_offset = MAX(max_offset, offset);
    table.table[i] = offset;
  }
  if (max_offset < CHAR_MAX) {
    IMAPPEDCHOICE_8 *ir2 = (IMAPPEDCHOICE_8 *) ir;
    ir2->base.opcode = OPCODE_IMAPPEDCHOICE_8;
    ir2->ndata = (char_table_t) __malloc(sizeof(struct char_table_t));
    for (int i = 0; i < 256; i++) {
      ir2->ndata->table[i] = (char)table.table[i];
    }
    return;
  }
  if (max_offset < SHRT_MAX) {
    IMAPPEDCHOICE_16 *ir2 = (IMAPPEDCHOICE_16 *) ir;
    ir2->base.opcode = OPCODE_IMAPPEDCHOICE_16;
    ir2->ndata =  (short_table_t) __malloc(sizeof(struct short_table_t));
    for (int i = 0; i < 256; i++) {
      ir2->ndata->table[i] = (short)table.table[i];
    }
    return;
  }
  ir->ndata = (int_table_t)__malloc(sizeof(struct int_table_t));
  for (int i = 0; i < 256; i++) {
      ir->ndata->table[i] = table.table[i];
  }
}

#define OPCODE_ISCAN 40
typedef struct ISCAN {
  PegVMInstructionBase base;
  int cardinals;
  int offset;
} ISCAN;

static void Emit_SCAN(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ISCAN *ir = (ISCAN *)inst;
  ir->base.opcode = OPCODE_ISCAN;
  ir->cardinals = Loader_Read32(loader);
  ir->offset = Loader_Read32(loader);
}
#define OPCODE_ICHECKEND 41
typedef struct ICHECKEND {
  PegVMInstructionBase base;
  int ndata;
  PegVMInstruction *jump;
} ICHECKEND;

static void Emit_CHECKEND(PegVMInstruction *inst, ByteCodeLoader *loader) {
  ICHECKEND *ir = (ICHECKEND *)inst;
  ir->base.opcode = OPCODE_ICHECKEND;
  ir->ndata = Loader_Read32(loader);
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_IDEF 42
typedef struct IDEF {
  PegVMInstructionBase base;
  int ndata;
} IDEF;

static void Emit_DEF(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IDEF *ir = (IDEF *)inst;
  ir->base.opcode = OPCODE_IDEF;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_IIS 43
typedef struct IIS {
  PegVMInstructionBase base;
  int ndata;
} IIS;

static void Emit_IS(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IIS *ir = (IIS *)inst;
  ir->base.opcode = OPCODE_IIS;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_IISA 44
typedef struct IISA {
  PegVMInstructionBase base;
  int ndata;
} IISA;

static void Emit_ISA(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IISA *ir = (IISA *)inst;
  ir->base.opcode = OPCODE_IISA;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_IBLOCKSTART 45
typedef struct IBLOCKSTART {
  PegVMInstructionBase base;
  int ndata;
} IBLOCKSTART;

static void Emit_BLOCKSTART(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IBLOCKSTART *ir = (IBLOCKSTART *)inst;
  ir->base.opcode = OPCODE_IBLOCKSTART;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_IBLOCKEND 46
typedef struct IBLOCKEND {
  PegVMInstructionBase base;
} IBLOCKEND;

static void Emit_BLOCKEND(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IBLOCKEND *ir = (IBLOCKEND *)inst;
  ir->base.opcode = OPCODE_IBLOCKEND;
}
#define OPCODE_IINDENT 47
typedef struct IINDENT {
  PegVMInstructionBase base;
  int ndata;
} IINDENT;

static void Emit_INDENT(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IINDENT *ir = (IINDENT *)inst;
  ir->base.opcode = OPCODE_IINDENT;
  ir->ndata = Loader_Read32(loader);
}
#define OPCODE_INOTBYTE 48
typedef struct INOTBYTE {
  PegVMInstructionBase base;
  char cdata;
  PegVMInstruction *jump;
} INOTBYTE;

static void Emit_NOTBYTE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  INOTBYTE *ir = (INOTBYTE *)inst;
  ir->base.opcode = OPCODE_INOTBYTE;
  ir->cdata = Loader_Read32(loader);
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_INOTANY 49
typedef struct INOTANY {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
} INOTANY;

static void Emit_NOTANY(PegVMInstruction *inst, ByteCodeLoader *loader) {
  INOTANY *ir = (INOTANY *)inst;
  ir->base.opcode = OPCODE_INOTANY;
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_INOTCHARSET 50
typedef struct INOTCHARSET {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  bit_table_t set;
} INOTCHARSET;

static void Emit_NOTCHARSET(PegVMInstruction *inst, ByteCodeLoader *loader) {
  int len = Loader_Read16(loader);
  INOTCHARSET *ir = (INOTCHARSET *)inst;
  ir->base.opcode = OPCODE_INOTCHARSET;
  ir->set = (bit_table_t)__malloc(sizeof(struct bit_table_t));
  for (int i = 0; i < len; i++) {
    char c = Loader_Read32(loader);
    ir->set->table[c / 8] |= 1 << (c % 8);
  }
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_INOTBYTERANGE 51
typedef struct INOTBYTERANGE {
  PegVMInstructionBase base;
  charpair cdata;
  PegVMInstruction *jump;
} INOTBYTERANGE;

static void Emit_NOTBYTERANGE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  INOTBYTERANGE *ir = (INOTBYTERANGE *)inst;
  ir->base.opcode = OPCODE_INOTBYTERANGE;
  ir->cdata.c1 = Loader_Read32(loader);
  ir->cdata.c2 = Loader_Read32(loader);
  ir->jump = Loader_GetJumpAddr(loader, inst);
}
#define OPCODE_INOTSTRING 52
typedef struct INOTSTRING {
  PegVMInstructionBase base;
  PegVMInstruction *jump;
  pegvm_string_ptr_t cdata;
} INOTSTRING;

static void Emit_NOTSTRING(PegVMInstruction *inst, ByteCodeLoader *loader) {
  INOTSTRING *ir = (INOTSTRING *)inst;
  ir->base.opcode = OPCODE_INOTSTRING;
  ir->jump = Loader_GetJumpAddr(loader, inst);
  ir->cdata = Loader_ReadString(loader);
}
#define OPCODE_IOPTIONALBYTE 53
typedef struct IOPTIONALBYTE {
  PegVMInstructionBase base;
  char cdata;
} IOPTIONALBYTE;

static void Emit_OPTIONALBYTE(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IOPTIONALBYTE *ir = (IOPTIONALBYTE *)inst;
  ir->base.opcode = OPCODE_IOPTIONALBYTE;
  ir->cdata = Loader_Read32(loader);
}
#define OPCODE_IOPTIONALCHARSET 54
typedef struct IOPTIONALCHARSET {
  PegVMInstructionBase base;
  bit_table_t set;
} IOPTIONALCHARSET;

static void Emit_OPTIONALCHARSET(PegVMInstruction *inst,
                                 ByteCodeLoader *loader) {
  int len = Loader_Read16(loader);
  IOPTIONALCHARSET *ir = (IOPTIONALCHARSET *)inst;
  ir->base.opcode = OPCODE_IOPTIONALCHARSET;
  ir->set = (bit_table_t)__malloc(sizeof(struct bit_table_t));
  for (int i = 0; i < len; i++) {
    char c = Loader_Read32(loader);
    ir->set->table[c / 8] |= 1 << (c % 8);
  }
}
#define OPCODE_IOPTIONALBYTERANGE 55
typedef struct IOPTIONALBYTERANGE {
  PegVMInstructionBase base;
  charpair cdata;
} IOPTIONALBYTERANGE;

static void Emit_OPTIONALBYTERANGE(PegVMInstruction *inst,
                                   ByteCodeLoader *loader) {
  IOPTIONALBYTERANGE *ir = (IOPTIONALBYTERANGE *)inst;
  ir->base.opcode = OPCODE_IOPTIONALBYTERANGE;
  ir->cdata.c1 = Loader_Read32(loader);
  ir->cdata.c2 = Loader_Read32(loader);
}
#define OPCODE_IOPTIONALSTRING 56
typedef struct IOPTIONALSTRING {
  PegVMInstructionBase base;
  pegvm_string_ptr_t cdata;
} IOPTIONALSTRING;

static void Emit_OPTIONALSTRING(PegVMInstruction *inst,
                                ByteCodeLoader *loader) {
  IOPTIONALSTRING *ir = (IOPTIONALSTRING *)inst;
  ir->base.opcode = OPCODE_IOPTIONALSTRING;
  ir->cdata = Loader_ReadString(loader);
}
#define OPCODE_IZEROMOREBYTERANGE 57
typedef struct IZEROMOREBYTERANGE {
  PegVMInstructionBase base;
  charpair cdata;
} IZEROMOREBYTERANGE;

static void Emit_ZEROMOREBYTERANGE(PegVMInstruction *inst,
                                   ByteCodeLoader *loader) {
  IZEROMOREBYTERANGE *ir = (IZEROMOREBYTERANGE *)inst;
  ir->base.opcode = OPCODE_IZEROMOREBYTERANGE;
  ir->cdata.c1 = Loader_Read32(loader);
  ir->cdata.c2 = Loader_Read32(loader);
}
#define OPCODE_IZEROMORECHARSET 58
typedef struct IZEROMORECHARSET {
  PegVMInstructionBase base;
  bit_table_t set;
} IZEROMORECHARSET;

static void Emit_ZEROMORECHARSET(PegVMInstruction *inst,
                                 ByteCodeLoader *loader) {
  int len = Loader_Read16(loader);
  IZEROMORECHARSET *ir = (IZEROMORECHARSET *)inst;
  ir->base.opcode = OPCODE_IZEROMORECHARSET;
  ir->set = (bit_table_t)__malloc(sizeof(struct bit_table_t));
  for (int i = 0; i < len; i++) {
    char c = Loader_Read32(loader);
    ir->set->table[c / 8] |= 1 << (c % 8);
  }
}
#define OPCODE_IZEROMOREWS 59
typedef struct IZEROMOREWS {
  PegVMInstructionBase base;
} IZEROMOREWS;

static void Emit_ZEROMOREWS(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IZEROMOREWS *ir = (IZEROMOREWS *)inst;
  ir->base.opcode = OPCODE_IZEROMOREWS;
}
#define OPCODE_IREPEATANY 60
typedef struct IREPEATANY {
  PegVMInstructionBase base;
  int ndata;
} IREPEATANY;

static void Emit_REPEATANY(PegVMInstruction *inst, ByteCodeLoader *loader) {
  IREPEATANY *ir = (IREPEATANY *)inst;
  ir->base.opcode = OPCODE_IREPEATANY;
  ir->ndata = Loader_Read32(loader);
}
#define LIR_MAX_OPCODE 61
#define LIR_EACH(OP)                                                           \
  OP(EXIT) OP(JUMP) OP(CALL) OP(RET) OP(CONDBRANCH) OP(CONDTRUE) OP(CONDFALSE) \
      OP(REPCOND) OP(CHARRANGE) OP(CHARSET) OP(STRING) OP(STRING1) OP(STRING2) \
      OP(STRING4) OP(ANY) OP(PUSHo) OP(PUSHconnect) OP(PUSHp1) OP(LOADp1)      \
      OP(LOADp2) OP(LOADp3) OP(POPp) OP(POPo) OP(STOREo) OP(STOREp)            \
      OP(STOREp1) OP(STOREp2) OP(STOREp3) OP(STOREflag) OP(NEW) OP(NEWJOIN)    \
      OP(COMMIT) OP(ABORT) OP(LINK) OP(SETendp) OP(TAG) OP(VALUE)              \
      OP(MAPPEDCHOICE) OP(MAPPEDCHOICE_8) OP(MAPPEDCHOICE_16) OP(SCAN)         \
      OP(CHECKEND) OP(DEF) OP(IS) OP(ISA) OP(BLOCKSTART) OP(BLOCKEND)          \
      OP(INDENT) OP(NOTBYTE) OP(NOTANY) OP(NOTCHARSET) OP(NOTBYTERANGE)        \
      OP(NOTSTRING) OP(OPTIONALBYTE) OP(OPTIONALCHARSET) OP(OPTIONALBYTERANGE) \
      OP(OPTIONALSTRING) OP(ZEROMOREBYTERANGE) OP(ZEROMORECHARSET)             \
      OP(ZEROMOREWS) OP(REPEATANY)

union PegVMInstruction {
#define DEF_PEGVM_INST_UNION(OP) I##OP _##OP;
  LIR_EACH(DEF_PEGVM_INST_UNION);
#undef DEF_PEGVM_INST_UNION
  PegVMInstructionBase base;
};

static PegVMInstruction *GetJumpAddr(PegVMInstruction *inst, int offset) {
  return inst + offset;
}

#if 0
int main(int argc, char const* argv[])
{
    fprintf(stdout, "%zd\n", sizeof(PegVMInstruction));
#define PRINT_OP(OP) fprintf(stdout, #OP ": %zd\n", sizeof(I##OP));
    LIR_EACH(PRINT_OP);
    return 0;
}
#endif

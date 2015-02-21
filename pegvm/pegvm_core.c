/* pegvm.c */

static int pegvm_string_equal(pegvm_string_ptr_t str, const char *t) {
  int len = str->len;
  const char *p = str->text;
  const char *end = p + len;
#if 0
  return (strncmp(p, t, len) == 0) ? len : 0;
#else
  while (p < end) {
    if (*p++ != *t++) {
      return 0;
    }
  }
  return len;
#endif
}

static ParsingObject P4D_newObject2(ParsingContext context, const char *cur, MemoryPool mpool, ParsingObject left)
{
    ParsingObject po = P4D_newObject(context, cur, mpool);
#if 1
    *po = *left;
#else
    memcpy(po, left, sizeof(*po));
#endif
    return po;
}

#if __GNUC__ >= 3
# define likely(x) __builtin_expect(!!(x), 1)
# define unlikely(x) __builtin_expect(!!(x), 0)
#else
# define likely(x) (x)
# define unlikely(x) (x)
#endif

#define PUSH_IP(PC) *cp++ = (PC)
#define POP_IP() --cp
#define SP_TOP(INST) (*sp)
#define PUSH_SP(INST) (*sp++ = (INST))
#define POP_SP(INST) (*--sp)
#define PUSH_OSP(INST) (*osp++ = (INST))
#define POP_OSP(INST) (*--osp)

#define GET_ADDR(PC) ((PC)->base).addr
#define DISPATCH_NEXT goto *GET_ADDR(++pc)
#define JUMP(dst)     goto *GET_ADDR(pc = dst)
#define JUMP_REL(dst) goto *GET_ADDR(pc += dst)
#define RET           goto *GET_ADDR(pc = *POP_IP())

#define OP(OP) PEGVM_OP_##OP:
long nez_VM_Execute(ParsingContext context, PegVMInstruction *inst) {
  static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
    LIR_EACH(DEFINE_TABLE)
#undef DEFINE_TABLE
  };

#if 1
#define LEFT left
  ParsingObject left = context->left;
#else
#define LEFT context->left
#endif
  register const char *cur = context->inputs + context->pos;
#if 0
#define MPOOL pool
  MemoryPool pool = context->mpool;
#else
#define MPOOL context->mpool
#endif

  const char *Reg1 = 0;
  const char *Reg2 = 0;
  const char *Reg3 = 0;
  register int failflag = 0;
  register const PegVMInstruction *pc;
  ParsingObject *osp;
  const PegVMInstruction **cp;
  const char **sp;
  pc = inst + context->startPoint;
  sp = (const char **)context->stack_pointer;
  osp = context->object_stack_pointer;
  cp = (const PegVMInstruction **) context->call_stack_pointer;

  if (inst == NULL) {
    return (long)table;
  }

  PUSH_IP(inst);
  LEFT = P4D_setObject_(context, LEFT, P4D_newObject(context, cur, MPOOL));

  goto *GET_ADDR(pc);

  OP(EXIT) {
    P4D_commitLog(context, 0, LEFT, MPOOL);
    context->left = LEFT;
    context->pos = cur - context->inputs;
    return failflag;
  }
  OP(JUMP) {
    PegVMInstruction *dst = ((IJUMP *)pc)->jump;
    JUMP(dst);
  }
  OP(CALL) {
    PegVMInstruction *dst = ((ICALL *)pc)->jump;
    PUSH_IP(pc + 1);
    JUMP(dst);
  }
  OP(RET) {
    RET;
  }
  OP(CONDBRANCH) {
#ifdef PEGVM_USE_CONDBRANCH
    PegVMInstruction *dst = ((ICONDBRANCH *)pc)->jump;
    if (failflag == ((ICONDBRANCH *)pc)->val) {
      JUMP(dst);
    }
    else {
      DISPATCH_NEXT;
    }
#endif
  }

  OP(CONDTRUE) {
    PegVMInstruction *dst = ((ICONDBRANCH *)pc)->jump;
    if (failflag == 1) {
      JUMP(dst);
    }
    else {
      DISPATCH_NEXT;
    }
  }
  OP(CONDFALSE) {
    PegVMInstruction *dst = ((ICONDBRANCH *)pc)->jump;
    if (failflag == 0) {
      JUMP(dst);
    }
    else {
      DISPATCH_NEXT;
    }
  }
  OP(REPCOND) {
    PegVMInstruction *dst = ((IREPCOND *)pc)->jump;
    if (cur != POP_SP()) {
      DISPATCH_NEXT;
    }
    else {
      JUMP(dst);
    }
  }
  OP(CHARRANGE) {
#ifdef PEGVM_USE_CHARRANGE
    char ch = *cur++;
    ICHARRANGE *inst = (ICHARRANGE *)pc;
    if (inst->c1 <= ch && ch <= inst->c2) {
      DISPATCH_NEXT;
    } else {
      --cur;
      failflag = 1;
      JUMP(inst->jump);
    }
#endif
  }
  OP(CHARSET) {
    ICHARSET *inst = (ICHARSET *)pc;
    if (bitset_get(inst->set, *cur++)) {
      DISPATCH_NEXT;
    }
    else {
      --cur;
      failflag = 1;
      JUMP(inst->jump);
    }
  }
  OP(STRING);
  OP(STRING1);
  OP(STRING2);
  OP(STRING4) {
    ISTRING *inst = (ISTRING *)pc;
    int next;
    if ((next = pegvm_string_equal(inst->chardata, cur)) > 0) {
      cur += next;
      DISPATCH_NEXT;
    }
    else {
      failflag = 1;
      JUMP(inst->jump);
    }
  }
  OP(ANY) {
    IANY *inst = (IANY *)pc;
    if (*cur++ != 0) {
      DISPATCH_NEXT;
    } else {
      --cur;
      failflag = 1;
      JUMP(inst->jump);
    }
  }
  OP(PUSHo) {
    ParsingObject po = P4D_newObject2(context, cur, MPOOL, LEFT);
    po->refc = 1;
    PUSH_OSP(po);
    DISPATCH_NEXT;
  }
  OP(PUSHconnect) {
    ParsingObject po = LEFT;
    LEFT->refc++;
    PUSH_OSP(po);
    PUSH_SP((const char *)(long)P4D_markLogStack(context)); // FIXME
    DISPATCH_NEXT;
  }
  OP(PUSHp1) {
    PUSH_SP(cur);
    DISPATCH_NEXT;
  }
  OP(LOADp1) {
    Reg1 = cur;
    DISPATCH_NEXT;
  }
  OP(LOADp2) {
    Reg2 = cur;
    DISPATCH_NEXT;
  }
  OP(LOADp3) {
    Reg3 = cur;
    DISPATCH_NEXT;
  }
  OP(STOREp) {
    cur = POP_SP();
    DISPATCH_NEXT;
  }
  OP(STOREp1) {
    cur = Reg1;
    DISPATCH_NEXT;
  }
  OP(STOREp2) {
    cur = Reg2;
    DISPATCH_NEXT;
  }
  OP(STOREp3) {
    cur = Reg3;
    DISPATCH_NEXT;
  }
  OP(POPp) {
    (void)POP_SP();
    DISPATCH_NEXT;
  }
  OP(POPo) {
    ParsingObject po = POP_OSP();
    P4D_setObject(context, &po, NULL);
    DISPATCH_NEXT;
  }
  OP(STOREo) {
    ParsingObject po = POP_OSP();
    P4D_setObject(context, &LEFT, po);
    DISPATCH_NEXT;
  }
  OP(STOREflag) {
    ISTOREflag *inst = (ISTOREflag *)pc;
    failflag = inst->val;
    DISPATCH_NEXT;
  }
  OP(NEW) {
    PUSH_SP((char *)(long)P4D_markLogStack(context)); // FIXME
    P4D_setObject(context, &LEFT, P4D_newObject(context, cur, MPOOL));
    // PUSH_SP(P4D_markLogStack(context));
    DISPATCH_NEXT;
  }
  OP(NEWJOIN) {
    INEWJOIN *inst = (INEWJOIN *)pc;
    ParsingObject po = NULL;
    P4D_setObject(context, &po, LEFT);
    P4D_setObject(context, &LEFT, P4D_newObject(context, cur, MPOOL));
    // PUSH_SP(P4D_markLogStack(context));
    P4D_lazyJoin(context, po, MPOOL);
    P4D_lazyLink(context, LEFT, inst->ndata, po, MPOOL);
    DISPATCH_NEXT;
  }
  OP(COMMIT) {
    ICOMMIT *inst = (ICOMMIT *)pc;
    P4D_commitLog(context, (int)(long)POP_SP(), LEFT, MPOOL);
    ParsingObject parent = (ParsingObject)POP_OSP();
    P4D_lazyLink(context, parent, inst->ndata, LEFT, MPOOL);
    P4D_setObject(context, &LEFT, parent);
    DISPATCH_NEXT;
  }
  OP(ABORT) {
    P4D_abortLog(context, (int)(long)POP_SP());
    DISPATCH_NEXT;
  }
  OP(LINK) {
    ILINK *inst = (ILINK *)pc;
    ParsingObject parent = (ParsingObject)POP_OSP();
    P4D_lazyLink(context, parent, inst->ndata, LEFT, MPOOL);
    // P4D_setObject(context, &LEFT, parent);
    PUSH_OSP(parent);
    DISPATCH_NEXT;
  }
  OP(SETendp) {
    LEFT->end_pos = cur - context->inputs;
    DISPATCH_NEXT;
  }
  OP(TAG) {
    ITAG *inst = (ITAG *)pc;
    LEFT->tag = (const char *)&inst->chardata->text;
    DISPATCH_NEXT;
  }
  OP(VALUE) {
    ITAG *inst = (ITAG *)pc;
    LEFT->value = (const char *)&inst->chardata->text;
    DISPATCH_NEXT;
  }
  OP(MAPPEDCHOICE) {
    IMAPPEDCHOICE *inst = (IMAPPEDCHOICE *)pc;
#ifdef PEGVM_USE_MAPPEDCHOISE_DIRECT_JMP
    JUMP(inst->ndata->table[(unsigned char)*cur]);
#else
    JUMP_REL(inst->ndata->table[(unsigned char)*cur]);
#endif
  }
  OP(MAPPEDCHOICE_8) {
#ifndef PEGVM_USE_MAPPEDCHOISE_DIRECT_JMP
    IMAPPEDCHOICE_8 *inst = (IMAPPEDCHOICE_8 *)pc;
    unsigned char ch = (unsigned char) *cur;
    char_table_t table = inst->ndata;
    JUMP_REL(table->table[ch]);
#endif
  }
  OP(MAPPEDCHOICE_16) {
#ifndef PEGVM_USE_MAPPEDCHOISE_DIRECT_JMP
    IMAPPEDCHOICE_16 *inst = (IMAPPEDCHOICE_16 *)pc;
    JUMP_REL(inst->ndata->table[(unsigned char)*cur]);
#endif
  }

  OP(SCAN) {
    ISCAN *inst = (ISCAN *)pc;
    const char *start = POP_SP();
    char *s = (char *)start;
    char *e = (char *)cur;
    long num = strtol(s, &e, inst->cardinals);
    context->repeat_table[inst->offset] = (int)num;
    DISPATCH_NEXT;
  }
  OP(CHECKEND) {
    ICHECKEND *inst = (ICHECKEND *)pc;
    if (context->repeat_table[inst->ndata] == 0) {
      DISPATCH_NEXT;
    }
    JUMP(inst->jump);
  }
  OP(DEF) {
    IDEF *inst = (IDEF *)pc;
    const char *start = POP_SP();
    int len = cur - start;
    char *value = malloc(len);
    memcpy(value, start, len);
    pushSymbolTable(context, inst->ndata, len, value);
    DISPATCH_NEXT;
  }
  OP(IS) {
    IIS *inst = (IIS *)pc;
    long offset = matchSymbolTableTop(context, cur, inst->ndata);
    failflag = offset >= 0;
    if (offset) {
        cur += offset;
    }
    DISPATCH_NEXT;
  }
  OP(ISA) {
    IISA *inst = (IISA *)pc;
    long offset = matchSymbolTable(context, cur, inst->ndata);
    failflag = offset >= 0;
    if (offset) {
        cur += offset;
    }
    DISPATCH_NEXT;
  }
  OP(BLOCKSTART) {
    IBLOCKSTART *inst = (IBLOCKSTART *)pc;
    long len;
    PUSH_SP((const char *)(long)context->stateValue); // FIXME
    char *value = getIndentText(context, cur, &len);
    PUSH_SP((const char *)(long)pushSymbolTable(context, inst->ndata, (int)len, value)); // FIXME
    DISPATCH_NEXT;
  }
  OP(BLOCKEND) {
    popSymbolTable(context, (int)(long)POP_SP());
    context->stateValue = (int)(long)POP_SP();
    DISPATCH_NEXT;
  }
  OP(INDENT) {
    IINDENT *inst = (IINDENT *)pc;
    long offset = matchSymbolTableTop(context, cur, inst->ndata);
    if (offset >= 0) {
        cur += offset;
    }
    DISPATCH_NEXT;
  }
  OP(NOTBYTE) {
    INOTBYTE *inst = (INOTBYTE *)pc;
    if (*cur == inst->cdata) {
      failflag = 1;
      JUMP(inst->jump);
    }
    DISPATCH_NEXT;
  }
  OP(NOTANY) {
    INOTANY *inst = (INOTANY *)pc;
    if (*cur != 0) {
      failflag = 1;
      JUMP(inst->jump);
    }
    DISPATCH_NEXT;
  }
  OP(NOTCHARSET) {
    INOTCHARSET *inst = (INOTCHARSET *)pc;
    if (bitset_get(inst->set, *cur)) {
      failflag = 1;
      JUMP(inst->jump);
    }
    DISPATCH_NEXT;
  }
  OP(NOTBYTERANGE) {
#ifdef PEGVM_USE_CHARRANGE
    INOTBYTERANGE *inst = (INOTBYTERANGE *)pc;
    if (!(*cur >= inst->c1 &&
          *cur <= inst->c2)) {
      DISPATCH_NEXT;
    }
    else {
      failflag = 1;
      JUMP(inst->jump);
    }
#endif
  }
  OP(NOTSTRING) {
    INOTSTRING *inst = (INOTSTRING *)pc;
    if (pegvm_string_equal(inst->cdata, cur) > 0) {
      failflag = 1;
      JUMP(inst->jump);
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALBYTE) {
    IOPTIONALBYTE *inst = (IOPTIONALBYTE *)pc;
    if (*cur == inst->cdata) {
      ++cur;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALCHARSET) {
    IOPTIONALCHARSET *inst = (IOPTIONALCHARSET *)pc;
    if (bitset_get(inst->set, *cur)) {
      ++cur;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALBYTERANGE) {
#ifdef PEGVM_USE_CHARRANGE
    IOPTIONALBYTERANGE *inst = (IOPTIONALBYTERANGE *)pc;
    if (*cur >= inst->c1 &&
        *cur <= inst->c2) {
      ++cur;
    }
    DISPATCH_NEXT;
#endif
  }

  OP(OPTIONALSTRING) {
    IOPTIONALSTRING *inst = (IOPTIONALSTRING *)pc;
    cur += pegvm_string_equal(inst->cdata, cur);
    DISPATCH_NEXT;
  }
  OP(ZEROMOREBYTERANGE) {
#ifdef PEGVM_USE_CHARRANGE
    IZEROMOREBYTERANGE *inst = (IZEROMOREBYTERANGE *)pc;
    while (1) {
      if (!(*cur >= inst->c1 &&
            *cur <= inst->c2)) {
        DISPATCH_NEXT;
      }
      ++cur;
    }
#endif
    DISPATCH_NEXT; // FIXME
  }
  OP(ZEROMORECHARSET) {
    IZEROMORECHARSET *inst = (IZEROMORECHARSET *)pc;
L_head:;
    if (bitset_get(inst->set, *cur)) {
      cur++;
      goto L_head;
    }
    DISPATCH_NEXT;
  }
  OP(ZEROMOREWS) {
    char c;
L_head2:
    c = *cur;
    if (c == 32 || c == 9 || c == 10 || c == 13) {
      ++cur;
      goto L_head2;
    }
    DISPATCH_NEXT;
  }
  OP(REPEATANY) {
    IREPEATANY *inst = (IREPEATANY *)pc;
    const char *end = context->inputs + context->input_size;
    if (cur + context->repeat_table[inst->ndata] < end) {
        cur += context->repeat_table[inst->ndata];
    }
    DISPATCH_NEXT;
  }

  return failflag;
}

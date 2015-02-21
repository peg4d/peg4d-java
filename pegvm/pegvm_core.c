/* pegvm.c */

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

#define DISPATCH_NEXT goto *(++pc)->ptr
#define JUMP          goto *(pc = (pc)->jump)->ptr
#define RET           goto *(pc = *POP_IP())->ptr

#define OP(OP) PEGVM_OP_##OP:
long nez_VM_Execute(ParsingContext context, PegVMInstruction *inst) {
  static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
    PEGVM_OP_EACH(DEFINE_TABLE)
#undef DEFINE_TABLE
  };

  int failflag = 0;
  ParsingObject left = context->left;
  long pos = context->pos;
  const char *inputs = context->inputs;
#define MPOOL context->mpool
  // MemoryPool pool = context->mpool;

  long Reg1 = 0;
  long Reg2 = 0;
  long Reg3 = 0;
  PegVMInstruction *pc;
  ParsingObject *osp;
  PegVMInstruction **cp;
  long *sp;
  pc = inst + context->startPoint;
  sp = context->stack_pointer;
  osp = context->object_stack_pointer;
  cp = context->call_stack_pointer;

  if (inst == NULL) {
    return (long)table;
  }

  PUSH_IP(inst);
  P4D_setObject(context, &left, P4D_newObject(context, context->pos, MPOOL));

  goto *(pc)->ptr;

  OP(EXIT) {
    P4D_commitLog(context, 0, left, MPOOL);
    context->left = left;
    context->pos = pos;
    return failflag;
  }
  OP(JUMP) {
      JUMP;
  }
  OP(CALL) {
    PUSH_IP(pc + 1);
    JUMP;
  }
  OP(RET) {
      RET;
  }
  OP(CONDBRANCH) {
    if (failflag == *pc->ndata) {
      JUMP;
    }
    else {
      DISPATCH_NEXT;
    }
  }
  OP(REPCOND) {
    if (pos != POP_SP()) {
      DISPATCH_NEXT;
    }
    else {
      JUMP;
    }
  }
  OP(CHARRANGE) {
    char ch = inputs[pos];
    if ((pc)->chardata[0] <= ch && ch <= (pc)->chardata[1]) {
      pos++;
      DISPATCH_NEXT;
    } else {
      failflag = 1;
      JUMP;
    }
  }
  OP(CHARSET) {
    int j = 0;
    int len = *(pc)->ndata;
    while (j < len) {
      if (inputs[pos] == (pc)->chardata[j]) {
        pos++;
        DISPATCH_NEXT;
      }
      j++;
    }
    failflag = 1;
    JUMP;
  }
  OP(STRING) {
    char *p = pc->chardata;
    int len = *pc->ndata;
    char *pend = pc->chardata + len;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        failflag = 1;
        JUMP;
      }
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(ANY) {
    if (unlikely(inputs[pos++] == 0)) {
      pos--;
      failflag = 1;
      JUMP;
    }
    DISPATCH_NEXT;
  }
  OP(PUSHo) {
    ParsingObject po = P4D_newObject(context, pos, MPOOL);
    *po = *left;
    po->refc = 1;
    PUSH_OSP(po);
    DISPATCH_NEXT;
  }
  OP(PUSHconnect) {
    ParsingObject po = left;
    left->refc++;
    PUSH_OSP(po);
    PUSH_SP(P4D_markLogStack(context));
    DISPATCH_NEXT;
  }
  OP(PUSHp1) {
    PUSH_SP(pos);
    DISPATCH_NEXT;
  }
  OP(LOADp1) {
    Reg1 = pos;
    DISPATCH_NEXT;
  }
  OP(LOADp2) {
    Reg2 = pos;
    DISPATCH_NEXT;
  }
  OP(LOADp3) {
    Reg3 = pos;
    DISPATCH_NEXT;
  }
  OP(STOREp) {
    pos = POP_SP();
    DISPATCH_NEXT;
  }
  OP(STOREp1) {
    pos = Reg1;
    DISPATCH_NEXT;
  }
  OP(STOREp2) {
    pos = Reg2;
    DISPATCH_NEXT;
  }
  OP(STOREp3) {
    pos = Reg3;
    DISPATCH_NEXT;
  }
  OP(POPp) {
    POP_SP();
    DISPATCH_NEXT;
  }
  OP(POPo) {
    ParsingObject po = POP_OSP();
    P4D_setObject(context, &po, NULL);
    DISPATCH_NEXT;
  }
  OP(STOREo) {
    ParsingObject po = POP_OSP();
    P4D_setObject(context, &left, po);
    DISPATCH_NEXT;
  }
  OP(STOREflag) {
    failflag = pc->ndata[0];
    DISPATCH_NEXT;
  }
  OP(NEW) {
    PUSH_SP(P4D_markLogStack(context));
    P4D_setObject(context, &left, P4D_newObject(context, pos, MPOOL));
    // PUSH_SP(P4D_markLogStack(context));
    DISPATCH_NEXT;
  }
  OP(NEWJOIN) {
    ParsingObject po = NULL;
    P4D_setObject(context, &po, left);
    P4D_setObject(context, &left, P4D_newObject(context, pos, MPOOL));
    // PUSH_SP(P4D_markLogStack(context));
    P4D_lazyJoin(context, po, MPOOL);
    P4D_lazyLink(context, left, *(pc)->ndata, po, MPOOL);
    DISPATCH_NEXT;
  }
  OP(COMMIT) {
    P4D_commitLog(context, (int)POP_SP(), left, MPOOL);
    ParsingObject parent = (ParsingObject)POP_OSP();
    P4D_lazyLink(context, parent, *(pc)->ndata, left, MPOOL);
    P4D_setObject(context, &left, parent);
    DISPATCH_NEXT;
  }
  OP(ABORT) {
    P4D_abortLog(context, (int)POP_SP());
    DISPATCH_NEXT;
  }
  OP(LINK) {
    ParsingObject parent = (ParsingObject)POP_OSP();
    P4D_lazyLink(context, parent, *(pc)->ndata, left, MPOOL);
    // P4D_setObject(context, &left, parent);
    PUSH_OSP(parent);
    DISPATCH_NEXT;
  }
  OP(SETendp) {
    left->end_pos = pos;
    DISPATCH_NEXT;
  }
  OP(TAG) {
    left->tag = (pc)->chardata;
    DISPATCH_NEXT;
  }
  OP(VALUE) {
    left->value = (pc)->chardata;
    DISPATCH_NEXT;
  }
  OP(MAPPEDCHOICE) {
    pc = inst + (pc)->ndata[(int)inputs[pos]];
    goto *(pc)->ptr;
  }
  OP(SCAN) {
    long start = POP_SP();
    long len = pos - start;
    char value[len];
    int j = 0;
    for (long i = start; i < pos; i++) {
      value[j] = inputs[i];
      j++;
    }
    if (pc->ndata[0] == 16) {
      long num = strtol(value, NULL, 16);
      context->repeat_table[pc->ndata[1]] = (int)num;
      DISPATCH_NEXT;
    }
    context->repeat_table[pc->ndata[1]] = atoi(value);
    DISPATCH_NEXT;
  }
  OP(CHECKEND) {
    if (context->repeat_table[pc->ndata[0]] == 0) {
      DISPATCH_NEXT;
    }
    JUMP;
  }
  OP(DEF) {
    long start = POP_SP();
    int len = (int)(pos - start);
    char *value = malloc(len);
    int j = 0;
    for (long i = start; i < pos; i++) {
      value[j] = inputs[i];
      j++;
    }
    pushSymbolTable(context, pc->ndata[0], len, value);
    DISPATCH_NEXT;
  }
  OP(IS) {
    failflag = matchSymbolTableTop(context, &pos, pc->ndata[0]);
    DISPATCH_NEXT;
  }
  OP(ISA) {
    failflag = matchSymbolTable(context, &pos, pc->ndata[0]);
    DISPATCH_NEXT;
  }
  OP(BLOCKSTART) {
    long len;
    PUSH_SP(context->stateValue);
    char *value = getIndentText(context, inputs, pos, &len);
    PUSH_SP(pushSymbolTable(context, pc->ndata[0], (int)len, value));
    DISPATCH_NEXT;
  }
  OP(BLOCKEND) {
    popSymbolTable(context, (int)POP_SP());
    context->stateValue = (int)POP_SP();
    DISPATCH_NEXT;
  }
  OP(INDENT) {
    matchSymbolTableTop(context, &pos, pc->ndata[0]);
    DISPATCH_NEXT;
  }
  OP(NOTBYTE) {
    if (inputs[pos] != *(pc)->ndata) {
      DISPATCH_NEXT;
    }
    failflag = 1;
    JUMP;
  }
  OP(NOTANY) {
    if (inputs[pos] != 0) {
      failflag = 1;
      JUMP;
    }
    DISPATCH_NEXT;
  }
  OP(NOTCHARSET) {
    int j = 0;
    int len = *(pc)->ndata;
    while (j < len) {
      if (inputs[pos] == (pc)->chardata[j]) {
        failflag = 1;
        JUMP;
      }
      j++;
    }
    DISPATCH_NEXT;
  }
  OP(NOTBYTERANGE) {
    if (!(inputs[pos] >= (pc)->chardata[0] &&
          inputs[pos] <= (pc)->chardata[1])) {
      DISPATCH_NEXT;
    }
    failflag = 1;
    JUMP;
  }
  OP(NOTSTRING) {
    char *p = pc->chardata;
    int len = *pc->ndata;
    long backtrack_pos = pos;
    char *pend = pc->chardata + len;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        pos = backtrack_pos;
        DISPATCH_NEXT;
      }
      pos++;
    }
    pos = backtrack_pos;
    failflag = 1;
    JUMP;
  }
  OP(OPTIONALBYTE) {
    if (inputs[pos] == *(pc)->ndata) {
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALCHARSET) {
    int j = 0;
    int len = *(pc)->ndata;
    while (j < len) {
      if (inputs[pos] == (pc)->chardata[j]) {
        pos++;
        DISPATCH_NEXT;
      }
      j++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALBYTERANGE) {
    if (inputs[pos] >= (pc)->chardata[0] && inputs[pos] <= (pc)->chardata[1]) {
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALSTRING) {
    char *p = pc->chardata;
    int len = *pc->ndata;
    long backtrack_pos = pos;
    char *pend = pc->chardata + len;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        pos = backtrack_pos;
        DISPATCH_NEXT;
      }
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(ZEROMOREBYTERANGE) {
    while (1) {
      if (!(inputs[pos] >= (pc)->chardata[0] &&
            inputs[pos] <= (pc)->chardata[1])) {
        DISPATCH_NEXT;
      }
      pos++;
    }
  }
  OP(ZEROMORECHARSET) {
    int j = 0;
    int len = *(pc)->ndata;
    while (j < len) {
      if (inputs[pos] == (pc)->chardata[j]) {
        pos++;
        j = 0;
      } else {
        j++;
      }
    }
    DISPATCH_NEXT;
  }
  OP(ZEROMOREWS) {
    while (1) {
      char c = inputs[pos];
      if (c == 32 || c == 9 || c == 10 || c == 13) {
        pos++;
      } else {
        break;
      }
    }
    DISPATCH_NEXT;
  }
  OP(REPEATANY) {
    long back = pos;
    pos = pos + context->repeat_table[pc->ndata[0]];
    if (pos - 1 > (long)context->input_size) {
      pos = back;
    }
    DISPATCH_NEXT;
  }

  return failflag;
}

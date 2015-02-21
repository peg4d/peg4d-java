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

#define GET_ADDR(PC) ((PegVMInstructionBase *)(PC))->addr
#define DISPATCH_NEXT goto *GET_ADDR(++pc)
#define JUMP(dst)     goto *GET_ADDR(pc += dst)
#define RET           goto *GET_ADDR(pc = *POP_IP())

#define OP(OP) PEGVM_OP_##OP:
long nez_VM_Execute(ParsingContext context, PegVMInstruction *inst) {
  static const void *table[] = {
#define DEFINE_TABLE(NAME) &&PEGVM_OP_##NAME,
    LIR_EACH(DEFINE_TABLE)
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

  goto *GET_ADDR(pc);

  OP(EXIT) {
    P4D_commitLog(context, 0, left, MPOOL);
    context->left = left;
    context->pos = pos;
    return failflag;
  }
  OP(JUMP) {
    long dst = ((IJUMP *)pc)->jump;
    JUMP(dst);
  }
  OP(CALL) {
    long dst = ((ICALL *)pc)->jump;
    PUSH_IP(pc + 1);
    JUMP(dst);
  }
  OP(RET) {
    RET;
  }
  OP(CONDBRANCH) {
    long dst = ((ICONDBRANCH *)pc)->jump;
    if (failflag == ((ICONDBRANCH *)pc)->val) {
      JUMP(dst);
    }
    else {
      DISPATCH_NEXT;
    }
  }

  OP(CONDTRUE) {
    long dst = ((ICONDBRANCH *)pc)->jump;
    if (failflag == 1) {
      JUMP(dst);
    }
    else {
      DISPATCH_NEXT;
    }
  }
  OP(CONDFALSE) {
    long dst = ((ICONDBRANCH *)pc)->jump;
    if (failflag == 0) {
      JUMP(dst);
    }
    else {
      DISPATCH_NEXT;
    }
  }
  OP(REPCOND) {
    long dst = ((IREPCOND *)pc)->jump;
    if (pos != POP_SP()) {
      DISPATCH_NEXT;
    }
    else {
      JUMP(dst);
    }
  }
  OP(CHARRANGE) {
    char ch = inputs[pos++];
    ICHARRANGE *inst = (ICHARRANGE *)pc;
    if (inst->cdata.c1 <= ch && ch <= inst->cdata.c2) {
      DISPATCH_NEXT;
    } else {
      long dst = inst->jump;
      pos--;
      failflag = 1;
      JUMP(dst);
    }
  }
  OP(CHARSET) {
    ICHARSET *inst = (ICHARSET *)pc;
    unsigned c = inputs[pos];
    if (inst->set->table[c / 8] & (1 << (c % 8))) {
      pos++;
      DISPATCH_NEXT;
    }
    failflag = 1;
    JUMP(inst->jump);
  }
  OP(STRING);
  OP(STRING1);
  OP(STRING2);
  OP(STRING4) {
    ISTRING *inst = (ISTRING *)pc;
    string_ptr_t str = inst->chardata;
    char *p = &str->text[0];
    char *pend = &str->text[0] + str->len;
    while (p < pend) {
      if (inputs[pos++] != *p++) {
        --pos;
        failflag = 1;
        JUMP(inst->jump);
      }
    }
    DISPATCH_NEXT;
  }
  OP(ANY) {
      IANY *inst = (IANY *)pc;
    if (unlikely(inputs[pos++] == 0)) {
      pos--;
      failflag = 1;
      JUMP(inst->jump);
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
    ISTOREflag *inst = (ISTOREflag *)pc;
    failflag = inst->val;
    DISPATCH_NEXT;
  }
  OP(NEW) {
    PUSH_SP(P4D_markLogStack(context));
    P4D_setObject(context, &left, P4D_newObject(context, pos, MPOOL));
    // PUSH_SP(P4D_markLogStack(context));
    DISPATCH_NEXT;
  }
  OP(NEWJOIN) {
    INEWJOIN *inst = (INEWJOIN *)pc;
    ParsingObject po = NULL;
    P4D_setObject(context, &po, left);
    P4D_setObject(context, &left, P4D_newObject(context, pos, MPOOL));
    // PUSH_SP(P4D_markLogStack(context));
    P4D_lazyJoin(context, po, MPOOL);
    P4D_lazyLink(context, left, inst->ndata, po, MPOOL);
    DISPATCH_NEXT;
  }
  OP(COMMIT) {
    ICOMMIT *inst = (ICOMMIT *)pc;
    P4D_commitLog(context, (int)POP_SP(), left, MPOOL);
    ParsingObject parent = (ParsingObject)POP_OSP();
    P4D_lazyLink(context, parent, inst->ndata, left, MPOOL);
    P4D_setObject(context, &left, parent);
    DISPATCH_NEXT;
  }
  OP(ABORT) {
    P4D_abortLog(context, (int)POP_SP());
    DISPATCH_NEXT;
  }
  OP(LINK) {
    //ILINK *inst = (ILINK *)pc;
    //ParsingObject parent = (ParsingObject)POP_OSP();
    //P4D_lazyLink(context, parent, inst->ndata, left, MPOOL);
    //// P4D_setObject(context, &left, parent);
    //PUSH_OSP(parent);
    //DISPATCH_NEXT;
  }
  OP(SETendp) {
    left->end_pos = pos;
    DISPATCH_NEXT;
  }
  OP(TAG) {
    ITAG *inst = (ITAG *)pc;
    left->tag = (const char *)&inst->chardata->text;
    DISPATCH_NEXT;
  }
  OP(VALUE) {
    ITAG *inst = (ITAG *)pc;
    left->value = (const char *)&inst->chardata->text;
    DISPATCH_NEXT;
  }
  OP(MAPPEDCHOICE) {
    IMAPPEDCHOICE *inst = (IMAPPEDCHOICE *)pc;
    JUMP(inst->ndata->table[(int)inputs[pos]]);
  }
  OP(MAPPEDCHOICE_16) {
    IMAPPEDCHOICE_16 *inst = (IMAPPEDCHOICE_16 *)pc;
    JUMP(inst->ndata->table[(int)inputs[pos]]);
  }
  OP(MAPPEDCHOICE_8) {
    IMAPPEDCHOICE_8 *inst = (IMAPPEDCHOICE_8 *)pc;
    JUMP(inst->ndata->table[(int)inputs[pos]]);
  }

  OP(SCAN) {
    ISCAN *inst = (ISCAN *)pc;
    long start = POP_SP();
    char *s = (char *)inputs + start;
    char *e = (char *)&inputs[pos];
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
    long start = POP_SP();
    int len = pos - start;
    char *value = malloc(len);
    memcpy(value, inputs + start, len);
    pushSymbolTable(context, inst->ndata, len, value);
    DISPATCH_NEXT;
  }
  OP(IS) {
    IIS *inst = (IIS *)pc;
    long offset = matchSymbolTableTop(context, pos, inst->ndata);
    failflag = offset >= 0;
    if (offset) {
        pos += offset;
    }
    DISPATCH_NEXT;
  }
  OP(ISA) {
    IISA *inst = (IISA *)pc;
    long offset = matchSymbolTable(context, pos, inst->ndata);
    failflag = offset >= 0;
    if (offset) {
        pos += offset;
    }
    DISPATCH_NEXT;
  }
  OP(BLOCKSTART) {
    IBLOCKSTART *inst = (IBLOCKSTART *)pc;
    long len;
    PUSH_SP(context->stateValue);
    char *value = getIndentText(context, inputs, pos, &len);
    PUSH_SP(pushSymbolTable(context, inst->ndata, (int)len, value));
    DISPATCH_NEXT;
  }
  OP(BLOCKEND) {
    popSymbolTable(context, (int)POP_SP());
    context->stateValue = (int)POP_SP();
    DISPATCH_NEXT;
  }
  OP(INDENT) {
    IINDENT *inst = (IINDENT *)pc;
    long offset = matchSymbolTableTop(context, pos, inst->ndata);
    if (offset >= 0) {
        pos += offset;
    }
    DISPATCH_NEXT;
  }
  OP(NOTBYTE) {
    INOTBYTE *inst = (INOTBYTE *)pc;
    if (inputs[pos] != inst->cdata) {
      DISPATCH_NEXT;
    }
    failflag = 1;
    JUMP(inst->jump);
  }
  OP(NOTANY) {
    INOTANY *inst = (INOTANY *)pc;
    if (inputs[pos] != 0) {
      failflag = 1;
      JUMP(inst->jump);
    }
    DISPATCH_NEXT;
  }
  OP(NOTCHARSET) {
    INOTCHARSET *inst = (INOTCHARSET *)pc;
    unsigned c = inputs[pos];
    if (inst->set->table[c / 8] & (1 << (c % 8))) {
      failflag = 1;
      JUMP(inst->jump);
    }
    DISPATCH_NEXT;
  }
  OP(NOTBYTERANGE) {
    INOTBYTERANGE *inst = (INOTBYTERANGE *)pc;
    if (!(inputs[pos] >= inst->cdata.c1 &&
          inputs[pos] <= inst->cdata.c2)) {
      DISPATCH_NEXT;
    }
    else {
      failflag = 1;
      JUMP(inst->jump);
    }
  }
  OP(NOTSTRING) {
    INOTSTRING *inst = (INOTSTRING *)pc;
    string_ptr_t str = inst->cdata;
    char *p = &str->text[0];
    char *pend = &str->text[0] + str->len;
    long backtrack_pos = pos;
    while (p < pend) {
      if (inputs[pos] != *p++) {
        pos = backtrack_pos;
        DISPATCH_NEXT;
      }
      pos++;
    }
    pos = backtrack_pos;
    failflag = 1;
    JUMP(inst->jump);
  }
  OP(OPTIONALBYTE) {
    IOPTIONALBYTE *inst = (IOPTIONALBYTE *)pc;
    if (inputs[pos] == inst->cdata) {
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALCHARSET) {
    IOPTIONALCHARSET *inst = (IOPTIONALCHARSET *)pc;
    unsigned c = inputs[pos];
    if (inst->set->table[c / 8] & (1 << (c % 8))) {
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALBYTERANGE) {
    IOPTIONALBYTERANGE *inst = (IOPTIONALBYTERANGE *)pc;
    if (inputs[pos] >= inst->cdata.c1 &&
        inputs[pos] <= inst->cdata.c2) {
      pos++;
    }
    DISPATCH_NEXT;
  }
  OP(OPTIONALSTRING) {
    IOPTIONALSTRING *inst = (IOPTIONALSTRING *)pc;
    string_ptr_t str = inst->cdata;
    char *p = &str->text[0];
    char *pend = &str->text[0] + str->len;
    long backtrack_pos = pos;
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
    IZEROMOREBYTERANGE *inst = (IZEROMOREBYTERANGE *)pc;
    while (1) {
      if (!(inputs[pos] >= inst->cdata.c1 &&
            inputs[pos] <= inst->cdata.c2)) {
        DISPATCH_NEXT;
      }
      pos++;
    }
    DISPATCH_NEXT; // FIXME
  }
  OP(ZEROMORECHARSET) {
    IZEROMORECHARSET *inst = (IZEROMORECHARSET *)pc;
    unsigned c;
L_head:;
    c = inputs[pos];
    if (inst->set->table[c / 8] & (1 << (c % 8))) {
      pos++;
      goto L_head;
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
    IREPEATANY *inst = (IREPEATANY *)pc;
    long back = pos;
    pos = pos + context->repeat_table[inst->ndata];
    if (pos - 1 > (long)context->input_size) {
      pos = back;
    }
    DISPATCH_NEXT;
  }

  return failflag;
}

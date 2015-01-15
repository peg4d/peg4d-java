#include <stdint.h>

#ifndef PEGVM_H
#define PEGVM_H
#define PEGVM_DEBUG 0
#define PEGVM_PROFILE 0
#define PEGVM_OP_MAX 46
#define PEGVM_PROFILE_MAX 46

typedef struct byteCodeInfo {
  int pos;
  uint8_t version0;
  uint8_t version1;
  uint32_t filename_length;
  uint8_t *filename;
  uint32_t pool_size_info;
  uint64_t bytecode_length;
} byteCodeInfo;

typedef struct Instruction {
  union {
    long opcode;
    const void *ptr;
  };
  //  union {
  int *ndata;
  char *chardata;
  struct Instruction *jump;
  //  };
} PegVMInstruction, Instruction;

#define PEGVM_OP_EACH(OP)                                                      \
  OP(EXIT) OP(JUMP) OP(CALL) OP(RET) OP(CONDBRANCH) OP(REPCOND) OP(CHARRANGE)  \
      OP(CHARSET) OP(STRING) OP(ANY) OP(PUSHo) OP(PUSHconnect) OP(PUSHp1)      \
      OP(PUSHp2) OP(POPp) OP(POPo) OP(STOREo) OP(STOREp) OP(STOREflag) OP(NEW) \
      OP(NEWJOIN) OP(COMMIT) OP(ABORT) OP(LINK) OP(SETendp) OP(TAG) OP(VALUE)  \
      OP(MAPPEDCHOICE) OP(SCAN) OP(CHECKEND) OP(NOTBYTE) OP(NOTANY)            \
      OP(NOTCHARSET) OP(NOTBYTERANGE) OP(NOTSTRING) OP(ANDBYTE) OP(ANDCHARSET) \
      OP(ANDBYTERANGE) OP(ANDSTRING) OP(OPTIONALBYTE) OP(OPTIONALCHARSET)      \
      OP(OPTIONALBYTERANGE) OP(OPTIONALSTRING) OP(ZEROMOREBYTERANGE)           \
      OP(ZEROMORECHARSET) OP(REPEATANY)
// OP(DTABLE)

enum pegvm_opcode {
#define DEFINE_ENUM(NAME) PEGVM_OP_##NAME,
  PEGVM_OP_EACH(DEFINE_ENUM)
#undef DEFINE_ENUM
  PEGVM_OP_ERROR = -1
};

#define PEGVM_PROFILE_json_EACH(RULE)                                        \
  RULE(export) RULE(S) RULE(File) RULE(Chunk) RULE(JSONObject) RULE(String)  \
      RULE(Member) RULE(Value) RULE(ObjectId) RULE(ID) RULE(Array) RULE(INT) \
      RULE(Number) RULE(True) RULE(False) RULE(Null) RULE(NAMESEP)           \
      RULE(VALUESEP) RULE(DIGIT) RULE(FRAC) RULE(EXP)

#define PEGVM_json_RULE_MAX 21

enum pegvm_json_rule {
#define DEFINE_json_ENUM(NAME) PEGVM_PROFILE_json_##NAME,
  PEGVM_PROFILE_json_EACH(DEFINE_json_ENUM)
#undef DEFINE_json_ENUM
  PROFILE_json_ERROR = -1
};

#define PEGVM_PROFILE_xml_EACH(RULE)                                 \
  RULE(export) RULE(S) RULE(File) RULE(Chunk) RULE(PROLOG) RULE(DTD) \
      RULE(Name) RULE(COMMENT) RULE(NAME) RULE(Xml) RULE(String)     \
      RULE(Attribute) RULE(Content) RULE(CDataSec) RULE(CDATA) RULE(Text)

#define PEGVM_xml_RULE_MAX 16

enum pegvm_xml_rule {
#define DEFINE_xml_ENUM(NAME) PEGVM_PROFILE_xml_##NAME,
  PEGVM_PROFILE_xml_EACH(DEFINE_xml_ENUM)
#undef DEFINE_xml_ENUM
  PROFILE_xml_ERROR = -1
};

#define PEGVM_PROFILE_c99_EACH(RULE)                                           \
  RULE(export) RULE(EOT) RULE(File) RULE(Chunk) RULE(S) RULE(_)                \
      RULE(BLOCKCOMMENT) RULE(LINECOMMENT) RULE(W) RULE(ATTRIBUTE) RULE(NAME)  \
      RULE(ATTRIBUTECONTENT) RULE(EOL) RULE(DIGIT) RULE(UCHAR) RULE(HEX)       \
      RULE(HEX4) RULE(Name) RULE(KEYWORD) RULE(TopLevel) RULE(Declaration)     \
      RULE(Directive) RULE(FunctionDeclaration) RULE(Annotation)               \
      RULE(AnnotationList) RULE(FunctionParamList) RULE(FunctionParam)         \
      RULE(TypeDef) RULE(VariableDeclaration) RULE(InitDecl)                   \
      RULE(InitDeclAssign) RULE(VarName) RULE(Initializer)                     \
      RULE(addInitializerList) RULE(Designation) RULE(Designator) RULE(Type)   \
      RULE(TypeSuffix) RULE(addFuncType) RULE(POINTER_QUALIFIER)               \
      RULE(PrimaryType) RULE(StructDeclaration) RULE(SIGN) RULE(NAME_T)        \
      RULE(addStructMember) RULE(StructMemberDeclaration)                      \
      RULE(StructMemberName) RULE(EnumeratorList) RULE(Enumerator) RULE(Block) \
      RULE(Statement) RULE(Expression) RULE(AssignmentExpression)              \
      RULE(addAssignmentOperator) RULE(ConstantExpression)                     \
      RULE(ConditionalExpression) RULE(LogicalORExpression)                    \
      RULE(LogicalANDExpression) RULE(InclusiveORExpression)                   \
      RULE(ExclusiveORExpression) RULE(ANDExpression) RULE(EqualityExpression) \
      RULE(RelationalExpression) RULE(ShiftExpression)                         \
      RULE(AdditiveExpression) RULE(MultiplicativeExpression)                  \
      RULE(UnaryExpression) RULE(CastExpression) RULE(PostfixExpression)       \
      RULE(addFunctionCall) RULE(addArgumentExpressionList) RULE(addIndex)     \
      RULE(addField) RULE(addPointerField) RULE(addInc) RULE(addDec)           \
      RULE(PrimaryExpression) RULE(Constant) RULE(CFloat) RULE(DECIMAL_FLOAT)  \
      RULE(FRACTION) RULE(EXPONENT) RULE(HEX_FLOAT) RULE(HEX_PREFIX)           \
      RULE(HEX_FRACTION) RULE(BINARY_EXPONENT) RULE(FLOAT_SUFFIX)              \
      RULE(CInteger) RULE(DECIMAL) RULE(HEXICAL) RULE(OCTAL) RULE(INT_SUFFIX)  \
      RULE(LONG_SUFFIX) RULE(CString) RULE(CChar) RULE(STRING_CONTENT)         \
      RULE(CHAR_CONTENT) RULE(ESCAPE) RULE(SIMPLE_ESCAPE) RULE(OCTAL_ESCAPE)   \
      RULE(HEX_ESCAPE)

#define PEGVM_c99_RULE_MAX 101

enum pegvm_c99_rule {
#define DEFINE_c99_ENUM(NAME) PEGVM_PROFILE_c99_##NAME,
  PEGVM_PROFILE_c99_EACH(DEFINE_c99_ENUM)
#undef DEFINE_c99_ENUM
  PROFILE_c99_ERROR = -1
};

PegVMInstruction *loadByteCodeFile(ParsingContext context,
                                   PegVMInstruction *inst,
                                   const char *fileName);
int ParserContext_Execute(ParsingContext context, PegVMInstruction *inst);
extern long PegVM_Execute(ParsingContext context, Instruction *inst,
                          MemoryPool pool);
extern Instruction *PegVM_Prepare(ParsingContext context, Instruction *inst,
                                  MemoryPool pool);
#endif

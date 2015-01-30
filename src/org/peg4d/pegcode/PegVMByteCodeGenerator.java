package org.peg4d.pegcode;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import nez.util.UList;

import org.peg4d.Grammar;
import org.peg4d.Main;
import org.peg4d.ParsingRule;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingApply;
import org.peg4d.expression.ParsingAssert;
import org.peg4d.expression.ParsingBlock;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingCatch;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingDef;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIs;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepeat;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingScan;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;
import org.peg4d.vm.Instruction;
import org.peg4d.vm.Opcode;

public class PegVMByteCodeGenerator extends GrammarGenerator {
	
	int codeIndex = 0;
	boolean backTrackFlag = false;
	boolean optChoiceMode = true;
	boolean optNonTerminalMode = true;
	int optimizationLevel = 0;
	int optimizationCount = 0;
	int choiceCaseCount = 0;
	int constructorCount = 0;
	int scanCount = 0;
	Grammar peg;
	
	UList<Opcode> codeList = new UList<Opcode>(new Opcode[256]);
	UList<Opcode> optimizedCodeList = new UList<Opcode>(new Opcode[256]);
	HashMap<Integer,Integer> labelMap = new HashMap<Integer,Integer>();
	HashMap<String, Integer> callMap = new HashMap<String, Integer>();
	HashMap<String, Integer> ruleMap = new HashMap<String, Integer>();
	HashMap<String, Integer> repeatMap = new HashMap<String, Integer>();
	
	public void writeByteCode(String grammerfileName, String outputFileName, Grammar peg) {
		generateProfileCode(peg);
		//System.out.println("choiceCase: " + choiceCaseCount + "\nconstructor: " + constructorCount);
		byte[] byteCode = new byte[codeList.size() * 64];
		int pos = 0;
		// Version of the specification (2 byte)
		byte[] version = new byte[2];
		version[0] = 0;
		version[1] = 1;
		byteCode[pos] = version[0];
		pos++;
		byteCode[pos] = version[1];
		pos++;
		
		// Length of grammerfileName (4 byte)
		int fileNamelen = grammerfileName.length();
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (fileNamelen >> 24));
		pos++;
		
		// GrammerfileName (n byte)
		byte[] name = grammerfileName.getBytes();
		for (int i = 0; i < fileNamelen; i++) {
			byteCode[pos] = name[i];
			pos++;
		}
		
		// pool_size_info
		int poolSizeInfo = choiceCaseCount * constructorCount;
		byteCode[pos] = (byte) (0x000000ff & (poolSizeInfo));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (poolSizeInfo >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (poolSizeInfo >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (poolSizeInfo >> 24));
		pos++;

		int bytecodelen_pos = pos;
		pos = pos + 8;
		
		// byte code (m byte)
		for(int i = 0; i < codeList.size(); i++) {
			Opcode code = codeList.ArrayValues[i];
			if (code.inst.equals(Instruction.SCAN)) {
				int a;
				a = 1;
			}
			byteCode[pos] = (byte) code.inst.ordinal();
			pos++;
			if (code.inst == Instruction.CALL) {
				byteCode[pos] = 1;
				pos++;
				byteCode[pos] = 0;
				pos++;
				int ndata = ruleMap.get(code.name);
				byteCode[pos] = (byte) (0x000000ff & (ndata));
				pos++;
				byteCode[pos] = (byte) (0x000000ff & (ndata >> 8));
				pos++;
				byteCode[pos] = (byte) (0x000000ff & (ndata >> 16));
				pos++;
				byteCode[pos] = (byte) (0x000000ff & (ndata >> 24));
				pos++;
			}
			else if (code.ndata != null) {
				byteCode[pos] = (byte) (0x000000ff & (code.ndata.size()));
				pos++;
				byteCode[pos] = (byte) (0x000000ff & (code.ndata.size() >> 8));
				pos++;
				for(int j = 0; j < code.ndata.size(); j++){
					byteCode[pos] = (byte) (0x000000ff & (code.ndata.get(j)));
					pos++;
					byteCode[pos] = (byte) (0x000000ff & (code.ndata.get(j) >> 8));
					pos++;
					byteCode[pos] = (byte) (0x000000ff & (code.ndata.get(j) >> 16));
					pos++;
					byteCode[pos] = (byte) (0x000000ff & (code.ndata.get(j) >> 24));
					pos++;
				}
			}
			else {
				byteCode[pos] = 0;
				pos++;
			}
			byteCode[pos] = (byte) (0x000000ff & (code.jump));
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (code.jump >> 8));
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (code.jump >> 16));
			pos++;
			byteCode[pos] = (byte) (0x000000ff & (code.jump >> 24));
			pos++;
			if(code.name != null) {
				int j = 0;
				byteCode[pos] = (byte) code.name.length();
				pos++;
				byte[] nameByte = code.name.getBytes();
				while (j < code.name.length()) {
					byteCode[pos] = nameByte[j];
					j++;
					pos++;
				}
			}
			else {
				byteCode[pos] = 0;
				pos++;
			}
		}
		// Length of byte code (8 byte) 
		long byteCodelength = codeList.size();
		pos = bytecodelen_pos;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 24));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 32));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 40));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 48));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (byteCodelength >> 56));
		
		try {
			if (outputFileName == null) {
				System.out.println("unspecified outputfile");
				System.exit(0);
			}
			FileOutputStream fos = new FileOutputStream(outputFileName);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(byteCode);
			bos.flush();
			bos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private void generateProfileCode(Grammar peg) {
		int count = 0;
		String grammerName = peg.getName();
		grammerName = grammerName.substring(0, grammerName.length() - 4);
		System.out.println("#define PEGVM_PROFILE_" + grammerName + "_EACH(RULE) \\");
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.localName.startsWith("\"")) {
				ruleMap.put(r.localName, count);
				System.out.println("  RULE("+ r.localName + ") \\");
				count++;
			}
		}
		System.out.println("#define PEGVM_" + grammerName + "_RULE_MAX " + count);
		System.out.println("enum pegvm_" + grammerName + "_rule {");
		System.out.println("#define DEFINE_" + grammerName + "_ENUM(NAME) PEGVM_PROFILE_" + grammerName + "_##NAME,");
		System.out.println("  PEGVM_PROFILE_" + grammerName + "_EACH(DEFINE_" + grammerName + "_ENUM)");
		System.out.println("#undef DEFINE_" + grammerName + "_ENUM");
		System.out.println("  PROFILE_" + grammerName + "_ERROR = -1");
		System.out.println("};");
		System.out.println("\nif (!strcmp(file_type, \"" + grammerName + "\")) {");
		System.out.println("  for (int i = 0; i < PEGVM_" + grammerName + "_RULE_MAX; i++) {");
		System.out.println("    fprintf(stderr, \"%llu %s\\n\", rule_count[i], get_" + grammerName + "_rule(i));");
		System.out.println("  }\n}");
		System.out.println("\nstatic const char *get_" + grammerName + "_rule(uint8_t " + grammerName + "_rule) {");
		System.out.println("  switch (" + grammerName + "_rule) {");
		System.out.println("#define " + grammerName + "_CASE(RULE)           \\");
		System.out.println("  case PEGVM_PROFILE_" + grammerName + "_##RULE: \\");
		System.out.println("    return \"\" #RULE;");
		System.out.println("    PEGVM_PROFILE_" + grammerName + "_EACH(" + grammerName + "_CASE);");
		System.out.println("  default:");
		System.out.println("    assert(0 && \"UNREACHABLE\");");
		System.out.println("    break;");
		System.out.println("#undef " + grammerName + "_CASE");
		System.out.println("  }\n  return \"\";\n}");
	}
	
	private Opcode newCode(Instruction inst) {
		Opcode code = new Opcode(inst);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata) {
		Opcode code = new Opcode(inst).append(ndata);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata1, int jump) {
		Opcode code = new Opcode(inst).append(ndata1);
		code.jump = jump;
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, int ndata1, int ndata2, int jump) {
		Opcode code = new Opcode(inst).append(ndata1).append(ndata2);
		code.jump = jump;
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newCode(Instruction inst, String name) {
		Opcode code = new Opcode(inst, name);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newJumpCode(Instruction inst, int jump) {
		Opcode code = new Opcode(inst);
		code.jump = jump;
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private Opcode newJumpCode(Instruction inst, int ndata, int jump) {
		Opcode code = new Opcode(inst);
		code.append(ndata);
		code.jump = jump;
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		return code;
	}
	
	private final void writeCode(Instruction inst) {
		codeList.add(newCode(inst));
	}
	
	private final void writeCode(Instruction inst, int ndata) {
		codeList.add(newCode(inst, ndata));
	}
	
	private final void writeCode(Instruction inst, int ndata1, int jump) {
		codeList.add(newCode(inst, ndata1, jump));
	}
	
	private final void writeCode(Instruction inst, int ndata1, int ndata2, int jump) {
		codeList.add(newCode(inst, ndata1, ndata2, jump));
	}
	
	private final void writeCode(Instruction inst, String name) {
		codeList.add(newCode(inst, name));
	}
	
	class FailurePoint {
		int id;
		FailurePoint prev;
		FailurePoint(int id, FailurePoint prev) {
			this.prev = prev;
			this.id = id;
		}
	}
	
	int labelId = 0;
	FailurePoint fLabel = null;
	private void pushFailureJumpPoint() {
		fLabel = new FailurePoint(labelId, fLabel);
		labelId += 1;
	}
	
	private int popFailureJumpPoint(ParsingRule r) {
		FailurePoint fLabel = this.fLabel;
		this.fLabel = this.fLabel.prev;
		labelMap.put(fLabel.id, codeIndex);
		System.out.println("L" + fLabel.id);
		return fLabel.id;
	}
	
	private int popFailureJumpPoint(ParsingExpression e) {
		FailurePoint fLabel = this.fLabel;
		this.fLabel = this.fLabel.prev;
		labelMap.put(fLabel.id, codeIndex);
		System.out.println("L" + fLabel.id);
		return fLabel.id;
	}
	
	private int jumpFailureJump() {
		return this.fLabel.id;
	}
	
	private int jumpPrevFailureJump() {
		return this.fLabel.prev.id;
	}
	
	private final int newLabel() {
		int l = labelId;
		labelId++;
		return l;
	}
	
	private final void writeLabel(int label) {
		labelMap.put(label, codeIndex);
		System.out.println("L" + label);
	}
	
	private final void writeJumpCode(Instruction inst, int labelId) {
		codeList.add(newJumpCode(inst, labelId));
	}
	
	private final void writeJumpCode(Instruction inst, int ndata, int labelId) {
		codeList.add(newJumpCode(inst, ndata, labelId));
	}
	
	private final int writeSequenceCode(ParsingExpression e, int index, int size) {
		int count = 0;
		for (int i = index; i < size; i++) {
			if (e.get(i) instanceof ParsingByte) {
				count++;
			}
			else {
				break;
			}
		}
		if (count <= 1) {
			e.get(index).visit(this);
			return index++;
		}
		Opcode code = new Opcode(Instruction.STRING);
		for(int i = index; i < index + count; i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		code.jump = this.jumpFailureJump();
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
		//writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
		return index + count - 1;
	}
	
	private final int writeChoiceCode(ParsingExpression e, int index, int size) {
		int charCount = 0;
		for (int i = index; i < size; i++) {
			if (e.get(i) instanceof ParsingByte) {
				charCount++;
			}
			else {
				break;
			}
		}
		if (charCount <= 1) {
			this.pushFailureJumpPoint();
			writeCode(Instruction.PUSHp1);
			e.get(index).visit(this);
			this.backTrackFlag = true;
			return index++;
		}
		if (charCount != size) {
			backTrackFlag = true;
			this.pushFailureJumpPoint();
			writeCode(Instruction.PUSHp1);
		}
		writeCharsetCode(e, index, charCount);
		//writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
		return index + charCount - 1;
	}
	
	private void writeCharsetCode(ParsingExpression e, int index, int charCount) {
		Opcode code = new Opcode(Instruction.CHARSET);
		for(int i = index; i < index + charCount; i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		code.jump = this.jumpFailureJump();
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeNotCode(ParsingNot e) {
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHp1);
		e.inner.visit(this);
		writeCode(Instruction.STOREp);
		writeCode(Instruction.STOREflag, 1);
		writeJumpCode(Instruction.JUMP, this.jumpPrevFailureJump());
		this.popFailureJumpPoint(e);
		writeCode(Instruction.STOREflag, 0);
		writeCode(Instruction.STOREp);
	}
	
	private void writeNotCharsetCode(ParsingChoice e) {
		Opcode code = new Opcode(Instruction.NOTCHARSET);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		code.jump = this.jumpFailureJump();
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeNotStringCode(ParsingSequence e) {
		Opcode code = new Opcode(Instruction.NOTSTRING);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		code.jump = this.jumpFailureJump();
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeAndCode(ParsingAnd e) {
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHp1);
		e.inner.visit(this);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.STOREp);
		writeJumpCode(Instruction.CONDBRANCH, 1, jumpFailureJump());
	}
	
	private void writeAndCharsetCode(ParsingChoice e) {
		Opcode code = new Opcode(Instruction.ANDCHARSET);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		code.jump = this.jumpFailureJump();
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeAndStringCode(ParsingSequence e) {
		Opcode code = new Opcode(Instruction.ANDSTRING);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		code.jump = this.jumpFailureJump();
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeOptionalCode(ParsingOption e) {
		int label = newLabel();
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHp1);
		e.inner.visit(this);
		writeCode(Instruction.POPp);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.STOREflag, 0);
		writeCode(Instruction.STOREp);
		writeLabel(label);
	}
	
	private void writeOptionalByteRangeCode(ParsingByteRange e) {
		Opcode code = new Opcode(Instruction.OPTIONALBYTERANGE);
		code.append(e.startByteChar);
		code.append(e.endByteChar);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeOptionalCharsetCode(ParsingChoice e) {
		Opcode code = new Opcode(Instruction.OPTIONALCHARSET);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeOptionalStringCode(ParsingSequence e) {
		Opcode code = new Opcode(Instruction.OPTIONALSTRING);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeRepetitionCode(ParsingRepetition e) {
		int label = newLabel();
		int end = newLabel();
		this.pushFailureJumpPoint();
		writeLabel(label);
		writeCode(Instruction.PUSHp1);
		e.inner.visit(this);
		writeJumpCode(Instruction.REPCOND, end);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.STOREflag, 0);
		writeCode(Instruction.STOREp);
		writeLabel(end);
	}
	
	private void writeZeroMoreByteRangeCode(ParsingByteRange e) {
		Opcode code = new Opcode(Instruction.ZEROMOREBYTERANGE);
		code.append(e.startByteChar);
		code.append(e.endByteChar);
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeZeroMoreCharsetCode(ParsingChoice e) {
		Opcode code = new Opcode(Instruction.ZEROMORECHARSET);
		for(int i = 0; i < e.size(); i++) {
			code.append(((ParsingByte)e.get(i)).byteChar);
		}
		System.out.println("\t" + code.toString());
		this.codeIndex++;
		codeList.add(code);
	}
	
	private void writeScanCode(Instruction inst, int num, int index) {
		Opcode code = newCode(inst);
		code.append(num).append(index);
		codeList.add(code);
	}
	
	private boolean checkCharset(ParsingChoice e) {
		for (int i = 0; i < e.size(); i++) {
			if (!(e.get(i) instanceof ParsingByte)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean checkString(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			if (!(e.get(i) instanceof ParsingByte)) {
				return false;
			}
		}
		return true;
	}
	
	private ParsingExpression checkChoice(ParsingChoice e, int c, ParsingExpression failed) {
		UList<ParsingExpression> l = new UList<ParsingExpression>(new ParsingExpression[2]);
		checkChoice(e, c, l);
		if(l.size() == 0) {
			l.add(failed);
		}
		return ParsingExpression.newChoice(l).intern();
	}
	
	private void checkChoice(ParsingChoice choice, int c, UList<ParsingExpression> l) {
		for(int i = 0; i < choice.size(); i++) {
			ParsingExpression e = getNonTerminalRule(choice.get(i));
			if (e instanceof ParsingChoice) {
				checkChoice((ParsingChoice)e, c, l);
			}
			else {
				short r = e.acceptByte(c);
				if(r != ParsingExpression.Reject) {
					l.add(e);
				}
			}
		}
	}
	
	private ParsingExpression getNonTerminalRule(ParsingExpression e) {
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}
	
	private void optimizeChoice(ParsingChoice e) {
		if (checkCharset(e)) {
			writeCharsetCode(e, 0, e.size());
		}
		else {
			ParsingExpression[] matchCase = new ParsingExpression[257];
			UList<ParsingExpression> caseList = new UList<ParsingExpression>(new ParsingExpression[257]);
			ParsingExpression fails = new ParsingFailure(e);
			for(int i = 0; i < 257; i++) {
				boolean isNotGenerated = true;
				matchCase[i] = checkChoice(e, i, fails);
				for(int j = 0; j < caseList.size(); j++) {
					if (matchCase[i] == caseList.get(j)) {
						isNotGenerated = false;
					}
				}
				if (isNotGenerated) {
					caseList.add(matchCase[i]);
				}
			}
			Opcode code = new Opcode(Instruction.MAPPEDCHOICE);
			System.out.println("\t" + code.toString());
			codeList.add(code);
			this.codeIndex++;
			HashMap<ParsingExpression, Integer> choiceMap = new HashMap<ParsingExpression, Integer>();
			optChoiceMode = false;
			int label = newLabel();
			for(int i = 0; i < caseList.size(); i++) {
				ParsingExpression caseElement = caseList.get(i);
				choiceMap.put(caseElement, codeIndex);
				caseElement.visit(this);
				if (caseElement instanceof ParsingFailure) {
					writeJumpCode(Instruction.JUMP, this.jumpFailureJump());
				}
				else {
					writeJumpCode(Instruction.JUMP, label);
				}
			}
			writeLabel(label);
			optChoiceMode = true;
			for(int i = 0; i < matchCase.length; i++) {
				code.append(choiceMap.get(matchCase[i]));
			}
		}
	}
	
	private boolean optimizeAnd(ParsingAnd e) {
		ParsingExpression inner = e.inner;
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ParsingByte) {
			writeCode(Instruction.ANDBYTE, ((ParsingByte)inner).byteChar, this.jumpFailureJump());
			return true;
		}
		if (inner instanceof ParsingByteRange) {
			writeCode(Instruction.ANDBYTERANGE, ((ParsingByteRange)inner).startByteChar, ((ParsingByteRange)inner).endByteChar, this.jumpFailureJump());
			return true;
		}
		if(inner instanceof ParsingChoice) {
			if (checkCharset((ParsingChoice)inner)) {
				writeAndCharsetCode((ParsingChoice)inner);
				return true;
			}
		}
		if (inner instanceof ParsingSequence) {
			if (checkString((ParsingSequence)inner)) {
				writeAndStringCode((ParsingSequence)inner);
				return true;
			}
		}
		return false;
	}
	
	private boolean optimizeNot(ParsingNot e) {
		ParsingExpression inner = e.inner;
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ParsingByte) {
			writeCode(Instruction.NOTBYTE, ((ParsingByte)inner).byteChar, this.jumpFailureJump());
			return true;
		}
		if (inner instanceof ParsingByteRange) {
			writeCode(Instruction.NOTBYTERANGE, ((ParsingByteRange)inner).startByteChar, ((ParsingByteRange)inner).endByteChar, this.jumpFailureJump());
			return true;
		}
		if(inner instanceof ParsingAny) {
			writeJumpCode(Instruction.NOTANY, this.jumpFailureJump());
			return true;
		}
		if(inner instanceof ParsingChoice) {
			if (checkCharset((ParsingChoice)inner)) {
				writeNotCharsetCode((ParsingChoice)inner);
				return true;
			}
		}
		if (inner instanceof ParsingSequence) {
			if (checkString((ParsingSequence)inner)) {
				writeNotStringCode((ParsingSequence)inner);
				return true;
			}
		}
		return false;
	}
	
	private boolean optimizeOptional(ParsingOption e) {
		ParsingExpression inner = e.inner;
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ParsingByte) {
			writeCode(Instruction.OPTIONALBYTE, ((ParsingByte)inner).byteChar);
			return true;
		}
		if (inner instanceof ParsingByteRange) {
			writeOptionalByteRangeCode((ParsingByteRange)inner);
			return true;
		}
		if(inner instanceof ParsingChoice) {
			if (checkCharset((ParsingChoice)inner)) {
				writeOptionalCharsetCode((ParsingChoice)inner);
				return true;
			}
		}
		if (inner instanceof ParsingSequence) {
			if (checkString((ParsingSequence)inner)) {
				writeOptionalStringCode((ParsingSequence)inner);
				return true;
			}
		}
		return false;
	}
	
	private boolean optimizeRepetition(ParsingRepetition e) {
		ParsingExpression inner = e.inner;
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ParsingByteRange) {
			writeZeroMoreByteRangeCode((ParsingByteRange)inner);
			return true;
		}
		if (inner instanceof ParsingChoice) {
			if (checkCharset((ParsingChoice)inner)) {
				writeZeroMoreCharsetCode((ParsingChoice)inner);
				return true;
			}
		}
		return false;
	}
	
	private boolean optimizeRepeat(ParsingRepeat e) {
		ParsingExpression inner = e.inner;
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof ParsingAny) {
			writeCode(Instruction.REPEATANY, e.inner.toString());
			return true;
		}
		return false;
	}
	
	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.optimizationLevel = Main.OptimizationLevel;
		this.peg = peg;
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			if (r.localName.equals("File")) {
				this.formatRule(r, sb);		//string builder is not used.
				break;
			}
		}
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.localName.equals("File")) {
				if (!r.localName.startsWith("\"")) {
					this.formatRule(r, sb);		//string builder is not used.
				}
			}
		}
		this.formatFooter();
	}

	@Override
	public void formatHeader() {
		System.out.println("\nGenerate Byte Code\n");
		writeCode(Instruction.EXIT);
	}
	
	@Override
	public void formatFooter() {
		System.out.println();
		for (int i = 0; i < codeList.size(); i++) {
			Opcode code = codeList.ArrayValues[i];
			if (code.isJumpCode()) {
				switch (code.inst) {
				case CALL:
					code.jump = this.callMap.get(code.name);
					//code.name = null;
					System.out.println("[" + i + "] " + code + " " + code.jump);
					break;
				case RET:
					System.out.println("[" + i + "] " + code);
					break;
				case EXIT:
					System.out.println("[" + i + "] " + code);
					break;
				default:
					code.jump = this.labelMap.get(code.jump);
					System.out.println("[" + i + "] " + code);
					break;
				}
			}
			else if (code.inst.equals(Instruction.REPEATANY) || code.inst.equals(Instruction.CHECKEND) ) {
				code.ndata.add(repeatMap.get(code.name));
				code.name = null;
			}
			else {
				System.out.println("[" + i + "] " + code);
			}
		}
	}

	@Override
	public String getDesc() {
		return "pegvm";
	}

	@Override
	public void visitRule(ParsingRule e) {
		this.callMap.put(e.localName, this.codeIndex);
		System.out.println(e.localName + ":");
		this.pushFailureJumpPoint();
		e.expr.visit(this);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.RET);
	}

	@Override
	public void visitNonTerminal(NonTerminal ne) {
		/*if (optimizationLevel > 1 && optNonTerminalMode) {
			optNonTerminalMode = false;
			ParsingExpression e = getNonTerminalRule(ne);
			e.visit(this);
			optNonTerminalMode = true;
		}
		else {*/
			writeCode(Instruction.CALL, ne.ruleName);
			writeJumpCode(Instruction.CONDBRANCH, 1, this.jumpFailureJump());
		//}
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		writeCode(Instruction.STOREflag, 1);
	}

	@Override
	public void visitByte(ParsingByte e) {
		writeCode(Instruction.CHARRANGE, e.byteChar, e.byteChar, this.jumpFailureJump());
		//writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
			writeCode(Instruction.CHARRANGE, e.startByteChar, e.endByteChar, this.jumpFailureJump());
			//writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeJumpCode(Instruction.ANY, this.jumpFailureJump());
		//writeJumpCode(Instruction.IFFAIL, this.jumpFailureJump());
	}

	@Override
	public void visitString(ParsingString e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitNot(ParsingNot e) {
		if (optimizationLevel > 2) {
			if (!optimizeNot(e)) {
				writeNotCode(e);
			}
		}
		else {
			writeNotCode(e);
		}
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		if (optimizationLevel > 2) {
			if (!optimizeAnd(e)) {
				writeAndCode(e);
			}
		}
		else {
			writeAndCode(e);
		}
	}

	@Override
	public void visitOptional(ParsingOption e) {
		if (optimizationLevel > 2) {
			if (!optimizeOptional(e)) {
				writeOptionalCode(e);
			}
		}
		else {
			writeOptionalCode(e);
		}
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		if (optimizationLevel > 2) {
			if (!optimizeRepetition(e)) {
				writeRepetitionCode(e);
			}
		}
		else {
			writeRepetitionCode(e);
		}
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			if (optimizationLevel > 0) {
				i = writeSequenceCode(e, i, e.size());
			}
			else {
				e.get(i).visit(this);
			}
		}
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		this.choiceCaseCount += e.size();
		int label = newLabel();
		if (optimizationLevel > 2 && optChoiceMode) {
			optimizeChoice(e);
		}
		else if (optimizationLevel > 0) {
			boolean backTrackFlag = this.backTrackFlag = false;
			for(int i = 0; i < e.size(); i++) {
				i = writeChoiceCode(e, i, e.size());
				backTrackFlag = this.backTrackFlag;
				if (backTrackFlag) {
					writeJumpCode(Instruction.JUMP, label);
					this.popFailureJumpPoint(e.get(i));
					if (i != e.size() - 1) {
						writeCode(Instruction.STOREflag, 0);
					}
					writeCode(Instruction.STOREp);
				}
			}
			if (backTrackFlag) {
				writeJumpCode(Instruction.JUMP, jumpFailureJump());
				writeLabel(label);
				writeCode(Instruction.POPp);
			}
			this.backTrackFlag = false;
		}
		else {
			for(int i = 0; i < e.size(); i++) {
				this.pushFailureJumpPoint();
				writeCode(Instruction.PUSHp1);
				e.get(i).visit(this);
				writeJumpCode(Instruction.JUMP, label);
				this.popFailureJumpPoint(e.get(i));
				if (i != e.size() - 1) {
					writeCode(Instruction.STOREflag, 0);
				}
				writeCode(Instruction.STOREp);
			}
			writeJumpCode(Instruction.JUMP, jumpFailureJump());
			writeLabel(label);
			writeCode(Instruction.POPp);
		}
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		this.constructorCount++;
		int label = newLabel();
		this.pushFailureJumpPoint();
		if (e.leftJoin) {
//			writeCode(Instruction.PUSHconnect);
			writeCode(Instruction.PUSHconnect);
			//writeCode(Instruction.PUSHm);
//			writeCode(Instruction.NEW);
			writeCode(Instruction.NEWJOIN, 0);
		}
		else {
			//writeCode(Instruction.PUSHo);
			//writeCode(Instruction.PUSHm);
			writeCode(Instruction.NEW);
		}
		for(int i = 0; i < e.size(); i++) {
			if (optimizationLevel > 0) {
				i = writeSequenceCode(e, i, e.size());
			}
			else {
				e.get(i).visit(this);
			}
		}
		writeCode(Instruction.SETendp);
		writeCode(Instruction.POPp);
		if (e.leftJoin) {
			writeCode(Instruction.POPo);
		}
		//writeCode(Instruction.POPo);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		writeCode(Instruction.ABORT);
		if (e.leftJoin) {
			writeCode(Instruction.STOREo);
		}
		//writeCode(Instruction.STOREo);
		writeJumpCode(Instruction.JUMP, this.jumpFailureJump());
		writeLabel(label);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		int label = newLabel();
		this.pushFailureJumpPoint();
		writeCode(Instruction.PUSHconnect);
		//writeCode(Instruction.PUSHm);
		e.inner.visit(this);
		writeCode(Instruction.COMMIT, e.index);
		//writeCode(Instruction.LINK, e.index);
		//writeCode(Instruction.STOREo);
		writeJumpCode(Instruction.JUMP, label);
		this.popFailureJumpPoint(e);
		//writeCode(Instruction.SUCC);
		writeCode(Instruction.ABORT);
		writeCode(Instruction.STOREo);
		writeJumpCode(Instruction.JUMP, jumpFailureJump());
		writeLabel(label);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		writeCode(Instruction.TAG, "#" + e.tag.toString());
	}

	@Override
	public void visitValue(ParsingValue e) {
		writeCode(Instruction.VALUE, e.value);
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitApply(ParsingApply e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

//	@Override
//	public void visitPermutation(ParsingPermutation e) {
//		throw new RuntimeException("unimplemented visit method: " + e.getClass());
//	}
//
	@Override
	public void visitScan(ParsingScan e) {
		writeCode(Instruction.PUSHp1);
		e.inner.visit(this);
		writeScanCode(Instruction.SCAN, e.number, this.scanCount);
		this.repeatMap.put(e.repeatExpression.toString(), this.scanCount);
		this.scanCount++;
	}

	@Override
	public void visitRepeat(ParsingRepeat e) {
		if (optimizationLevel > 0) {
			if (!optimizeRepeat(e)) {
				int label = newLabel();
				int end = newLabel();
				this.pushFailureJumpPoint();
				writeLabel(label);
				writeCode(Instruction.PUSHp1);
				e.inner.visit(this);
				writeJumpCode(Instruction.CHECKEND, this.repeatMap.get(e.inner.toString()));
				writeJumpCode(Instruction.JUMP, label);
				this.popFailureJumpPoint(e);
				writeCode(Instruction.STOREflag, 0);
				writeCode(Instruction.STOREp);
				writeLabel(end);
			}
		}
		else {
			int label = newLabel();
			int end = newLabel();
			this.pushFailureJumpPoint();
			writeLabel(label);
			writeCode(Instruction.PUSHp1);
			e.inner.visit(this);
			writeJumpCode(Instruction.CHECKEND, this.repeatMap.get(e.inner.toString()));
			writeJumpCode(Instruction.JUMP, label);
			this.popFailureJumpPoint(e);
			writeCode(Instruction.STOREflag, 0);
			writeCode(Instruction.STOREp);
			writeLabel(end);
		}
	}

	@Override
	public void visitDef(ParsingDef e) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void visitIs(ParsingIs e) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

}

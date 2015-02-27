package org.peg4d.pegcode;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import org.peg4d.Grammar;
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
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIs;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingPermutation;
import org.peg4d.expression.ParsingRepeat;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingScan;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;

public class Compiler extends GrammarGenerator {
	
	Grammar peg;
	Module module;
	Function func;
	BasicBlock currentBB;
	
	Stack<BasicBlock> bbStack = new Stack<BasicBlock>();
	
	int codeIndex;
	
	public void writeByteCode(String grammerfileName, String outputFileName, Grammar peg) {
		//generateProfileCode(peg);
		//System.out.println("choiceCase: " + choiceCaseCount + "\nconstructor: " + constructorCount);
		byte[] byteCode = new byte[this.codeIndex * 64];
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
		pos = write32(byteCode, fileNamelen, pos);
		
		// GrammerfileName (n byte)
		byte[] name = grammerfileName.getBytes();
		for (int i = 0; i < fileNamelen; i++) {
			byteCode[pos] = name[i];
			pos++;
		}
		
		// pool_size_info
		int poolSizeInfo = 1064;
		pos = write32(byteCode, poolSizeInfo, pos);
		
		// rule table
		int ruleSize = this.module.size();
		pos = write32(byteCode, ruleSize, pos);
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			byte[] ruleName = func.funcName.getBytes();
			int ruleNamelen = ruleName.length;
			long entryPoint = func.get(0).codeIndex;
			pos = write32(byteCode, ruleNamelen, pos);
			for(int j = 0; j < ruleName.length; j++) {
				byteCode[pos] = ruleName[j];
				pos++;
			}
			pos = write64(byteCode, entryPoint, pos);
		}

		int bytecodelen_pos = pos;
		pos = pos + 8;
		
		int index = 0;
		// byte code (m byte)
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					pos = writeOpcode(inst, byteCode, pos, index);
					index++;
				}
			}
		}
		// Length of byte code (8 byte) 
		long byteCodelength = this.codeIndex;
		pos = bytecodelen_pos;
		write64(byteCode, byteCodelength, pos);
		
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
	
	private int write16(byte[] byteCode, long num, int pos) {
		byteCode[pos] = (byte) (0x000000ff & num);
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 8));
		pos++;
		return pos;
	}

	private int write32(byte[] byteCode, long num, int pos) {
		byteCode[pos] = (byte) (0x000000ff & (num));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 8));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 16));
		pos++;
		byteCode[pos] = (byte) (0x000000ff & (num >> 24));
		pos++;
		return pos;
	}
	
	private int write64(byte[] byteCode, long num, int pos) {
		pos = write32(byteCode, num, pos);
		pos = write32(byteCode, num >> 32, pos);
		return pos;
	}

	private int writeCdataByteCode(byte[] byteCode, String cdata, int pos) {
		int j = 0;
		pos = write16(byteCode, cdata.length(), pos);
		byte[] nameByte = cdata.getBytes();
		while (j < cdata.length()) {
			byteCode[pos] = nameByte[j];
			j++;
			pos++;
		}
		return pos;
	}

	private int writeOpcode(Instruction code, byte[] byteCode, int pos, int index) {
		byteCode[pos] = (byte)code.op.ordinal();
		pos++;
		switch (code.op) {
		case JUMP:
			pos = write32(byteCode, ((JUMP)code).jump.codeIndex-index, pos);
			break;
		case CALL:
			//pos = write32(byteCode, code.jump-index, pos);
			pos = write32(byteCode, ((CALL)code).jumpIndex-index, pos);
			break;
		case CONDBRANCH:
			pos = write32(byteCode, ((CONDBRANCH)code).val, pos);
			pos = write32(byteCode, ((CONDBRANCH)code).jump.codeIndex-index, pos);
			break;
		case REPCOND:
			pos = write32(byteCode, ((REPCOND)code).jump.codeIndex-index, pos);
			break;
		case CHARRANGE:
			pos = write32(byteCode, ((CHARRANGE)code).jump.codeIndex-index, pos);
			pos = write32(byteCode, ((CHARRANGE)code).getc(0), pos);
			pos = write32(byteCode, ((CHARRANGE)code).getc(1), pos);
			break;
		case CHARSET:
			pos = write16(byteCode, ((CHARSET)code).size(), pos);
			for(int j = 0; j < ((CHARSET)code).size(); j++){
				pos = write32(byteCode, ((CHARSET)code).getc(j), pos);
			}
			pos = write32(byteCode, ((CHARSET)code).jump.codeIndex-index, pos);
			break;
		case STRING:
			pos = write32(byteCode, ((STRING)code).jump.codeIndex-index, pos);
			pos = write16(byteCode, ((STRING)code).size(), pos);
			for(int j = 0; j < ((STRING)code).size(); j++){
				pos = write32(byteCode, ((STRING)code).getc(j), pos);
			}
			break;
		case ANY:
			pos = write32(byteCode, ((ANY)code).jump.codeIndex-index, pos);
			break;
		case STOREflag:
			pos = write32(byteCode, ((STOREflag)code).val, pos);
			break;
		case NEWJOIN:
			pos = write32(byteCode, ((NEWJOIN)code).ndata, pos);
			break;
		case COMMIT:
			pos = write32(byteCode, ((COMMIT)code).ndata, pos);
			break;
		case TAG:
			pos = writeCdataByteCode(byteCode, ((TAG)code).cdata, pos);
			break;
		case VALUE:
			pos = writeCdataByteCode(byteCode, ((VALUE)code).cdata, pos);
			break;
//		case MAPPEDCHOICE:
//			for(int j = 0; j < 256; j++){
//				pos = write32(byteCode, code.ndata.get(j)-index, pos);
//			}
//			break;
//		case SCAN:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.get(1), pos);
//			break;
//		case CHECKEND:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.jump-index, pos);
//			break;
//		case DEF:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case IS:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case ISA:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case BLOCKSTART:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case INDENT:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case NOTBYTE:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.jump-index, pos);
//			break;
//		case NOTANY:
//			pos = write32(byteCode, code.jump-index, pos);
//			break;
//		case NOTCHARSET:
//			pos = write16(byteCode, code.ndata.size(), pos);
//			for(int j = 0; j < code.ndata.size(); j++){
//				pos = write32(byteCode, code.ndata.get(j), pos);
//			}
//			pos = write32(byteCode, code.jump-index, pos);
//			break;
//		case NOTBYTERANGE:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.get(1), pos);
//			pos = write32(byteCode, code.jump-index, pos);
//			break;
//		case NOTSTRING:
//			pos = write32(byteCode, code.jump-index, pos);
//			pos = write16(byteCode, code.ndata.size(), pos);
//			for(int j = 0; j < code.ndata.size(); j++){
//				pos = write32(byteCode, code.ndata.get(j), pos);
//			}
//			break;
//		case OPTIONALBYTE:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
//		case OPTIONALCHARSET:
//			pos = write16(byteCode, code.ndata.size(), pos);
//			for(int j = 0; j < code.ndata.size(); j++){
//				pos = write32(byteCode, code.ndata.get(j), pos);
//			}
//			break;
//		case OPTIONALBYTERANGE:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.get(1), pos);
//			break;
//		case OPTIONALSTRING:
//			pos = write16(byteCode, code.ndata.size(), pos);
//			for(int j = 0; j < code.ndata.size(); j++){
//				pos = write32(byteCode, code.ndata.get(j), pos);
//			}
//			break;
//		case ZEROMOREBYTERANGE:
//			pos = write32(byteCode, code.get(0), pos);
//			pos = write32(byteCode, code.get(1), pos);
//			break;
//		case ZEROMORECHARSET:
//			pos = write16(byteCode, code.ndata.size(), pos);
//			for(int j = 0; j < code.ndata.size(); j++){
//				pos = write32(byteCode, code.ndata.get(j), pos);
//			}
//			break;
//		case REPEATANY:
//			pos = write32(byteCode, code.get(0), pos);
//			break;
		default:
			break;
		}
		return pos;
	}
	
	class FailureBB {
		BasicBlock fbb;
		FailureBB prev;
		public FailureBB(BasicBlock bb, FailureBB prev) {
			this.fbb = bb;
			this.prev = prev;
		}
	}
	
	FailureBB fLabel = null;
	private void pushFailureJumpPoint(BasicBlock bb) {
		this.fLabel = new FailureBB(bb, this.fLabel);
	}
	
	private void popFailureJumpPoint(ParsingRule r) {
		this.fLabel = this.fLabel.prev;
	}
	
	private void popFailureJumpPoint(ParsingExpression e) {
		this.fLabel = this.fLabel.prev;
	}
	
	private BasicBlock jumpFailureJump() {
		return this.fLabel.fbb;
	}
	
	private BasicBlock jumpPrevFailureJump() {
		return this.fLabel.prev.fbb;
	}

	@Override
	public String getDesc() {
		return "nezvm";
	}
	
	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.peg = peg;
		this.module = new Module();
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			if (r.ruleName.equals("File")) {
				this.formatRule(r, sb);
				break;
			}
		}
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.ruleName.equals("File")) {
				if (!r.ruleName.startsWith("\"")) {
					this.formatRule(r, sb);
				}
			}
		}
		this.formatFooter();
	}

	@Override
	public void formatHeader() {
		System.out.println("\nGenerate Byte Code\n");
		//writeCode(Instruction.EXIT);
		Function func = new Function(this.module, "EXIT");
		BasicBlock bb = new BasicBlock(func);
		this.createEXIT(null, bb);
	}
	
	@Override
	public void formatFooter() {
		System.out.println(this.module.stringfy(this.sb));
		Optimizer o = new Optimizer();
		o.optimize(this.module);
		this.labeling();
		this.dumpLastestCode();
	}
	
	HashMap<String, Integer> callMap = new HashMap<String, Integer>();
	private void labeling() {
		int codeIndex = 0;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			this.callMap.put(func.funcName, codeIndex);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				bb.codeIndex = codeIndex;
				codeIndex += bb.size();
			}
		}
		this.codeIndex = codeIndex;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					if (inst instanceof CALL) {
						CALL cinst = (CALL)inst;
						cinst.jumpIndex = this.callMap.get(cinst.ruleName);
					}
				}
			}
		}
	}
	
	private void dumpLastestCode() {
		int codeIndex = 0;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					System.out.println("[" + codeIndex + "] " + inst.toString());
					codeIndex++;
				}
			}
		}
	}
	
	public BasicBlock getCurrentBasicBlock() {
		return this.currentBB;
	}
	
	public void setCurrentBasicBlock(BasicBlock bb) {
		this.currentBB = bb;
	}
	
	private Instruction createEXIT(ParsingExpression e, BasicBlock bb) {
		return new EXIT(e, bb);
	}
	
	private Instruction createJUMP(ParsingExpression e, BasicBlock bb, BasicBlock jump) {
		return new JUMP(e, bb, jump);
	}
	
	private Instruction createCALL(ParsingExpression e, BasicBlock bb, String name) {
		return new CALL(e, bb, name);
	}
	
	private Instruction createRET(ParsingExpression e, BasicBlock bb) {
		return new RET(e, bb);
	}
	
	private Instruction createCONDBRANCH(ParsingExpression e, BasicBlock bb, BasicBlock jump, int val) {
		return new CONDBRANCH(e, bb, jump, val);
	}
	
	private Instruction createREPCOND(ParsingExpression e, BasicBlock bb, BasicBlock jump) {
		return new REPCOND(e, bb, jump);
	}
	
	private Instruction createCHARRANGE(ParsingExpression e, BasicBlock bb, BasicBlock jump, int ...cdata) {
		return new CHARRANGE(e, bb, jump, cdata);
	}
	
	private Instruction createCHARSET(ParsingExpression e, BasicBlock bb, BasicBlock jump, int ...cdata) {
		return new CHARSET(e, bb, jump, cdata);
	}
	
	private Instruction createSTRING(ParsingExpression e, BasicBlock bb, BasicBlock jump, int ...cdata) {
		return new STRING(e, bb, jump, cdata);
	}
	
	private Instruction createANY(ParsingExpression e, BasicBlock bb, BasicBlock jump) {
		return new ANY(e, bb, jump);
	}
	
	private Instruction createPUSHo(ParsingExpression e, BasicBlock bb) {
		return new PUSHo(e, bb);
	}
	
	private Instruction createPUSHp(ParsingExpression e, BasicBlock bb) {
		return new PUSHp(e, bb);
	}
	
	private Instruction createPOPo(ParsingExpression e, BasicBlock bb) {
		return new POPo(e, bb);
	}
	
	private Instruction createPOPp(ParsingExpression e, BasicBlock bb) {
		return new POPp(e, bb);
	}
	
	private Instruction createSTOREo(ParsingExpression e, BasicBlock bb) {
		return new STOREo(e, bb);
	}
	private Instruction createSTOREp(ParsingExpression e, BasicBlock bb) {
		return new STOREp(e, bb);
	}
	
	private Instruction createSTOREflag(ParsingExpression e, BasicBlock bb, int val) {
		return new STOREflag(e, bb, val);
	}
	
	private Instruction createSETendp(ParsingExpression e, BasicBlock bb) {
		return new SETendp(e, bb);
	}
	
	private Instruction createNEW(ParsingExpression e, BasicBlock bb) {
		return new NEW(e, bb);
	}
	
	private Instruction createLEFTJOIN(ParsingExpression e, BasicBlock bb, int ndata) {
		return new NEWJOIN(e, bb, ndata);
	}
	
	private Instruction createCOMMITLINK(ParsingExpression e, BasicBlock bb, int ndata) {
		return new COMMIT(e, bb, ndata);
	}
	
	private Instruction createABORT(ParsingExpression e, BasicBlock bb) {
		return new ABORT(e, bb);
	}
	
	private Instruction createTAG(ParsingExpression e, BasicBlock bb, String name) {
		return new TAG(e, bb, name);
	}
	
	private Instruction createVALUE(ParsingExpression e, BasicBlock bb, String name) {
		return new VALUE(e, bb, name);
	}

	@Override
	public void visitRule(ParsingRule e) {
		this.func = new Function(this.module, e.ruleName);
		this.setCurrentBasicBlock(new BasicBlock(this.func));
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		e.expr.visit(this);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(func);
		new RET(e.expr, fbb);
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		BasicBlock rbb = new BasicBlock();
		this.createCALL(e, this.getCurrentBasicBlock(), e.ruleName);
		rbb.setInsertPoint(this.func);
		this.createCONDBRANCH(e, rbb, this.jumpFailureJump(), 1);
		BasicBlock bb = new BasicBlock(this.func);
		this.setCurrentBasicBlock(bb);
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {
	}

	@Override
	public void visitFailure(ParsingFailure e) {
		new STOREflag(e, this.getCurrentBasicBlock(), 1);
	}

	@Override
	public void visitByte(ParsingByte e) {
		this.createCHARRANGE(e, this.getCurrentBasicBlock(), this.jumpFailureJump(), e.byteChar, e.byteChar);
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		this.createCHARRANGE(e, this.getCurrentBasicBlock(), this.jumpFailureJump(), e.startByteChar, e.endByteChar);
	}

	@Override
	public void visitAny(ParsingAny e) {
		this.createANY(e, this.getCurrentBasicBlock(), this.jumpFailureJump());
	}

	@Override
	public void visitString(ParsingString e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitNot(ParsingNot e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.createPUSHp(e, bb);
		e.inner.visit(this);
		bb = this.getCurrentBasicBlock();
		this.createSTOREp(e, bb);
		this.createSTOREflag(e, bb, 1);
		this.createJUMP(e, bb, this.jumpPrevFailureJump());
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.setCurrentBasicBlock(fbb);
		this.createSTOREp(e, fbb);
		this.createSTOREflag(e, fbb, 0);
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.createPUSHp(e, bb);
		e.inner.visit(this);
		bb = this.getCurrentBasicBlock();
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createSTOREp(e, fbb);
		this.createCONDBRANCH(e, fbb, this.jumpFailureJump(), 1);
		this.setCurrentBasicBlock(fbb);
	}

	@Override
	public void visitOptional(ParsingOption e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.createPUSHp(e, bb);
		e.inner.visit(this);
		bb = this.getCurrentBasicBlock();
		this.createPOPp(e, bb);
		this.createJUMP(e, bb, mergebb);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createSTOREflag(e, fbb, 0);
		this.createSTOREp(e, fbb);
		this.setCurrentBasicBlock(mergebb);
		mergebb.setInsertPoint(this.func);
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		BasicBlock bb = new BasicBlock(this.func);
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		this.setCurrentBasicBlock(bb);
		this.createPUSHp(e, bb);
		e.inner.visit(this);
		BasicBlock current = getCurrentBasicBlock();
		this.createREPCOND(e, current, mergebb);
		this.createJUMP(e, current, bb);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createSTOREflag(e, fbb, 0);
		this.createSTOREp(e, fbb);
		mergebb.setInsertPoint(this.func);
		this.setCurrentBasicBlock(mergebb);
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		BasicBlock bb = null;
		BasicBlock fbb = null;
		BasicBlock endbb = new BasicBlock();
		for(int i = 0; i < e.size(); i++) {
			bb = this.getCurrentBasicBlock();
			fbb =  new BasicBlock();
			this.pushFailureJumpPoint(fbb);
			this.createPUSHp(e, bb);
			e.get(i).visit(this);
			bb = getCurrentBasicBlock();
			this.createJUMP(e, bb, endbb);
			this.popFailureJumpPoint(e.get(i));
			fbb.setInsertPoint(this.func);
			if (i != e.size() - 1) {
				this.createSTOREflag(e, fbb, 0);
			}
			this.createSTOREp(e, fbb);
			this.setCurrentBasicBlock(fbb);
		}
		this.createJUMP(e, fbb, this.jumpFailureJump());
		endbb.setInsertPoint(this.func);
		this.createPOPp(e, endbb);
		this.setCurrentBasicBlock(endbb);
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock();
		this.pushFailureJumpPoint(fbb);
		if (e.leftJoin) {
			this.createPUSHo(e, bb);
			this.createLEFTJOIN(e, bb, 0);
		}
		else {
			this.createNEW(e, bb);
		}
		for(int i = 0; i < e.size(); i++){
			e.get(i).visit(this);
		}
		bb = this.getCurrentBasicBlock();
		createSETendp(e, bb);
		createPOPp(e, bb);
		createJUMP(e, bb, mergebb);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		createABORT(e, fbb);
		if (e.leftJoin) {
			this.createPOPo(e, fbb);
		}
		this.createJUMP(e, fbb, this.jumpFailureJump());
		mergebb.setInsertPoint(this.func);
		this.setCurrentBasicBlock(mergebb);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		BasicBlock bb = this.getCurrentBasicBlock();
		BasicBlock fbb = new BasicBlock();
		BasicBlock mergebb = new BasicBlock(); 
		this.pushFailureJumpPoint(fbb);
		this.createPUSHo(e, bb);
		e.inner.visit(this);
		bb = this.getCurrentBasicBlock();
		this.createCOMMITLINK(e, bb, e.index);
		this.createJUMP(e, bb, mergebb);
		this.popFailureJumpPoint(e);
		fbb.setInsertPoint(this.func);
		this.createABORT(e, fbb);
		this.createSTOREo(e, fbb);
		this.createJUMP(e, fbb, this.jumpFailureJump());
		mergebb.setInsertPoint(this.func);
		this.setCurrentBasicBlock(mergebb);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		createTAG(e, this.getCurrentBasicBlock(), "#" + e.tag.toString());
	}

	@Override
	public void visitValue(ParsingValue e) {
		createVALUE(e, this.getCurrentBasicBlock(), e.value);
	}

	@Override
	public void visitExport(ParsingExport e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
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
	public void visitDef(ParsingDef e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitIs(ParsingIs e) {
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

	@Override
	public void visitPermutation(ParsingPermutation e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitScan(ParsingScan e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

	@Override
	public void visitRepeat(ParsingRepeat e) {
		throw new RuntimeException("unimplemented visit method: " + e.getClass());
	}

}

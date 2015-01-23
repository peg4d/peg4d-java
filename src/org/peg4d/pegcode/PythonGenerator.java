package org.peg4d.pegcode;

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

public class PythonGenerator extends GrammarGenerator {

	@Override
	public String getDesc() {
		return "Python Generator";
	}

	String indent = "";

	protected void writeLine(String s) {
		formatString("\n" + indent + s);
	}
	
	protected void openIndent() {
		//writeLine("{");
		indent = "  " + indent;
	}
	protected void closeIndent() {
		indent = indent.substring(2);
		//writeLine("}");
	}
	
	@Override
	public void formatHeader() {
		writeLine("import \"libp4d/parsing.py\"");
	}
	
	class FailurePoint {
		int id;
		FailurePoint prev;
		FailurePoint(int id, FailurePoint prev) {
			this.prev = prev;
			this.id = id;
		}
	}

	int fID = 0;
	FailurePoint fLabel = null;
	void initFailureJumpPoint() {
		fID = 0;
		fLabel = null;
	}
	void pushFailureJumpPoint() {
		fLabel = new FailurePoint(fID, fLabel);
		fID += 1;
		writeLine("try :");
		this.openIndent();
	}
	void popFailureJumpPoint(ParsingRule r) {
		this.closeIndent();
		writeLine("except :" + " ## " + r.localName );
		this.openIndent();
		fLabel = fLabel.prev;
	}
	void popFailureJumpPoint(ParsingExpression e) {
		this.closeIndent();
		writeLine("except :" + " ## " + e );
		this.openIndent();
		fLabel = fLabel.prev;
	}
	void endFailureJumpPoint() {
		this.closeIndent();
	}
	void jumpFailureJump() {
		writeLine("raise Exception");
	//		writeLine("goto CATCH_FAILURE" + fLabel.id + "");
	}
	void jumpPrevFailureJump() {
		writeLine("raise Exception");
		//writeLine("goto CATCH_FAILURE" + fLabel.prev.id + "");
	}
	void let(String type, String var, String expr) {
		writeLine("" + var + " = " + expr);		
	}

	String funcName(String symbol) {
		return "p" + symbol;
	}
	
	@Override
	public void visitRule(ParsingRule e) {
		this.initFailureJumpPoint();
		writeLine("def " + funcName(e.localName) + "(c) :");
		openIndent();
		this.let("long", "pos", "c.pos");
		this.pushFailureJumpPoint();
		e.expr.visit(this);
		
		this.let(null, "c.pos", "pos");
		writeLine("return True");
		
		this.popFailureJumpPoint(e);
		writeLine("return False");
		endFailureJumpPoint();
		closeIndent();
		writeLine("");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		writeLine("if not " + funcName(e.ruleName) + "(c) :");
		openIndent();
		jumpFailureJump();
		closeIndent();
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {

	}

	@Override
	public void visitFailure(ParsingFailure e) {
		jumpFailureJump();
	}

	@Override
	public void visitByte(ParsingByte e) {
		writeLine("if c.inputs[pos] != " + e.byteChar + " :");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos+=1");
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		writeLine("if(c.inputs[pos] < " + e.startByteChar + " and c.inputs[pos] > " + e.endByteChar + "): ");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos+=1");
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeLine("if(c.inputs[pos] == 0):");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos+=1");
	}

	@Override
	public void visitString(ParsingString e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitNot(ParsingNot e) {
		this.pushFailureJumpPoint();
		String posName = "pos" + this.fID;
		this.let("long", posName, "pos");
		e.inner.visit(this);
		this.let(null, "pos", posName);
		this.jumpPrevFailureJump();
		this.popFailureJumpPoint(e);
		this.let(null, "pos", posName);
		endFailureJumpPoint();
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		String posName = "pos" + this.fID;
		this.let("long", posName, "pos");
		e.inner.visit(this);
		this.let(null, "pos", posName);
	}

	@Override
	public void visitOptional(ParsingOption e) {
		this.pushFailureJumpPoint();
		e.inner.visit(this);
		this.popFailureJumpPoint(e);
		endFailureJumpPoint();
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		this.pushFailureJumpPoint();
		writeLine("while(True): ");
		this.openIndent();
		e.inner.visit(this);
		this.closeIndent();
		this.popFailureJumpPoint(e);
		endFailureJumpPoint();
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		int fid = this.fID;
		String labelName = "EXIT_CHOICE" + fid;
		String posName = "pos" + this.fID;
		this.let("long", posName, "pos");
		for(int i = 0; i < e.size() - 1; i++) {
			this.pushFailureJumpPoint();
			e.get(i).visit(this);
			this.writeLine("goto " + labelName + "");
			this.popFailureJumpPoint(e.get(i));
			this.let(null, "pos", posName);
			endFailureJumpPoint();
		}
		e.get(e.size() - 1).visit(this);
		closeIndent();
		this.writeLine(labelName + ": ");
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		for(int i = 0; i < e.prefetchIndex; i++) {
			e.get(i).visit(this);
		}
		this.pushFailureJumpPoint();
		String leftName = "left" + this.fID;
		String labelName = "EXIT_NEW" + this.fID;
		openIndent();
		writeLine("ParsingObject" + leftName+ " = NULL");
		writeLine("Parsing_setObject(c, &" + leftName+ ", c.left)");
		writeLine("int tid = Parsing_markLogStack(c)");
		writeLine("Parsing_setObject(c, &c.left, Parsing_newObject(c, pos))");
		if(e.leftJoin) {
			//context.lazyJoin(context.left);
			//context.lazyLink(newnode, 0, context.left);
			writeLine("Parsing_lazyJoin(c, left)");
			writeLine("Parsing_lazyLink(c, c.left, 0, left)");
		}
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		writeLine("c.left.end_pos = pos");
		writeLine("Parsing_setObject(c, &" + leftName+ ", NULL)");
		writeLine("goto " + labelName + "");

		this.popFailureJumpPoint(e);
		writeLine("Parsing_setObject(c, &c.left, " + leftName+ ")");
		writeLine("Parsing_setObject(c, &" + leftName+ ", NULL)");
		this.jumpFailureJump();
		endFailureJumpPoint();
		closeIndent();
		writeLine(labelName + ": ");
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		this.pushFailureJumpPoint();
		int uid = this.fID;
		String leftName = "left" + uid;
		String labelName = "EXIT_CONNECTOR" + uid;
		openIndent();
		writeLine("ParsingObject" + leftName+ " = NULL");
		writeLine("Parsing_setObject(c, &" + leftName+ ", c.left)");
		writeLine("int tid = Parsing_markLogStack(c)");
		e.inner.visit(this);
		writeLine("Parsing_commitLog(c, tid)");
		writeLine("Parsing_lazyLink(c, " + leftName + ", c.left)");
		writeLine("Parsing_setObject(c, &c.left, " + leftName+ ")");
		writeLine("Parsing_setObject(c, &" + leftName+ ", NULL)");
		writeLine("goto " + labelName + "");
		this.popFailureJumpPoint(e);
		writeLine("Parsing_abortLog(c, tid)");
		writeLine("Parsing_setObject(c, &c.left, " + leftName+ ")");
		writeLine("Parsing_setObject(c, &" + leftName+ ", NULL)");
		this.jumpFailureJump();
		endFailureJumpPoint();
		writeLine(labelName + ":");
		closeIndent();
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		writeLine("c.left.tag = \"#" + e.tag + "\"");
	}

	@Override
	public void visitValue(ParsingValue e) {
		writeLine("c.left.value = \"" + e.value + "\"");
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitDef(ParsingDef e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitApply(ParsingApply e) {
		// TODO Auto-generated method stub
		
	}
//
//	@Override
//	public void visitPermutation(ParsingPermutation e) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public void visitIs(ParsingIs e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitScan(ParsingScan e) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

	@Override
	public void visitRepeat(ParsingRepeat e) {
		// TODO 自動生成されたメソッド・スタブ
		
	}

}

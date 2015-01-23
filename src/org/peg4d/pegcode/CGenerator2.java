package org.peg4d.pegcode;

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

public class CGenerator2 extends GrammarGenerator {

	@Override
	public String getDesc() {
		return "c";
	}
	
	String indent = "";

	protected void writeLine(String s) {
		formatString("\n" + indent + s);
	}
	
	protected void openIndent() {
		writeLine("{");
		indent = "  " + indent;
	}
	protected void closeIndent() {
		indent = indent.substring(2);
		writeLine("}");
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
	}
	void popFailureJumpPoint(ParsingRule r) {
		writeLine("CATCH_FAILURE" + fLabel.id + ":" + "/* " + r.localName + " */");
		fLabel = fLabel.prev;
	}
	void popFailureJumpPoint(ParsingExpression e) {
		writeLine("CATCH_FAILURE" + fLabel.id + ":" + "/* " + " */");
		fLabel = fLabel.prev;
	}
	void popFailureJumpPointEmptyState(ParsingExpression e) {
		writeLine("CATCH_FAILURE" + fLabel.id + ": ;;" + "/* " + " */");
		fLabel = fLabel.prev;
	}
	void jumpFailureJump() {
		writeLine("goto CATCH_FAILURE" + fLabel.id + ";");
	}
	void jumpPrevFailureJump() {
		writeLine("goto CATCH_FAILURE" + fLabel.prev.id + ";");
	}
	void consume(int length) {
		writeLine("P4D_consume(&c->pos, " + length + ");");
	}
	void let(String type, String var, String expr) {
		if(type != null) {
			writeLine(type + " " + var + " = " + expr + ";");		
		}
		else {
			writeLine("" + var + " = " + expr + ";");
		}
	}
	void letO(String type, String var, String expr) {
		if(type != null) {
			writeLine(type + " " + var + " = NULL;");		
			writeLine("P4D_setObject(c, &" + var + ", " + expr + ");");		
		}
		else {
			writeLine("P4D_setObject(c, &" + var + ", " + expr + ");");		
		}
	}
	void gotoLabel(String label) {
		writeLine("goto " + label + ";");
	}
	void exitLabel(String label) {
		writeLine(label + ": ;; /* <- this is required for avoiding empty statement */");
	}

	String funcName(String symbol) {
		return "p" + symbol;
	}
	
	void generateMainFunction() {
		writeLine("int main(int argc, char * const argv[])");
		writeLine("{");
		writeLine("    struct ParsingContext context;");
		writeLine("    const char *orig_argv0 = argv[0];");
		writeLine("    const char *input_fileName = NULL;");
		writeLine("    const char *output_type = NULL;");
		writeLine("    int opt;");
		writeLine("    while ((opt = getopt(argc, argv, \"p:t:\")) != -1) {");
		writeLine("        switch (opt) {");
		writeLine("            case 'p':");
		writeLine("                input_fileName = optarg;");
		writeLine("                break;");
		writeLine("            case 't':");
		writeLine("                output_type = optarg;");
		writeLine("                break;");
		writeLine("            default: /* '?' */");
		writeLine("                peg_usage(orig_argv0);");
		writeLine("        }");
		writeLine("    }");
		writeLine("    ParsingContext_Init(&context, input_fileName);");
		writeLine("    fprintf(stderr, \"generatedParserMode\\n\");");
		writeLine("    fprintf(stderr, \"input_file:%s\\n\", input_fileName);");
		writeLine("    if(output_type == NULL || !strcmp(output_type, \"pego\")) {");
		writeLine("        if (pFile(&context)) {");
		writeLine("            peg_error(\"parse_error\");");
		writeLine("        }");
		writeLine("        P4D_commitLog(&context, 0, context.left);");
		writeLine("        dump_pego(context.left, context.inputs, 0);");
		writeLine("        ParsingContext_Dispose(&context);");
		writeLine("    }");
		writeLine("    else if(!strcmp(output_type, \"stat\")) {");
		writeLine("        for (int i = 0; i < 20; i++) {");
		writeLine("            ParsingContext_Init(&context, input_fileName);");
		writeLine("            clock_t start = clock();");
		writeLine("            if (pFile(&context)) {");
		writeLine("                peg_error(\"parse_error\");");
		writeLine("            }");
		writeLine("            P4D_commitLog(&context, 0, context.left);");
		writeLine("            clock_t end = clock();");
		writeLine("            fprintf(stderr, \"EraspedTime: %lf\\n\", (double)(end - start) / CLOCKS_PER_SEC);");
		writeLine("            ParsingContext_Dispose(&context);");
		writeLine("        }");
		writeLine("    }");
		writeLine("    return 0;");
		writeLine("}");
	}
	
	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.localName.startsWith("\"")) {
				this.writeLine("int " + funcName(r.localName) + "(ParsingContext c);");
			}
		}
		this.generateMainFunction();
		for(ParsingRule r: peg.getRuleList()) {
			if (!r.localName.startsWith("\"")) {
				this.formatRule(r, sb);
			}
		}
		this.formatFooter();
		System.out.println(sb.toString());
	}

	@Override
	public void formatHeader() {
		writeLine("#include \"parsing.h\"");
		writeLine("#include <time.h>");
	}


	@Override
	public void visitRule(ParsingRule e) {
		this.initFailureJumpPoint();
		writeLine("int " + funcName(e.localName) + "(ParsingContext c)");
		openIndent();
		let("long", "pos", "c->pos");

		this.pushFailureJumpPoint();
		e.expr.visit(this);
		//let(null, "c->pos", "pos");
		writeLine("return 0;");
		
		this.popFailureJumpPoint(e);
		let(null, "c->pos", "pos");
		writeLine("return -1;");
		closeIndent();
		writeLine("");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		writeLine("if(" + funcName(e.ruleName) + "(c))");
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
		writeLine("if(c->inputs[c->pos] != " + e.byteChar + ")");
		openIndent();
		jumpFailureJump();
		closeIndent();
		consume(1);
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		writeLine("if(!(c->inputs[c->pos] >= " + e.startByteChar + " && c->inputs[c->pos] <= " + e.endByteChar + "))");
		openIndent();
		jumpFailureJump();
		closeIndent();
		consume(1);
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeLine("if(c->inputs[c->pos] == 0)");
		openIndent();
		jumpFailureJump();
		closeIndent();
		consume(1);
	}

	@Override
	public void visitString(ParsingString e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitNot(ParsingNot e) {
		this.openIndent();
		this.pushFailureJumpPoint();
		String posName = "pos" + this.fID;
		let("long", posName, "c->pos");
		e.inner.visit(this);
		let(null, "c->pos", posName);
		this.jumpPrevFailureJump();
		this.popFailureJumpPoint(e);
		let(null, "c->pos", posName);
		this.closeIndent();
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		this.openIndent();
		String posName = "pos" + this.fID;
		let("long", posName, "c->pos");
		e.inner.visit(this);
		let(null, "c->pos", posName);
		this.closeIndent();
	}

	@Override
	public void visitOptional(ParsingOption e) {
		this.pushFailureJumpPoint();
		String labelName = "EXIT_OPTIONAL" + this.fID;
		String posName = "pos" + this.fID;
		let("long", posName, "c->pos");
		e.inner.visit(this);
		this.gotoLabel(labelName);
		this.popFailureJumpPoint(e);
		let(null, "c->pos", posName);
		this.exitLabel(labelName);
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		this.pushFailureJumpPoint();
		String posName = "pos" + this.fID;
		String pposName = "ppos" + this.fID;
		let("long", posName, "c->pos");
		let("long", pposName, "-1");
		writeLine("while(" + pposName + " < " + posName + ")");
		this.openIndent();
		let(null, posName, "c->pos");
		e.inner.visit(this);
		let(null, pposName, posName);
		let(null, posName, "c->pos");
		this.closeIndent();
		this.popFailureJumpPointEmptyState(e);
		let(null, "c->pos", posName);
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
		String posName = "pos" + fid;
		openIndent();
		let("long", posName, "c->pos");
		for(int i = 0; i < e.size(); i++) {
			this.pushFailureJumpPoint();
			e.get(i).visit(this);
			this.gotoLabel(labelName);
			this.popFailureJumpPoint(e.get(i));
			let(null, "c->pos", posName);
		}
		jumpFailureJump();
		closeIndent();
		this.exitLabel(labelName);
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
		this.letO("ParsingObject", leftName, "P4D_newObject(c, c->pos)");
		this.let(null, "*" + leftName, "*c->left");
		this.let("int", "tid", "P4D_markLogStack(c)");
		this.letO(null, "c->left", "P4D_newObject(c, c->pos)");
		if(e.leftJoin) {
			//context.lazyJoin(context.left);
			//context.lazyLink(newnode, 0, context.left);
			//writeLine("ParsingObject", leftName, "P4D_setObject(c, &c->left, P4D_newObject(c, c->pos))");
			writeLine("P4D_lazyJoin(c, "+ leftName +");");
			writeLine("P4D_lazyLink(c, c->left, 0, " + leftName + ");");
		}
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		writeLine("c->left->end_pos = c->pos;");
		this.letO(null, leftName, "NULL");
		this.gotoLabel(labelName);

		this.popFailureJumpPoint(e);
		writeLine("P4D_abortLog(c, tid);");
		this.letO(null, "c->left", leftName);
		this.letO(null, leftName, "NULL");
		this.jumpFailureJump();
		closeIndent();
		this.exitLabel(labelName);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		this.pushFailureJumpPoint();
		int uid = this.fID;
		String leftName = "left" + uid;
		String labelName = "EXIT_CONNECTOR" + uid;
		String markName = "mark" + uid;
		openIndent();
		this.letO("ParsingObject", leftName, "c->left");
		this.let("int", markName, "P4D_markLogStack(c);");
		e.inner.visit(this);
		writeLine("P4D_commitLog(c, " + markName + ", c->left);");
		writeLine("P4D_lazyLink(c, " + leftName + "," + e.index + " , c->left);");
		this.letO(null, "c->left", leftName);
		this.letO(null, leftName, "NULL");
		this.gotoLabel(labelName);

		this.popFailureJumpPoint(e);
		writeLine("P4D_abortLog(c, " + markName + ");");
		this.letO(null, "c->left", leftName);
		this.letO(null, leftName, "NULL");
		this.jumpFailureJump();
		closeIndent();
		this.exitLabel(labelName);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		writeLine("c->left->tag = \"#" + e.tag + "\";");
	}

	@Override
	public void visitValue(ParsingValue e) {
		writeLine("c->left->value = \"" + e.value + "\";");
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

	@Override
	public void visitMatch(ParsingMatch e) {
		// TODO Auto-generated method stub
		
	}

}

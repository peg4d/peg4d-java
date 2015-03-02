package nez.expr;

import java.util.TreeMap;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

import org.peg4d.ParsingSource;

public class AnyChar extends Terminal {
	AnyChar(SourcePosition s) {
		super(s);
	}
	@Override
	public String getPredicate() {
		return "any";
	}
	@Override
	public String getInterningKey() { 
		return ".";
	}
	@Override
	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferNodeTransition(UMap<String> visited) {
		return NodeTransition.BooleanType;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public short acceptByte(int ch) {
		return (ch == ParsingSource.EOF) ? Reject : Accept;
	}
	@Override
	public boolean match(SourceContext context) {
		if(context.charAt(context.pos) != -1) {
			int len = context.charLength(context.pos);
			context.consume(len);
			return true;
		}
		return context.failure2(this);
	}
}
package nez.expr;

import org.peg4d.Utils;

import nez.util.StringUtils;

public class NezFormatter extends ExpressionVisitor {
	private StringBuilder sb = null;
	private final static String NewIndent = "\n\t";

	public NezFormatter() {
		this(new StringBuilder());
	}
	
	public NezFormatter(StringBuilder sb) {
		this.sb = sb;
	}
	
	public final String format(Expression e) {
		visit(e);
		return sb.toString();
	}

	public void visitRule(Rule rule) {
		Expression e = rule.getExpression();
		sb.append(rule.getLocalName());
		sb.append(NewIndent);
		sb.append("= ");
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					sb.append(NewIndent);
					sb.append("/ ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
	}	
	
	public void visitEmpty(Empty e) {
		sb.append("''");
	}

	
	public void visitFailure(Failure e) {
		sb.append("!''/*failure*/");
	}

	public void visitNonTerminal(NonTerminal e) {
		sb.append(e.getLocalName());
	}
	
	public void visitByte(ByteChar e) {
		sb.append(StringUtils.stringfyByte(e.byteChar));
	}

//	public void visitByteRange(ByteRange e) {
//		sb.append("[");
//		sb.append(GrammarGenerator.stringfyByte2(e.startByteChar));
//		sb.append("-");
//		sb.append(GrammarGenerator.stringfyByte2(e.endByteChar));
//		sb.append("]");
//	}
	
	public void visitAny(AnyChar e) {
		sb.append(".");
	}
	
	public void visitTagging(Tagging e) {
		sb.append("#");
		sb.append(e.tag.toString());
	}
	
	public void visitValue(Replace e) {
		sb.append(StringUtils.quoteString('`', e.value, '`'));
	}
	
	protected void format(String prefix, Unary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(/*e.inner instanceof String ||*/ e.inner instanceof NonTerminal || e.inner instanceof New) {
			this.visit(e.inner);
		}
		else {
			sb.append("(");
			this.visit(e.inner);
			sb.append(")");
		}
		if(suffix != null) {
			sb.append(suffix);
		}
	}
	
	public void visitOption(Option e) {
		this.format( null, e, "?");
	}
	
	public void visitRepetition(Repetition e) {
		this.format(null, e, "*");
	}
	
	public void visitAnd(And e) {
		this.format( "&", e, null);
	}

	
	public void visitNot(Not e) {
		this.format( "!", e, null);
	}

	
	public void visitConnector(Link e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		this.format(predicate, e, null);
	}

	protected void appendSequence(ExpressionList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			int n = appendAsString(l, i);
			if(n > i) {
				i = n;
				continue;
			}
			Expression e = l.get(i);
			if(e instanceof Choice || e instanceof Sequence) {
				sb.append("( ");
				visit(e);
				sb.append(" )");
				continue;
			}
			visit(e);
		}
	}

	private int appendAsString(ExpressionList l, int start) {
		int end = l.size();
		String s = "";
		for(int i = start; i < end; i++) {
			Expression e = l.get(i);
			if(e instanceof ByteChar) {
				char c = (char)(((ByteChar) e).byteChar);
				if(c >= ' ' && c < 127) {
					s += c;
					continue;
				}
			}
			end = i;
			break;
		}
		if(s.length() > 1) {
			sb.append(Utils.quoteString('\'', s, '\''));
		}
		return end - 1;
	}
	
	
	public void visitSequence(Sequence e) {
		this.appendSequence( e);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			visit(e.get(i));
		}
	}

	public void visitNew(New e) {
		sb.append("{ ");
		this.appendSequence(e);
		sb.append(" }");
	}

	public void visitNewLeftLink(NewLeftLink e) {
		sb.append("{@ ");
		this.appendSequence(e);
		sb.append(" }");
	}

	@Override
	public void visitExpression(Expression e) {
		sb.append("<");
		sb.append(e.getPredicate());
		for(Expression se : e) {
			sb.append(" ");
			visit(se);
		}
		sb.append(">");
	}


}


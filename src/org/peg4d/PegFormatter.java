package org.peg4d;


public class PegFormatter {

	public String getDesc() {
		return "PEG4d ";
	}
	
	public void formatHeader(UStringBuilder sb) {
		
	}

	public void formatFooter(UStringBuilder sb) {
		
	}
	
	public void formatRule(UStringBuilder sb, String ruleName, Peg e) {
		sb.appendNewLine(ruleName);
		sb.append(this.getNewLine(), this.getSetter(), " ");
		if(e instanceof PegChoice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					sb.append(this.getNewLine(), "/ ");
				}
				e.get(i).stringfy(sb, this);
			}
		}
		else {
			e.stringfy(sb, this);
		}
		sb.append(getNewLine(), this.getSemiColon());
	}

	public String getNewLine() {
		return "\n\t";
	}

	public String getSetter() {
		return "=";
	}

	public String getSemiColon() {
		return ";";
	}

	public void formatNonTerminal(UStringBuilder sb, PegNonTerminal e) {
		sb.append(e.symbol);
	}

	public void formatString(UStringBuilder sb, PegString e) {
		char quote = '\'';
		if(e.text.indexOf("'") != -1) {
			quote = '"';
		}
		sb.append(UCharset._QuoteString(quote, e.text, quote));
	}

	public void formatCharacter(UStringBuilder sb, PegCharacter e) {
		sb.append("[" + e.charset, "]");
	}

	public void formatAny(UStringBuilder sb, PegAny e) {
		sb.append(".");
	}

	public void formatTagging(UStringBuilder sb, PegTagging e) {
		sb.append(e.symbol);
	}
	
	public void formatMessage(UStringBuilder sb, PegMessage e) {
		sb.append("`", e.symbol, "`");
	}

	public void formatIndent(UStringBuilder sb, PegIndent e) {
		sb.append("indent");
	}

	public void formatIndex(UStringBuilder sb, PegIndex e) {
		sb.appendInt(e.index);
	}

	
	protected void format(UStringBuilder sb, String prefix, PegUnary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(e.inner instanceof PegTerm || e.inner instanceof PegNewObject) {
			e.inner.stringfy(sb, this);
		}
		else {
			sb.append("(");
			e.inner.stringfy(sb, this);
			sb.append(")");
		}
		if(suffix != null) {
			sb.append(suffix);
		}
	}

	public void formatOptional(UStringBuilder sb, PegOptional e) {
		this.format(sb,  null, e, "?");
	}

	public void formatRepeat(UStringBuilder sb, PegRepeat e) {
		if(e.atleast == 1) {
			this.format(sb,  null, e, "+");
		}
		else {
			this.format(sb, null, e, "*");
		}
	}

	public void formatAnd(UStringBuilder sb, PegAnd e) {
		this.format(sb,  "&", e, null);
	}

	public void formatNot(UStringBuilder sb, PegNot e) {
		this.format(sb,  "!", e, null);
	}

	public void formatSetter(UStringBuilder sb, PegSetter e) {
		this.format(sb,  null, e, "^");
		if(e.index != -1) {
			sb.appendInt(e.index);
		}
	}

	public void formatExport(UStringBuilder sb, PegExport e) {
		sb.append("<| ");
		if(e.inner instanceof PegNewObject) {
			this.format(sb, (PegNewObject)e.inner);
		}
		else {
			e.inner.stringfy(sb, this);
		}
		sb.append(" |>");
	}

	protected void format(UStringBuilder sb, PegList l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			Peg e = l.get(i);
			if(e instanceof PegChoice || e instanceof PegSequence) {
				sb.append("( ");
				e.stringfy(sb, this);
				sb.append(" )");
			}
			else {
				e.stringfy(sb, this);
			}
		}
	}

	public void formatSequence(UStringBuilder sb, PegSequence e) {
		//sb.append("( ");
		this.format(sb,  e);
		//sb.append(" )");
	}

	public void formatChoice(UStringBuilder sb, PegChoice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			e.get(i).stringfy(sb, this);
		}
	}

	public void formatNewObject(UStringBuilder sb, PegNewObject e) {
		if(e.leftJoin) {
			sb.append("<{^ ");
		}
		else {
			sb.append("<{ ");
		}
		this.format(sb, e);
		sb.append(" }>");
	}
	

}
package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.ReportLevel;
import nez.util.StringUtils;

public class Tagging extends Unconsumed {
	Tag tag;
	Tagging(SourcePosition s, Tag tag) {
		super(s);
		this.tag = tag;
	}
	Tagging(SourcePosition s, String name) {
		this(s, Tag.tag(name));
	}
	@Override
	public String getPredicate() {
		return "tag " + StringUtils.quoteString('"', tag.name, '"');
	}
	@Override
	public String getInterningKey() {
		return "#" + this.tag.toString();
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected tagging");
			return Factory.newEmpty(this.s);
		}
		return this;
	}
	@Override
	public Expression removeNodeOperator() {
		return Factory.newEmpty(null);
	}
	@Override
	public boolean match(SourceContext context) {
		context.left.setTag(this.tag);
		return true;
	}
}
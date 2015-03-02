package nez.expr;

import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.StringUtils;

public class Replace extends Unconsumed {
	public String value;
	Replace(SourcePosition s, String value) {
		super(s);
		this.value = value;
	}
	@Override
	public String getPredicate() {
		return "replace " + StringUtils.quoteString('"', value, '"');
	}
	@Override
	public String getInterningKey() {
		return "`" + this.value;
	}
	@Override
	public Expression checkNodeTransition(NodeTransition c) {
		if(c.required != NodeTransition.OperationType) {
			this.report(ReportLevel.warning, "unexpected replace and ignored");
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
		context.left.setValue(this.value);
		return true;
	}
}
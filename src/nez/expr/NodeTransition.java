package nez.expr;

public class NodeTransition {
	public final static int Undefined         = -1;
	public final static int BooleanType       = 0;
	public final static int ObjectType        = 1;
	public final static int OperationType     = 2;
	public int required = BooleanType;
}

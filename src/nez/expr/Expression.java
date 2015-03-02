package nez.expr;
import java.util.AbstractList;
import java.util.TreeMap;

import nez.Recognizer;
import nez.ast.SourcePosition;
import nez.util.ReportLevel;
import nez.util.UList;
import nez.util.UMap;

public abstract class Expression extends AbstractList<Expression> implements Recognizer {
	protected Expression(SourcePosition s) {
		this.s = s;
		this.internId = 0;
		this.matcher = this;
	}
	
	public abstract String getPredicate();
	
	int    internId   = 0;
	SourcePosition s      = null;

	final boolean isInterned() {
		return (this.internId > 0);
	}
	
	final Expression intern() {
		return Factory.intern(this);
	}

	public abstract String getInterningKey();
	
	public boolean isAlwaysConsumed() {
		return this.checkAlwaysConsumed(null, null);
	}
	public int inferNodeTransition() {
		return this.inferNodeTransition(null);
	}

	public abstract boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack);
	public abstract int inferNodeTransition(UMap<String> visited);
	public abstract Expression checkNodeTransition(NodeTransition c);
	public abstract Expression removeNodeOperator();
	public abstract Expression removeFlag(TreeMap<String, String> undefedFlags);
	
	public static boolean hasReachableFlag(Expression e, String flagName, UMap<String> visited) {
		if(e instanceof WithFlag) {
			if(flagName.equals(((WithFlag) e).flagName)) {
				return false;
			}
		}
		for(Expression se : e) {
			if(hasReachableFlag(se, flagName, visited)) {
				return true;
			}
		}
		if(e instanceof IfFlag) {
			return flagName.equals(((IfFlag) e).flagName);
		}
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal)e;
			String un = ne.getUniqueName();
			if(!visited.hasKey(un)) {
				visited.put(un, un);
				Rule r = ne.getRule();
				return hasReachableFlag(r.body, flagName, visited);
			}
		}
		return false;
	}

	public static boolean hasReachableFlag(Expression e, String flagName) {
		return hasReachableFlag(e, flagName, new UMap<String>());
	}

	final static short Unconsumed = 0;
	final static short Accept = 1;
	final static short Reject = 2;
	public abstract short acceptByte(int ch);
	
	public Recognizer matcher;
	
	public final void report(ReportLevel level, String msg) {
//		if(WarningLevel == 0 && level.compareTo(ReportLevel.error) != 0) {
//			return;
//		}
//		if(this.po != null) {
//			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
//		}
//		else {
//			System.out.println("" + level.toString() + ": " + msg + " in " + this);
//		}
	}
	
	@Override
	public String toString() {
		return new NezFormatter().format(this);
	}

	public final UList<Expression> toList() {
		UList<Expression> l = new UList<Expression>(new Expression[this.size()]);
		for(Expression e : this) {
			l.add(e);
		}
		return l;
	}

}

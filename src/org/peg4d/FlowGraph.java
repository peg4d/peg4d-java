//package org.peg4d;
//
//class FlowGraph  {
//	public final static FlowGraph TAIL = new FlowGraph(null, null, null);
//	String key;
//	
//	ParsingExpression e;
//	FlowGraph inner;
//	FlowGraph next;
//	FlowGraph choice = null;
//	boolean isRecursion = false;
//	
//	FlowGraph(ParsingExpression e, FlowGraph inner, FlowGraph next) {
//		this.e = e;
//		this.inner = inner;
//		this.next = next;
//	}
//	
//	void dump() {
//		stack(new UList<ParsingExpression>(new ParsingExpression[256]), null, true);
//	}
//	
//	void stack(UList<ParsingExpression> stack, UList<ParsingExpression[]> list, boolean mainPath) {
//		int pos = stack.size();
////		System.out.print("N<" + note(e) + "> ");
//		stack.add(e);
//		if(this.inner != null) {
//			if(e instanceof PNonTerminal /* && !checkRecursion(e, stack) */) {
//				//buffer(stack, list);
//			}
//			else {
//				this.inner.stack(stack, list, false);
//			}
//		}
//		if(this.next != null) {
//			this.next.stack(stack, list, mainPath);
//		}
//		else {
//			if(mainPath) {
//				buffer(stack, list);
//			}
//		}
//		if(this.choice != null) {
//			System.out.println("choice: " + this.choice.e);
//			stack.clear(pos);
//			this.choice.stack(stack, list, true);		
//		}
//	}
//
//	private void buffer(UList<ParsingExpression> stack, UList<ParsingExpression[]> list) {
//		if(list == null) {
//			System.out.print("\t");
//			for(int i = 0; i < stack.size(); i++) {
//				System.out.print(note(stack.ArrayValues[i]) + " ");
//			}
//			System.out.println("");
//			System.out.println("");
//		}
//	}
//
//	private String note(ParsingExpression e) {
//		if(e instanceof PNonTerminal) {
//			return ((PNonTerminal) e).getUniqueName();
//		}
//		if(e.size() == 0) {
//			return e.toString();
//		}
//		return e.getClass().getSimpleName();
//	}
//
//	private boolean checkRecursion(ParsingExpression e, UList<ParsingExpression> stack) {
//		for(int i = 0; i < stack.size() - 1; i++) {
//			if(stack.ArrayValues[i] == e) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	int checkLeftRecursion(ParsingRule rule, String uName, UList<ParsingExpression> stack, int consume) {
//		int length = consume;
//		if(e instanceof ParsingAtom) {
//			consume += 1;
//		}
//		if(e instanceof PNonTerminal) {
//			String n = ((PNonTerminal) e).getUniqueName();
//			if(n.equals(uName) && consume == 0) {
//				//rule.reportError("left recursion: " + ((PNonTerminal) e).symbol);
//			}
//			if(!checkRecursion(e, stack)) {
//				int pos = stack.size();
//				stack.add(e);
//				consume = this.inner.checkLeftRecursion(rule, uName, stack, consume);
//				stack.clear(pos);
//			}
//		}
//		else {
//			if(this.inner != null) {
//				consume = this.inner.checkLeftRecursion(rule, uName, stack, consume);
//			}
//		}
//		if(this.next != null) {
//			consume = this.next.checkLeftRecursion(rule, uName, stack, consume);
//		}
//		if(this.choice != null) {
//			int nc = this.choice.checkLeftRecursion(rule, uName, stack, length);
//			if(nc < consume) {
//				consume = nc;
//			}
//		}
//		return consume;
//	}
//
//	boolean checkObjectType(ParsingRule rule, UList<ParsingExpression> stack) {
//		boolean foundObjectType = false;
//		if(e instanceof PConstructor) {
//			foundObjectType = true;
//		}
//		if(e instanceof PNonTerminal) {
//			if(!checkRecursion(e, stack)) {
//				int pos = stack.size();
//				stack.add(e);
//				if(this.inner.checkObjectType(rule, stack)) {
//					foundObjectType = true;
//				}
//				stack.clear(pos);
//			}
//		}
//		else {
//			if(this.inner != null && !(e instanceof PConnector)) {
//				if(this.inner.checkObjectType(rule, stack)) {
//					foundObjectType = true;
//				}
//			}
//		}
//		if(this.next != null) {
//			if(this.next.checkObjectType(rule, stack)) {
//				foundObjectType = true;
//			}
//		}
//		if(this.choice != null) {
//			boolean cf = this.choice.checkObjectType(rule, stack);
//			if(cf != foundObjectType) {
//				foundObjectType = true;
//			}
//		}
//		return foundObjectType;
//	}
//
////	void checkExpression(PegRule rule, UList<ParsingExpression> stack) {
////		if(e instanceof PNot) {
////			checkNot((PNot)e, inner);
////		}
////		if(this.inner != null && !(e instanceof PNonTerminal)) {
////			this.inner.checkExpression(rule, stack);
////		}
////		if(this.next != null) {
////			this.next.checkExpression(rule, stack);
////		}
////		if(this.choice != null) {
////			this.choice.checkExpression(rule, stack);
////		}
////	}
////	
////	void checkNot(PegRule rule, PNot e, FlowGraph inner,  UList<ParsingExpression> stack) {
////		stack.clear(0);
////		boolean objectType = inner.checkObjectType(null, stack);
////		if(objectType) {
////			rule.reportWarning(e, "object type");
////		}
////	}
//	
//	
//	
//	// static
//
//	public static FlowGraph makeFlowGraph(String uniqueName, ParsingExpression e, UMap<FlowGraph> buffer) {
//		FlowGraph graph = new FlowGraph(e, null, null);
//		if(uniqueName != null && buffer != null) {
//			graph = buffer.get(uniqueName);
//			if(graph != null) {
//				if(graph.key == null) {
//					return graph;
//				}
//			}
//			else {
//				graph = new FlowGraph(e, null, null);				
//			}
//			buffer.put(uniqueName, graph);
//		}
//		else {
//			graph = new FlowGraph(e, null, null);
//		}
//		replaceGraph(graph, makeGraph(e, null, buffer));
//		if(buffer != null) {
//			checkAllRule(buffer);			
//		}
//		return graph;
//	}
//
//	private static void replaceGraph(FlowGraph refone, FlowGraph newone) {
//		refone.e = newone.e;
//		refone.inner = newone.inner;
//		refone.next = newone.next;
//		refone.choice = newone.choice;
//		refone.key = null;
//	}
//
//	private static void checkAllRule(UMap<FlowGraph> rule) {
//		boolean allChecked = false;
//		while(!allChecked) {
//			allChecked = true;
//			UList<String> l = rule.keys();
//			for(int i = 0; i < l.size(); i++) {
//				FlowGraph refone = rule.get(l.ArrayValues[i]);
//				if(refone.key != null) {
//					allChecked = false;
//					FlowGraph newone = makeGraph(refone.e, null, rule);
//					replaceGraph(refone, newone);
//				}
//			}
//		}
//	}
//	
//	private static FlowGraph makeGraph(ParsingExpression e, FlowGraph tail, UMap<FlowGraph> rule) {
//		if(e instanceof PSequence) {
//			for(int i = e.size() - 1; i >=0; i--) {
//				tail = makeGraph(e.get(i), tail, rule);
//			}
//			return tail;
//		}
//		if(e instanceof PConstructor) {
//			FlowGraph inner = null;
//			for(int i = e.size() - 1; i >=0; i--) {
//				inner = makeGraph(e.get(i), inner, rule);
//			}
//			return new FlowGraph(e, inner, tail);
//		}
//		if(e instanceof PChoice) {
//			FlowGraph choice = null;
//			for(int i = e.size() - 1; i >=0; i--) {
//				FlowGraph p = makeGraph(e.get(i), tail, rule);				
//				p.choice = choice;
//				choice = p;
//			}
//			return choice;
//		}
//		if(e instanceof PNonTerminal) {
//			String key = ((PNonTerminal) e).getUniqueName();
//			FlowGraph inner = rule.get(key);
//			if(inner == null) {
//				inner = new FlowGraph(((PNonTerminal) e).getNext(), null, null);
//				inner.key = key;
//				rule.put(key, inner);
//			}
//			return new FlowGraph(e, inner, tail);
//		}
//		if(e instanceof ParsingOperation) {
//			return new FlowGraph(e, makeGraph(((ParsingOperation) e).inner, null, rule), tail);
//		}
//		if(e instanceof ParsingUnary) {
//			return new FlowGraph(e, makeGraph(((ParsingUnary) e).inner, null, rule), tail);
//		}
//		return new FlowGraph(e, null, tail);
//	}
//}
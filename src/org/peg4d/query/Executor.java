package org.peg4d.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingTag;
import org.peg4d.query.Path.Segment;

public class Executor extends QueryVisitor<Object, ParsingObject> {
	private final Map<String, QueryFunction> funcMap;

	public Executor() {
		this.funcMap = new HashMap<>();

		this.funcMap.put("count", new QueryFunction() {
			@SuppressWarnings("unchecked")
			@Override
			public Object invoke(List<Object> argList) {
				List<ParsingObject> arg = (List<ParsingObject>) argList.get(0);
				return arg.size();
			}
		});
	}

	/**
	 * 
	 * @param queryTree
	 * not null
	 * @param targetObject
	 * not null
	 * @return
	 * @throws QueryExecutionException
	 */
	public RList execQuery(ParsingObject queryTree, ParsingObject targetObject) 
			throws QueryExecutionException {
		assert queryTree != null;
		assert targetObject != null;
		ParsingObject dummyRoot = Helper.dummyRoot(targetObject);
		try {
			RList resultList = this.dispatchAndCast(queryTree, dummyRoot, RList.class);
			if(resultList == null) {
				throw new QueryExecutionException("illegal query:" + System.lineSeparator() + queryTree);
			}
			return resultList;
		}
		catch(IllegalArgumentException e) {
			QueryExecutionException.propagate(e);
		}
		return null;
	}

	@Override
	public RList visitSelect(ParsingObject queryTree, ParsingObject data) {
		ParsingObject fromTree = this.getChildAt(queryTree, "from");
		ParsingObject whereTree = this.getChildAt(queryTree, "where");

		RList resultList = new RList();
		// select 
		if(fromTree == null && whereTree == null) {
			Object tree = this.dispatch(queryTree.get(0), data);
			resultList.addAndFlat(tree);
			return resultList;
		}

		// select [from, (where)]
		if(fromTree != null) {
			@SuppressWarnings("unchecked")
			List<ParsingObject> foundTreeList = this.dispatchAndCast(fromTree, data, List.class);

			if(whereTree == null) {	// select [from]
				for(ParsingObject tree : foundTreeList) {
					Object treeList = this.dispatch(queryTree.get(0), tree);
					resultList.addAndFlat(treeList);
				}
			}
			else {	// select [from, where]
				for(ParsingObject tree : foundTreeList) {
					if((boolean) this.dispatch(whereTree, tree)) {
						Object treeList = this.dispatch(queryTree.get(0), tree);
						resultList.addAndFlat(treeList);
					}
				}
			}
			return resultList;
		}
		return null;
	}
	
	private static final int TAGID_TAG = ParsingTag.tagId("tag");

	/**
	 * create path and evaluate. return List<ParsingObject>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object visitPath(ParsingObject queryTree, ParsingObject data) {
		Path path = new Path(data.getParent() == null);
		final int size = queryTree.size();
		for(int i = 0; i < size; i++) {
			ParsingObject child = queryTree.get(i);
			
			if(!child.is(TAGID_TAG)) {
				throw new QueryExecutionException("illegal path:" + System.lineSeparator() + queryTree);
			}
			String segName = this.dispatchAndCast(child.get(0), data, String.class);
			Segment segment = null;
			switch(segName) {
			case "/root":
				segment = new Path.RootSegment();
				break;
			case "/descendant": {
				child = queryTree.get(++i);
				String nextSegName = this.dispatchAndCast(child.get(0), data, String.class);
				if(nextSegName.equals("*")) {
					segment = new Path.WildCardDescendantSegment();
				}
				else {
					segment = new Path.DescendantSegment(nextSegName);
				}
				break;
			}
			case "/parent":
				segment = new Path.ParentSegment();
				break;
			case "/self":
				segment = new Path.SelfSegment();
				break;
			case "*":
				segment = new Path.WildCardSegment();
				break;
			default:
				segment = new Path.TagNameSegment(segName);
				break;
			}
			if(child.size() == 2) {
				Object index = this.dispatch(child.get(1), data);
				if(index instanceof Pair) {
					segment = new Path.RangeSegment(segment, (Pair<Integer, Integer>) index);
				}
				else if(index instanceof List) {
					segment = new Path.IndexListSegment(segment, (List<Integer>) index);
				}
				else if(index instanceof String) {
					segment = new Path.MatchSegment(segment, (String) index);
				}
				else {
					throw new RuntimeException("unsupported index type: " + index);
				}
			}
			path.addSegment(segment);
		}
		return path.apply(data);
	}

	@Override
	public Object visitError(ParsingObject queryTree, ParsingObject data) {
		throw new QueryExecutionException("invalid query:" + System.lineSeparator() + queryTree);
	}

	public static class QueryExecutionException extends RuntimeException {
		private static final long serialVersionUID = -5774942100147563362L;

		public QueryExecutionException(String message) {
			super(message);
		}

		private QueryExecutionException(Throwable cause) {
			super(cause);
		}

		public static void propagate(Throwable cause) throws QueryExecutionException {
			if(cause instanceof QueryExecutionException) {
				throw (QueryExecutionException) cause;
			}
			throw new QueryExecutionException(cause);
		}
	}

	@Override
	public Object visitFrom(ParsingObject queryTree, ParsingObject data) {
		return this.dispatch(queryTree.get(0), data);
	}

	@Override
	public Object visitWhere(ParsingObject queryTree, ParsingObject data) {
		return this.dispatch(queryTree.get(0), data);
	}

	@Override
	public Object visitAnd(ParsingObject queryTree, ParsingObject data) {
		Object left = this.dispatch(queryTree.get(0), data);
		if((left instanceof Boolean) && (Boolean) left) {
			Object right = this.dispatch(queryTree.get(1), data);
			return (right instanceof Boolean) && (Boolean) right;
		}
		return false;
	}

	@Override
	public Object visitOr(ParsingObject queryTree, ParsingObject data) {
		Object left = this.dispatch(queryTree.get(0), data);
		if((left instanceof Boolean) && (Boolean) left) {
			return true;
		}
		Object right = this.dispatch(queryTree.get(1), data);
		return (right instanceof Boolean) && (Boolean) right;
	}

	@Override
	public Object visitEQ(ParsingObject queryTree, ParsingObject data) {
		String leftStr = this.asString(this.dispatch(queryTree.get(0), data));
		String rightStr = this.asString(this.dispatch(queryTree.get(1), data));
		Number left = this.asNumber(leftStr);
		Number right = this.asNumber(rightStr);
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left == (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left == (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left == (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left == (long) right;
		}
		if(leftStr != null){
			return leftStr.equals(rightStr);
		}
		else if(rightStr != null){
			return rightStr.equals(leftStr);
		}
		return true;
	}

	@Override
	public Object visitNEQ(ParsingObject queryTree, ParsingObject data) {
		Object eq = this.visitEQ(queryTree, data);
		if(eq == null || !(eq instanceof Boolean)){
			return false;
		}
		return !((Boolean)eq).booleanValue();
	}

	@Override
	public Object visitLE(ParsingObject queryTree, ParsingObject data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left <= (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left <= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left <= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left <= (long) right;
		}
		return left == null;
	}

	@Override
	public Object visitGE(ParsingObject queryTree, ParsingObject data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left >= (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left >= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left >= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left >= (long) right;
		}
		return right == null;
	}

	@Override
	public Object visitLT(ParsingObject queryTree, ParsingObject data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left < (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left < (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left < (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left < (long) right;
		}
		return left == null && right != null;
	}

	@Override
	public Object visitGT(ParsingObject queryTree, ParsingObject data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left > (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left > (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left > (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left > (long) right;
		}
		return left != null && right == null;
	}

	@Override
	public Object visitNum(ParsingObject queryTree, ParsingObject data) {
		return this.asNumber(queryTree.getText());
	}

	@SuppressWarnings("unchecked")
	private String asString(Object value) {
		if(value == null) {
			return null;
		}
		if(value instanceof String) {
			return (String) value;
		}
		String str = null;
		if(value instanceof List) {
			List<ParsingObject> list = (List<ParsingObject>) value;
			if(list.size() == 0){
				return null;
			}
			str = list.get(0).getText();
		}
		else if(value instanceof ParsingObject) {
			str = ((ParsingObject) value).getText();
		}
		return str;
	}
	
	@SuppressWarnings("unchecked")
	private Number asNumber(Object value) {
		if(value == null) {
			return null;
		}
		if(value instanceof Number) {
			return (Number) value;
		}
		String str = this.asString(value);
		try {
			if(str.indexOf(".") == -1) {
				return Long.parseLong(str);	// as long
			}
			return Double.parseDouble(str);	// as double
		}
		catch(NumberFormatException e) {
		}
		return null;
	}

	@Override
	public Object visitName(ParsingObject queryTree, ParsingObject data) {
		return queryTree.getText();
	}

	@Override
	public Object visitRange(ParsingObject queryTree, ParsingObject data) {
		Number left = this.dispatchAndCast(queryTree.get(0), data, Number.class);
		Number right = this.dispatchAndCast(queryTree.get(1), data, Number.class);
		return new Pair<Integer, Integer>(left.intValue(), right.intValue());
	}

	@Override
	public Object visitIndex(ParsingObject queryTree, ParsingObject data) {
		final int size = queryTree.size();
		List<Integer> indexList = new ArrayList<>(size);
		for(int i = 0; i < size; i++) {
			indexList.add(this.dispatchAndCast(queryTree.get(i), data, Number.class).intValue());
		}
		return indexList;
	}

	@Override
	public Object visitCall(ParsingObject queryTree, ParsingObject data) {
		String funcName = this.dispatchAndCast(queryTree.get(0), data, String.class);
		@SuppressWarnings("unchecked")
		List<Object> argList = this.dispatchAndCast(queryTree.get(1), data, List.class);

		QueryFunction func = this.funcMap.get(funcName);
		if(func == null) {
			throw new IllegalArgumentException("undefined function: " + funcName);
		}
		return func.invoke(argList);
	}

	@Override
	public Object visitFuncName(ParsingObject queryTree, ParsingObject data) {
		return queryTree.getText();
	}

	@Override
	public Object visitArgs(ParsingObject queryTree, ParsingObject data) {
		List<Object> argList = new ArrayList<>();
		for(ParsingObject child : queryTree) {
			argList.add(this.dispatch(child, data));
		}
		return argList;
	}
	

	private String stringfyParsingObject(List<ParsingObject> obj){
		final int n = obj.size();
		if(n == 0){
			return "";
		}
		StringBuilder sBuilder = new StringBuilder();
		for(int i = 0; i < n; ++i){
			if(i > 0){
				sBuilder.append(',');
			}
			ParsingObject child = obj.get(i);
			if(child.size() == 0){
				sBuilder.append(child.getText());
			}else{
				sBuilder.append('[');
				sBuilder.append(stringfyParsingObject(child));
				sBuilder.append(']');
			}
		}
		return sBuilder.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visitString(ParsingObject queryTree, ParsingObject data) {
		StringBuilder sBuilder = new StringBuilder();
		final int size = queryTree.size();
		for(int i = 0; i < size; i++) {
			Object ret = this.dispatch(queryTree.get(i), data);
			if(ret instanceof List<?>){
				sBuilder.append(stringfyParsingObject((List<ParsingObject>)ret));
			}else{
				sBuilder.append(ret);
			}
		}
		return sBuilder.toString();
	}

	@Override
	public Object visitSegment(ParsingObject queryTree, ParsingObject data) {
		return queryTree.getText();
	}
}

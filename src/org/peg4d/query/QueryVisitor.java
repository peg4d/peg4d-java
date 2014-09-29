package org.peg4d.query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.peg4d.ParsingObject;
import org.peg4d.ParsingTag;

public abstract class QueryVisitor <R, T> {
	protected final Map<String, BiFunction<ParsingObject, T, R>> dispatchMap;

	protected QueryVisitor() {
		this.dispatchMap = new HashMap<>();

		this.dispatchMap.put("error",     this::visitError);
		this.dispatchMap.put("select",    this::visitSelect);
		this.dispatchMap.put("where",     this::visitWhere);
		this.dispatchMap.put("from",      this::visitFrom);

		this.dispatchMap.put("path",      this::visitPath);
		this.dispatchMap.put("name",      this::visitName);
		this.dispatchMap.put("range",     this::visitRange);
		this.dispatchMap.put("indexlist", this::visitIndex);

		this.dispatchMap.put("call",      this::visitCall);
		this.dispatchMap.put("funcname",  this::visitFuncName);
		this.dispatchMap.put("arguments", this::visitArgs);

		this.dispatchMap.put("and",       this::visitAnd);
		this.dispatchMap.put("or",        this::visitOr);
		this.dispatchMap.put("eq",        this::visitEQ);
		this.dispatchMap.put("neq",       this::visitNEQ);
		this.dispatchMap.put("le",        this::visitLE);
		this.dispatchMap.put("ge",        this::visitGE);
		this.dispatchMap.put("lt",        this::visitLT);
		this.dispatchMap.put("gt",        this::visitGT);

		this.dispatchMap.put("number",    this::visitNum);
		this.dispatchMap.put("string",    this::visitString);
		this.dispatchMap.put("segment",   this::visitSegment);
	}

	/**
	 * 
	 * @param parent
	 * @param tag
	 * @return
	 * may be null
	 */
	protected ParsingObject getChildAt(final ParsingObject parent, String tag) {
		int tagid = ParsingTag.tagId(tag);

		final int size = parent.size();
		for(int i = 0; i < size; i++) {
			ParsingObject chid = parent.get(i);
			if(chid.is(tagid)) {
				return chid;
			}
		}
		return null;
	}

	/**
	 * lookup method from dispatchMap and invoke.
	 * @param tree
	 * @param data
	 * may be null
	 * @return
	 * return value of looked up method
	 */
	protected R dispatch(ParsingObject tree, T data) {
		BiFunction<ParsingObject, T, R> func = this.dispatchMap.get(tree.getTag().key());
		if(func == null) {
			throw new RuntimeException("undefined action: " + tree.getTag().key());
		}
		return func.apply(tree, data);
	}

	@SuppressWarnings("unchecked")
	protected <S> S dispatchAndCast(ParsingObject tree, T data, Class<S> clazz) {
		return (S) this.dispatch(tree, data);
	}

	public abstract R visitError  (ParsingObject queryTree, T data);
	public abstract R visitSelect (ParsingObject queryTree, T data);
	public abstract R visitWhere  (ParsingObject queryTree, T data);
	public abstract R visitFrom   (ParsingObject queryTree, T data);

	public abstract R visitPath   (ParsingObject queryTree, T data);
	public abstract R visitName   (ParsingObject queryTree, T data);
	public abstract R visitRange  (ParsingObject queryTree, T data);
	public abstract R visitIndex  (ParsingObject queryTree, T data);

	public abstract R visitCall   (ParsingObject queryTree, T data);
	public abstract R visitFuncName(ParsingObject queryTree, T data);
	public abstract R visitArgs   (ParsingObject queryTree, T data);

	// conditional expression
	public abstract R visitAnd    (ParsingObject queryTree, T data);
	public abstract R visitOr     (ParsingObject queryTree, T data);
	public abstract R visitEQ     (ParsingObject queryTree, T data);
	public abstract R visitNEQ    (ParsingObject queryTree, T data);
	public abstract R visitLE     (ParsingObject queryTree, T data);
	public abstract R visitGE     (ParsingObject queryTree, T data);
	public abstract R visitLT     (ParsingObject queryTree, T data);
	public abstract R visitGT     (ParsingObject queryTree, T data);

	public abstract R visitNum    (ParsingObject queryTree, T data);
	public abstract R visitString (ParsingObject queryTree, T data);
	public abstract R visitSegment(ParsingObject queryTree, T data);
}

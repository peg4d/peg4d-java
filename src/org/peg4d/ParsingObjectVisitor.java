package org.peg4d;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ParsingObjectVisitor {
	HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	public final void visit(ParsingObject po) {
		ParsingTag tag = po.getTag();
		try {
			Method m = lookupMethod(tag.tagId);
			m.invoke(this, po);
		} 
		catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}
	
	public final boolean isSupported(String tagName) {
		try {
			return lookupMethod(ParsingTag.tagId(tagName)) != null;
		} catch (NoSuchMethodException e) {
		} catch (SecurityException e) {
		}
		return false;
	}
	
	private Method lookupMethod(int tagId) throws NoSuchMethodException, SecurityException {
		Integer key = tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			String name = "visit" + ParsingTag.tagName(tagId);
			try{
				m = this.getClass().getMethod(name, ParsingObject.class);
			}catch(NoSuchMethodException e){
				m = this.getClass().getMethod("onUnsupported", ParsingObject.class);
			}
			this.methodMap.put(key, m);
		}
		return m;
	}
	
	public void onUnsupported(ParsingObject po){
	}
	
	private static final int TAG_TEXT = ParsingTag.tagId("Text");
	protected boolean isNullOrEmpty(ParsingObject po){
		return po == null || po.is(TAG_TEXT) && po.size() == 0 && po.size() == 0 && po.getText().length() == 0;
	}
	protected boolean isNullOrEmpty(ParsingObject node, int index){
		return node.size() <= index || isNullOrEmpty(node.get(index));
	}
}


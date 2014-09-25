package org.peg4d;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ParsingBuilder {
	HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	public final Object build(ParsingObject po) {
		ParsingTag tag = po.getTag();
		Method m = lookupMethod(tag.tagId);
		if(m != null) {
			try {
				return m.invoke(this, po);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				System.err.println(po);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public final boolean isSupported(String tagName) {
		return lookupMethod(ParsingTag.tagId(tagName)) != null;
	}
	
	protected Method lookupMethod(int tagId) {
		Integer key = tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			String name = "to" + ParsingTag.tagName(tagId);
			try {
				m = this.getClass().getMethod(name, ParsingObject.class);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(key, m);
		}
		return m;
	}
}

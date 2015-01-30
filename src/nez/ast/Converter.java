package nez.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class Converter {
	HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	public final Object build(AST node) {
		Tag tag = node.getTag();
		Method m = lookupMethod(tag.id);
		if(m != null) {
			try {
				return m.invoke(this, node);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				System.err.println(node);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public final boolean isSupported(String tagName) {
		return lookupMethod(Tag.tag(tagName).id) != null;
	}
	
	protected Method lookupMethod(int tagId) {
		Integer key = tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			String name = "to" + Tag.tag(tagId).name;
			try {
				m = this.getClass().getMethod(name, AST.class);
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

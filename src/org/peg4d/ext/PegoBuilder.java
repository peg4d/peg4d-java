package org.peg4d.ext;
//package org.peg4d;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//
//import org.libbun.UMap;
//
//public class PegoBuilder {
//	private UMap<Constructor<?>> tagMap = new UMap<Constructor<?>>();
//	
//	public PegoBuilder() {
//		this.set("#error", Error.class);
//	}
//
//	class Error {
//		public Error(PegObject pego, PegoBuilder builder) {
//			String msg = pego.getText();
//			System.out.println(pego.source.formatErrorMessage("error", pego.startIndex, msg));
//		}
//	}
//	
//	public final <T> T newObject(PegObject pego, T defval) {
//		Constructor<T> c = this.lookup(pego.tag);
//		if(c != null) {
//			try {
//				T value;
//				if(c.getParameterCount() == 2) {
//					value = c.newInstance(pego, defval);
//				}
//				else {
//					value = c.newInstance(pego.getText());
//				}
//				System.out.println("OK? " + value.getClass().isAssignableFrom(c.getDeclaringClass()));
//				return value;
//			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//				e.printStackTrace();
//			}
//		}		
//		return defval;
//	}
//	
//	private <T> Constructor<T> lookup(String tag) {
//		Constructor<?> c = this.tagMap.get(tag);
//		if(c == null) {
//			try {
//				this.set(tag, Class.forName(tag.substring(1)));
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//		return (Constructor<T>)c;
//	}
//	
//	public boolean set(String tag, Class<?> c) {
//		Constructor<?> constructor = null;
//		try {
//			constructor = c.getConstructor(PegObject.class, PegoBuilder.class);
//		} catch (NoSuchMethodException | SecurityException e) {
//		}
//		if(constructor == null) {
//			try {
//				constructor = c.getConstructor(String.class);
//			} catch (NoSuchMethodException | SecurityException e) {
//				e.printStackTrace();
//			}
//		}
//		if(constructor != null) {
//			this.tagMap.put(tag, constructor);
//			return true;
//		}
//		else {
//			System.out.println("no peg constructor: " + c.getName());
//		}
//		return false;
//	}
//
//	public final PegoBuilder define(String tag, Class<?> c) {
//		this.set(tag, c);
//		return this;
//	}
//	
//	public final void loadJavaBasicType() {
//		this.define("#int", Integer.class);
//		this.define("#int64", Long.class);
//		this.define("#float", Float.class);
//		this.define("#double", Double.class);
//		this.define("#string", String.class);
//	}
//
//	public void test() {
//		String s = this.newObject(null, null);
//		int n = this.newObject(null, null);
//	}
//	
//}

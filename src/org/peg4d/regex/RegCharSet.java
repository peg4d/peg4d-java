package org.peg4d.regex;

import java.util.LinkedHashSet;
import java.util.Set;

import org.peg4d.ParsingObject;

public class RegCharSet extends RegexObject {

	private Set<String> set;

	public RegCharSet(ParsingObject po) {
		this(po, null);
		this.addQuantifier(po);
	}

	public RegCharSet(ParsingObject po, RegexObject parent) {
		super(po, parent);
		set = new LinkedHashSet<String>();
		setCharSet(po.get(1).getText());
	}

	private void setCharSet(String s) {
		if(s.length() == 1) {
			set.add(s);
			return;
		}else if(s.length() == 2 && s.charAt(0)=='\\'){
			//escapedchar
			set.add(s);
			return;
		}
		int i = 1;
		int max = s.length() - 1;
		do {
			//bracket expression
			int next = i + 1;
			int next2 = i + 2;
			if(i == 1 && s.charAt(i)=='^'){
				//exceptfor
				this.not = true;
				i++;
				continue;
			}
			if(next < s.length() && s.charAt(next)=='-' && next2 < s.length()) {
				//range
				for(char j = s.charAt(i); j <= s.charAt(next2); j++) {
					set.add(String.valueOf(j));
				}
				i += 3;
			} else if(s.charAt(i)=='\\'){
				//escapedchar
				set.add(s.substring(i, next2));
				i += 2;
			} else {
				set.add(s.substring(i, next));
				++i;
			}
		} while(i < max);
	}

	public String getLetter() {
		StringBuilder sb = new StringBuilder();
		for(RegexObject e: list) {
			sb.append(e.toString());
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String[] arr = set.toArray(new String[set.size()]);
		if(this.quantifier != null && this.quantifier.hasRepeat() && arr.length > 1){
			sb.append("(");
		}
		if(this.not == true){
			sb.append("!");
		}
		if(arr.length == 1){
			sb.append("'");
			sb.append(arr[0]);
			sb.append("'");
		}else{
			sb.append("( ");
			sb.append("'");
			sb.append(arr[0]);
			sb.append("' ");
			for(int i = 1; i < arr.length; i++) {
				sb.append("/ '");
				sb.append(arr[i]);
				sb.append("' ");
			}
			sb.append(")");
		}
		if(this.quantifier != null) {
			sb.append(this.quantifier.toString());
		}
		if(this.not == true){
			sb.append(" .");
		}
		if(this.quantifier != null && this.quantifier.hasRepeat()){
			if(arr.length > 1) sb.append(")");
			return this.quantifier.repeatRule(sb.toString());
		}else{
			return sb.toString();
		}
	}

	public boolean contains(RegexObject obj) {
		if (obj instanceof RegCharSet) {
			RegCharSet that = (RegCharSet) obj;
			boolean b = false;
			for(String e: that.set.toArray(new String[that.set.size()])) {
				b = b || this.set.contains(e);
			}
			return b;
		}
		return false;
	}
}

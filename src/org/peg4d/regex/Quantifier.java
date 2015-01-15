package org.peg4d.regex;

import org.peg4d.ParsingObject;

public class Quantifier {

	private String label;
	private String sign;
	private int time = -1;
	private int min = -1;
	private int max = -1;

	public Quantifier(ParsingObject po) {
		this.label = po.getTag().toString();
		if(label.equals("Times")) {
			ParsingObject p = po.get(0);
			if(p.getTag().toString().equals("AndMore")) {
				this.setMin(Integer.parseInt(p.getText()));
			} else if(p.getTag().toString().equals("Time")) {
				this.setTime(Integer.parseInt(p.getText()));
			}
			if(po.size() > 1) {
				this.setMax(Integer.parseInt(po.get(1).getText()));
			}
		} else {
			sign = po.getText();
		}
	}

	public String getLabel() {
		return label;
	}

	public int getTime() {
		return time;
	}

	private void setTime(int time) {
		this.time = time;
	}

	public int getMin() {
		return min;
	}

	private void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	private void setMax(int max) {
		this.max = max;
	}

	private String getSign() {
		return sign;
	}

	public boolean hasRepeat(){
		if(this.time == -1 && this.min == -1 && this.max == -1) return false;
		else return true;
	}

	public String repeatRule(String rule){
		StringBuilder sb = new StringBuilder();
		sb.append("( ");
		if(this.time != -1){
			sb.append(rule);
			for(int i = 1; i < this.time; i++){
				sb.append(" ");
				sb.append(rule);
			}
		}
		else if(this.max != -1 && this.min != -1){
			for(int i = max ; i >= min; i--){
				for(int j = 0; j < i; j++){
					if(i != max || j != 0) sb.append(" ");
					sb.append(rule);
				}
				if(i != min) sb.append(" /");
			}
		}
		else if(this.max != -1){
			for(int i = max; i >= 1; i--){
				for(int j = 0; j < i; j++){
					if(i != max || j != 0) sb.append(" ");
					sb.append(rule);
				}
				if(i != 1) sb.append(" /");
			}
		}
		else if(this.min != -1){
			sb.append(rule);
			for(int i = 1; i < min; i++){
				sb.append(" ");
				sb.append(rule);
			}
			sb.append("+");
		}
		sb.append(" )");
		return sb.toString();
	}

	@Override
	public String toString() {
		String s = getSign();
		if(s != null) {
			return s;
		} else {
			return "";
		}
	}
}

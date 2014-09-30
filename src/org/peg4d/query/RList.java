package org.peg4d.query;

import java.util.LinkedList;
import java.util.List;

public class RList extends LinkedList<Object> {
	private static final long serialVersionUID = -3634406185210488565L;

	@SuppressWarnings("unchecked")
	public void addAndFlat(Object value) {
		if(value instanceof List) {
			this.addAll((List<Object>) value);
			return;
		}
		this.add(value);
	}
}

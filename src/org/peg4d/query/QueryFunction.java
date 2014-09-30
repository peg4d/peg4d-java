package org.peg4d.query;

import java.util.List;

public interface QueryFunction {
	public Object invoke(List<Object> argList);
}

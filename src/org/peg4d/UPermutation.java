package org.peg4d;

import java.util.Arrays;

public class UPermutation {

	private int[] a;

	public UPermutation(int size) {
		a = new int[size];
		for(int i = 0; i < size; i++) {
			a[i] = i;
		}
	}

	public boolean hasNext() {
		for (int i = a.length - 1; i > 0; --i) {
			if (a[i - 1] < a[i]) {
				int swap = find(a[i - 1], a, i, a.length - 1);
				int tmp = a[swap];
				a[swap] = a[i - 1];
				a[i - 1] = tmp;
				Arrays.sort(a, i, a.length);
				return true;
			}
		}
		return false;
	}

	private int find(int index, int[] a2, int start, int end) {
		if (start == end) {
			return start;
		}
		int m = (start + end + 1) / 2;
		return a2[m] <= index ? find(index, a2, start, m - 1) : find(index, a2, m, end);
	}

	public int[] next() {
		return a;
	}
}

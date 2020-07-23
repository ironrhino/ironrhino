package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class CounterUtilsTest {

	@Test
	public void testGetAndIncrement() {
		AtomicInteger counter = new AtomicInteger();
		int mod = 5;
		int[] expected = new int[] { 0, 1, 2, 3, 4, 0, 1, 2, 3 };
		verify(counter, mod, false, expected);

		counter.set(mod - 1);
		expected = new int[] { 4, 0, 1, 2, 3, 4, 0 };
		verify(counter, mod, false, expected);
	}

	@Test
	public void testIncrementAndGet() {
		AtomicInteger counter = new AtomicInteger();
		int mod = 5;
		int[] expected = new int[] { 1, 2, 3, 4, 0, 1, 2, 3, 4, 0 };
		verify(counter, mod, true, expected);

		counter.set(mod - 1);
		expected = new int[] { 0, 1, 2, 3, 4, 0, 1 };
		verify(counter, mod, true, expected);
	}

	@Test
	public void testMaxValue() {
		AtomicInteger counter = new AtomicInteger(Integer.MAX_VALUE);
		int mod = 5;
		int[] expected = new int[] { 4, 0, 1, 2, 3, 4, 0, 1, 2, 3 };
		verify(counter, mod, false, expected);
		counter = new AtomicInteger(Integer.MAX_VALUE);
		expected = new int[] { 0, 1, 2, 3, 4, 0, 1, 2, 3 };
		verify(counter, mod, true, expected);
	}

	@Test
	public void testNegativeValue() {
		AtomicInteger counter = new AtomicInteger(-4);
		int mod = 5;
		int[] expected = new int[] { 1, 2, 3, 4, 0, 1, 2, 3 };
		verify(counter, mod, false, expected);
		counter = new AtomicInteger(-4);
		expected = new int[] { 2, 3, 4, 0, 1, 2, 3, 4 };
		verify(counter, mod, true, expected);
	}

	private void verify(AtomicInteger counter, int mod, boolean incrementAndGet, int[] expected) {
		int[] arr = new int[expected.length];
		for (int i = 0; i < arr.length; i++)
			arr[i] = incrementAndGet ? CounterUtils.incrementAndGet(counter, mod)
					: CounterUtils.getAndIncrement(counter, mod);
		assertThat(arr, equalTo(expected));
	}
}

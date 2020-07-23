package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class CyclicAtomicIntegerTest {

	@Test(expected = IllegalArgumentException.class)
	public void testZeroUpperBound() {
		new CyclicAtomicInteger(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeUpperBound() {
		new CyclicAtomicInteger(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpperBoundEqualThanLowerBound() {
		new CyclicAtomicInteger(0, 2, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpperBoundLessThanLowerBound() {
		new CyclicAtomicInteger(0, 3, 2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitialValueGreaterOrEqualThanUpperBound() {
		new CyclicAtomicInteger(4, 2, 4);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitialValueLessThanUpperBound() {
		new CyclicAtomicInteger(1, 2, 4);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewValueLessThanUpperBound() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(1, -1, 4);
		cai.set(4);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpdateValueLessThanUpperBound() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(1, -1, 4);
		cai.compareAndSet(1, 4);
	}

	@Test
	public void testSet() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(1, -1, 4);
		assertThat(cai.get(), equalTo(1));
		cai.set(-1);
		assertThat(cai.get(), equalTo(-1));
	}

	@Test
	public void testCompareAndSet() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(1, -1, 4);
		assertThat(cai.compareAndSet(1, -1), equalTo(true));
		assertThat(cai.get(), equalTo(-1));
		assertThat(cai.compareAndSet(1, 2), equalTo(false));
		assertThat(cai.get(), equalTo(-1));
	}

	@Test
	public void testGetAndAdd() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(3);
		int[] arr = new int[] { 0, 1, 2, 0, 1, 2, 0 };
		int[] arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, 0, 3);
		arr = new int[] { 1, 2, 0, 1, 2, 0 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, 1, 3);
		arr = new int[] { 1, 2, 1, 2, 1, 2 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new int[] { 1, 2, -1, 0, 1, 2, -1, 0 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new int[] { 1, -1, 1, -1 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(2);
		assertThat(arr2, equalTo(arr));
	}

	@Test
	public void testAddAndGet() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(3);
		int[] arr = new int[] { 1, 2, 0, 1, 2, 0 };
		int[] arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, 0, 3);
		arr = new int[] { 2, 0, 1, 2, 0 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, 1, 3);
		arr = new int[] { 2, 1, 2, 1, 2 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new int[] { 2, -1, 0, 1, 2, -1, 0 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertThat(arr2, equalTo(arr));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new int[] { -1, 1, -1 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(2);
		assertThat(arr2, equalTo(arr));
	}

	@Test
	public void testMaxValue() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(Integer.MAX_VALUE - 1, 1, Integer.MAX_VALUE);
		int[] arr = new int[] { Integer.MAX_VALUE - 1, 1, 2, 3, 4 };
		int[] arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertThat(arr2, equalTo(arr));
	}

	@Test
	public void testGetAndDecrement() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(4, 4, 10);
		int[] arr = new int[] { 4, 9, 8, 7, 6, 5, 4, 9 };
		int[] arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndDecrement();
		assertThat(arr2, equalTo(arr));

		cai.set(9);
		arr = new int[] { 9, 8, 7, 6, 5, 4, 9 };
		arr2 = new int[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndDecrement();
		assertThat(arr2, equalTo(arr));
	}
}

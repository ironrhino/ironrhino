package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

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
		assertEquals(1, cai.get());
		cai.set(-1);
		assertEquals(-1, cai.get());
	}

	@Test
	public void testCompareAndSet() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(1, -1, 4);
		assertTrue(cai.compareAndSet(1, -1));
		assertEquals(-1, cai.get());
		assertFalse(cai.compareAndSet(1, 2));
		assertEquals(-1, cai.get());
	}

	@Test
	public void testGetAndAdd() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(3);
		Integer[] arr = new Integer[] { 0, 1, 2, 0, 1, 2, 0 };
		Integer[] arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, 0, 3);
		arr = new Integer[] { 1, 2, 0, 1, 2, 0 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, 1, 3);
		arr = new Integer[] { 1, 2, 1, 2, 1, 2 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new Integer[] { 1, 2, -1, 0, 1, 2, -1, 0 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new Integer[] { 1, -1, 1, -1 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.getAndAdd(2);
		assertTrue(Arrays.equals(arr, arr2));
	}

	@Test
	public void testAddAndGet() {
		CyclicAtomicInteger cai = new CyclicAtomicInteger(3);
		Integer[] arr = new Integer[] { 1, 2, 0, 1, 2, 0 };
		Integer[] arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, 0, 3);
		arr = new Integer[] { 2, 0, 1, 2, 0 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, 1, 3);
		arr = new Integer[] { 2, 1, 2, 1, 2 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new Integer[] { 2, -1, 0, 1, 2, -1, 0 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(1);
		assertTrue(Arrays.equals(arr, arr2));
		cai = new CyclicAtomicInteger(1, -1, 3);
		arr = new Integer[] { -1, 1, -1 };
		arr2 = new Integer[arr.length];
		for (int i = 0; i < arr2.length; i++)
			arr2[i] = cai.addAndGet(2);
		assertTrue(Arrays.equals(arr, arr2));
	}

}

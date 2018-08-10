package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RoundRobinTest {

	@Test
	public void testSimple() {
		RoundRobin<String> rr = new RoundRobin<>(Arrays.asList("a", "b", "c"));
		assertEquals("a", rr.pick());
		assertEquals("b", rr.pick());
		assertEquals("c", rr.pick());
		assertEquals("a", rr.pick());
	}

	@Test
	public void testWithWeight() {
		Map<String, Integer> targets = new LinkedHashMap<>();
		targets.put("a", 1);
		targets.put("b", 2);
		targets.put("c", 3);
		RoundRobin<String> rr = new RoundRobin<>(targets);
		Map<String, AtomicInteger> counters = new LinkedHashMap<>();
		for (int i = 0; i < 600; i++) {
			String s = rr.pick();
			AtomicInteger ai = counters.computeIfAbsent(s, k -> new AtomicInteger());
			ai.incrementAndGet();
		}
		assertEquals(100, counters.get("a").get());
		assertEquals(200, counters.get("b").get());
		assertEquals(300, counters.get("c").get());
	}

	@Test
	public void testWithWeightAndUsableChecker() {
		Map<String, Integer> targets = new LinkedHashMap<>();
		targets.put("a", 1);
		targets.put("b", 2);
		targets.put("c", 3);
		RoundRobin<String> rr = new RoundRobin<>(targets, key -> {
			return true;
		});
		Map<String, AtomicInteger> counters = new LinkedHashMap<>();
		for (int i = 0; i < 600; i++) {
			String s = rr.pick();
			AtomicInteger ai = counters.computeIfAbsent(s, k -> new AtomicInteger());
			ai.incrementAndGet();
		}
		assertEquals(100, counters.get("a").get());
		assertEquals(200, counters.get("b").get());
		assertEquals(300, counters.get("c").get());

		counters = new LinkedHashMap<>();
		final Map<String, AtomicInteger> mc = counters;
		rr = new RoundRobin<>(targets, key -> {
			if (key.equals("c")) {
				AtomicInteger current = mc.get(key);
				if (current != null && current.get() >= 100)
					return false;
			}
			return true;
		});
		for (int i = 0; i < 600; i++) {
			String s = rr.pick();
			AtomicInteger ai = counters.computeIfAbsent(s, k -> new AtomicInteger());
			ai.incrementAndGet();
		}
		assertEquals(167, counters.get("a").get());
		assertEquals(333, counters.get("b").get());
		assertEquals(100, counters.get("c").get());
	}

}

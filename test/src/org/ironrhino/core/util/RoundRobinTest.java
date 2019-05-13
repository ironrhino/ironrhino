package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class RoundRobinTest {

	@Test
	public void testSimple() {
		RoundRobin<String> rr = new RoundRobin<>(Arrays.asList("a", "b", "c"));
		assertThat(rr.pick(), equalTo("a"));
		assertThat(rr.pick(), equalTo("b"));
		assertThat(rr.pick(), equalTo("c"));
		assertThat(rr.pick(), equalTo("a"));
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
		assertThat(counters.get("a").get(), equalTo(100));
		assertThat(counters.get("b").get(), equalTo(200));
		assertThat(counters.get("c").get(), equalTo(300));
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
		assertThat(counters.get("a").get(), equalTo(100));
		assertThat(counters.get("b").get(), equalTo(200));
		assertThat(counters.get("c").get(), equalTo(300));

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
		assertThat(counters.get("a").get(), equalTo(167));
		assertThat(counters.get("b").get(), equalTo(333));
		assertThat(counters.get("c").get(), equalTo(100));
	}

}

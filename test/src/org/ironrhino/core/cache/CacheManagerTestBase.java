package org.ironrhino.core.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class CacheManagerTestBase {

	private static final String NAMESPACE = "test:test";

	@Autowired
	private CacheManager cacheManager;

	@Test
	public void testCrud() {
		String key = "key";
		Object value = "value";
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		assertTrue(cacheManager.exists(key, NAMESPACE));
		assertEquals(value, cacheManager.get(key, NAMESPACE));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertNull(cacheManager.get(key, NAMESPACE));
		assertFalse(cacheManager.exists(key, NAMESPACE));
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		assertTrue(cacheManager.exists(key, NAMESPACE));
		assertEquals(value, cacheManager.get(key, NAMESPACE));
		cacheManager.put(key, value, 3, TimeUnit.SECONDS, NAMESPACE);
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(value, cacheManager.get(key, NAMESPACE));
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertFalse(cacheManager.exists(key, NAMESPACE));
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		assertTrue(cacheManager.exists(key, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
		assertFalse(cacheManager.exists(key, NAMESPACE));
	}

	@Test
	public void testMulti() {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < 10; i++)
			map.put("test" + i, "value" + i);
		cacheManager.mput(map, 2, TimeUnit.SECONDS, NAMESPACE);
		assertEquals(map, cacheManager.mget(map.keySet(), NAMESPACE));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertNull(cacheManager.mget(map.keySet(), NAMESPACE).get("test1"));
		cacheManager.mput(map, 2, TimeUnit.SECONDS, NAMESPACE);
		cacheManager.mdelete(map.keySet(), NAMESPACE);
		assertNull(cacheManager.mget(map.keySet(), NAMESPACE).get("test2"));
	}

	@Test
	public void testTtlAndIdle() {
		String key = "key";
		Object value = "value";
		try {
			assertEquals(0, cacheManager.ttl(key, NAMESPACE));
		} catch (UnsupportedOperationException e) {

		}
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		if (cacheManager.supportsGetTtl())
			assertTrue(cacheManager.ttl(key, NAMESPACE) > 1000);
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cacheManager.setTtl(key, NAMESPACE, 2, TimeUnit.SECONDS);
		if (cacheManager.supportsTti()) {
			cacheManager.putWithTti(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
			assertEquals(value, cacheManager.get(key, NAMESPACE));
			for (int i = 0; i < 3; i++) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				assertEquals(value, cacheManager.get(key, NAMESPACE));
			}
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertFalse(cacheManager.exists(key, NAMESPACE));
		} else if (cacheManager.supportsUpdateTtl()) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertEquals(value, cacheManager.getWithTti(key, NAMESPACE, 2, TimeUnit.SECONDS));
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertTrue(cacheManager.exists(key, NAMESPACE));
			try {
				TimeUnit.MILLISECONDS.sleep(2100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertFalse(cacheManager.exists(key, NAMESPACE));
		}
		cacheManager.delete(key, NAMESPACE);
	}

	@Test
	public void testAtomic() {
		String key = "key";
		Object value = "value";
		assertTrue(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE));
		assertFalse(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
		assertEquals(0, cacheManager.increment(key, 0, 2, TimeUnit.SECONDS, NAMESPACE));
		assertEquals(2, cacheManager.increment(key, 2, 2, TimeUnit.SECONDS, NAMESPACE));
		assertEquals(5, cacheManager.increment(key, 3, 2, TimeUnit.SECONDS, NAMESPACE));
		assertEquals(4, cacheManager.decrement(key, 1, 2, TimeUnit.SECONDS, NAMESPACE));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertFalse(cacheManager.exists(key, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);

		assertTrue(cacheManager.putIfAbsent(key, value, -1, TimeUnit.SECONDS, NAMESPACE));
		assertFalse(cacheManager.putIfAbsent(key, value, -1, TimeUnit.SECONDS, NAMESPACE));
		try {
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertFalse(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
		assertEquals(2, cacheManager.increment(key, 2, -1, TimeUnit.SECONDS, NAMESPACE));
		assertEquals(5, cacheManager.increment(key, 3, -1, TimeUnit.SECONDS, NAMESPACE));
		try {
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(cacheManager.exists(key, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
	}

	@Test
	public void testDecrementAndReturnNonnegative() {
		String key = "key";
		boolean error = false;
		try {
			cacheManager.decrementAndReturnNonnegative(key, -2, -1, TimeUnit.SECONDS, NAMESPACE);
		} catch (IllegalArgumentException e) {
			error = true;
		}
		assertTrue(error);
		error = false;
		try {
			cacheManager.decrementAndReturnNonnegative(key, 2, -1, TimeUnit.SECONDS, NAMESPACE);
		} catch (IllegalStateException e) {
			error = true;
		}
		assertTrue(error);
		assertEquals(2, cacheManager.increment(key, 2, -1, TimeUnit.SECONDS, NAMESPACE));
		assertEquals(1, cacheManager.decrementAndReturnNonnegative(key, 1, -1, TimeUnit.SECONDS, NAMESPACE));
		error = false;
		try {
			cacheManager.decrementAndReturnNonnegative(key, 2, -1, TimeUnit.SECONDS, NAMESPACE);
		} catch (IllegalStateException e) {
			error = true;
		}
		assertEquals(0, cacheManager.decrementAndReturnNonnegative(key, 1, -1, TimeUnit.SECONDS, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
	}

}

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

	private static final String NAMESPACE = "test";

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
			TimeUnit.SECONDS.sleep(3);
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
		map.put("test1", "value1");
		map.put("test2", "value2");
		cacheManager.mput(map, 2, TimeUnit.SECONDS, NAMESPACE);
		assertEquals(map, cacheManager.mget(map.keySet(), NAMESPACE));
		try {
			TimeUnit.SECONDS.sleep(3);
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
		try {
			assertTrue(cacheManager.ttl(key, NAMESPACE) > 1000);
		} catch (UnsupportedOperationException e) {

		}
		if (cacheManager.supportsTimeToIdle()) {
			cacheManager.put(key, value, 2, 0, TimeUnit.SECONDS, NAMESPACE);
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
		} else if (cacheManager.supportsUpdateTimeToLive()) {
			try {
				TimeUnit.SECONDS.sleep(1);
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
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
		assertEquals(2, cacheManager.increment(key, 2, 2, TimeUnit.SECONDS, NAMESPACE));
		assertEquals(5, cacheManager.increment(key, 3, 2, TimeUnit.SECONDS, NAMESPACE));
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertFalse(cacheManager.exists(key, NAMESPACE));
		cacheManager.delete(key, NAMESPACE);
	}

}

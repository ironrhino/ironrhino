package org.ironrhino.core.cache;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
		assertThat(cacheManager.exists(key, NAMESPACE), is(true));
		assertThat(cacheManager.get(key, NAMESPACE), is(value));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.get(key, NAMESPACE), is(nullValue()));
		assertThat(cacheManager.exists(key, NAMESPACE), is(false));
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		assertThat(cacheManager.exists(key, NAMESPACE), is(true));
		assertThat(cacheManager.get(key, NAMESPACE), is(value));
		cacheManager.put(key, value, 3, TimeUnit.SECONDS, NAMESPACE);
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.get(key, NAMESPACE), is(value));
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.exists(key, NAMESPACE), is(false));
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		assertThat(cacheManager.exists(key, NAMESPACE), is(true));
		cacheManager.delete(key, NAMESPACE);
		assertThat(cacheManager.exists(key, NAMESPACE), is(false));
	}

	@Test
	public void testMulti() {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < 10; i++)
			map.put("test" + i, "value" + i);
		cacheManager.mput(map, 2, TimeUnit.SECONDS, NAMESPACE);
		for (int i = 0; i < 10; i++)
			assertThat(cacheManager.mget(map.keySet(), NAMESPACE).get("test" + i), is(map.get("test" + i)));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.mget(map.keySet(), NAMESPACE).get("test1"), is(nullValue()));
		cacheManager.mput(map, 2, TimeUnit.SECONDS, NAMESPACE);
		cacheManager.mdelete(map.keySet(), NAMESPACE);
		assertThat(cacheManager.mget(map.keySet(), NAMESPACE).get("test2"), is(nullValue()));
	}

	@Test
	public void testTtlAndIdle() {
		String key = "key";
		Object value = "value";
		try {
			assertThat(cacheManager.ttl(key, NAMESPACE), is(-2L));
			cacheManager.put(key, value, -1, TimeUnit.SECONDS, NAMESPACE);
			assertThat(cacheManager.ttl(key, NAMESPACE), is(-1L));
		} catch (UnsupportedOperationException e) {

		}
		cacheManager.put(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
		if (cacheManager.supportsGetTtl())
			assertThat(cacheManager.ttl(key, NAMESPACE) > 1000, is(true));
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cacheManager.setTtl(key, NAMESPACE, 2, TimeUnit.SECONDS);
		if (cacheManager.supportsTti()) {
			cacheManager.putWithTti(key, value, 2, TimeUnit.SECONDS, NAMESPACE);
			assertThat(cacheManager.get(key, NAMESPACE), is(value));
			for (int i = 0; i < 3; i++) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				assertThat(cacheManager.get(key, NAMESPACE), is(value));
			}
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertThat(cacheManager.exists(key, NAMESPACE), is(false));
		} else if (cacheManager.supportsUpdateTtl()) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertThat(cacheManager.getWithTti(key, NAMESPACE, 2, TimeUnit.SECONDS), is(value));
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertThat(cacheManager.exists(key, NAMESPACE), is(true));
			try {
				TimeUnit.MILLISECONDS.sleep(2100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			assertThat(cacheManager.exists(key, NAMESPACE), is(false));
		}
		cacheManager.delete(key, NAMESPACE);
	}

	@Test
	public void testAtomic() {
		String key = "key";
		Object value = "value";
		assertThat(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE), is(true));
		assertThat(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE), is(false));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE), is(true));
		cacheManager.delete(key, NAMESPACE);
		assertThat(cacheManager.increment(key, 0, 2, TimeUnit.SECONDS, NAMESPACE), is(0L));
		assertThat(cacheManager.increment(key, 2, 2, TimeUnit.SECONDS, NAMESPACE), is(2L));
		assertThat(cacheManager.increment(key, 3, 2, TimeUnit.SECONDS, NAMESPACE), is(5L));
		assertThat(cacheManager.decrement(key, 1, 2, TimeUnit.SECONDS, NAMESPACE), is(4L));
		try {
			TimeUnit.MILLISECONDS.sleep(2100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.exists(key, NAMESPACE), is(false));
		cacheManager.delete(key, NAMESPACE);

		assertThat(cacheManager.putIfAbsent(key, value, -1, TimeUnit.SECONDS, NAMESPACE), is(true));
		assertThat(cacheManager.putIfAbsent(key, value, -1, TimeUnit.SECONDS, NAMESPACE), is(false));
		try {
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.putIfAbsent(key, value, 2, TimeUnit.SECONDS, NAMESPACE), is(false));
		cacheManager.delete(key, NAMESPACE);
		assertThat(cacheManager.increment(key, 2, -1, TimeUnit.SECONDS, NAMESPACE), is(2L));
		assertThat(cacheManager.increment(key, 3, -1, TimeUnit.SECONDS, NAMESPACE), is(5L));
		try {
			TimeUnit.MILLISECONDS.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.exists(key, NAMESPACE), is(true));
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
		assertThat(error, is(true));
		error = false;
		try {
			cacheManager.decrementAndReturnNonnegative(key, 2, -1, TimeUnit.SECONDS, NAMESPACE);
		} catch (IllegalStateException e) {
			error = true;
		}
		assertThat(error, is(true));
		assertThat(cacheManager.increment(key, 2, -1, TimeUnit.SECONDS, NAMESPACE), is(2L));
		assertThat(cacheManager.decrementAndReturnNonnegative(key, 1, -1, TimeUnit.SECONDS, NAMESPACE), is(1L));
		error = false;
		try {
			cacheManager.decrementAndReturnNonnegative(key, 2, -1, TimeUnit.SECONDS, NAMESPACE);
		} catch (IllegalStateException e) {
			error = true;
		}
		assertThat(cacheManager.decrementAndReturnNonnegative(key, 1, -1, TimeUnit.SECONDS, NAMESPACE), is(0L));
		assertThat(cacheManager.increment(key, 2, -1, TimeUnit.SECONDS, NAMESPACE), is(2L));
		assertThat(cacheManager.decrementAndReturnNonnegative(key, 2, 1, TimeUnit.SECONDS, NAMESPACE), is(0L));
		assertThat(cacheManager.exists(key, NAMESPACE), is(true));
		try {
			TimeUnit.MILLISECONDS.sleep(1100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertThat(cacheManager.exists(key, NAMESPACE), is(false));
	}

}

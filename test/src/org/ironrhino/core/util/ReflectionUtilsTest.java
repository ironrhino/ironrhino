package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.metadata.Param;
import org.junit.Test;

public class ReflectionUtilsTest {

	@Test
	public void testGetAllFields() throws Exception {
		List<String> fieldNames = ReflectionUtils.getAllFields(B.class);
		assertEquals(2, fieldNames.size());
		assertEquals("a", fieldNames.get(0));
		assertEquals("b", fieldNames.get(1));
	}

	@Test
	public void testGetGenericClass() throws Exception {
		assertEquals(A.class, ReflectionUtils.getGenericClass(D.class));
		assertEquals(A.class, ReflectionUtils.getGenericClass(D.class, 0));
		assertEquals(A.class, ReflectionUtils.getGenericClass(F.class, C.class));
		assertEquals(B.class, ReflectionUtils.getGenericClass(F.class, E.class));
	}

	@Test
	public void testGetField() throws Exception {
		Field f1 = ReflectionUtils.getField(A.class, "a");
		Field f2 = ReflectionUtils.getField(B.class, "a");
		assertEquals(f1, f2);
		B b = new B();
		b.setA("test");
		assertEquals("test", ReflectionUtils.getFieldValue(b, "a"));
		ReflectionUtils.setFieldValue(b, "a", "test2");
		assertEquals("test2", ReflectionUtils.getFieldValue(b, "a"));
	}

	@Test
	public void testGetParameterNames() throws Exception {
		Method m = Service.class.getMethod("echo", String.class, String.class);
		assertNotNull(ReflectionUtils.getParameterNames(m));
		assertEquals("name1", ReflectionUtils.getParameterNames(m)[0]);
		assertEquals("test2", ReflectionUtils.getParameterNames(m)[1]);
		m = ServiceImpl.class.getMethod("echo", String.class, String.class);
		assertNotNull(ReflectionUtils.getParameterNames(m));
		assertEquals("name3", ReflectionUtils.getParameterNames(m)[0]);
		assertEquals("test4", ReflectionUtils.getParameterNames(m)[1]);
	}

	@Test
	public void testProcessCallback() throws Exception {
		B b = new B();
		ReflectionUtils.processCallback(b, PostConstruct.class);
		assertEquals("pc1", b.getA());
		assertEquals("pc2", b.getB());
	}

	static class A {
		private String a;

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}

		@PostConstruct
		private void pc1() {
			a = "pc1";
		}

	}

	static class B extends A {
		private String b;

		public String getB() {
			return b;
		}

		public void setB(String b) {
			this.b = b;
		}

		@PostConstruct
		private void pc2() {
			b = "pc2";
		}

	}

	static class C<T> {

	}

	static class D extends C<A> {

	}

	static class E<T> extends D {

	}

	static class F extends E<B> {

	}

	static interface Service {
		String echo(String name1, @Param("test2") String name2);
	}

	static class ServiceImpl implements Service {

		@Override
		public String echo(String name3, @Param("test4") String name4) {
			return name3 + name4;
		}
	}

}

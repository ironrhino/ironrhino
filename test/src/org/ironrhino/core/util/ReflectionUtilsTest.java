package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.metadata.Param;
import org.junit.Test;

import lombok.Getter;
import lombok.Setter;

public class ReflectionUtilsTest {

	@Test
	public void testGetAllFields() throws Exception {
		List<String> fieldNames = ReflectionUtils.getAllFields(B.class);
		assertThat(fieldNames.size(), equalTo(2));
		assertThat(fieldNames.get(0), equalTo("a"));
		assertThat(fieldNames.get(1), equalTo("b"));
	}

	@Test
	public void testGetGenericClass() throws Exception {
		assertThat(ReflectionUtils.getGenericClass(D.class), equalTo(A.class));
		assertThat(ReflectionUtils.getGenericClass(D.class, 0), equalTo(A.class));
		assertThat(ReflectionUtils.getGenericClass(F.class, C.class), equalTo(A.class));
		assertThat(ReflectionUtils.getGenericClass(F.class, E.class), equalTo(B.class));
	}

	@Test
	public void testGetField() throws Exception {
		Field f1 = ReflectionUtils.getField(A.class, "a");
		Field f2 = ReflectionUtils.getField(B.class, "a");
		assertThat(f2, equalTo(f1));
	}

	@Test
	public void testGetAndSetFieldValue() throws Exception {
		B b = new B();
		b.setA("test");
		assertThat(ReflectionUtils.getFieldValue(b, "a"), equalTo("test"));
		ReflectionUtils.setFieldValue(b, "a", "test2");
		assertThat(ReflectionUtils.getFieldValue(b, "a"), equalTo("test2"));
	}

	@Test(expected = NoSuchFieldException.class)
	public void testGetFieldWithException() throws Exception {
		ReflectionUtils.getField(A.class, "none");
	}

	@Test
	public void testGetParameterNames() throws Exception {
		Method m = Service.class.getMethod("echo", String.class, String.class);
		assertThat(ReflectionUtils.getParameterNames(m), notNullValue());
		assertThat(ReflectionUtils.getParameterNames(m)[0], equalTo("name1"));
		assertThat(ReflectionUtils.getParameterNames(m)[1], equalTo("test2"));
		m = ServiceImpl.class.getMethod("echo", String.class, String.class);
		assertThat(ReflectionUtils.getParameterNames(m), notNullValue());
		assertThat(ReflectionUtils.getParameterNames(m)[0], equalTo("name3"));
		assertThat(ReflectionUtils.getParameterNames(m)[1], equalTo("test4"));
	}

	@Test
	public void testProcessCallback() throws Exception {
		B b = new B();
		ReflectionUtils.processCallback(b, PostConstruct.class);
		assertThat(b.getA(), equalTo("pc1"));
		assertThat(b.getB(), equalTo("pc2"));
	}

	static class A {
		@Getter
		@Setter
		private String a;

		@PostConstruct
		private void pc1() {
			a = "pc1";
		}

	}

	static class B extends A {
		@Getter
		@Setter
		private String b;

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

	interface Service {
		String echo(String name1, @Param("test2") String name2);
	}

	static class ServiceImpl implements Service {

		@Override
		public String echo(String name3, @Param("test4") String name4) {
			return name3 + name4;
		}
	}

}

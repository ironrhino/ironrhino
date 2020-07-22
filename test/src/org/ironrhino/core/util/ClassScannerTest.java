package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import org.junit.Test;
import org.springframework.core.annotation.Order;

public class ClassScannerTest {

	@Test
	public void testScanAnnotated() {
		Collection<Class<?>> classes = ClassScanner.scanAnnotated("org.ironrhino.core.util", TestAnnotation.class);
		assertThat(classes, is(notNullValue()));
		assertThat(classes.size(), is(1));
		assertThat(classes.contains(TestBean.class), is(true));
	}

	@Test
	public void testScanAnnotatedDoesNotMatch() {
		Collection<Class<?>> classes = ClassScanner.scanAnnotated("org.ironrhino.core.util", TestAnnotation.class,
				UnusedAnnotation.class);
		assertThat(classes, is(notNullValue()));
		assertThat(classes.size(), is(0));
	}

	@Test
	public void testScanAssignable() {
		Collection<Class<?>> classes = ClassScanner.scanAssignable("org.ironrhino.core.util", TestBean.class);
		assertThat(classes, is(notNullValue()));
		assertThat(classes.size(), is(2));

		Class<?>[] arr = classes.toArray(new Class<?>[classes.size()]);
		assertThat(arr[0] == TestBean.class, is(true));
		assertThat(arr[1] == SubclassOfTestBean.class, is(true));
	}

	@Test
	public void testScanAssignableFromInterface() {
		Collection<Class<?>> classes = ClassScanner.scanAssignable("org.ironrhino.core.util", ITestBean.class);
		assertThat(classes, is(notNullValue()));
		assertThat(classes.size(), is(3));

		Class<?>[] arr = classes.toArray(new Class<?>[classes.size()]);
		assertThat(arr[0] == ITestBean.class, is(true));
		assertThat(arr[1] == TestBean.class, is(true));
		assertThat(arr[2] == SubclassOfTestBean.class, is(true));
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface TestAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface UnusedAnnotation {
	}

	@Order(0)
	interface ITestBean {
	}

	@Order(1)
	@TestAnnotation
	static class TestBean implements ITestBean {
	}

	@Order(2)
	static class SubclassOfTestBean extends TestBean {

	}
}
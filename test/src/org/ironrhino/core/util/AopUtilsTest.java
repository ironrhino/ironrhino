package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;

/**
 * https://github.com/spring-projects/spring-framework/issues/21843
 */
public class AopUtilsTest {

	@Test
	public void testGetMostSpecificMethod() throws Exception {
		String methodName = "feed";
		Class<?> targetClass = DogService.class;
		Method targetMethod = targetClass.getDeclaredMethod(methodName, Dog.class);
		Method originalMethod = AnimalService.class.getDeclaredMethod(methodName, Animal.class);
		Method actualMethod = AopUtils.getMostSpecificMethod(originalMethod, targetClass);
		assertThat("Please ensure class compiled with javac not eclipse", actualMethod, equalTo(targetMethod));
	}

	public static class Animal {

	}

	public static abstract class Mammal extends Animal {

	}

	public static class Dog extends Mammal {

	}

	public static class AnimalService<T extends Animal> {
		public void feed(T obj) {
		}
	}

	public static class MammalService<T extends Mammal> extends AnimalService<T> {
		@Override
		public void feed(T obj) {
		}
	}

	public static class DogService extends MammalService<Dog> {
		@Override
		public void feed(Dog obj) {
		}
	}

}

package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;

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
		assertEquals("Please ensure class compiled with javac not eclipse", targetMethod, actualMethod);
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

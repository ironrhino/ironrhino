package org.ironrhino.core.spring.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

public class ServiceImplementationConditionTest {

	@Test
	public void testMethodMetadata() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestServiceConfiguration.class);
		assertThat(context.getBean("testService") instanceof TestServiceMethodMetadataImpl, is(true));
		assertThat(context.getBean("testServiceWithDefaultServiceInterface") instanceof TestServiceMethodMetadataImpl,
				is(true));
		expectNotPresent(context, UnusedTestServiceMethodMetadataImpl.class);
		context.close();
	}

	@Test
	public void testClassMetadata() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestServiceClassMetadataImpl.class, UnusedTestServiceClassMetadataImpl.class);
		assertThat(context.getBean(TestServiceClassMetadata.class) instanceof TestServiceClassMetadataImpl, is(true));
		expectNotPresent(context, UnusedTestServiceClassMetadataImpl.class);
		context.close();
	}

	@Test
	public void testClassNotFound() {
		try {
			new AnnotationConfigApplicationContext(FooServiceImpl.class);
			fail("Expected exception");
		} catch (RuntimeException e) {
			assertThat(e.getCause() instanceof ClassNotFoundException, is(true));
		}
	}

	private void expectNotPresent(ApplicationContext context, Class<?> beanType) {
		try {
			context.getBean(beanType);
			fail("Expect not present: " + beanType);
		} catch (NoSuchBeanDefinitionException e) {
			// ignore
		}
	}

	@Configuration
	static class TestServiceConfiguration {

		@Bean
		@ServiceImplementationConditional(serviceInterface = TestServiceMethodMetadata.class)
		public TestServiceMethodMetadataImpl testService() {
			return new TestServiceMethodMetadataImpl();
		}

		@Bean
		@ServiceImplementationConditional
		public TestServiceMethodMetadataImpl testServiceWithDefaultServiceInterface() {
			return new TestServiceMethodMetadataImpl();
		}

		@Bean
		@ServiceImplementationConditional(serviceInterface = TestServiceMethodMetadata.class)
		public UnusedTestServiceMethodMetadataImpl unusedTestService() {
			return new UnusedTestServiceMethodMetadataImpl();
		}

	}

	interface TestService {
	}

	interface TestServiceMethodMetadata extends TestService {
	}

	interface TestServiceClassMetadata extends TestService {
	}

	static class TestServiceMethodMetadataImpl implements TestServiceMethodMetadata {
	}

	static class UnusedTestServiceMethodMetadataImpl implements TestServiceMethodMetadata {
	}

	@Component
	@ServiceImplementationConditional(serviceInterface = TestServiceClassMetadata.class)
	static class TestServiceClassMetadataImpl implements TestServiceClassMetadata {
	}

	@Component
	@ServiceImplementationConditional(serviceInterface = TestServiceClassMetadata.class)
	static class UnusedTestServiceClassMetadataImpl implements TestServiceClassMetadata {
	}

	interface FooService {
	}

	@Component
	@ServiceImplementationConditional
	static class FooServiceImpl implements FooService {
	}
}
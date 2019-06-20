package org.ironrhino.core.spring.configuration;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

public class BeanPresentConditionTest {

	@Test
	public void testMethodMetadata() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TestBeanConfiguration.class);
		context.getBean(TestBean.class);
		context.getBean(BeanNamePresentBean.class);
		context.getBean(BeanTypePresentBean.class);
		expectNotPresent(context, BeanNameNotPresentBean.class);
		expectNotPresent(context, BeanTypeNotPresentBean.class);
		context.close();
	}

	@Test
	public void testClassMetadata() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestBean.class,
				BeanNamePresentClassMetadataBean.class, BeanNameNotPresentClassMetadataBean.class,
				BeanTypePresentClassMetadataBean.class, BeanTypeNotPresentClassMetadataBean.class);
		context.getBean(TestBean.class);
		context.getBean(BeanNamePresentClassMetadataBean.class);
		context.getBean(BeanTypePresentClassMetadataBean.class);
		expectNotPresent(context, BeanNameNotPresentClassMetadataBean.class);
		expectNotPresent(context, BeanTypeNotPresentClassMetadataBean.class);
		context.close();
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
	static class TestBeanConfiguration {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}

		@Bean
		@BeanPresentConditional("testBean")
		public BeanNamePresentBean beanNamePresentBean() {
			return new BeanNamePresentBean();
		}

		@Bean
		@BeanPresentConditional(type = TestBean.class)
		public BeanTypePresentBean beanTypePresentBean() {
			return new BeanTypePresentBean();
		}

		@Bean
		@BeanPresentConditional("beanPresentConditionTest")
		public BeanNameNotPresentBean beanNameNotPresentBean() {
			return new BeanNameNotPresentBean();
		}

		@Bean
		@BeanPresentConditional(type = BeanPresentConditionTest.class)
		public BeanTypeNotPresentBean beanTypeNotPresentBean() {
			return new BeanTypeNotPresentBean();
		}
	}

	@Component("testBean")
	static class TestBean {
	}

	static class BeanNamePresentBean {
	}

	static class BeanTypePresentBean {
	}

	static class BeanNameNotPresentBean {
	}

	static class BeanTypeNotPresentBean {
	}

	@Component
	@BeanPresentConditional("testBean")
	static class BeanNamePresentClassMetadataBean {
	}

	@Component
	@BeanPresentConditional("beanPresentConditionTest")
	static class BeanNameNotPresentClassMetadataBean {
	}

	@Component
	@BeanPresentConditional(type = TestBean.class)
	static class BeanTypePresentClassMetadataBean {
	}

	@Component
	@BeanPresentConditional(type = BeanPresentConditionTest.class)
	static class BeanTypeNotPresentClassMetadataBean {
	}
}
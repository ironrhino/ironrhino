package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import org.junit.Test;
import org.springframework.beans.BeanWrapperImpl;

import lombok.Data;

public class SampleObjectCreatorTest {

	@Test
	public void testNotNull() {
		SampleObjectCreator creator = new SampleObjectCreator();
		TestObject object = (TestObject) creator.createSample(TestObject.class);
		assertThat(object, not(nullValue()));
		BeanWrapperImpl bw = new BeanWrapperImpl(object);
		for (PropertyDescriptor pd : bw.getPropertyDescriptors())
			assertThat(bw.getPropertyValue(pd.getName()), not(nullValue()));
	}

	@Test
	public void testDeterministic() {
		SampleObjectCreator creator = new SampleObjectCreator();
		TestObject object1 = (TestObject) creator.createSample(TestObject.class);
		TestObject object2 = (TestObject) creator.createSample(TestObject.class);
		assertThat(object1, equalTo(object2));
	}

	@Test
	public void testRandom() {
		SampleObjectCreator creator = new SampleObjectCreator(true);
		TestObject object1 = (TestObject) creator.createSample(TestObject.class);
		TestObject object2 = (TestObject) creator.createSample(TestObject.class);
		assertThat(object1, not(equalTo(object2)));
		BeanWrapperImpl bw1 = new BeanWrapperImpl(object1);
		BeanWrapperImpl bw2 = new BeanWrapperImpl(object2);
		for (PropertyDescriptor pd : bw1.getPropertyDescriptors())
			if (!pd.getName().equals("class"))
				assertThat(bw1.getPropertyValue(pd.getName()), not(equalTo(bw2.getPropertyValue(pd.getName()))));
	}

	@Data
	public static class TestObject {
		private String string;
		private short shortValue;
		private int intValue;
		private float floatValue;
		private double doubleValue;
		private BigDecimal bigDecimal;
		private LocalDateTime localDateTime;
		private LocalDate localDate;
		private LocalTime localTime;
		private YearMonth yearMonth;
		private Duration duration;
	}
}

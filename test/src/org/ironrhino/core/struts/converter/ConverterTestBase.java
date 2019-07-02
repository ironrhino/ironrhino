package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.core.util.ReflectionUtils;
import org.junit.Test;

public abstract class ConverterTestBase<T extends StrutsTypeConverter> {

	T converter;

	@SuppressWarnings("unchecked")
	ConverterTestBase() {
		try {
			converter = (T) ReflectionUtils.getGenericClass(this.getClass()).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testNull() {
		assertThat(converter.convertFromString(null, new String[]{null}, null), is(nullValue()));
		assertThat(converter.convertFromString(null, new String[]{""}, null), is(nullValue()));
	}
}

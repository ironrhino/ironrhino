package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.struts2.util.StrutsTypeConverter;
import org.junit.Test;
import org.springframework.core.GenericTypeResolver;

public abstract class ConverterTestBase<T extends StrutsTypeConverter> {

	final T converter;

	@SuppressWarnings("unchecked")
	ConverterTestBase() {
		try {
			converter = (T) GenericTypeResolver.resolveTypeArgument(getClass(), ConverterTestBase.class)
					.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testNull() {
		assertThat(converter.convertFromString(null, new String[] { null }, null), is(nullValue()));
		assertThat(converter.convertFromString(null, new String[] { "" }, null), is(nullValue()));
	}
}

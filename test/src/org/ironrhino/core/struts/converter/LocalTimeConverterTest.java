package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;

public class LocalTimeConverterTest extends ConverterTestBase<LocalTimeConverter> {

	@Test
	public void convertFromString() {
		assertThat(converter.convertFromString(null, new String[]{"00:00:00"}, LocalTime.class), is(LocalTime.MIN));
		assertThat(converter.convertFromString(null, new String[]{"12:12:12.000000012"}, LocalDateTime.class),
				is(LocalTime.of(12, 12, 12, 12)));
		assertThat(converter.convertFromString(null, new String[]{"23:59:59.999999999"}, LocalTime.class),
				is(LocalTime.MAX));
	}

	@Test
	public void convertToString() {
		assertThat(converter.convertToString(null, LocalTime.MIN), is("00:00:00"));
		assertThat(converter.convertToString(null, LocalTime.of(12, 12, 12, 12)), is("12:12:12"));
		assertThat(converter.convertToString(null, LocalTime.MAX), is("23:59:59"));
	}
}
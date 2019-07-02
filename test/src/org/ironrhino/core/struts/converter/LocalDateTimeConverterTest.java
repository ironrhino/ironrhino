package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

public class LocalDateTimeConverterTest extends ConverterTestBase<LocalDateTimeConverter> {

	@Test
	public void convertFromString() {
		assertThat(converter.convertFromString(null, new String[]{"1970-01-01 12:00:00"}, LocalDateTime.class),
				is(LocalDateTime.of(1970, 1, 1, 12, 0)));
		assertThat(converter.convertFromString(null, new String[]{"2019-07-02 12:00:00"}, LocalDateTime.class),
				is(LocalDateTime.of(2019, 7, 2, 12, 0)));
	}

	@Test
	public void convertToString() {
		assertThat(converter.convertToString(null, LocalDateTime.of(1970, 1, 1, 12, 0)), is("1970-01-01 12:00:00"));
		assertThat(converter.convertToString(null, LocalDateTime.of(2019, 7, 2, 12, 0)), is("2019-07-02 12:00:00"));
	}
}
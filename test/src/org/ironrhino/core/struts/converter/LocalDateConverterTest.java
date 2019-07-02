package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.time.LocalDate;

import org.junit.Test;

public class LocalDateConverterTest extends ConverterTestBase<LocalDateConverter> {

	@Test
	public void convertFromString() {
		assertThat(converter.convertFromString(null, new String[]{"1970-01-01"}, LocalDate.class),
				is(LocalDate.of(1970, 1, 1)));
		assertThat(converter.convertFromString(null, new String[]{"2019-07-02"}, LocalDate.class),
				is(LocalDate.of(2019, 7, 2)));
	}

	@Test
	public void convertToString() {
		assertThat(converter.convertToString(null, LocalDate.of(1970, 1, 1)), is("1970-01-01"));
		assertThat(converter.convertToString(null, LocalDate.of(2019, 7, 2)), is("2019-07-02"));
		try {
			converter.convertToString(null, LocalDate.MIN); // -999999999-01-01
			fail("Expected arithmeticException: long overflow");
		} catch (Exception e) {
			// ignore
		}
		try {
			converter.convertToString(null, LocalDate.MAX); // +999999999-12-31
			fail("Expected arithmeticException: long overflow");
		} catch (Exception e) {
			// ignore
		}

	}
}
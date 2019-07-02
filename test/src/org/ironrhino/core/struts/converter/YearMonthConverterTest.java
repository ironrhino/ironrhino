package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.junit.Test;

public class YearMonthConverterTest extends ConverterTestBase<YearMonthConverter> {

	@Test
	public void convertFromString() {
		assertThat(converter.convertFromString(null, new String[]{"2019-07"}, YearMonth.class),
				is(YearMonth.of(2019, 7)));
		try {
			assertThat(converter.convertFromString(null, new String[]{"2019-7"}, YearMonth.class),
					is(YearMonth.of(2019, 7)));
			fail("Expected exception");
		} catch (DateTimeParseException e) {
			// ignore
		}
	}

	@Test
	public void convertToString() {
		assertThat(converter.convertToString(null, YearMonth.of(2019, 7)), is("2019-07"));
	}
}
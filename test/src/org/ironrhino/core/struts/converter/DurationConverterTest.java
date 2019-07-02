package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;

import org.junit.Test;

public class DurationConverterTest extends ConverterTestBase<DurationConverter> {

	@Test
	public void convertFromString() {
		assertThat(converter.convertFromString(null, new String[]{"PT100S"}, Duration.class),
				is(Duration.ofSeconds(100)));
		assertThat(converter.convertFromString(null, new String[]{"PT2400H"}, Duration.class),
				is(Duration.ofDays(100)));
	}

	@Test
	public void convertToString() {
		assertThat(converter.convertToString(null, Duration.ofDays(-10)), is("PT-240H"));
		assertThat(converter.convertToString(null, Duration.ofHours(10)), is("PT10H"));
		assertThat(converter.convertToString(null, Duration.ofSeconds(10)), is("PT10S"));
		assertThat(converter.convertToString(null, Duration.ofMillis(10)), is("PT0.01S"));
	}
}
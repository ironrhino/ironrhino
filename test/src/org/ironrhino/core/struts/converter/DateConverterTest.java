package org.ironrhino.core.struts.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.ironrhino.core.util.DateUtils;
import org.junit.Test;

public class DateConverterTest extends ConverterTestBase<DateConverter> {

	@Test
	public void convertFromString() {
		Date date = (Date) converter.convertFromString(null, new String[]{"2019-07-02 12:12:12"}, Date.class);
		assertThat(DateUtils.formatDatetime(date), is("2019-07-02 12:12:12"));
	}

	@Test
	public void convertToString() {
		assertThat(converter.convertToString(null, DateUtils.parseDatetime("2019-07-02 12:12:12")), is("2019-07-02"));
	}
}
package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;

import org.junit.Test;

public class DateUtilsTest {

	@Test
	public void testFormat() {
		Date date = DateUtils.parseDatetime("2012-12-12 12:12:12");
		assertThat(DateUtils.formatDate8(date), equalTo("20121212"));
		assertThat(DateUtils.formatDate10(date), equalTo("2012-12-12"));
		assertThat(DateUtils.formatDatetime(date), equalTo("2012-12-12 12:12:12"));
		assertThat(DateUtils.formatYearMonth(date), equalTo("201212"));
		assertThat(DateUtils.format(date, "yyyy-MM"), equalTo("2012-12"));
	}

	@Test
	public void testAddDays() {
		Date date = DateUtils.parseDatetime("2012-12-12 12:12:12");
		date = DateUtils.addDays(date, -1);
		assertThat(DateUtils.formatDatetime(date), equalTo("2012-12-11 12:12:12"));
	}

	@Test
	public void testGetIntervalDays() {
		Date date1 = DateUtils.parseDatetime("2012-12-12 12:12:12");
		Date date2 = DateUtils.addDays(date1, 1);
		assertThat(DateUtils.getIntervalDays(date1, date2), equalTo(2));
	}

	@Test
	public void testIsLeapYear() {
		assertThat(DateUtils.isLeapYear(2012), equalTo(true));
		assertThat(DateUtils.isLeapYear(2011), equalTo(false));
		assertThat(DateUtils.isLeapYear(2000), equalTo(true));
		assertThat(DateUtils.isLeapYear(2100), equalTo(false));
	}

	@Test
	public void testNextLeapDay() {
		Date since = DateUtils.parseDatetime("2011-12-12 12:12:12");
		Date leapDay = DateUtils.nextLeapDay(since);
		assertThat(DateUtils.formatDate10(leapDay), equalTo("2012-02-29"));
	}

	@Test
	public void testIsSpanLeapDay() {
		Date date1 = DateUtils.parseDatetime("2011-12-12 12:12:12");
		Date date2 = DateUtils.parseDatetime("2012-02-28 12:12:12");
		assertThat(DateUtils.isSpanLeapDay(date1, date2), equalTo(false));
		date2 = DateUtils.parseDatetime("2012-12-12 12:12:12");
		assertThat(DateUtils.isSpanLeapDay(date1, date2), equalTo(true));
	}

	@Test
	public void testBeginAndEndOfDay() {
		Date date = DateUtils.parseDatetime("2011-12-12 12:12:12");
		assertThat(DateUtils.formatDatetime(DateUtils.beginOfDay(date)), equalTo("2011-12-12 00:00:00"));
		assertThat(DateUtils.formatTimestamp(DateUtils.endOfDay(date)), equalTo("2011-12-12 23:59:59.999"));
	}

}

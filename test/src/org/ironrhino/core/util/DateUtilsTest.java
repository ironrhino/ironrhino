package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

public class DateUtilsTest {

	@Test
	public void testFormat() {
		Date date = DateUtils.parseDatetime("2012-12-12 12:12:12");
		assertEquals("20121212", DateUtils.formatDate8(date));
		assertEquals("2012-12-12", DateUtils.formatDate10(date));
		assertEquals("2012-12-12 12:12:12", DateUtils.formatDatetime(date));
		assertEquals("201212", DateUtils.formatYearMonth(date));
		assertEquals("2012-12", DateUtils.format(date, "yyyy-MM"));
	}

	@Test
	public void testAddDays() {
		Date date = DateUtils.parseDatetime("2012-12-12 12:12:12");
		date = DateUtils.addDays(date, -1);
		assertEquals("2012-12-11 12:12:12", DateUtils.formatDatetime(date));
	}

	@Test
	public void testGetIntervalDays() {
		Date date1 = DateUtils.parseDatetime("2012-12-12 12:12:12");
		Date date2 = DateUtils.addDays(date1, 1);
		assertEquals(2, DateUtils.getIntervalDays(date1, date2));
	}

	@Test
	public void testIsLeapYear() {
		assertEquals(true, DateUtils.isLeapYear(2012));
		assertEquals(false, DateUtils.isLeapYear(2011));
		assertEquals(true, DateUtils.isLeapYear(2000));
		assertEquals(false, DateUtils.isLeapYear(2100));
	}

	@Test
	public void testNextLeapDay() {
		Date since = DateUtils.parseDatetime("2011-12-12 12:12:12");
		Date leapDay = DateUtils.nextLeapDay(since);
		assertEquals("2012-02-29", DateUtils.formatDate10(leapDay));
	}

	@Test
	public void testIsSpanLeapDay() {
		Date date1 = DateUtils.parseDatetime("2011-12-12 12:12:12");
		Date date2 = DateUtils.parseDatetime("2012-02-28 12:12:12");
		assertEquals(false, DateUtils.isSpanLeapDay(date1, date2));
		date2 = DateUtils.parseDatetime("2012-12-12 12:12:12");
		assertEquals(true, DateUtils.isSpanLeapDay(date1, date2));
	}

	@Test
	public void testBeginAndEndOfDay() {
		Date date = DateUtils.parseDatetime("2011-12-12 12:12:12");
		assertEquals("2011-12-12 00:00:00", DateUtils.formatDatetime(DateUtils.beginOfDay(date)));
		assertEquals("2011-12-12 23:59:59.999", DateUtils.formatTimestamp(DateUtils.endOfDay(date)));
	}

}

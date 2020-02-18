package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.Test;

public class TypeUtilsTest {

	@Test
	public void testIsNumeric() {
		assertThat(TypeUtils.isNumeric(Short.class), equalTo(true));
		assertThat(TypeUtils.isNumeric(Long.TYPE), equalTo(true));
		assertThat(TypeUtils.isNumeric(BigDecimal.class), equalTo(true));
		assertThat(TypeUtils.isNumeric(String.class), equalTo(false));
	}
	
	@Test
	public void testIsIntegralNumeric() {
		assertThat(TypeUtils.isIntegralNumeric(Short.class), equalTo(true));
		assertThat(TypeUtils.isIntegralNumeric(Long.TYPE), equalTo(true));
		assertThat(TypeUtils.isIntegralNumeric(Integer.TYPE), equalTo(true));
		assertThat(TypeUtils.isIntegralNumeric(BigDecimal.class), equalTo(false));
		assertThat(TypeUtils.isIntegralNumeric(Float.class), equalTo(false));
		assertThat(TypeUtils.isIntegralNumeric(String.class), equalTo(false));
	}
	
	@Test
	public void testIsDecimalNumeric() {
		assertThat(TypeUtils.isDecimalNumeric(Short.class), equalTo(false));
		assertThat(TypeUtils.isDecimalNumeric(Long.TYPE), equalTo(false));
		assertThat(TypeUtils.isDecimalNumeric(Integer.TYPE), equalTo(false));
		assertThat(TypeUtils.isDecimalNumeric(BigDecimal.class), equalTo(true));
		assertThat(TypeUtils.isDecimalNumeric(Float.class), equalTo(true));
		assertThat(TypeUtils.isDecimalNumeric(String.class), equalTo(false));
	}

	@Test
	public void testIsTemporal() {
		assertThat(TypeUtils.isTemporal(Date.class), equalTo(true));
		assertThat(TypeUtils.isTemporal(java.sql.Date.class), equalTo(true));
		assertThat(TypeUtils.isTemporal(LocalDate.class), equalTo(true));
		assertThat(TypeUtils.isTemporal(LocalDateTime.class), equalTo(true));
		assertThat(TypeUtils.isTemporal(Short.class), equalTo(false));
		assertThat(TypeUtils.isTemporal(String.class), equalTo(false));
	}
}

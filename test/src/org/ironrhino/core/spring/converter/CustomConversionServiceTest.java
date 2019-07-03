package org.ironrhino.core.spring.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.sample.crud.Customer;
import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

import lombok.Getter;
import lombok.Setter;

public class CustomConversionServiceTest {

	private final CustomConversionService conversionService = CustomConversionService.getSharedInstance();

	@Test
	public void testStringToDate() {
		testStringToDate("2019-07-02 16:20:30", "2019-07-02 16:20:30");
		testStringToDate("2019-7-2 16:20:30", "2019-07-02 16:20:30");
		testStringToDate("2019-07-02", "2019-07-02 00:00:00");
		testStringToDate("2019-7-2", "2019-07-02 00:00:00");
		testStringToDate("2019/07/02", "2019-07-02 00:00:00");
		testStringToDate("2019/7/2", "2019-07-02 00:00:00");
		testStringToDate("2019-07-02T16:20:30", "2019-07-02 16:20:30");
		testStringToDate("2019-07-02T16:20:30.666", "2019-07-02 16:20:30");
		testStringToDate(DateUtils.parseDatetime("2019-07-02 16:20:30").getTime() + "", "2019-07-02 16:20:30");
	}

	private void testStringToDate(String source, String target) {
		Date date = conversionService.convert(source, Date.class);
		assertThat(DateUtils.formatDatetime(date), is(target));
	}

	@Test
	public void testStringToDuration() {
		assertThat(conversionService.convert("PT-100S", Duration.class), is(Duration.ofSeconds(-100)));
		assertThat(conversionService.convert("PT100S", Duration.class), is(Duration.ofSeconds(100)));
		assertThat(conversionService.convert("PT240H", Duration.class), is(Duration.ofDays(10)));
	}

	@Test
	public void testStringToLocalDate() {
		assertThat(conversionService.convert("2019-07-02", LocalDate.class), is(LocalDate.of(2019, 7, 2)));
	}

	@Test
	public void testStringToLocalDateTime() {
		assertThat(conversionService.convert("2019-07-02 16:40:30", LocalDateTime.class),
				is(LocalDateTime.of(2019, 7, 2, 16, 40, 30)));
	}

	@Test
	public void testStringToLocalTime() {
		assertThat(conversionService.convert("16:40:30", LocalTime.class), is(LocalTime.of(16, 40, 30)));
		assertThat(conversionService.convert("16:40:30.000000999", LocalTime.class), is(LocalTime.of(16, 40, 30, 999)));
		assertThat(conversionService.convert("00:00:00", LocalTime.class), is(LocalTime.MIN));
		assertThat(conversionService.convert("23:59:59.999999999", LocalTime.class), is(LocalTime.MAX));
	}

	@Test
	public void testStringToYearMonth() {
		assertThat(conversionService.convert("2019-07", YearMonth.class), is(YearMonth.of(2019, 7)));
	}

	@Test
	public void testStringToMap() {
		Map<String, Integer> expectedMap = new HashMap<>();
		expectedMap.put("1", 1);
		expectedMap.put("2", 2);
		expectedMap.put("3", 3);
		TypeDescriptor typeDescriptor = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(Integer.class));
		assertThat(conversionService.convert("{\"1\":1, \"2\":2, \"3\":3}", typeDescriptor), is(expectedMap));
		assertThat(conversionService.convert(JsonUtils.toJson(expectedMap), typeDescriptor), is(expectedMap));
		assertThat(conversionService.convert("{}", typeDescriptor), is(Collections.emptyMap()));
	}

	@Test
	public void testMapToString() {
		Map<String, Integer> map = new HashMap<>();
		map.put("1", 1);
		map.put("2", 2);
		map.put("3", 3);
		assertThat(conversionService.convert(map, String.class), is(JsonUtils.toJson(map)));
	}

	@Test
	public void testEnumToEnum() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(TestTargetEnum.class);
		assertThat(conversionService.convert(TestSourceEnum.BAR, typeDescriptor), is(TestTargetEnum.BAR));
		assertThat(conversionService.convert(TestSourceEnum.BAZ, typeDescriptor), is(TestTargetEnum.BAZ));
		assertThat(conversionService.convert(TestSourceEnum.FOO, typeDescriptor), is(nullValue()));
	}

	enum TestSourceEnum {
		BAR, BAZ, FOO
	}

	enum TestTargetEnum {
		BAR, BAZ
	}

	@Test
	public void testSerializableToSerializable() {
		String id = UUID.randomUUID().toString();
		Customer customer = conversionService.convert(id, Customer.class);
		assertThat(customer, is(notNullValue()));
		assertThat(customer.getId(), is(id));

		TestBean testBean = conversionService.convert(id, TestBean.class);
		assertThat(testBean, is(notNullValue()));
		assertThat(testBean.getId(), is(id));

		TestBeanWithInteger testBean1 = new TestBeanWithInteger();
		testBean1.setId(1);
		assertThat(conversionService.convert(testBean1, Integer.class), is(1));

		TestBeanWithInt testBean2 = new TestBeanWithInt();
		testBean2.setId(2);
		assertThat(conversionService.convert(testBean2, Integer.TYPE), is(2));

		TestBeanWithLongObject testBean3 = new TestBeanWithLongObject();
		testBean3.setId(3L);
		assertThat(conversionService.convert(testBean3, Long.class), is(3L));

		TestBeanWithLong testBean4 = new TestBeanWithLong();
		testBean4.setId(4L);
		assertThat(conversionService.convert(testBean4, Long.TYPE), is(4L));

		TestBeanWithString testBean5 = new TestBeanWithString();
		testBean5.setId("5");
		assertThat(conversionService.convert(testBean5, String.class), is("5"));

		TestBeanWithString fromTestBean1 = conversionService.convert(testBean1, TestBeanWithString.class);
		assertThat(fromTestBean1, is(notNullValue()));
		assertThat(fromTestBean1.getId(), is("1"));

		TestBeanWithInteger fromTestBean5 = conversionService.convert(testBean5, TestBeanWithInteger.class);
		assertThat(fromTestBean5, is(notNullValue()));
		assertThat(fromTestBean5.getId(), is(5));
	}

	@Setter
	@Getter
	static class TestBean implements Serializable {

		private static final long serialVersionUID = 4276415823156283448L;

		private String id;

		public TestBean(String id) {
			this.id = id;
		}
	}

	@Setter
	@Getter
	public static class TestBeanWithLong implements Serializable {
		private static final long serialVersionUID = 3489096393194880323L;
		private long id;
	}

	@Setter
	@Getter
	public static class TestBeanWithLongObject implements Serializable {
		private static final long serialVersionUID = -9039106015259338766L;
		private Long id;
	}

	@Setter
	@Getter
	public static class TestBeanWithInt implements Serializable {
		private static final long serialVersionUID = -356370593069089147L;
		private int id;
	}

	@Setter
	@Getter
	public static class TestBeanWithInteger implements Serializable {
		private static final long serialVersionUID = -6752860308801626730L;
		private Integer id;
	}

	@Setter
	@Getter
	public static class TestBeanWithString implements Serializable {
		private static final long serialVersionUID = 112845643427186972L;
		private String id;
	}

}
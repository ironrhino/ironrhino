package com.opensymphony.xwork2.conversion.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.opensymphony.xwork2.XWorkJUnit4TestCase;

public class XWorkBasicConverterTest extends XWorkJUnit4TestCase {

	private XWorkBasicConverter basicConverter;

	private Map<String, Object> context;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		basicConverter = container.getInstance(XWorkBasicConverter.class);
		context = Collections.singletonMap("_typeConverter", basicConverter);
	}

	private <T> void testConvertToExpectedValue(Object value, T expectedValue, Class<T> toClass) {
		assertThat(basicConverter.convertValue(context, null, null, null, value, toClass), is(expectedValue));
		if (!toClass.isArray() && !Collection.class.isAssignableFrom(toClass)) {
			assertThat(basicConverter.convertValue(context, null, null, null, new Object[]{value}, toClass),
					is(expectedValue));
		}
	}

	private void testConvertToNullValue(Object value, Class<?> toClass) {
		assertThat(basicConverter.convertValue(context, null, null, null, value, toClass), is(nullValue()));
	}

	@Test
	public void testConvertToPrimitiveBooleanType() {
		testConvertToExpectedValue("true", true, boolean.class);
		testConvertToExpectedValue("yes", true, boolean.class);
		testConvertToExpectedValue("on", true, boolean.class);
		testConvertToExpectedValue("1", true, boolean.class);

		testConvertToExpectedValue("false", false, boolean.class);
		testConvertToExpectedValue("no", false, boolean.class);
		testConvertToExpectedValue("off", false, boolean.class);
		testConvertToExpectedValue("0", false, boolean.class);

		testConvertToExpectedValue("", false, boolean.class);

		testConvertToExpectedValue(new String[]{"true", "yes", "on", "1", "false", "no", "off", "0", ""},
				new boolean[]{true, true, true, true, false, false, false, false, false}, boolean[].class);
	}

	@Test
	public void testConvertToBooleanObject() {
		testConvertToExpectedValue("true", Boolean.TRUE, Boolean.class);
		testConvertToExpectedValue("yes", Boolean.TRUE, Boolean.class);
		testConvertToExpectedValue("on", Boolean.TRUE, Boolean.class);
		testConvertToExpectedValue("1", Boolean.TRUE, Boolean.class);

		testConvertToExpectedValue("false", Boolean.FALSE, Boolean.class);
		testConvertToExpectedValue("no", Boolean.FALSE, Boolean.class);
		testConvertToExpectedValue("off", Boolean.FALSE, Boolean.class);
		testConvertToExpectedValue("0", Boolean.FALSE, Boolean.class);

		testConvertToNullValue("", Boolean.class);

		testConvertToExpectedValue(new String[]{"true", "yes", "on", "1", "false", "no", "off", "0", ""},
				new Boolean[]{true, true, true, true, false, false, false, false, null}, Boolean[].class);
	}

	@Test
	public void testConvertToChar() {
		testConvertToNullValue("", char.class);
		testConvertToExpectedValue("a", 'a', char.class);
		testConvertToExpectedValue("b", 'b', char.class);
		testConvertToExpectedValue(0, (char) 0, char.class);
		testConvertToExpectedValue(1, (char) 1, char.class);
		testConvertToExpectedValue(true, (char) 1, char.class);
		testConvertToExpectedValue(false, (char) 0, char.class);
	}

	@Test
	public void testConvertToCharacter() {
		testConvertToNullValue("", Character.class);
		testConvertToExpectedValue("a", 'a', Character.class);
		testConvertToExpectedValue("b", 'b', Character.class);
		testConvertToExpectedValue(0, (char) 0, Character.class);
		testConvertToExpectedValue(1, (char) 1, Character.class);
		testConvertToExpectedValue(true, (char) 1, Character.class);
		testConvertToExpectedValue(false, (char) 0, Character.class);
	}

	@Test
	public void testConvertToNumber() {
		for (int i = -10; i < 10; i++) {
			testConvertToExpectedValue("" + i, (byte) i, byte.class);
			testConvertToExpectedValue("" + i, (byte) i, Byte.class);
			testConvertToExpectedValue("" + i, (short) i, short.class);
			testConvertToExpectedValue("" + i, (short) i, Short.class);
			testConvertToExpectedValue("" + i, i, int.class);
			testConvertToExpectedValue("" + i, i, Integer.class);
			testConvertToExpectedValue("" + i, (long) i, long.class);
			testConvertToExpectedValue("" + i, (long) i, Long.class);
		}
		testConvertToExpectedValue("123456789123456789", new BigInteger("123456789123456789"), BigInteger.class);
		testConvertToExpectedValue("0.123456789123456789", new BigDecimal("0.123456789123456789"), BigDecimal.class);

		for (float i = 1; i > 0.0001; i /= 2) {
			testConvertToExpectedValue("" + i, i, float.class);
			testConvertToExpectedValue("" + i, i, Float.class);
		}
		for (double i = 1; i > 0.0001; i /= 2) {
			testConvertToExpectedValue("" + i, i, double.class);
			testConvertToExpectedValue("" + i, i, Double.class);
		}
		testConvertToNullValue("", Byte.class);
		testConvertToNullValue("", Short.class);
		testConvertToNullValue("", Integer.class);
		testConvertToNullValue("", Long.class);
		testConvertToNullValue("", Float.class);
		testConvertToNullValue("", Double.class);

		Class<?>[] primitiveNumberTypes = new Class<?>[]{byte.class, short.class, int.class, long.class};
		for (Class<?> primitiveNumberType : primitiveNumberTypes) {
			try {
				basicConverter.convertValue(context, null, null, null, "", primitiveNumberType);
				fail("Expect exception");
			} catch (Exception e) {
				// ignore
			}
		}
		testConvertToExpectedValue("", 0.0D, double.class);
		testConvertToExpectedValue("", 0.0f, float.class);

	}

	@Test
	public void testConvertToCollection() {
		testConvertToExpectedValue(new Object[]{"1", "2", "3", "4"}, Arrays.asList("1", "2", "3", "4"), List.class);
		testConvertToExpectedValue(new Object[]{"1", "2", "3", "4"}, new HashSet<>(Arrays.asList("1", "2", "3", "4")),
				Set.class);
	}
}
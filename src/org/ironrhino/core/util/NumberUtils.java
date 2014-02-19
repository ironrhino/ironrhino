package org.ironrhino.core.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class NumberUtils {

	public static String format(int value, int digit) {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(false);
		nf.setMinimumIntegerDigits(digit);
		return nf.format(value);
	}

	public static String format(double value, int fractionDigits) {
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(fractionDigits);
		nf.setRoundingMode(RoundingMode.DOWN);
		return nf.format(value);
	}

	public static double round(double value, int decimalDigits) {
		return round(new BigDecimal(String.valueOf(value)), decimalDigits)
				.doubleValue();
	}

	public static BigDecimal round(BigDecimal bd, int decimalDigits) {
		if (bd == null)
			return null;
		int precision = decimalDigits;
		String s = String.valueOf(bd.intValue());
		if (!s.equals("0"))
			precision += s.length();
		return new BigDecimal(bd.round(
				new MathContext(precision, RoundingMode.HALF_UP)).doubleValue());
	}

	public static String formatPercent(double value, int fractionDigits) {
		value *= 100;
		return format(value, fractionDigits) + "%";
	}

	public static final String NUMBERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-";

	public static String decimalToX(int scale, BigInteger decimalValue) {
		if (scale > NUMBERS.length())
			throw new IllegalArgumentException("n must less or equal to "
					+ NUMBERS.length());
		BigInteger bscale = BigInteger.valueOf(scale);
		boolean negative = decimalValue.compareTo(BigInteger.valueOf(0)) < 0;
		if (negative)
			decimalValue = decimalValue.abs();
		StringBuilder sb = new StringBuilder();

		String sub = NUMBERS.substring(0, scale);
		while (decimalValue.compareTo(BigInteger.valueOf(0)) != 0) {
			BigInteger b = decimalValue.mod(bscale);
			sb.insert(0, sub.charAt(b.intValue()));
			decimalValue = decimalValue.add(b.negate()).divide(bscale);
		}
		if (negative)
			sb.insert(0, '-');
		return sb.toString();
	}

	public static BigInteger xToDecimal(int scale, String xScaleValue) {
		if (scale > NUMBERS.length())
			throw new IllegalArgumentException("n must less or equal to "
					+ NUMBERS.length());
		BigInteger bscale = BigInteger.valueOf(scale);
		String a = NUMBERS.substring(0, scale);
		BigInteger c = BigInteger.valueOf(1);
		BigInteger t = BigInteger.valueOf(0);
		for (int x = xScaleValue.length() - 1; x > -1; x--) {
			t = t.add(c.multiply(BigInteger.valueOf(a.indexOf(xScaleValue
					.charAt(x)))));
			c = c.multiply(bscale);
		}
		return t;
	}

	public static String xToY(int xScale, int yScale, String value) {
		return decimalToX(yScale, xToDecimal(xScale, value));
	}

	private static final String[] CNUMBERS = { "零", "壹", "贰", "叁", "肆", "伍",
			"陆", "柒", "捌", "玖" };
	private static final String[] IUNIT = { "元", "拾", "佰", "仟", "万", "拾", "佰",
			"仟", "亿", "拾", "佰", "仟", "万", "拾", "佰", "仟" };
	private static final String[] DUNIT = { "角", "分", "厘" };

	public static String toChineseUpperCase(double input) {
		return _toChineseUpperCase(new BigDecimal(String.valueOf(input))
				.toString());
	}

	public static String toChineseUpperCase(BigDecimal input) {
		return _toChineseUpperCase(input.toString());
	}

	private static String _toChineseUpperCase(String input) {
		String integer;
		String decimal;
		if (input.indexOf(".") > 0) {
			integer = input.substring(0, input.indexOf("."));
			decimal = input.substring(input.indexOf(".") + 1);
		} else if (input.indexOf(".") == 0) {
			integer = "";
			decimal = input.substring(1);
		} else {
			integer = input;
			decimal = "";
		}
		if (!integer.equals("")) {
			integer = Long.toString(Long.parseLong(integer));
			if (integer.equals("0")) {
				integer = "";
			}
		}
		if (integer.length() > IUNIT.length) {
			return input;
		}
		int[] integers = _toArray(integer);
		boolean isMust5;
		int length = integer.length();
		if (length > 4) {
			String subInteger = "";
			if (length > 8) {
				subInteger = integer.substring(length - 8, length - 4);
			} else {
				subInteger = integer.substring(0, length - 4);
			}
			isMust5 = Integer.parseInt(subInteger) > 0;
		} else {
			isMust5 = false;
		}
		int[] decimals = _toArray(decimal);
		return _getChineseUpperCaseInteger(integers, isMust5)
				+ _getChineseUpperCaseDecimal(decimals);
	}

	private static int[] _toArray(String number) {
		int[] array = new int[number.length()];
		for (int i = 0; i < number.length(); i++) {
			array[i] = Integer.parseInt(number.substring(i, i + 1));
		}
		return array;
	}

	private static String _getChineseUpperCaseInteger(int[] integers,
			boolean isMust5) {
		StringBuffer chineseInteger = new StringBuffer("");
		int length = integers.length;
		for (int i = 0; i < length; i++) {
			String key = "";
			if (integers[i] == 0) {
				if ((length - i) == 13)
					key = IUNIT[4];
				else if ((length - i) == 9)
					key = IUNIT[8];
				else if ((length - i) == 5 && isMust5)
					key = IUNIT[4];
				else if ((length - i) == 1)
					key = IUNIT[0];
				if ((length - i) > 1 && integers[i + 1] != 0)
					key += CNUMBERS[0];
			}
			chineseInteger.append(integers[i] == 0 ? key
					: (CNUMBERS[integers[i]] + IUNIT[length - i - 1]));
		}
		return chineseInteger.toString();
	}

	private static String _getChineseUpperCaseDecimal(int[] decimals) {
		StringBuffer chineseDecimal = new StringBuffer("");
		for (int i = 0; i < decimals.length; i++) {
			if (i == 3)
				break;
			chineseDecimal.append(decimals[i] == 0 ? ""
					: (CNUMBERS[decimals[i]] + DUNIT[i]));
		}
		return chineseDecimal.toString();
	}

}

package org.ironrhino.core.validation.validators;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.NumberUtils;
import org.ironrhino.core.validation.constraints.MobilePhoneNumber;

public class MobilePhoneNumberValidator implements ConstraintValidator<MobilePhoneNumber, String> {

	@Override
	public boolean isValid(String input, ConstraintValidatorContext constraintValidatorContext) {
		if (StringUtils.isEmpty(input))
			return true;
		return isValid(input);
	}

	public static boolean isValid(String input) {
		if (input.startsWith("+86"))
			input = input.substring(3);
		if (!StringUtils.isNumeric(input) || input.length() != 11)
			return false;
		for (Carrier car : Carrier.values())
			for (String prefix : car.getNumberPrefixes())
				if (input.startsWith(prefix))
					return true;
		return false;
	}

	public static enum Carrier {
		// https://zh.wikipedia.org/wiki/%E4%B8%AD%E5%9B%BD%E5%86%85%E5%9C%B0%E7%A7%BB%E5%8A%A8%E7%BB%88%E7%AB%AF%E9%80%9A%E8%AE%AF%E5%8F%B7%E6%AE%B5
		MOBILE("134", "135", "136", "137", "138", "139", "144", "147", "148", "150", "151", "152", "157", "158", "159",
				"165", "172", "178", "182", "183", "184", "187", "188", "195", "197", "198", "1703", "1705", "1706"),

		UNICOM("130", "131", "132", "140", "145", "146", "155", "156", "166", "167", "171", "175", "176", "185", "186",
				"196", "1704", "1707", "1708", "1709"),

		TELECOM("133", "141", "149", "153", "173", "174", "177", "180", "181", "189", "191", "193", "199", "1349",
				"1700", "1701", "1702"),

		BROADCASTING("192"),

		UNKNOWN("142", "143", "154", "161", "162", "164", "190", "194");

		private List<String> numberPrefixes;

		private Carrier(String... strings) {
			numberPrefixes = Collections.unmodifiableList(Arrays.asList(strings));
		}

		public List<String> getNumberPrefixes() {
			return numberPrefixes;
		}

		public static Carrier parse(String number) {
			if (StringUtils.isBlank(number))
				return null;
			for (Carrier c : values()) {
				for (String prefix : c.getNumberPrefixes()) {
					if (number.startsWith(prefix))
						return c;
				}
			}
			throw new RuntimeException("Unknown number: " + number);
		}

	}

	public static String randomValue() {
		Random random = new Random();
		Carrier carrier = Carrier.values()[random.nextInt(Carrier.values().length)];
		String prefix = carrier.numberPrefixes.get(random.nextInt(carrier.numberPrefixes.size()));
		int digit = 11 - prefix.length();
		int value = 1;
		for (int i = 0; i < digit; i++)
			value *= 10;
		return prefix + NumberUtils.format(random.nextInt(value), digit);
	}

}
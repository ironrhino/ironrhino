package org.ironrhino.core.validation.validators;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
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

		MOBILE("134", "135", "136", "137", "138", "139", "147", "150", "151", "152", "157", "158", "159", "178", "182",
				"183", "184", "187", "188", "198", "1703", "1705", "1706"),

		UNICOM("130", "131", "132", "140", "145", "146", "155", "156", "166", "171", "175", "176", "185", "186", "1704",
				"1707", "1708", "1709"),

		TELECOM("133", "149", "153", "173", "174", "177", "180", "181", "189", "199", "1349", "1700", "1701", "1702"),

		UNKNOWN("141", "142", "143", "144", "154");

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

}
package org.ironrhino.core.validation.validators;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.validation.constraints.CitizenIdentificationNumber;

/**
 * GB11643-1999
 */
public class CitizenIdentificationNumberValidator implements ConstraintValidator<CitizenIdentificationNumber, String> {

	@Override
	public boolean isValid(String input, ConstraintValidatorContext constraintValidatorContext) {
		if (StringUtils.isEmpty(input))
			return true;
		return isValid(input);
	}

	public static boolean isValid(String input) {
		if (input == null || input.length() != 18)
			return false;
		char[] bits = new char[input.length() - 1];
		char checkBit = 0;
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			if ((ch < '0' || ch > '9') && (ch != 'X' && i == input.length() - 1))
				return false;
			if (i < input.length() - 1)
				bits[i] = ch;
			else
				checkBit = ch;
		}
		String province = input.substring(0, 2);
		if (!provinces.contains(province))
			return false;
		String dob = input.substring(6, 14);
		try {
			new SimpleDateFormat("yyyyMMdd").parse(dob);
		} catch (ParseException pe) {
			return false;
		}
		return getCheckBit(getPowerSum(bits)) == checkBit;
	}

	private static int getPowerSum(char[] bits) {
		int sum = 0;
		for (int i = 0; i < bits.length; i++) {
			int bit = bits[i] - '0';
			sum += bit * power[i];
		}
		return sum;
	}

	private static char getCheckBit(int sum) {
		int modulus = sum % 11;
		return modulus == 0 ? '1' : modulus == 1 ? '0' : modulus == 2 ? 'X' : (char) ('0' + (12 - modulus));
	}

	private static final List<String> provinces = Arrays.asList(new String[] { "11", "12", "13", "14", "15", "21", "22",
			"23", "31", "32", "33", "34", "35", "36", "37", "41", "42", "43", "44", "45", "46", "50", "51", "52", "53",
			"54", "61", "62", "63", "64", "65", "71", "81", "82", "91" });

	private static final int[] power = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
}
package org.ironrhino.core.validation.validators;

import java.util.Arrays;
import java.util.Random;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.NumberUtils;
import org.ironrhino.core.validation.constraints.OrganizationCode;

/**
 * GB11714-1997
 */
public class OrganizationCodeValidator implements ConstraintValidator<OrganizationCode, String> {

	@Override
	public boolean isValid(String input, ConstraintValidatorContext constraintValidatorContext) {
		if (input == null)
			return true;
		return isValid(input);
	}

	public static boolean isValid(String input) {
		if (input == null || input.length() != 9 || !StringUtils.isAlphanumeric(input))
			return false;
		char[] bits = Arrays.copyOfRange(input.toCharArray(), 0, input.length() - 1);
		char checkBit = input.charAt(input.length() - 1);
		return getCheckBit(getPowerSum(bits)) == checkBit;
	}

	private static int getPowerSum(char[] bits) {
		int sum = 0;
		for (int i = 0; i < bits.length; i++) {
			char ch = bits[i];
			int bit = ch > '9' ? (ch - 'A' + 10) : bits[i] - '0';
			sum += bit * power[i];
		}
		return sum;
	}

	private static char getCheckBit(int sum) {
		int i = 11 - sum % 11;
		return i == 10 ? 'X' : i == 11 ? '0' : (char) (i + '0');
	}

	private static final int[] power = { 3, 7, 9, 10, 5, 8, 4, 2 };

	public static String randomValue() {
		Random random = new Random();
		String seq = NumberUtils.format(10000000 + random.nextInt(89999999), 8);
		return seq + getCheckBit(getPowerSum(seq.toCharArray()));
	}

}
package org.ironrhino.core.validation.validators;

import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.validation.constraints.SocialCreditIdentifier;

/**
 * GB32100-2015
 */
public class SocialCreditIdentifierValidator implements ConstraintValidator<SocialCreditIdentifier, String> {

	@Override
	public boolean isValid(String input, ConstraintValidatorContext constraintValidatorContext) {
		if (input == null)
			return true;
		return isValid(input);
	}

	public static boolean isValid(String input) {
		if (input == null || input.length() != 18 || !StringUtils.isAlphanumeric(input))
			return false;
		char ch = input.charAt(0);
		if (ch != '1' && ch != '5' && ch != '9' && ch != 'Y')
			return false;
		ch = input.charAt(1);
		if (ch != '1' && ch != '2' && ch != '3' && ch != '9')
			return false;
		String province = input.substring(2, 4);
		if (!provinces.contains(province))
			return false;
		String organizationCode = input.substring(8, 17);
		if (!OrganizationCodeValidator.isValid(organizationCode))
			return false;
		char[] bits = Arrays.copyOfRange(input.toCharArray(), 0, input.length() - 1);
		char checkBit = input.charAt(input.length() - 1);
		return getCheckBit(getPowerSum(bits)) == checkBit;
	}

	private static int getPowerSum(char[] bits) {
		int sum = 0;
		for (int i = 0; i < bits.length; i++) {
			char ch = bits[i];
			int bit = Character.isDigit(ch) ? (int) bits[i] - '0' : characters.indexOf(ch) + 10;
			sum += bit * power[i];
		}
		return sum;
	}

	private static char getCheckBit(int sum) {
		int i = 31 - sum % 31;
		return i < 10 ? (char) (i + '0') : characters.charAt(i - 10);
	}

	private static final List<String> provinces = Arrays.asList(new String[] { "11", "12", "13", "14", "15", "21", "22",
			"23", "31", "32", "33", "34", "35", "36", "37", "41", "42", "43", "44", "45", "46", "50", "51", "52", "53",
			"54", "61", "62", "63", "64", "65", "71", "81", "82", "91" });

	private static final int[] power = { 1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28 };

	private static final String characters = "ABCDEFGHJKLMNPQRTUWXY";

}
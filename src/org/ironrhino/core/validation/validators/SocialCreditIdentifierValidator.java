package org.ironrhino.core.validation.validators;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.validation.constraints.SocialCreditIdentifier;

/**
 * GB32100-2015
 * https://zh.wikisource.org/zh-hans/GB_32100-2015_%E6%B3%95%E4%BA%BA%E5%92%8C%E5%85%B6%E4%BB%96%E7%BB%84%E7%BB%87%E7%BB%9F%E4%B8%80%E7%A4%BE%E4%BC%9A%E4%BF%A1%E7%94%A8%E4%BB%A3%E7%A0%81%E7%BC%96%E7%A0%81%E8%A7%84%E5%88%99
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
		if (!(ch >= '1' && ch <= '9' || ch == 'A' || ch == 'N' || ch == 'Y'))
			return false;
		ch = input.charAt(1);
		if (!(ch >= '1' && ch <= '5' || ch == '9'))
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
			int bit = Character.isDigit(ch) ? bits[i] - '0' : characters.indexOf(ch) + 10;
			sum += bit * power[i];
		}
		return sum;
	}

	private static char getCheckBit(int sum) {
		int i = 31 - sum % 31;
		if (i == 31)
			i = 0;
		return i < 10 ? (char) (i + '0') : characters.charAt(i - 10);
	}

	private static final List<String> provinces = Arrays.asList(new String[] { "11", "12", "13", "14", "15", "21", "22",
			"23", "31", "32", "33", "34", "35", "36", "37", "41", "42", "43", "44", "45", "46", "50", "51", "52", "53",
			"54", "61", "62", "63", "64", "65", "71", "81", "82", "91" });

	private static final int[] power = { 1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28 };

	private static final String characters = "ABCDEFGHJKLMNPQRTUWXY";

	public static String randomValue() {
		Random random = new Random();
		String province = provinces.get(random.nextInt(provinces.size())) + '0';
		String area = String.valueOf(1 + random.nextInt(3)) + String.valueOf(1 + random.nextInt(3))
				+ String.valueOf(1 + random.nextInt(7));
		String organizationCode = OrganizationCodeValidator.randomValue();
		String s = String.valueOf(1 + random.nextInt(9)) + String.valueOf(1 + random.nextInt(5)) + province + area
				+ organizationCode;
		return s + getCheckBit(getPowerSum(s.toCharArray()));
	}

}
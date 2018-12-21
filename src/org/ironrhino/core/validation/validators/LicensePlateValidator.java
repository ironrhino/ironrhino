package org.ironrhino.core.validation.validators;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.validation.constraints.LicensePlate;

/*
 * GA36－2007
 */
public class LicensePlateValidator implements ConstraintValidator<LicensePlate, String> {

	@Override
	public boolean isValid(String input, ConstraintValidatorContext constraintValidatorContext) {
		if (StringUtils.isEmpty(input))
			return true;
		return isValid(input);
	}

	public static boolean isValid(String input) {
		if (input == null || input.length() != 7 && input.length() != 8)
			return false;
		String a = input.substring(0, 1);
		String prefix = prefixes.get(a);
		if (prefix != null) {
			if (prefix.indexOf(input.charAt(1)) < 0)
				return false;
			String s = input.substring(2);
			if (!s.toUpperCase(Locale.ROOT).equals(s) || !StringUtils.isAlphanumeric(s) || StringUtils.isAlpha(s))
				return false;
			return true;
		} else {
			return false;
		}
	}

	private static final Map<String, String> prefixes = new LinkedHashMap<>();

	static {
		prefixes.put("京", "ABCDEFGHJKLMNPGY");
		prefixes.put("沪", "ABCDEGHJKLMNR");
		prefixes.put("津", "ABCDEFGHJKLMNQ");
		prefixes.put("渝", "ABCFGH");
		prefixes.put("宁", "ABCDE");
		prefixes.put("琼", "ABCDEF");
		prefixes.put("藏", "ABCDEFGHJ");
		prefixes.put("青", "ABCDEFGH");
		prefixes.put("川", "ABCDEFGHJKLMQRSTUVWXYZ");
		prefixes.put("粤", "ABCDEFGHJKLMNPQRSTUVWXYZ");
		prefixes.put("吉", "ABCDEFGHJK");
		prefixes.put("闽", "ABCDEFGHJK");
		prefixes.put("贵", "ABCDEFGHJ");
		prefixes.put("晋", "ABCDEFHJKLM");
		prefixes.put("蒙", "ABCDEFGHJKLM");
		prefixes.put("陕", "ABCDEFGHJKV");
		prefixes.put("鄂", "ABCDEFGHJKLMNPQRS");
		prefixes.put("桂", "ABCDEFGHJKLMNPR");
		prefixes.put("甘", "ABCDEFGHJKLMNP");
		prefixes.put("苏", "ABCDEFGHJKLMN");
		prefixes.put("浙", "ABCDEFGHJKL");
		prefixes.put("赣", "ABCDEFGHJKLM");
		prefixes.put("皖", "ABCDEFGHJKLMNPRS");
		prefixes.put("鲁", "ABCDEFGHJKLMNPQRSUVWY");
		prefixes.put("新", "ABCDEFGHJKLMNPQR");
		prefixes.put("辽", "ABCDEFGHJKLMNP");
		prefixes.put("黑", "ABCDEFGHJKLMNPR");
		prefixes.put("湘", "ABCDEFGHJKLMNUS");
		prefixes.put("冀", "ABCDEFGHJRT");
		prefixes.put("豫", "ABCDEFGHJKLMNPQRSU");
		prefixes.put("云", "ACDEFGHJKLMNPQRS");
	}
}
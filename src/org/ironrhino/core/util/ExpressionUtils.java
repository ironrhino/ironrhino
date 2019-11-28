package org.ironrhino.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExpressionUtils {

	public static Object evalExpression(String expression, Map<String, ?> context) {
		if (StringUtils.isBlank(expression))
			return expression;
		try {
			return ExpressionEngine.SPEL.evalExpression(expression, context);
		} catch (Exception e) {
			return ExpressionEngine.MVEL.evalExpression(expression, context);
		}
	}

	public static Object eval(String template, Map<String, ?> context) {
		if (StringUtils.isBlank(template))
			return template;
		if (template.contains("@{") || template.contains("@if{"))
			return ExpressionEngine.MVEL.eval(template, context);
		try {
			return ExpressionEngine.SPEL.eval(template, context);
		} catch (Exception e) {
			return ExpressionEngine.MVEL.eval(template, context);
		}
	}

	public static String evalString(String template, Map<String, ?> context) {
		Object obj = eval(template, context);
		if (obj == null)
			return null;
		return obj.toString();
	}

	public static boolean evalBoolean(String template, Map<String, ?> context, boolean defaultValue) {
		if (StringUtils.isBlank(template))
			return defaultValue;
		Object obj = eval(template, context);
		if (obj == null)
			return defaultValue;
		if (obj instanceof Boolean)
			return (Boolean) obj;
		return Boolean.parseBoolean(obj.toString());
	}

	public static int evalInt(String template, Map<String, ?> context, int defaultValue) {
		if (StringUtils.isBlank(template))
			return defaultValue;
		Object obj = eval(template, context);
		if (obj == null)
			return defaultValue;
		if (obj instanceof Integer)
			return (Integer) obj;
		return Integer.parseInt(obj.toString());
	}

	public static long evalLong(String template, Map<String, ?> context, long defaultValue) {
		if (StringUtils.isBlank(template))
			return defaultValue;
		Object obj = eval(template, context);
		if (obj == null)
			return defaultValue;
		if (obj instanceof Long)
			return (Long) obj;
		return Long.parseLong(obj.toString());
	}

	public static double evalDouble(String template, Map<String, ?> context, double defaultValue) {
		if (StringUtils.isBlank(template))
			return defaultValue;
		Object obj = eval(template, context);
		if (obj == null)
			return defaultValue;
		if (obj instanceof Double)
			return (Double) obj;
		return Double.parseDouble(obj.toString());
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> evalList(String template, Map<String, ?> context) {
		Object obj = eval(template, context);
		if (obj == null)
			return null;
		if (obj instanceof List)
			return (List<T>) obj;
		return (List<T>) Arrays.asList(obj.toString().split("\\s*,\\s*"));
	}

}

package org.ironrhino.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExpressionUtils {

	private static Map<String, CompiledTemplate> templateCache = new ConcurrentHashMap<>();

	private static Map<String, CompiledExpression> expressionCache = new ConcurrentHashMap<>();

	private static ParserContext parserContext = new ParserContext();
	static {
		try {
			parserContext.addImport("max", MathUtils.class.getMethod("max", Number[].class));
			parserContext.addImport("min", MathUtils.class.getMethod("min", Number[].class));
			parserContext.addImport("sum", MathUtils.class.getMethod("sum", Number[].class));
			parserContext.addImport("avg", MathUtils.class.getMethod("avg", Number[].class));
		} catch (Exception e) {
			e.printStackTrace();
		}
		parserContext.addImport("java.lang.System", Object.class);
		parserContext.addImport("System", Object.class);
		parserContext.addImport("Runtime", Object.class);
		parserContext.addImport("Class", Object.class);
	}

	public static Object evalExpression(String expression, Map<String, ?> context) {
		if (StringUtils.isBlank(expression))
			return expression;
		if (expression.contains("java.") || expression.contains("javax."))
			throw new IllegalArgumentException("Illegal expression: " + expression);
		CompiledExpression ce = expressionCache.computeIfAbsent(expression,
				key -> new ExpressionCompiler(key, parserContext).compile());
		return MVEL.executeExpression(ce, context);
	}

	public static Object eval(String template, Map<String, ?> context) {
		if (StringUtils.isBlank(template))
			return template;
		CompiledTemplate ct = templateCache.computeIfAbsent(template,
				key -> new TemplateCompiler(key, false, parserContext).compile());
		return TemplateRuntime.execute(ct, context);
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

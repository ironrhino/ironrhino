package org.ironrhino.core.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {

	static final Map<String, Method> mathMethods = new HashMap<>();

	static {
		for (Method m : MathUtils.class.getMethods())
			mathMethods.put(m.getName(), m);
	}

	@SafeVarargs
	public static <T extends Number> T max(T... numbers) {
		return Stream.of(numbers).max(Comparator.comparing(Number::doubleValue)).orElse(null);
	}

	@SafeVarargs
	public static <T extends Number> T min(T... numbers) {
		return Stream.of(numbers).min(Comparator.comparing(Number::doubleValue)).orElse(null);
	}

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T extends Number> T sum(T... numbers) {
		if (numbers.length == 0)
			return (T) ((Integer) 0);
		Number sum = Stream.of(numbers).mapToDouble(n -> n.doubleValue()).sum();
		if (numbers[0] instanceof Integer)
			return (T) ((Integer) sum.intValue());
		else if (numbers[0] instanceof Short)
			return (T) ((Short) sum.shortValue());
		else if (numbers[0] instanceof Long)
			return (T) ((Long) sum.longValue());
		else if (numbers[0] instanceof Float)
			return (T) ((Float) sum.floatValue());
		else if (numbers[0] instanceof BigInteger)
			return (T) (BigInteger.valueOf(sum.longValue()));
		else if (numbers[0] instanceof BigDecimal)
			return (T) (BigDecimal.valueOf(sum.doubleValue()));
		else
			return (T) ((Double) sum);
	}

	public static Double avg(Number... numbers) {
		return Stream.of(numbers).mapToDouble(n -> n.doubleValue()).sum() / numbers.length;
	}

}

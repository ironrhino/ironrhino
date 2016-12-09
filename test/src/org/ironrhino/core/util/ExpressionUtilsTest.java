package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ExpressionUtilsTest {

	@Test
	public void testEvalExpression() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		context.put("bool", true);
		Object result = ExpressionUtils.evalExpression("string+12", context);
		assertEquals("IAMSTRING12", result);
		result = ExpressionUtils.evalExpression("integer+12", context);
		assertEquals(24, result);
		result = ExpressionUtils.evalExpression("!bool", context);
		assertEquals(false, result);
	}

	@Test
	public void testEval() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		context.put("bool", true);
		Object result = ExpressionUtils.eval("${string+12}", context);
		assertEquals("IAMSTRING12", result);
		result = ExpressionUtils.eval("${string+12}12", context);
		assertEquals("IAMSTRING1212", result);
		result = ExpressionUtils.eval("${integer+12}", context);
		assertEquals(24, result);
		result = ExpressionUtils.eval("${integer+12}12", context);
		assertEquals("2412", result);
		result = ExpressionUtils.eval("${!bool}", context);
		assertEquals(false, result);
		result = ExpressionUtils.eval("${!bool}12", context);
		assertEquals("false12", result);
	}

	@Test
	public void testEvalString() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		Object result = ExpressionUtils.evalString("${string+12}", context);
		assertEquals("IAMSTRING12", result);
		result = ExpressionUtils.evalString("${integer+12}12", context);
		assertEquals("2412", result);
		result = ExpressionUtils.evalString("${null}", context);
		assertNull(result);
		result = ExpressionUtils.evalString("iam@if{integer > 10}large@else{}small@end{}", context);
		assertEquals("iamlarge", result);
	}

	@Test
	public void testEvalBoolean() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalBoolean("${integer > 10}", context, false);
		assertEquals(true, result);
	}

	@Test
	public void testEvalInt() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalInt("${integer - 10}", context, 12);
		assertEquals(2, result);
		result = ExpressionUtils.evalInt("${integer - 10}0", context, 12);
		assertEquals(20, result);
	}

	@Test
	public void testEvalLong() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalLong("${integer - 10}", context, 12);
		assertEquals(2L, result);
		result = ExpressionUtils.evalLong("${integer - 10}0", context, 12);
		assertEquals(20L, result);
	}

	@Test
	public void testEvalDouble() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalDouble("${integer - 10.00}", context, 12);
		assertEquals(2D, result);
		result = ExpressionUtils.evalDouble("${integer - 10.00}0", context, 12);
		assertEquals(2D, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEvalList() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "STRING");
		List<String> result = ExpressionUtils.evalList("${[string]}", context);
		assertEquals(1, result.size());
		assertEquals("STRING", result.get(0));
		result = ExpressionUtils.evalList("${string}", context);
		assertEquals(1, result.size());
		assertEquals("STRING", result.get(0));
	}

}

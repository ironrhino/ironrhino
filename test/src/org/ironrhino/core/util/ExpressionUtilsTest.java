package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
		context.put("$bool", false);
		Object result = ExpressionUtils.evalExpression("string+12", context);
		assertThat(result, equalTo("IAMSTRING12"));
		result = ExpressionUtils.evalExpression("integer+12", context);
		assertThat(result, equalTo(24));
		result = ExpressionUtils.evalExpression("!bool", context);
		assertThat(result, equalTo(false));
		result = ExpressionUtils.evalExpression("!$bool", context);
		assertThat(result, equalTo(true));
	}

	@Test
	public void testMathFunction() {
		Map<String, Object> context = new HashMap<>();
		context.put("number1", 1);
		context.put("number2", 2);
		context.put("number3", 3);
		assertThat(ExpressionUtils.evalExpression("min(number2,number3)", context), equalTo(2));
		assertThat(ExpressionUtils.evalExpression("min(number1,number2,number3)", context), equalTo(1));
		assertThat(ExpressionUtils.evalExpression("max(number1,number2)", context), equalTo(2));
		assertThat(ExpressionUtils.evalExpression("max(number1,number2,number3)", context), equalTo(3));
		assertThat(ExpressionUtils.evalExpression("sum(number1,number2,number3)", context), equalTo(6));
		assertThat(ExpressionUtils.evalExpression("avg(number1,number2,number3)", context), equalTo(2.0));
	}

	@Test
	public void testEval() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		context.put("bool", true);
		Object result = ExpressionUtils.eval("${string+12}", context);
		assertThat(result, equalTo("IAMSTRING12"));
		result = ExpressionUtils.eval("${string+12}12", context);
		assertThat(result, equalTo("IAMSTRING1212"));
		result = ExpressionUtils.eval("${integer+12}", context);
		assertThat(result, equalTo(24));
		result = ExpressionUtils.eval("${integer+12}12", context);
		assertThat(result, equalTo("2412"));
		result = ExpressionUtils.eval("${!bool}", context);
		assertThat(result, equalTo(false));
		result = ExpressionUtils.eval("${!bool}12", context);
		assertThat(result, equalTo("false12"));
	}
	
	
	@Test
	public void testEvalHashtag() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		context.put("bool", true);
		Object result = ExpressionUtils.eval("#{string+12}", context);
		assertThat(result, equalTo("IAMSTRING12"));
		result = ExpressionUtils.eval("#{string+12}12", context);
		assertThat(result, equalTo("IAMSTRING1212"));
		result = ExpressionUtils.eval("#{integer+12}", context);
		assertThat(result, equalTo(24));
		result = ExpressionUtils.eval("#{integer+12}12", context);
		assertThat(result, equalTo("2412"));
		result = ExpressionUtils.eval("#{!bool}", context);
		assertThat(result, equalTo(false));
		result = ExpressionUtils.eval("#{!bool}12", context);
		assertThat(result, equalTo("false12"));
	}


	@Test
	public void testEvalString() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		Object result = ExpressionUtils.evalString("${string+12}", context);
		assertThat(result, equalTo("IAMSTRING12"));
		result = ExpressionUtils.evalString("${integer+12}12", context);
		assertThat(result, equalTo("2412"));
		result = ExpressionUtils.evalString("${null}", context);
		assertThat(result, nullValue());
	}

	@Test
	public void testEvalConditionalString() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "IAMSTRING");
		context.put("integer", 12);
		Object result = ExpressionUtils.evalString("iam@if{integer > 10}large@else{}small@end{}", context);
		assertThat(result, equalTo("iamlarge"));
	}

	@Test
	public void testEvalBoolean() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalBoolean("${integer > 10}", context, false);
		assertThat(result, equalTo(true));
	}

	@Test
	public void testEvalInt() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalInt("${integer - 10}", context, 12);
		assertThat(result, equalTo(2));
		result = ExpressionUtils.evalInt("${integer - 10}0", context, 12);
		assertThat(result, equalTo(20));
	}

	@Test
	public void testEvalLong() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalLong("${integer - 10}", context, 12);
		assertThat(result, equalTo(2L));
		result = ExpressionUtils.evalLong("${integer - 10}0", context, 12);
		assertThat(result, equalTo(20L));
	}

	@Test
	public void testEvalDouble() {
		Map<String, Object> context = new HashMap<>();
		context.put("integer", 12);
		Object result = ExpressionUtils.evalDouble("${integer - 10.00}", context, 12);
		assertThat(result, equalTo(2D));
		result = ExpressionUtils.evalDouble("${integer - 10.00}0", context, 12);
		assertThat(result, equalTo(2D));
	}

	@Test
	public void testEvalList() {
		Map<String, Object> context = new HashMap<>();
		context.put("string", "STRING");
		List<String> result = ExpressionUtils.evalList("${[string]}", context);
		assertThat(result.size(), equalTo(1));
		assertThat(result.get(0), equalTo("STRING"));
		result = ExpressionUtils.evalList("${string}", context);
		assertThat(result.size(), equalTo(1));
		assertThat(result.get(0), equalTo("STRING"));
	}

}

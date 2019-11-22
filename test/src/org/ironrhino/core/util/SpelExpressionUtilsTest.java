package org.ironrhino.core.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SpelExpressionUtilsTest extends ExpressionUtilsTest {

	@BeforeClass
	public static void setup() {
		System.setProperty(ExpressionEngine.KEY, "SPEL");
	}

	@Test
	public void testEvalConditionalString() {
		// not supported
	}

	@AfterClass
	public static void cleanup() {
		System.clearProperty(ExpressionEngine.KEY);
	}

}

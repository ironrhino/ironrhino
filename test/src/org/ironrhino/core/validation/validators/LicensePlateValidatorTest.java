package org.ironrhino.core.validation.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LicensePlateValidatorTest {

	@Test
	public void testIsValid() {
		assertFalse(LicensePlateValidator.isValid(""));
		assertFalse(LicensePlateValidator.isValid("湖A04322"));
		assertFalse(LicensePlateValidator.isValid("湘AAAAAA"));
		assertFalse(LicensePlateValidator.isValid("湘Z2WF04"));
		assertFalse(LicensePlateValidator.isValid("湘A2wF04"));
		assertFalse(LicensePlateValidator.isValid("湘AAWFWF"));
		assertTrue(LicensePlateValidator.isValid("湘A2WF04"));
		assertTrue(LicensePlateValidator.isValid("粤BD12345"));
		assertTrue(LicensePlateValidator.isValid("粤B12345D"));
	}

}

package org.ironrhino.core.validation.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MobilePhoneNumberValidatorTest {

	@Test
	public void testIsValid() {
		assertFalse(MobilePhoneNumberValidator.isValid("10000000"));
		assertFalse(MobilePhoneNumberValidator.isValid("11811111111"));
		assertTrue(MobilePhoneNumberValidator.isValid("13811111111"));
		assertTrue(MobilePhoneNumberValidator.isValid("15800000000"));
		assertTrue(MobilePhoneNumberValidator.isValid("18900000000"));
	}

}

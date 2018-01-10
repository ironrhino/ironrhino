package org.ironrhino.core.validation.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrganizationCodeValidatorTest {

	@Test
	public void testIsValid() {
		assertFalse(OrganizationCodeValidator.isValid(""));
		assertFalse(OrganizationCodeValidator.isValid("10000000"));
		assertFalse(OrganizationCodeValidator.isValid("123456789"));
		assertFalse(OrganizationCodeValidator.isValid("111111111"));
		assertFalse(OrganizationCodeValidator.isValid("AABBCCDDE"));
		assertTrue(OrganizationCodeValidator.isValid("183888881"));
		assertTrue(OrganizationCodeValidator.isValid("183974050"));
		assertTrue(OrganizationCodeValidator.isValid("183807033"));
		assertTrue(OrganizationCodeValidator.isValid("344701003"));
		assertTrue(OrganizationCodeValidator.isValid("352864865"));
		assertTrue(OrganizationCodeValidator.isValid("329420684"));
		assertTrue(OrganizationCodeValidator.isValid("329436141"));
		assertTrue(OrganizationCodeValidator.isValid("329420684"));
		assertTrue(OrganizationCodeValidator.isValid("320714547"));
		assertTrue(OrganizationCodeValidator.isValid("M000100Y4"));
		assertTrue(OrganizationCodeValidator.isValid("MA2REGCG2"));
		assertTrue(OrganizationCodeValidator.isValid("MA152C47X"));
		assertTrue(OrganizationCodeValidator.isValid("MA2REG3M4"));
		assertTrue(OrganizationCodeValidator.isValid("MA6C8G954"));
		assertTrue(OrganizationCodeValidator.isValid("MA3MJ0PK9"));

	}

}

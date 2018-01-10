package org.ironrhino.core.validation.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CitizenIdentificationNumberValidatorTest {

	@Test
	public void testIsValid() {
		assertFalse(CitizenIdentificationNumberValidator.isValid(""));
		assertFalse(CitizenIdentificationNumberValidator.isValid("10000000"));
		assertFalse(CitizenIdentificationNumberValidator.isValid("43022419840628423A"));
		assertFalse(CitizenIdentificationNumberValidator.isValid("43022419840628423X"));
		assertFalse(CitizenIdentificationNumberValidator.isValid("430224198309145163"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197108227711"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197302188242"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197301154323"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197303223134"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197509196995"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197405232066"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000197708142017"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000198406198968"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000198207196919"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("440000199002119420"));
		assertTrue(CitizenIdentificationNumberValidator.isValid("44000019810613759X"));
	}

}

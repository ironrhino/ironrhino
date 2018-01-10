package org.ironrhino.core.validation.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SocialCreditIdentifierValidatorTest {

	@Test
	public void testIsValid() {
		assertFalse(SocialCreditIdentifierValidator.isValid(""));
		assertFalse(SocialCreditIdentifierValidator.isValid("10000000"));
		assertFalse(SocialCreditIdentifierValidator.isValid("43022419840628423A"));
		assertFalse(SocialCreditIdentifierValidator.isValid("43022419840628423X"));
		assertFalse(SocialCreditIdentifierValidator.isValid("430224198309145163"));
		assertFalse(SocialCreditIdentifierValidator.isValid("91350100M000100y43"));
		assertTrue(SocialCreditIdentifierValidator.isValid("91350100M000100Y43"));
		assertTrue(SocialCreditIdentifierValidator.isValid("92341321MA2REGCG29"));
		assertTrue(SocialCreditIdentifierValidator.isValid("92220103MA152C47XT"));
		assertTrue(SocialCreditIdentifierValidator.isValid("92340202MA2REG3M4F"));
		assertTrue(SocialCreditIdentifierValidator.isValid("91510108MA6C8G954F"));
		assertTrue(SocialCreditIdentifierValidator.isValid("92370725MA3MJ0PK96"));
	}

}

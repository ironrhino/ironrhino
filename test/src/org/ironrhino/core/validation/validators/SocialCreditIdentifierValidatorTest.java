package org.ironrhino.core.validation.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class SocialCreditIdentifierValidatorTest {

	@Test
	public void testIsValid() {
		assertThat(SocialCreditIdentifierValidator.isValid(""), is(false));
		assertThat(SocialCreditIdentifierValidator.isValid("10000000"), is(false));
		assertThat(SocialCreditIdentifierValidator.isValid("43022419840628423A"), is(false));
		assertThat(SocialCreditIdentifierValidator.isValid("43022419840628423X"), is(false));
		assertThat(SocialCreditIdentifierValidator.isValid("430224198309145163"), is(false));
		assertThat(SocialCreditIdentifierValidator.isValid("91350100M000100y43"), is(false));
		assertThat(SocialCreditIdentifierValidator.isValid("91350100M000100Y43"), is(true));
		assertThat(SocialCreditIdentifierValidator.isValid("92341321MA2REGCG29"), is(true));
		assertThat(SocialCreditIdentifierValidator.isValid("92220103MA152C47XT"), is(true));
		assertThat(SocialCreditIdentifierValidator.isValid("92340202MA2REG3M4F"), is(true));
		assertThat(SocialCreditIdentifierValidator.isValid("91510108MA6C8G954F"), is(true));
		assertThat(SocialCreditIdentifierValidator.isValid("92370725MA3MJ0PK96"), is(true));
		assertThat(SocialCreditIdentifierValidator.isValid("31110000358343139K"), is(true));
	}

	@Test
	public void testRandomValue() {
		for (int i = 0; i < 100; i++)
			assertThat(SocialCreditIdentifierValidator.isValid(SocialCreditIdentifierValidator.randomValue()),
					is(true));
	}

}

package org.ironrhino.core.validation.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class OrganizationCodeValidatorTest {

	@Test
	public void testIsValid() {
		assertThat(OrganizationCodeValidator.isValid(""), is(false));
		assertThat(OrganizationCodeValidator.isValid("10000000"), is(false));
		assertThat(OrganizationCodeValidator.isValid("123456789"), is(false));
		assertThat(OrganizationCodeValidator.isValid("111111111"), is(false));
		assertThat(OrganizationCodeValidator.isValid("AABBCCDDE"), is(false));
		assertThat(OrganizationCodeValidator.isValid("183888881"), is(true));
		assertThat(OrganizationCodeValidator.isValid("183974050"), is(true));
		assertThat(OrganizationCodeValidator.isValid("183807033"), is(true));
		assertThat(OrganizationCodeValidator.isValid("344701003"), is(true));
		assertThat(OrganizationCodeValidator.isValid("352864865"), is(true));
		assertThat(OrganizationCodeValidator.isValid("329420684"), is(true));
		assertThat(OrganizationCodeValidator.isValid("329436141"), is(true));
		assertThat(OrganizationCodeValidator.isValid("329420684"), is(true));
		assertThat(OrganizationCodeValidator.isValid("320714547"), is(true));
		assertThat(OrganizationCodeValidator.isValid("M000100Y4"), is(true));
		assertThat(OrganizationCodeValidator.isValid("MA2REGCG2"), is(true));
		assertThat(OrganizationCodeValidator.isValid("MA152C47X"), is(true));
		assertThat(OrganizationCodeValidator.isValid("MA2REG3M4"), is(true));
		assertThat(OrganizationCodeValidator.isValid("MA6C8G954"), is(true));
		assertThat(OrganizationCodeValidator.isValid("MA3MJ0PK9"), is(true));

	}

}

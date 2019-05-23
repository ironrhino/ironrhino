package org.ironrhino.core.validation.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class CitizenIdentificationNumberValidatorTest {

	@Test
	public void testIsValid() {
		assertThat(CitizenIdentificationNumberValidator.isValid(""), is(false));
		assertThat(CitizenIdentificationNumberValidator.isValid("10000000"), is(false));
		assertThat(CitizenIdentificationNumberValidator.isValid("43022419840628423A"), is(false));
		assertThat(CitizenIdentificationNumberValidator.isValid("43022419840628423X"), is(false));
		assertThat(CitizenIdentificationNumberValidator.isValid("430224198309145163"), is(false));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197108227711"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197302188242"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197301154323"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197303223134"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197509196995"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197405232066"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000197708142017"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000198406198968"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000198207196919"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("440000199002119420"), is(true));
		assertThat(CitizenIdentificationNumberValidator.isValid("44000019810613759X"), is(true));
	}

}

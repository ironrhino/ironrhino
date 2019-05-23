package org.ironrhino.core.validation.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class LicensePlateValidatorTest {

	@Test
	public void testIsValid() {
		assertThat(LicensePlateValidator.isValid(""), is(false));
		assertThat(LicensePlateValidator.isValid("湖A04322"), is(false));
		assertThat(LicensePlateValidator.isValid("湘AAAAAA"), is(false));
		assertThat(LicensePlateValidator.isValid("湘Z2WF04"), is(false));
		assertThat(LicensePlateValidator.isValid("湘A2wF04"), is(false));
		assertThat(LicensePlateValidator.isValid("湘AAWFWF"), is(false));
		assertThat(LicensePlateValidator.isValid("湘A2WF04"), is(true));
		assertThat(LicensePlateValidator.isValid("粤BD12345"), is(true));
		assertThat(LicensePlateValidator.isValid("粤B12345D"), is(true));
	}

}

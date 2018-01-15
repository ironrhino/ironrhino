package org.ironrhino.sample.crud;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.validation.validators.CitizenIdentificationNumberValidator;

public class IdentityCheckValidator implements ConstraintValidator<IdentityCheck, Identity> {

	@Override
	public boolean isValid(Identity input, ConstraintValidatorContext constraintValidatorContext) {
		if (input == null)
			return true;
		String identityType = input.getIdentityType();
		String identityNo = input.getIdentityNo();
		if (identityType == null || identityNo == null)
			return true;
		boolean valid;
		if (identityType.equals("A")) {
			valid = CitizenIdentificationNumberValidator.isValid(identityNo);
		} else if (identityType.equals("B")) {
			valid = identityNo.length() == 8 && StringUtils.isNumeric(identityNo);
		} else {
			valid = true;
		}
		if (!valid) {
			constraintValidatorContext.disableDefaultConstraintViolation();
			constraintValidatorContext
					.buildConstraintViolationWithTemplate(
							"{" + IdentityCheck.class.getName() + "." + identityType + ".message}")
					.addPropertyNode("identityNo").addConstraintViolation();
		}
		return valid;
	}

}
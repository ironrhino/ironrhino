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
		if (identityType.equals("A")) {
			boolean valid = CitizenIdentificationNumberValidator.isValid(identityNo);
			if (!valid) {
				constraintValidatorContext.disableDefaultConstraintViolation();
				constraintValidatorContext
						.buildConstraintViolationWithTemplate(
								constraintValidatorContext.getDefaultConstraintMessageTemplate())
						.addPropertyNode("identityNo").addConstraintViolation();
			}
			return valid;
		} else if (identityType.equals("B")) {
			boolean valid = identityNo.length() == 8 && StringUtils.isNumeric(identityNo);
			if (!valid) {
				constraintValidatorContext.disableDefaultConstraintViolation();
				constraintValidatorContext.buildConstraintViolationWithTemplate("不是正确的军官证")
						.addPropertyNode("identityNo").addConstraintViolation();
			}
			return valid;
		} else {
			return true;
		}
	}

}
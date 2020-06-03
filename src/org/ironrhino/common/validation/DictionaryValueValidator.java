package org.ironrhino.common.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.ironrhino.common.support.DictionaryControl;
import org.ironrhino.core.util.ApplicationContextUtils;

public class DictionaryValueValidator implements ConstraintValidator<DictionaryValue, String> {

	private String templateName;

	@Override
	public void initialize(DictionaryValue parameters) {
		this.templateName = parameters.templateName();
	}

	@Override
	public boolean isValid(String input, ConstraintValidatorContext constraintValidatorContext) {
		DictionaryControl dc = ApplicationContextUtils.getBean(DictionaryControl.class);
		if (dc == null || input == null)
			return true;
		return dc.getItemsAsMap(templateName).containsKey(input);
	}

}
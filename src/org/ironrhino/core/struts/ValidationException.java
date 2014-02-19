package org.ironrhino.core.struts;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.opensymphony.xwork2.ValidationAwareSupport;

public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = -603245390369744994L;

	private final ValidationAwareSupport validationAware = new ValidationAwareSupport();

	public ValidationException() {

	}

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(Throwable throwable) {
		super(throwable);
	}

	public ValidationException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public Collection<String> getActionErrors() {
		return validationAware.getActionErrors();
	}

	public Collection<String> getActionMessages() {
		return validationAware.getActionMessages();
	}

	public Map<String, List<String>> getFieldErrors() {
		return validationAware.getFieldErrors();
	}

	public void addActionError(String anErrorMessage) {
		validationAware.addActionError(anErrorMessage);
	}

	public void addActionMessage(String aMessage) {
		validationAware.addActionMessage(aMessage);
	}

	public void addFieldError(String fieldName, String errorMessage) {
		validationAware.addFieldError(fieldName, errorMessage);
	}

}

package org.ironrhino.core.struts;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.lang3.StringUtils;

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

	@Override
	public String getMessage() {
		String msg = super.getMessage();
		if (msg == null) {
			StringBuilder sb = new StringBuilder();
			Collection<String> actionErrors = getActionErrors();
			if (actionErrors != null && !actionErrors.isEmpty())
				sb.append(StringUtils.join(actionErrors, ";"));
			Map<String, List<String>> fieldErrors = getFieldErrors();
			if (fieldErrors != null && !fieldErrors.isEmpty()) {
				if (sb.length() > 0)
					sb.append(';');
				for (Map.Entry<String, List<String>> entry : fieldErrors.entrySet())
					sb.append(entry.getKey()).append(": ").append(StringUtils.join(entry.getValue(), ",")).append(';');
				sb.deleteCharAt(sb.length() - 1);
			}
			msg = sb.toString();
		}
		return msg;
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

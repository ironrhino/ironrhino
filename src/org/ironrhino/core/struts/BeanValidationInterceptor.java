package org.ironrhino.core.struts;

import java.lang.reflect.Method;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;

public class BeanValidationInterceptor extends MethodFilterInterceptor {

	private static final long serialVersionUID = -5875777623327743340L;

	@Autowired
	private Validator validator;

	@Override
	protected String doIntercept(ActionInvocation invocation) throws Exception {
		Object action = invocation.getAction();
		Method method = action.getClass().getMethod(invocation.getProxy().getMethod());
		if (action instanceof ValidationAware && method.isAnnotationPresent(Valid.class))
			validate((ValidationAware) action, validator);
		return invocation.invoke();
	}

	protected void validate(ValidationAware action, Validator validator) {
		Set<ConstraintViolation<Object>> constraintViolations = validator.validate(action);
		if (constraintViolations != null && !constraintViolations.isEmpty()) {
			for (ConstraintViolation<Object> violation : constraintViolations) {
				String message = violation.getMessage();
				if (violation.getLeafBean() == violation.getInvalidValue()) {
					action.addActionError(message);
				} else {
					action.addFieldError(violation.getPropertyPath().toString(), message);
				}
			}
		}
	}

}
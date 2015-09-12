package org.ironrhino.core.struts;

import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

import ognl.MethodFailedException;

public class ExceptionInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = 6419734583295725844L;
	protected static final Logger log = LoggerFactory.getLogger(ExceptionInterceptor.class);

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		String result;
		try {
			result = invocation.invoke();
		} catch (Throwable e) {
			if (e instanceof MethodFailedException)
				e = e.getCause();
			if (e instanceof NoSuchMethodException) {
				result = BaseAction.NOTFOUND;
			} else {
				Object action = invocation.getAction();
				if (action instanceof ValidationAware) {
					ValidationAware validationAwareAction = (ValidationAware) action;
					Throwable cause = e.getCause();
					if (e instanceof ConstraintViolationException) {
						ConstraintViolationException cve = (ConstraintViolationException) e;
						for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
							validationAwareAction
									.addFieldError(StringUtils.uncapitalize(cv.getRootBeanClass().getSimpleName()) + "."
											+ cv.getPropertyPath(), cv.getMessage());
						}
					} else if (e instanceof OptimisticLockingFailureException
							|| cause instanceof OptimisticLockingFailureException) {
						validationAwareAction.addActionError(findText("try.again.later", null));
					} else {
						if (cause != null)
							while (cause.getCause() != null)
								cause = cause.getCause();
						if (e instanceof ValidationException || cause instanceof ValidationException) {
							ValidationException ve = (ValidationException) ((e instanceof ValidationException) ? e
									: cause);
							for (String s : ve.getActionMessages())
								validationAwareAction.addActionMessage(findText(s, null));
							for (String s : ve.getActionErrors())
								validationAwareAction.addActionError(findText(s, null));
							for (Map.Entry<String, List<String>> entry : ve.getFieldErrors().entrySet()) {
								for (String s : entry.getValue())
									validationAwareAction.addFieldError(entry.getKey(), findText(s, null));
							}
						} else if (e instanceof ErrorMessage || cause instanceof ErrorMessage) {
							ErrorMessage em = (ErrorMessage) ((e instanceof ErrorMessage) ? e : cause);
							validationAwareAction.addActionError(em.getLocalizedMessage());
						} else {

							String msg = e.getMessage();
							if (cause != null)
								msg = cause.getMessage();
							if (msg == null)
								msg = ExceptionUtils.getDetailMessage(e);
							validationAwareAction.addActionError(msg);
							log.error(e.getMessage(), e);
						}
					}
				}
				result = BaseAction.ERROR;
			}
		}
		return result;
	}

	private static String findText(String text, Object[] args) {
		if (text == null)
			return null;
		text = text.replaceAll("\\{", "[");
		text = text.replaceAll("\\}", "]");
		return LocalizedTextUtil.findText(ExceptionInterceptor.class, text, ActionContext.getContext().getLocale(),
				text, args);
	}

}

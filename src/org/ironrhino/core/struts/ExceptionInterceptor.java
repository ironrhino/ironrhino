package org.ironrhino.core.struts;

import java.util.List;
import java.util.Map;

import org.ironrhino.core.util.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

public class ExceptionInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = 6419734583295725844L;
	protected static final Logger log = LoggerFactory
			.getLogger(ExceptionInterceptor.class);

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		String result;
		try {
			result = invocation.invoke();
		} catch (Throwable e) {
			if (e instanceof NoSuchMethodException) {
				result = BaseAction.NOTFOUND;
			} else {
				Object action = invocation.getAction();
				if (action instanceof ValidationAware) {
					ValidationAware validationAwareAction = (ValidationAware) action;
					Throwable cause = e.getCause();
					if (e instanceof OptimisticLockingFailureException
							|| cause instanceof OptimisticLockingFailureException) {
						validationAwareAction.addActionError(findText(
								"try.again.later", null));
					} else if (e instanceof DataIntegrityViolationException
							|| cause instanceof DataIntegrityViolationException) {
						validationAwareAction.addActionError(findText(
								"validation.already.exists", null));
						log.error(e.getMessage(), e);
					} else if (e instanceof ValidationException
							|| cause instanceof ValidationException) {
						ValidationException ve = (ValidationException) ((e instanceof ValidationException) ? e
								: cause);
						for (String s : ve.getActionMessages())
							validationAwareAction.addActionMessage(findText(s,
									null));
						for (String s : ve.getActionErrors())
							validationAwareAction.addActionError(findText(s,
									null));
						for (Map.Entry<String, List<String>> entry : ve
								.getFieldErrors().entrySet()) {
							for (String s : entry.getValue())
								validationAwareAction.addFieldError(
										entry.getKey(), findText(s, null));
						}
					} else if (e instanceof ErrorMessage) {
						ErrorMessage em = (ErrorMessage) ((e instanceof ErrorMessage) ? e
								: cause);
						validationAwareAction.addActionError(em
								.getLocalizedMessage());
					} else {
						String msg = findText(e.getMessage(), null);
						if (msg == null)
							msg = e.toString();
						validationAwareAction.addActionError(msg);
						log.error(e.getMessage(), e);
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
		return LocalizedTextUtil.findText(ExceptionInterceptor.class, text,
				ActionContext.getContext().getLocale(), text, args);
	}

}

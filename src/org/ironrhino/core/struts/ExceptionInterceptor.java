package org.ironrhino.core.struts;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.servlet.RequestDispatcher;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.LocalizedException;
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
	protected static final Logger logger = LoggerFactory.getLogger(ExceptionInterceptor.class);

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		try {
			return invocation.invoke();
		} catch (Throwable e) {
			ServletActionContext.getRequest().setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
			if (e instanceof LocalizedException || e instanceof ErrorMessage)
				logger.error(e.getLocalizedMessage());
			else if (!(e instanceof ValidationException))
				logger.error(e.getMessage(), e);
			if (e instanceof MethodFailedException || e instanceof CompletionException)
				e = e.getCause();
			if (e instanceof NoSuchMethodException)
				return BaseAction.NOTFOUND;
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
				} else if (e instanceof javax.validation.ValidationException) {
					// dehydrated ConstraintViolationException for remoting service
					boolean parsed = false;
					String message = e.getMessage();
					int i = message.indexOf("List of constraint violations:[");
					if (i > 0) {
						message = message.substring(message.lastIndexOf('[') + 2, message.lastIndexOf(']'));
						for (String violation : message.split("\n")) {
							if (violation.contains("ConstraintViolationImpl{")) {
								int index = violation.indexOf("interpolatedMessage='");
								if (index > 0) {
									index += 21;
									String interpolatedMessage = violation.substring(index,
											violation.indexOf("'", index + 1));
									index = violation.indexOf("propertyPath=");
									if (index > 0) {
										index += 13;
										String propertyPath = violation.substring(index,
												violation.indexOf(",", index + 1));
										index = violation.indexOf("rootBeanClass=class ");
										if (index > 0) {
											index += 20;
											String rootBeanClass = violation.substring(index,
													violation.indexOf(",", index + 1));
											validationAwareAction.addFieldError(StringUtils.uncapitalize(
													rootBeanClass.substring(rootBeanClass.lastIndexOf('.') + 1)) + "."
													+ propertyPath, interpolatedMessage);
											parsed = true;
										}
									}
								}
							}
						}
					}
					if (!parsed) {
						validationAwareAction.addActionError(message);
					}
				} else if (e instanceof OptimisticLockingFailureException
						|| cause instanceof OptimisticLockingFailureException) {
					validationAwareAction.addActionError(findText("try.again.later", null));
				} else {
					if (cause != null)
						while (cause.getCause() != null)
							cause = cause.getCause();
					if (e instanceof ValidationException || cause instanceof ValidationException) {
						ValidationException ve = (ValidationException) ((e instanceof ValidationException) ? e : cause);
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
					} else if (e instanceof LocalizedException || cause instanceof LocalizedException) {
						LocalizedException le = (LocalizedException) ((e instanceof LocalizedException) ? e : cause);
						validationAwareAction.addActionError(le.getLocalizedMessage());
					} else {
						String msg = e.getMessage();
						if (cause != null)
							msg = cause.getMessage();
						if (msg == null)
							msg = ExceptionUtils.getDetailMessage(e);
						validationAwareAction.addActionError(msg);
					}
				}
			}
			return BaseAction.ERROR;
		}
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

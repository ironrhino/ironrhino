package org.ironrhino.core.struts;

import java.net.SocketTimeoutException;
import java.util.concurrent.CompletionException;

import javax.servlet.RequestDispatcher;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.LocalizedException;
import org.springframework.dao.OptimisticLockingFailureException;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.util.LocalizedTextUtil;

import lombok.extern.slf4j.Slf4j;
import ognl.MethodFailedException;

@Slf4j
public class ExceptionInterceptor extends AbstractInterceptor {

	private static final long serialVersionUID = 6419734583295725844L;

	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		try {
			return invocation.invoke();
		} catch (Exception e) {
			if (e instanceof NoSuchMethodException) {
				log.error("{}: {}", e.getClass().getName(), e.getMessage());
				return BaseAction.NOTFOUND;
			}
			ServletActionContext.getRequest().setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
			if (e instanceof LocalizedException || e instanceof ErrorMessage)
				log.error(e.getClass().getCanonicalName() + ": " + e.getLocalizedMessage());
			else if (!(e instanceof ValidationException) && !(e instanceof javax.validation.ValidationException))
				log.error(e.getMessage(), e);
			if ((e instanceof MethodFailedException || e instanceof CompletionException)
					&& e.getCause() instanceof Exception)
				e = (Exception) e.getCause();
			Object action = invocation.getAction();
			if (action instanceof ValidationAware) {
				ValidationAware validationAwareAction = (ValidationAware) action;
				Throwable cause = e.getCause();
				if (e instanceof ConstraintViolationException) {
					ConstraintViolationException cve = (ConstraintViolationException) e;
					for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
						String propertyPath = cv.getPropertyPath().toString();
						if (propertyPath.indexOf(".<") > 0) {
							// collection[].<iterable element>
							// list[0].<list element>
							// map[test].<map value>
							propertyPath = propertyPath.substring(0, propertyPath.lastIndexOf("["));
						}
						validationAwareAction.addFieldError(
								StringUtils.uncapitalize(cv.getRootBeanClass().getSimpleName()) + "." + propertyPath,
								cv.getMessage());
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
										if (propertyPath.indexOf(".<") > 0) {
											// collection[].<iterable element>
											// list[0].<list element>
											// map[test].<map value>
											propertyPath = propertyPath.substring(0, propertyPath.lastIndexOf("["));
										}
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
						ve.getFieldErrors().forEach((k, v) -> {
							for (String s : v)
								validationAwareAction.addFieldError(k, findText(s, null));
						});
					} else if (e instanceof ErrorMessage || cause instanceof ErrorMessage) {
						ErrorMessage em = (ErrorMessage) ((e instanceof ErrorMessage) ? e : cause);
						validationAwareAction.addActionError(em.getLocalizedMessage());
					} else if (e instanceof UnsupportedOperationException || e instanceof SocketTimeoutException) {
						validationAwareAction.addActionError(findText(e.getClass().getCanonicalName(), null));
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

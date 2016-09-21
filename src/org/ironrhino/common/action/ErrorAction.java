package org.ironrhino.common.action;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.servlet.HttpErrorHandler;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetails;

@AutoConfig(namespace = "/")
public class ErrorAction extends BaseAction {

	private static final long serialVersionUID = 7684824080798968019L;

	private static Logger logger = LoggerFactory.getLogger(ErrorAction.class);

	private Throwable exception;

	@Value("${password.entryPoint:}")
	private String passwordEntryPoint;

	@Autowired(required = false)
	private HttpErrorHandler httpErrorHandler;

	public Throwable getException() {
		return exception;
	}

	@Override
	public String execute() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		int errorcode = 404;
		exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		if (exception instanceof ErrorMessage) {
			response.setStatus(HttpServletResponse.SC_OK);
			addActionError(((ErrorMessage) exception).getLocalizedMessage());
			return ERROR;
		} else if (exception instanceof AccountStatusException) {
			if (exception instanceof CredentialsExpiredException) {
				UserDetails ud = AuthzUtils.getUserDetails();
				if (ud != null) {
					if (StringUtils.isNotBlank(passwordEntryPoint))
						targetUrl = passwordEntryPoint;
					else
						targetUrl = "/" + StringUtils.uncapitalize(ud.getClass().getSimpleName()) + "/password";
					return REDIRECT;
				}
			}
			addActionError(getText(exception.getClass().getName()));
			return "accountStatus";
		} else if (exception != null)
			logger.error(exception.getMessage(), exception);
		try {
			errorcode = Integer.valueOf(getUid());
		} catch (Exception e) {

		}
		if (httpErrorHandler != null && httpErrorHandler.handle(request, response, errorcode,
				exception != null ? exception.getMessage() : null))
			return NONE;
		String result;
		switch (errorcode) {
		case HttpServletResponse.SC_UNAUTHORIZED:
			result = ACCESSDENIED;
			break;
		case HttpServletResponse.SC_FORBIDDEN:
			result = ERROR;
			break;
		case HttpServletResponse.SC_NOT_FOUND:
			result = NOTFOUND;
			break;
		case 500:
			result = "internalServerError";
			break;
		default:
			result = NOTFOUND;
		}
		response.setStatus(errorcode);
		response.setCharacterEncoding("utf-8"); // fix for jetty
		return result;
	}

	@Deprecated
	public String handle() {
		return execute();
	}

}

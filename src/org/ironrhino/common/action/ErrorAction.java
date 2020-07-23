package org.ironrhino.common.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.security.SecurityConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.LocalizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.userdetails.UserDetails;

import com.opensymphony.xwork2.ActionSupport;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AutoConfig(namespace = "/")
@Slf4j
public class ErrorAction extends ActionSupport {

	private static final long serialVersionUID = 7684824080798968019L;

	@Getter
	@Setter
	private String[] id;

	@Getter
	private String targetUrl;

	@Getter
	private Throwable exception;

	@Autowired(required = false)
	private SecurityConfig securityConfig;

	public String getUid() {
		if (id != null && id.length > 0)
			return id[0];
		else
			return null;
	}

	@Override
	public String execute() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		String result = BaseAction.NOTFOUND;
		int errorcode = 404;
		if (request.getDispatcherType() == DispatcherType.ERROR) {
			exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
			if (exception instanceof ErrorMessage) {
				response.setStatus(HttpServletResponse.SC_OK);
				addActionError(((ErrorMessage) exception).getLocalizedMessage());
				request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION);
				return ERROR;
			} else if (exception instanceof AccountStatusException) {
				if (exception instanceof CredentialsExpiredException) {
					UserDetails ud = AuthzUtils.getUserDetails();
					if (ud != null) {
						targetUrl = securityConfig != null ? securityConfig.getPasswordEntryPoint() : "/password";
						String s = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
						if (StringUtils.isNotBlank(s)) {
							String qs = request.getQueryString();
							if (qs != null)
								s += "?" + qs;
							try {
								targetUrl += "?targetUrl=" + URLEncoder.encode(s, "UTF-8");
							} catch (UnsupportedEncodingException e) {
							}
						}
						return BaseAction.REDIRECT;
					}
				}
				addActionError(getText(exception.getClass().getName()));
				return ERROR;
			} else if (exception != null) {
				if (exception instanceof LocalizedException || exception instanceof ErrorMessage)
					log.error(exception.getLocalizedMessage());
				else
					log.error(exception.getMessage(), exception);
			}
			try {
				errorcode = Integer.parseInt(getUid());
			} catch (Exception e) {

			}
			switch (errorcode) {
			case HttpServletResponse.SC_UNAUTHORIZED:
				result = BaseAction.ACCESSDENIED;
				break;
			case HttpServletResponse.SC_FORBIDDEN:
				result = ERROR;
				break;
			case HttpServletResponse.SC_NOT_FOUND:
				result = BaseAction.NOTFOUND;
				break;
			case 500:
				result = "internalServerError";
				break;
			default:
				result = BaseAction.NOTFOUND;
			}
		}
		response.setStatus(errorcode);
		response.setCharacterEncoding("utf-8"); // fix for jetty
		return result;
	}

}

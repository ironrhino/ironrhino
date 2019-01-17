package org.ironrhino.core.struts;

import java.io.BufferedReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.metadata.Csrf;
import org.ironrhino.core.metadata.CurrentPassword;
import org.ironrhino.core.metadata.DoubleChecker;
import org.ironrhino.core.metadata.Redirect;
import org.ironrhino.core.metadata.VerboseMode;
import org.ironrhino.core.security.captcha.CaptchaManager;
import org.ironrhino.core.security.captcha.CaptchaStatus;
import org.ironrhino.core.security.dynauth.DynamicAuthorizer;
import org.ironrhino.core.security.dynauth.DynamicAuthorizerManager;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.interceptor.annotations.Before;
import com.opensymphony.xwork2.interceptor.annotations.BeforeResult;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.util.ValueStack;

import lombok.Getter;
import lombok.Setter;

public class BaseAction extends ActionSupport {

	private static final long serialVersionUID = -3183957331611790404L;

	private static final String SESSION_KEY_CURRENT_PASSWORD_THRESHOLD = "c_p_t";
	private static final String COOKIE_NAME_CSRF = "csrf";

	public static final String HOME = "_";
	public static final String LIST = "list";
	public static final String VIEW = "view";
	public static final String PICK = "pick";
	public static final String TABS = "tabs";
	public static final String REFERER = "referer";
	public static final String JSON = "json";
	public static final String REDIRECT = "redirect";
	public static final String SUGGEST = "suggest";
	public static final String ACCESSDENIED = "accessDenied";
	public static final String NOTFOUND = "notFound";
	public static final String ERROR = "error";

	private boolean returnInput;

	// logic id or natrual id
	@Getter
	@Setter
	private String[] id;

	@Getter
	@Setter
	protected String keyword;

	protected String requestBody;

	protected String originalActionName;

	protected String originalMethod;

	@Getter
	@Setter
	protected String targetUrl;

	@Getter
	protected String responseBody;

	protected CaptchaStatus captchaStatus;

	@Setter
	protected String csrf;

	@Getter
	protected boolean csrfRequired;

	@Autowired(required = false)
	protected CaptchaManager captchaManager;

	@Autowired(required = false)
	protected DynamicAuthorizerManager dynamicAuthorizerManager;

	@Getter
	private String actionWarning;

	@Getter
	private String actionSuccessMessage;

	public String getCsrf() {
		if (csrf == null) {
			csrf = CodecUtils.nextId();
			RequestUtils.saveCookie(ServletActionContext.getRequest(), ServletActionContext.getResponse(),
					COOKIE_NAME_CSRF, csrf, false, true);
		}
		return csrf;
	}

	public boolean isCaptchaRequired() {
		return captchaStatus != null && captchaStatus.isRequired();
	}

	public String getActionBaseUrl() {
		return getActionNamespace() + "/" + ActionContext.getContext().getActionInvocation().getProxy().getActionName();
	}

	public String getActionNamespace() {
		String namespace = ActionContext.getContext().getActionInvocation().getProxy().getNamespace();
		if (namespace == null || namespace.equals("/"))
			namespace = "";
		return ServletActionContext.getRequest().getContextPath() + namespace;
	}

	public String getUid() {
		if (id != null && id.length > 0)
			return id[0];
		else
			return null;
	}

	public void setUid(String id) {
		this.id = new String[] { id };
	}

	public boolean isUseJson() {
		return JSON.equalsIgnoreCase(ServletActionContext.getRequest().getHeader("X-Data-Type"));
	}

	public boolean isAjax() {
		return "XMLHttpRequest".equalsIgnoreCase(ServletActionContext.getRequest().getHeader("X-Requested-With"));
	}

	protected void setActionWarning(String actionWarning) {
		this.actionWarning = actionWarning;
	}

	protected void setActionSuccessMessage(String actionSuccessMessage) {
		this.actionSuccessMessage = actionSuccessMessage;
	}

	protected void notify(String message) {
		switch (VerboseMode.current()) {
		case LOW:
			break;
		case MEDIUM:
			setActionSuccessMessage(getText(message));
			break;
		case HIGH:
			addActionMessage(getText(message));
			break;
		default:
			break;
		}
	}

	@Override
	public void clearMessages() {
		super.clearMessages();
		setActionWarning(null);
		setActionSuccessMessage(null);
	}

	@Override
	public void clearErrorsAndMessages() {
		super.clearErrorsAndMessages();
		setActionWarning(null);
		setActionSuccessMessage(null);
	}

	@Override
	public String execute() throws Exception {
		return SUCCESS;
	}

	@Override
	public String input() throws Exception {
		return INPUT;
	}

	public String save() throws Exception {
		return SUCCESS;
	}

	public String view() throws Exception {
		return VIEW;
	}

	public String delete() throws Exception {
		return SUCCESS;
	}

	public String pick() throws Exception {
		execute();
		return PICK;
	}

	public String tabs() throws Exception {
		return TABS;
	}

	@Before(priority = 20)
	protected String preAction() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		Authorize authorize = findAuthorize();
		if (authorize != null) {
			boolean authorized;
			if (StringUtils.isNotBlank(authorize.access()))
				authorized = AuthzUtils.authorize(authorize.access());
			else
				authorized = AuthzUtils.authorize(evalExpression(authorize.ifAllGranted()),
						evalExpression(authorize.ifAnyGranted()), evalExpression(authorize.ifNotGranted()));
			if (!authorized && dynamicAuthorizerManager != null
					&& !authorize.authorizer().equals(DynamicAuthorizer.class)) {
				String resource = authorize.resource();
				if (StringUtils.isBlank(resource)) {
					ActionProxy ap = ActionContext.getContext().getActionInvocation().getProxy();
					StringBuilder sb = new StringBuilder(ap.getNamespace());
					sb.append(ap.getNamespace().endsWith("/") ? "" : "/");
					sb.append(ap.getActionName());
					sb.append(ap.getMethod().equals("execute") ? "" : "/" + ap.getMethod());
					resource = sb.toString();
				}
				UserDetails user = AuthzUtils.getUserDetails();
				authorized = dynamicAuthorizerManager.authorize(authorize.authorizer(), user, resource);
			}
			if (!authorized) {
				if (isAjax())
					addActionError(getText("access.denied"));
				return ACCESSDENIED;
			}
		}
		Captcha captcha = getAnnotation(Captcha.class);
		if (captcha != null && captchaManager != null) {
			captchaStatus = captchaManager.getCaptchaStatus(request, captcha);
		}
		csrfRequired = captchaStatus == null && getAnnotation(Csrf.class) != null;
		return null;
	}

	@Before(priority = 10)
	protected String returnInputOrExtractRequestBody() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		String method = request.getMethod();
		boolean postMethodLike = method.startsWith("P"); // POST PUT PATCH
		InputConfig inputConfig = getAnnotation(InputConfig.class);
		if (inputConfig != null && !postMethodLike) {
			returnInput = true;
			if (!inputConfig.methodName().equals("")) {
				ActionInvocation ai = ActionContext.getContext().getActionInvocation();
				originalActionName = ai.getProxy().getActionName();
				originalMethod = ai.getProxy().getMethod();
				// ai.getProxy().setMethod(annotation.methodName());
				return (String) this.getClass().getMethod(inputConfig.methodName()).invoke(this);
			} else {
				return inputConfig.resultName();
			}
		}
		if (postMethodLike) {
			String contentType = request.getHeader("Content-Type");
			if (contentType != null) {
				if (contentType.indexOf(';') > 0)
					contentType = contentType.substring(0, contentType.indexOf(';')).trim();
				if ((contentType.contains("text") || contentType.contains("xml") || contentType.contains("json")
						|| contentType.contains("javascript")))
					try (BufferedReader reader = request.getReader()) {
						requestBody = reader.lines().collect(Collectors.joining("\n"));
					} catch (IllegalStateException e) {

					}
			}
		}
		return null;
	}

	@Override
	public void validate() {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		if (captchaManager != null
				&& (request.getParameter(CaptchaManager.KEY_CAPTCHA) != null
						|| isCaptchaRequired() && !captchaStatus.isFirstReachThreshold())
				&& !captchaManager.verify(request, request.getSession().getId(), true))
			addFieldError(CaptchaManager.KEY_CAPTCHA, getText("captcha.error"));
		if (csrfRequired) {
			String value = RequestUtils.getCookieValue(request, COOKIE_NAME_CSRF);
			RequestUtils.deleteCookie(request, response, COOKIE_NAME_CSRF);
			if (csrf == null || !csrf.equals(value))
				addActionError(getText("csrf.error"));
		}
		if (!hasErrors())
			validateDoubleCheck(request, response);
		if (!hasErrors())
			validateCurrentPassword(request, response);
	}

	private void validateDoubleCheck(HttpServletRequest request, HttpServletResponse response) {
		DoubleChecker doubleCheck = findDoubleChecker();
		if (doubleCheck != null) {
			String username = request.getParameter(DoubleChecker.PARAMETER_NAME_USERNAME);
			String password = request.getParameter(DoubleChecker.PARAMETER_NAME_PASSWORD);
			if (username == null) {
				response.setHeader("X-Double-Check", "1");
				addFieldError("X-" + DoubleChecker.PARAMETER_NAME_USERNAME, getText("validation.required"));
				return;
			}
			if (StringUtils.isBlank(username)) {
				addFieldError(DoubleChecker.PARAMETER_NAME_USERNAME, getText("validation.required"));
				return;
			}
			if (username.equals(AuthzUtils.getUsername())) {
				addFieldError(DoubleChecker.PARAMETER_NAME_USERNAME, getText("access.denied"));
				return;
			}
			try {
				UserDetails doubleChecker = AuthzUtils.getUserDetails(username, password);
				if (!AuthzUtils.authorizeUserDetails(doubleChecker, null, doubleCheck.value(), null)) {
					addFieldError(DoubleChecker.PARAMETER_NAME_USERNAME, getText("access.denied"));
					return;
				}
				AuthzUtils.DOUBLE_CHCKER_HOLDER.set(doubleChecker);
			} catch (UsernameNotFoundException e) {
				addFieldError(DoubleChecker.PARAMETER_NAME_USERNAME, getText(e.getClass().getName()));
				return;
			} catch (BadCredentialsException e) {
				addFieldError(DoubleChecker.PARAMETER_NAME_PASSWORD, getText(e.getClass().getName()));
				return;
			}
		}
	}

	private void validateCurrentPassword(HttpServletRequest request, HttpServletResponse response) {
		CurrentPassword currentPasswordAnn = getAnnotation(CurrentPassword.class);
		if (currentPasswordAnn == null)
			return;
		HttpSession session = request.getSession();
		String currentPasswordThreshold = (String) session.getAttribute(SESSION_KEY_CURRENT_PASSWORD_THRESHOLD);
		int threshold = StringUtils.isNumeric(currentPasswordThreshold) ? Integer.valueOf(currentPasswordThreshold) : 0;
		String currentPassword = request.getParameter(CurrentPassword.PARAMETER_NAME_CURRENT_PASSWORD);
		if (currentPassword == null) {
			response.setHeader("X-Current-Password", "1");
			addFieldError("X-" + CurrentPassword.PARAMETER_NAME_CURRENT_PASSWORD, getText("validation.required"));
			return;
		}
		boolean valid = currentPassword != null && AuthzUtils.isPasswordValid(currentPassword);
		if (!valid) {
			addFieldError("currentPassword", getText("currentPassword.error"));
			threshold++;
			if (threshold >= currentPasswordAnn.threshold()) {
				session.invalidate();
				targetUrl = RequestUtils.getRequestUri(request);
			} else {
				session.setAttribute(SESSION_KEY_CURRENT_PASSWORD_THRESHOLD, String.valueOf(threshold));
			}
		} else {
			session.removeAttribute(SESSION_KEY_CURRENT_PASSWORD_THRESHOLD);
		}
	}

	@BeforeResult
	protected void preResult() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpServletResponse response = ServletActionContext.getResponse();
		if (StringUtils.isNotBlank(targetUrl)
				&& REDIRECT.equals(ActionContext.getContext().getActionInvocation().getResultCode()) && !hasErrors()
				&& RequestUtils.isSameOrigin(request.getRequestURL().toString(), targetUrl)) {
			targetUrl = response.encodeRedirectURL(targetUrl);
			response.setHeader(Redirect.RESPONSE_HEADER_NAME, targetUrl);
		}
		if (!(returnInput || !isAjax() || (isCaptchaRequired() && captchaStatus.isFirstReachThreshold())
				|| !(isUseJson() || hasErrors()))) {
			ActionContext.getContext().getActionInvocation().setResultCode(JSON);
			if (csrfRequired) {
				csrf = CodecUtils.nextId();
				RequestUtils.saveCookie(request, response, COOKIE_NAME_CSRF, csrf, false, true);
				response.addHeader("X-Postback", "csrf=" + csrf);
			}
		} else if (!isUseJson() && hasFieldErrors()) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, List<String>> entry : getFieldErrors().entrySet()) {
				sb.append(entry.getKey()).append(": ").append(String.join("\t", entry.getValue())).append("; ");
			}
			sb.delete(sb.length() - 2, sb.length() - 1);
			response.setHeader("X-Field-Errors", sb.toString());
		}
	}

	protected <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		Method method = BeanUtils.findMethod(getClass(),
				ActionContext.getContext().getActionInvocation().getProxy().getMethod());
		return method != null ? method.getAnnotation(annotationClass) : null;
	}

	protected Authorize findAuthorize() {
		Authorize authorize = getAnnotation(Authorize.class);
		if (authorize == null)
			authorize = getClass().getAnnotation(Authorize.class);
		return authorize;
	}

	protected DoubleChecker findDoubleChecker() {
		return getAnnotation(DoubleChecker.class);
	}

	private static String[] evalExpression(String[] arr) {
		if (arr == null || arr.length == 0)
			return arr;
		ValueStack vs = ActionContext.getContext().getValueStack();
		for (int i = 0; i < arr.length; i++) {
			String str = arr[i];
			while (true) {
				int start = str.indexOf("${");
				if (start > -1) {
					int end = str.indexOf('}', start + 2);
					if (end > 0) {
						String prefix = str.substring(0, start);
						String exp = str.substring(start + 2, end);
						String suffix = str.substring(end + 1);
						str = prefix + vs.findString(exp) + suffix;
					} else {
						break;
					}
				} else {
					break;
				}
			}
			arr[i] = str;
		}
		return arr;
	}

}
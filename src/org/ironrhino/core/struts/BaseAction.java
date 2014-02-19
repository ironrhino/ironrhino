package org.ironrhino.core.struts;

import java.io.BufferedReader;
import java.lang.annotation.Annotation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.metadata.Csrf;
import org.ironrhino.core.metadata.CurrentPassword;
import org.ironrhino.core.security.captcha.CaptchaManager;
import org.ironrhino.core.security.dynauth.DynamicAuthorizer;
import org.ironrhino.core.security.dynauth.DynamicAuthorizerManager;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.interceptor.annotations.Before;
import com.opensymphony.xwork2.interceptor.annotations.BeforeResult;
import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

public class BaseAction extends ActionSupport {

	private static final long serialVersionUID = -3183957331611790404L;

	private static final String SESSION_KEY_CURRENT_PASSWORD_THRESHOLD = "c_p_t";
	private static final String COOKIE_NAME_CSRF = "csrf";

	public static final String LIST = "list";
	public static final String VIEW = "view";
	public static final String PICK = "pick";
	public static final String TABS = "tabs";
	public static final String REFERER = "referer";
	public static final String JSON = "json";
	public static final String DYNAMICREPORTS = "dynamicreports";
	public static final String QRCODE = "qrcode";
	public static final String REDIRECT = "redirect";
	public static final String SUGGEST = "suggest";
	public static final String ACCESSDENIED = "accessDenied";
	public static final String NOTFOUND = "notFound";
	public static final String ERROR = "error";

	private boolean returnInput;

	// logic id or natrual id
	private String[] id;

	protected String keyword;

	protected String requestBody;

	protected String currentPassword;

	protected String originalActionName;

	protected String originalMethod;

	protected String targetUrl;

	protected String responseBody;

	protected boolean captchaRequired;

	private boolean firstReachCaptchaThreshold;

	protected String csrf;

	protected boolean csrfRequired;

	private String actionBaseUrl;

	@Autowired(required = false)
	protected transient CaptchaManager captchaManager;

	@Autowired(required = false)
	protected transient DynamicAuthorizerManager dynamicAuthorizerManager;

	public void setCsrf(String csrf) {
		this.csrf = csrf;
	}

	public String getCsrf() {
		if (csrfRequired && csrf == null) {
			csrf = CodecUtils.nextId();
			RequestUtils.saveCookie(ServletActionContext.getRequest(),
					ServletActionContext.getResponse(), COOKIE_NAME_CSRF, csrf,
					false, true);
		}
		return csrf;
	}

	public boolean isCsrfRequired() {
		return csrfRequired;
	}

	public boolean isCaptchaRequired() {
		return captchaRequired;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public String getActionBaseUrl() {
		if (actionBaseUrl == null) {
			ActionProxy proxy = ActionContext.getContext()
					.getActionInvocation().getProxy();
			String namespace = proxy.getNamespace();
			StringBuilder sb = new StringBuilder(ServletActionContext
					.getRequest().getContextPath()).append(namespace)
					.append(namespace.endsWith("/") ? "" : "/")
					.append(proxy.getActionName());
			actionBaseUrl = sb.toString();
		}
		return actionBaseUrl;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
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

	public void setId(String[] id) {
		this.id = id;
	}

	public String[] getId() {
		return id;
	}

	public boolean isUseJson() {
		return JSON.equalsIgnoreCase(ServletActionContext.getRequest()
				.getHeader("X-Data-Type"));
	}

	public boolean isAjax() {
		return "XMLHttpRequest".equalsIgnoreCase(ServletActionContext
				.getRequest().getHeader("X-Requested-With"));
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
	public String preAction() throws Exception {
		Authorize authorize = findAuthorize();
		if (authorize != null) {
			boolean authorized = AuthzUtils.authorize(authorize.ifAllGranted(),
					authorize.ifAnyGranted(), authorize.ifNotGranted());
			if (!authorized && dynamicAuthorizerManager != null
					&& !authorize.authorizer().equals(DynamicAuthorizer.class)) {
				ActionProxy ap = ActionContext.getContext()
						.getActionInvocation().getProxy();
				StringBuilder sb = new StringBuilder(ap.getNamespace());
				sb.append(ap.getNamespace().endsWith("/") ? "" : "/");
				sb.append(ap.getActionName());
				sb.append(ap.getMethod().equals("execute") ? "" : "/"
						+ ap.getMethod());
				String resource = sb.toString();
				UserDetails user = AuthzUtils.getUserDetails();
				authorized = dynamicAuthorizerManager.authorize(
						authorize.authorizer(), user, resource);
			}
			if (!authorized) {
				addActionError(getText("access.denied"));
				return ACCESSDENIED;
			}
		}
		Captcha captcha = getAnnotation(Captcha.class);
		if (captcha != null && captchaManager != null) {
			boolean[] array = captchaManager.isCaptchaRequired(
					ServletActionContext.getRequest(), captcha);
			captchaRequired = array[0];
			firstReachCaptchaThreshold = array[1];
		}
		csrfRequired = !captchaRequired && getAnnotation(Csrf.class) != null;
		return null;
	}

	@Before(priority = 10)
	public String returnInputOrExtractRequestBody() throws Exception {
		String method = ServletActionContext.getRequest().getMethod();
		InputConfig inputConfig = getAnnotation(InputConfig.class);
		if (inputConfig != null && "GET".equalsIgnoreCase(method)) {
			returnInput = true;
			if (!inputConfig.methodName().equals("")) {
				ActionInvocation ai = ActionContext.getContext()
						.getActionInvocation();
				originalActionName = ai.getProxy().getActionName();
				originalMethod = ai.getProxy().getMethod();
				// ai.getProxy().setMethod(annotation.methodName());
				return (String) this.getClass()
						.getMethod(inputConfig.methodName()).invoke(this);
			} else {
				return inputConfig.resultName();
			}
		}
		if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
			try {
				BufferedReader reader = ServletActionContext.getRequest()
						.getReader();
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null)
					sb.append(line).append("\n");
				reader.close();
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
					requestBody = sb.toString();
				}
			} catch (IllegalStateException e) {

			}
		}
		return null;
	}

	@Override
	public void validate() {
		if (captchaManager != null
				&& captchaRequired
				&& !firstReachCaptchaThreshold
				&& !captchaManager.validate(ServletActionContext.getRequest(),
						ServletActionContext.getRequest().getSession().getId()))
			addFieldError(CaptchaManager.KEY_CAPTCHA, getText("captcha.error"));
		if (csrfRequired) {
			String value = RequestUtils.getCookieValue(
					ServletActionContext.getRequest(), COOKIE_NAME_CSRF);
			RequestUtils.deleteCookie(ServletActionContext.getRequest(),
					ServletActionContext.getResponse(), COOKIE_NAME_CSRF);
			if (csrf == null || !csrf.equals(value))
				addActionError(getText("csrf.error"));
		}
		validateCurrentPassword();
	}

	private void validateCurrentPassword() {
		CurrentPassword currentPasswordAnn = getAnnotation(CurrentPassword.class);
		if (currentPasswordAnn == null)
			return;
		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		String currentPasswordThreshold = (String) session
				.getAttribute(SESSION_KEY_CURRENT_PASSWORD_THRESHOLD);
		int threshold = StringUtils.isNumeric(currentPasswordThreshold) ? Integer
				.valueOf(currentPasswordThreshold) : 0;
		boolean valid = currentPassword != null
				&& AuthzUtils.isPasswordValid(currentPassword);
		if (!valid) {
			addFieldError("currentPassword", getText("currentPassword.error"));
			threshold++;
			if (threshold >= currentPasswordAnn.threshold()) {
				session.invalidate();
				targetUrl = RequestUtils.getRequestUri(request);
			} else {
				session.setAttribute(SESSION_KEY_CURRENT_PASSWORD_THRESHOLD,
						String.valueOf(threshold));
			}
		} else {
			session.removeAttribute(SESSION_KEY_CURRENT_PASSWORD_THRESHOLD);
		}
	}

	@BeforeResult
	public void preResult() throws Exception {
		if (StringUtils.isNotBlank(targetUrl)
				&& !hasErrors()
				&& RequestUtils.isSameOrigin(ServletActionContext.getRequest()
						.getRequestURL().toString(), targetUrl)) {
			targetUrl = ServletActionContext.getResponse().encodeRedirectURL(
					targetUrl);
			ServletActionContext.getResponse().setHeader("X-Redirect-To",
					targetUrl);
		}
		if (!(returnInput || !isAjax()
				|| (captchaRequired && firstReachCaptchaThreshold) || !(isUseJson() || hasErrors())))
			ActionContext.getContext().getActionInvocation()
					.setResultCode(JSON);
	}

	protected <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return AnnotationUtils.getAnnotation(getClass(), annotationClass,
				ActionContext.getContext().getActionInvocation().getProxy()
						.getMethod());
	}

	protected Authorize findAuthorize() {
		Authorize authorize = getAnnotation(Authorize.class);
		if (authorize == null)
			authorize = getClass().getAnnotation(Authorize.class);
		return authorize;
	}

}
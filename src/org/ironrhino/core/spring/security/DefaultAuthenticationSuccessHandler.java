package org.ironrhino.core.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 权限验证成功默认实现类
 */
@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class DefaultAuthenticationSuccessHandler implements
		AuthenticationSuccessHandler {

	public final static String COOKIE_NAME_LOGIN_USER = "U";
	// 设置用户名写入cookie中的开关
	@Value("${authenticationSuccessHandler.usernameInCookie:true}")
	private boolean usernameInCookie;
	// 用户名在cookie中存活的时长，时间单位毫秒
	@Value("${authenticationSuccessHandler.usernameInCookieMaxAge:31536000}")
	private int usernameInCookieMaxAge;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication)
			throws ServletException, IOException {
		if (usernameInCookie && request.isRequestedSessionIdFromCookie()) {
			RequestUtils.saveCookie(request, response, COOKIE_NAME_LOGIN_USER,
					authentication.getName(), usernameInCookieMaxAge, true,
					false);
		}
	}

}

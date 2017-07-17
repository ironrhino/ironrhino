package org.ironrhino.security.oauth.server.component;

import javax.servlet.http.HttpServletRequest;

import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class OAuthAuthorizationArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavc, NativeWebRequest request,
			WebDataBinderFactory binderFactory) throws Exception {
		return ((HttpServletRequest) request.getNativeRequest())
				.getAttribute(OAuthHandler.REQUEST_ATTRIBUTE_KEY_OAUTH_AUTHORIZATION);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Qualifier.class)
				&& parameter.getParameterType() == OAuthAuthorization.class;
	}

}

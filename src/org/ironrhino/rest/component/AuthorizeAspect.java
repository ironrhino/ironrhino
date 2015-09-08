package org.ironrhino.rest.component;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.rest.RestStatus;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;

@Aspect
@ControllerAdvice
public class AuthorizeAspect extends BaseAspect {

	public AuthorizeAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Before("execution(public * *(..)) and @annotation(requestMapping) and not @annotation(org.ironrhino.core.metadata.Authorize)")
	public void authorizeClass(JoinPoint jp, RequestMapping requestMapping) throws Throwable {
		authorize(jp.getTarget().getClass().getAnnotation(Authorize.class));
	}

	@Before("execution(public * *(..)) and @annotation(requestMapping) and @annotation(authorize)")
	public void authorizeMethod(JoinPoint jp, RequestMapping requestMapping, Authorize authorize) throws Throwable {
		authorize(authorize);
	}

	private void authorize(Authorize authorize) {
		if (authorize != null
				&& !AuthzUtils.authorize(authorize.ifAllGranted(), authorize.ifAnyGranted(), authorize.ifNotGranted()))
			throw RestStatus.UNAUTHORIZED;
	}

}

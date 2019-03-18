package org.ironrhino.rest.component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.aop.BaseAspect;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.Value;

abstract class AbstractInstrumentAspect extends BaseAspect {

	protected final String servletMapping;

	public AbstractInstrumentAspect(String servletMapping) {
		if (servletMapping.endsWith("/*"))
			servletMapping = servletMapping.substring(0, servletMapping.length() - 2);
		this.servletMapping = servletMapping;
	}

	protected Mapping getMapping(ProceedingJoinPoint pjp) {
		RequestMapping requestMapping = AnnotatedElementUtils
				.findMergedAnnotation(((MethodSignature) pjp.getSignature()).getMethod(), RequestMapping.class);
		if (requestMapping == null)
			return null;
		RequestMapping requestMappingWithClass = AnnotatedElementUtils.findMergedAnnotation(pjp.getTarget().getClass(),
				RequestMapping.class);
		String method = requestMapping.method().length > 0 ? requestMapping.method()[0].toString() : "GET";
		StringBuilder sb = new StringBuilder(servletMapping);
		if (requestMappingWithClass != null) {
			String[] pathWithClass = requestMappingWithClass.path();
			if (pathWithClass.length > 0)
				sb.append(pathWithClass[0]);
		}
		String[] path = requestMapping.path();
		sb.append(path.length > 0 ? path[0] : "");
		String uri = sb.toString();
		return new Mapping(method, uri);
	}

	@Value
	static class Mapping {
		String method;
		String uri;
	}

}

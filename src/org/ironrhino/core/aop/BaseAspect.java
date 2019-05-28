package org.ironrhino.core.aop;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

public class BaseAspect implements Ordered {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Getter
	@Setter
	protected int order;

	protected boolean isBypass() {
		return AopContext.isBypass(this.getClass());
	}

	protected Map<String, Object> buildContext(JoinPoint jp) {
		Map<String, Object> context = new HashMap<>();
		Object[] args = jp.getArgs();
		String[] paramNames = ReflectionUtils.getParameterNames(jp);
		if (paramNames == null) {
			throw new RuntimeException("No parameter names discovered for method, please consider using @Param");
		} else {
			for (int i = 0; i < args.length; i++)
				context.put(paramNames[i], args[i]);
		}
		context.put(AopContext.CONTEXT_KEY_THIS, jp.getThis());
		context.put(AopContext.CONTEXT_KEY_METHOD_NAME, jp.getSignature().getName());
		context.put(AopContext.CONTEXT_KEY_ARGS, jp.getArgs());
		context.put(AopContext.CONTEXT_KEY_REQUEST, RequestContext.getRequest());
		context.put(AopContext.CONTEXT_KEY_USER, AuthzUtils.getUserDetails());
		return context;
	}

	protected void putReturnValueIntoContext(Map<String, Object> context, Object value) {
		String oldName = AopContext.CONTEXT_KEY_RETVAL.substring(1);
		if (!context.containsKey(oldName))
			context.put(oldName, value);
		context.put(AopContext.CONTEXT_KEY_RETVAL, value);
	}

	protected String buildKey(ProceedingJoinPoint jp) {
		StringBuilder sb = new StringBuilder();
		Class<?> beanClass = jp.getTarget().getClass();
		String beanName = NameGenerator.buildDefaultBeanName(beanClass.getName());
		Component comp = AnnotatedElementUtils.getMergedAnnotation(beanClass, Component.class);
		if (comp != null && StringUtils.isNotBlank(comp.value()))
			beanName = comp.value();
		MethodSignature signature = (MethodSignature) jp.getSignature();
		sb.append(beanName).append('.').append(signature.getName()).append('(');
		Class<?>[] parameterTypes = signature.getParameterTypes();
		if (parameterTypes.length > 0) {
			for (int i = 0; i < parameterTypes.length; i++) {
				sb.append(parameterTypes[i].getSimpleName());
				if (i != parameterTypes.length - 1)
					sb.append(',');
			}
		}
		sb.append(')');
		return sb.toString();
	}

}

package org.ironrhino.rest.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.Setter;

public class RestApiFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	private final Class<?> restApiClass;

	@Getter
	@Setter
	private RestTemplate restTemplate;

	@Getter
	@Setter
	private String apiBaseUrl;

	private Object restApiBean;

	public RestApiFactoryBean(Class<?> restApiClass) {
		this(restApiClass, null);
	}

	public RestApiFactoryBean(Class<?> restApiClass, RestTemplate restTemplate) {
		Assert.notNull(restApiClass, "restApiClass shouldn't be null");
		if (restTemplate == null) {
			restTemplate = new RestTemplate();
			Iterator<HttpMessageConverter<?>> it = restTemplate.getMessageConverters().iterator();
			while (it.hasNext()) {
				if (it.next() instanceof MappingJackson2XmlHttpMessageConverter)
					it.remove();
			}
		}
		if (!restApiClass.isInterface())
			throw new IllegalArgumentException(restApiClass.getName() + " should be interface");
		this.restApiClass = restApiClass;
		this.restTemplate = restTemplate;
		this.restApiBean = new ProxyFactory(restApiClass, this).getProxy(restApiClass.getClassLoader());
	}

	public void setRestClient(RestClient restClient) {
		this.restTemplate = restClient.getRestTemplate();
	}

	@Override
	public Object getObject() throws Exception {
		return restApiBean;
	}

	@Override
	public Class<?> getObjectType() {
		return restApiClass;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod()))
			return "RestApi for  [" + getObjectType().getName() + "]";
		Method method = methodInvocation.getMethod();
		RequestMapping classRequestMapping = AnnotatedElementUtils.findMergedAnnotation(restApiClass,
				RequestMapping.class);
		RequestMapping methodRequestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (classRequestMapping == null && methodRequestMapping == null)
			throw new UnsupportedOperationException("@RequestMapping should be present");
		StringBuilder url = new StringBuilder(apiBaseUrl);
		if (classRequestMapping != null)
			url.append(classRequestMapping.value()[0]);
		if (methodRequestMapping != null)
			url.append(methodRequestMapping.value()[0]);
		RequestMethod[] requestMethods = methodRequestMapping != null ? methodRequestMapping.method()
				: classRequestMapping.method();
		RequestMethod requestMethod = requestMethods.length > 0 ? requestMethods[0] : RequestMethod.GET;

		Map<String, Object> pathVariables = new HashMap<>(8);
		MultiValueMap<String, String> headers = new HttpHeaders();
		Map<String, String> requestParams = null;
		Map<String, String> cookieValues = null;
		Object body = null;
		String[] parameterNames = ReflectionUtils.getParameterNames(method);
		Object[] arguments = methodInvocation.getArguments();
		Annotation[][] array = method.getParameterAnnotations();
		for (int i = 0; i < array.length; i++) {
			Object argument = arguments[i];
			if (argument == null)
				continue;
			for (Annotation anno : array[i]) {
				if (anno instanceof PathVariable) {
					String name = ((PathVariable) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					pathVariables.put(name, argument);
				}
				if (anno instanceof RequestHeader) {
					String name = ((RequestHeader) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					headers.add(name, argument.toString());
				}
				if (anno instanceof RequestParam) {
					String name = ((RequestParam) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					if (requestParams == null)
						requestParams = new HashMap<>(8);
					requestParams.put(name, argument.toString());
				}
				if (anno instanceof CookieValue) {
					String name = ((CookieValue) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					if (cookieValues == null)
						cookieValues = new HashMap<>(8);
					cookieValues.put(name, argument.toString());
				}
				if (anno instanceof RequestBody) {
					body = argument;
				}
			}
		}
		if (cookieValues != null) {
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> entry : cookieValues.entrySet())
				sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("; ");
			sb.delete(sb.length() - 2, sb.length());
			headers.add(HttpHeaders.COOKIE, sb.toString());
		}
		if (requestParams != null) {
			for (Map.Entry<String, String> entry : requestParams.entrySet()) {
				url.append(url.indexOf("?") < 0 ? '?' : '&').append(entry.getKey()).append("=")
						.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			}
		}
		RequestEntity<Object> requestEntity = new RequestEntity<>(body, headers,
				HttpMethod.valueOf(requestMethod.name()),
				restTemplate.getUriTemplateHandler().expand(url.toString(), pathVariables));
		ResponseEntity<?> responseEntity;
		Type grt = method.getGenericReturnType();
		if (grt == Void.class || grt == Void.TYPE)
			grt = null;
		final Type type = grt;
		responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<Object>() {
			@Override
			public Type getType() {
				return type;
			}
		});
		return responseEntity.getBody();
	}

}

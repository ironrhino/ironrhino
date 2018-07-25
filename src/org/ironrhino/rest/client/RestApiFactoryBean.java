package org.ironrhino.rest.client;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.throttle.CircuitBreaking;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestApiFactoryBean implements MethodInterceptor, FactoryBean<Object> {

	private final Class<?> restApiClass;

	private final RestTemplate restTemplate;

	private final String apiBaseUrl;

	private final Object restApiBean;

	private final Map<Method, MethodHandle> defaultMethods = new ConcurrentHashMap<>();

	@Autowired
	private ApplicationContext ctx;

	public RestApiFactoryBean(Class<?> restApiClass) {
		this(restApiClass, (RestTemplate) null);
	}

	public RestApiFactoryBean(Class<?> restApiClass, RestClient restClient) {
		this(restApiClass, restClient.getRestTemplate());
	}

	public RestApiFactoryBean(Class<?> restApiClass, RestTemplate restTemplate) {
		Assert.notNull(restApiClass, "restApiClass shouldn't be null");
		if (!restApiClass.isInterface())
			throw new IllegalArgumentException(restApiClass.getName() + " should be interface");
		this.restApiClass = restApiClass;
		RestApi annotation = restApiClass.getAnnotation(RestApi.class);
		this.apiBaseUrl = (annotation != null) ? annotation.apiBaseUrl() : "";
		if (restTemplate == null) {
			restTemplate = new org.ironrhino.core.spring.http.client.RestTemplate();
			Iterator<HttpMessageConverter<?>> it = restTemplate.getMessageConverters().iterator();
			while (it.hasNext()) {
				if (it.next() instanceof MappingJackson2XmlHttpMessageConverter)
					it.remove();
			}
		}
		this.restTemplate = restTemplate;
		this.restApiBean = new ProxyFactory(restApiClass, this).getProxy(restApiClass.getClassLoader());
	}

	@Override
	public Object getObject() {
		return restApiBean;
	}

	@Override
	@NonNull
	public Class<?> getObjectType() {
		return restApiClass;
	}

	@Override
	public Object invoke(final MethodInvocation methodInvocation) throws Exception {
		return CircuitBreaking.execute(restApiClass.getName(), ex -> ex.getCause() instanceof IOException,
				() -> doInvoke(methodInvocation));
	}

	@SuppressWarnings("unchecked")
	public Object doInvoke(MethodInvocation methodInvocation) throws Exception {
		Method method = methodInvocation.getMethod();
		if (method.isDefault()) {
			MethodHandle mh = defaultMethods.computeIfAbsent(method, m -> {
				try {
					if (ClassUtils.isPresent("java.lang.StackWalker", System.class.getClassLoader())) {
						// jdk 9 and later
						return MethodHandles.lookup()
								.findSpecial(restApiClass, m.getName(),
										MethodType.methodType(m.getReturnType(), m.getParameterTypes()), restApiClass)
								.bindTo(restApiBean);
					} else {
						Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
						constructor.setAccessible(true);
						return constructor.newInstance(restApiClass).in(restApiClass)
								.unreflectSpecial(method, restApiClass).bindTo(restApiBean);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			try {
				return mh.invokeWithArguments(methodInvocation.getArguments());
			} catch (Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		if (AopUtils.isToStringMethod(methodInvocation.getMethod()))
			return "RestApi for  [" + getObjectType().getName() + "]";
		RequestMapping classRequestMapping = AnnotatedElementUtils.findMergedAnnotation(restApiClass,
				RequestMapping.class);
		RequestMapping methodRequestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (classRequestMapping == null && methodRequestMapping == null)
			throw new UnsupportedOperationException("@RequestMapping should be present");
		StringBuilder sb = new StringBuilder(apiBaseUrl);
		if (classRequestMapping != null && classRequestMapping.value().length > 0)
			sb.append(classRequestMapping.value()[0]);
		if (methodRequestMapping != null && methodRequestMapping.value().length > 0)
			sb.append(methodRequestMapping.value()[0]);
		String url = sb.toString().trim();
		if (ctx != null)
			url = ctx.getEnvironment().resolvePlaceholders(url);
		RequestMethod[] requestMethods = methodRequestMapping != null ? methodRequestMapping.method()
				: classRequestMapping.method();
		RequestMethod requestMethod = requestMethods.length > 0 ? requestMethods[0] : RequestMethod.GET;

		Map<String, Object> pathVariables = new HashMap<>(8);
		MultiValueMap<String, String> headers = new HttpHeaders();
		MultiValueMap<String, Object> requestParams = null;
		Map<String, String> cookieValues = null;
		List<Object> requestParamsObjectCandidates = new ArrayList<>(1);
		Object body = null;
		String[] parameterNames = ReflectionUtils.getParameterNames(method);
		Object[] arguments = methodInvocation.getArguments();
		Annotation[][] array = method.getParameterAnnotations();
		InputStream is = null;
		boolean multipart = false;
		for (int i = 0; i < array.length; i++) {
			Object argument = arguments[i];
			if (argument == null)
				continue;
			if (argument instanceof InputStream)
				is = (InputStream) argument;
			boolean annotationPresent = false;
			for (Annotation anno : array[i]) {
				if (anno instanceof PathVariable) {
					annotationPresent = true;
					String name = ((PathVariable) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					pathVariables.put(name, argument);
				}
				if (anno instanceof RequestHeader) {
					annotationPresent = true;
					String name = ((RequestHeader) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					if (argument instanceof Collection) {
						for (Object o : (Collection<Object>) argument)
							if (o != null)
								headers.add(name, o.toString());
					} else {
						headers.add(name, argument.toString());
					}
				}
				if (anno instanceof RequestParam) {
					annotationPresent = true;
					String name = ((RequestParam) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					if (requestParams == null)
						requestParams = new LinkedMultiValueMap<>(8);
					if (argument instanceof Collection) {
						for (Object o : (Collection<Object>) argument)
							if (o != null) {
								if (o instanceof File)
									o = new FileSystemResource((File) o);
								else if (o instanceof InputStream)
									o = new InputStreamResource((InputStream) o);
								if (o instanceof Resource)
									multipart = true;
								else
									o = o.toString();
								requestParams.add(name, o);
							}
					} else {
						Object o = argument;
						if (o instanceof File)
							o = new FileSystemResource((File) o);
						else if (o instanceof InputStream)
							o = new InputStreamResource((InputStream) o);
						if (o instanceof Resource)
							multipart = true;
						else
							o = o.toString();
						requestParams.add(name, o);
					}
				}
				if (anno instanceof CookieValue) {
					annotationPresent = true;
					String name = ((CookieValue) anno).value();
					if (StringUtils.isBlank(name))
						name = parameterNames[i];
					if (cookieValues == null)
						cookieValues = new HashMap<>(8);
					cookieValues.put(name, argument.toString());
				}
				if (anno instanceof RequestBody) {
					annotationPresent = true;
					body = argument;
				}
			}
			// Put arguments to pathVariable even if @PathVariable not present
			if (!annotationPresent) {
				pathVariables.put(parameterNames[i], argument);
				String clazz = argument.getClass().getName();
				if (!argument.getClass().isEnum() && !clazz.startsWith("java.") && !clazz.startsWith("javax.")) {
					requestParamsObjectCandidates.add(argument);
				}
			}
		}
		if (requestParamsObjectCandidates.size() == 1 && requestParams == null) {
			requestParams = new LinkedMultiValueMap<>(8);
			BeanWrapperImpl bw = new BeanWrapperImpl(requestParamsObjectCandidates.get(0));
			for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
				if (pd.getReadMethod() == null || pd.getWriteMethod() == null)
					continue;
				String name = pd.getName();
				Object argument = bw.getPropertyValue(name);
				if (argument == null)
					continue;
				if (requestParams == null)
					requestParams = new LinkedMultiValueMap<>(8);
				if (argument instanceof Collection) {
					for (Object o : (Collection<Object>) argument)
						if (o != null)
							requestParams.add(name, o.toString());
				} else {
					requestParams.add(name, argument.toString());
				}
			}
		}

		while (url.contains("{")) {
			int index = url.indexOf('{');
			String prefix = url.substring(0, index);
			if (!url.endsWith("}"))
				prefix = null;
			url = URLDecoder.decode(restTemplate.getUriTemplateHandler().expand(url, pathVariables).toString(),
					"UTF-8");
			if (url.indexOf("http://") > 0)
				url = url.substring(url.indexOf("http://"));
			else if (url.indexOf("https://") > 0)
				url = url.substring(url.indexOf("https://"));
			else if (prefix != null && prefix.length() > 0 && url.substring(index).startsWith(prefix))
				url = url.substring(index);
		}
		if (cookieValues != null) {
			StringBuilder cookie = new StringBuilder();
			for (Map.Entry<String, String> entry : cookieValues.entrySet())
				cookie.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"))
						.append("; ");
			cookie.delete(cookie.length() - 2, cookie.length());
			headers.set(HttpHeaders.COOKIE, cookie.toString());
		}
		if (requestMethod.name().startsWith("P")) {
			if (body == null && is != null)
				body = is;
			if (body instanceof InputStream)
				body = new InputStreamResource((InputStream) body);
		}
		if (requestParams != null) {
			if (body == null && requestMethod.name().startsWith("P")) {
				if (!headers.containsKey(HttpHeaders.CONTENT_TYPE))
					headers.set(HttpHeaders.CONTENT_TYPE, multipart ? MediaType.MULTIPART_FORM_DATA_VALUE
							: MediaType.APPLICATION_FORM_URLENCODED_VALUE);
				body = requestParams;
			} else {
				StringBuilder temp = new StringBuilder(url);
				for (Map.Entry<String, List<Object>> entry : requestParams.entrySet())
					for (Object value : entry.getValue())
						if (value instanceof String)
							temp.append(temp.indexOf("?") < 0 ? '?' : '&').append(entry.getKey()).append("=")
									.append(URLEncoder.encode(value.toString(), "UTF-8"));
				url = temp.toString();
			}
		}

		RequestEntity<Object> requestEntity = new RequestEntity<>(body, headers,
				HttpMethod.valueOf(requestMethod.name()), URI.create(url));
		Type type = method.getGenericReturnType();
		if (type == InputStream.class) {
			Resource resource = restTemplate.exchange(requestEntity, Resource.class).getBody();
			if (resource == null)
				return null;
			return resource.getInputStream();
		} else if (type == Void.TYPE) {
			restTemplate.exchange(requestEntity, Resource.class);
			return null;
		} else {
			JsonPointer pointer = method.getAnnotation(JsonPointer.class);
			if (pointer == null) {
				return restTemplate.exchange(requestEntity, ParameterizedTypeReference.forType(type)).getBody();
			} else {
				AbstractJackson2HttpMessageConverter jackson = null;
				for (HttpMessageConverter<?> mc : restTemplate.getMessageConverters()) {
					if (mc instanceof AbstractJackson2HttpMessageConverter) {
						jackson = (AbstractJackson2HttpMessageConverter) mc;
						break;
					}
				}
				if (jackson == null)
					throw new RuntimeException("AbstractJackson2HttpMessageConverter not present");
				JsonNode tree = restTemplate.exchange(requestEntity, JsonNode.class).getBody();
				Class<? extends JsonValidator> validatorClass = pointer.validator();
				if (validatorClass != JsonValidator.class) {
					JsonValidator validator = null;
					if (ctx != null)
						try {
							validator = ctx.getBean(validatorClass);
						} catch (NoSuchBeanDefinitionException e) {
							// fallback
						}
					if (validator == null)
						validator = validatorClass.getConstructor().newInstance();
					validator.validate(tree);
				}
				if (!pointer.value().isEmpty())
					tree = tree.at(pointer.value());
				ObjectMapper mapper = jackson.getObjectMapper();
				if (type instanceof Class && ((Class<?>) type).isAssignableFrom(JsonNode.class))
					return tree;
				return mapper.readValue(mapper.treeAsTokens(tree), mapper.getTypeFactory().constructType(type));
			}
		}
	}

}

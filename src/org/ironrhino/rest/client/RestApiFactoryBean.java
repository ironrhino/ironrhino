package org.ironrhino.rest.client;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.spring.FallbackSupportMethodInterceptorFactoryBean;
import org.ironrhino.core.spring.http.client.PrependBaseUrlClientHttpRequestInterceptor;
import org.ironrhino.core.throttle.CircuitBreakerRegistry;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.MaxAttemptsExceededException;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.RestStatus;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.Getter;
import lombok.Setter;

public class RestApiFactoryBean extends FallbackSupportMethodInterceptorFactoryBean {

	public static final String BASE_URL_SUFFIX = ".apiBaseUrl";

	private static final Predicate<Throwable> IO_ERROR_PREDICATE = ex -> ex instanceof ResourceAccessException
			&& ex.getCause() instanceof IOException;

	private static final Predicate<MethodInvocation> NOT_RETRYABLE_PREDICATE = mi -> Stream.of(mi.getArguments())
			.anyMatch(arg -> arg instanceof InputStream);

	private Object serviceRegistry;

	private final Class<?> restApiClass;

	private final RestApi annotation;

	private final RestTemplate restTemplate;

	private String apiBaseUrl;

	private final Map<String, String> requestHeaders;

	private final Object restApiBean;

	private ObjectMapper objectMapper;

	@Getter
	@Setter
	private int maxAttempts = 3;

	@Autowired(required = false)
	private CircuitBreakerRegistry circuitBreakerRegistry;

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
		this.annotation = AnnotationUtils.findAnnotation(restApiClass, RestApi.class);
		this.apiBaseUrl = (annotation != null) ? annotation.apiBaseUrl() : "";
		Map<String, String> map = null;
		if (annotation != null) {
			org.ironrhino.rest.client.RequestHeader[] rhs = annotation.requestHeaders();
			if (rhs.length > 0) {
				map = new HashMap<>(rhs.length * 2);
				for (org.ironrhino.rest.client.RequestHeader h : annotation.requestHeaders())
					map.put(h.name(), h.value());
			}
		}
		this.requestHeaders = map != null ? map : Collections.emptyMap();
		if (restTemplate == null) {
			restTemplate = new org.ironrhino.core.spring.http.client.RestTemplate();
			Iterator<HttpMessageConverter<?>> it = restTemplate.getMessageConverters().iterator();
			while (it.hasNext()) {
				if (it.next() instanceof MappingJackson2XmlHttpMessageConverter)
					it.remove();
			}
		}
		this.restTemplate = restTemplate;
		this.restTemplate.getInterceptors().add(new PrependBaseUrlClientHttpRequestInterceptor(() -> apiBaseUrl));
		this.restApiBean = new ProxyFactory(restApiClass, this).getProxy(restApiClass.getClassLoader());
		for (HttpMessageConverter<?> mc : restTemplate.getMessageConverters()) {
			if (mc instanceof AbstractJackson2HttpMessageConverter) {
				objectMapper = ((AbstractJackson2HttpMessageConverter) mc).getObjectMapper();
				break;
			}
		}
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
	protected boolean shouldFallBackFor(Throwable ex) {
		return ex instanceof CallNotPermittedException;
	}

	@PostConstruct
	private void init() {
		ApplicationContext ctx = getApplicationContext();
		Environment env = ctx.getEnvironment();
		String baseUrl = env.getProperty(restApiClass.getName() + BASE_URL_SUFFIX);
		if (StringUtils.isNotBlank(baseUrl)) {
			baseUrl = env.resolvePlaceholders(baseUrl);
			apiBaseUrl = baseUrl;
			log.info("Discover baseUrl \"{}\" for service {} from environment", baseUrl, restApiClass.getName());
		} else if (StringUtils.isNotBlank(apiBaseUrl)) {
			apiBaseUrl = env.resolvePlaceholders(apiBaseUrl);
		}
		if (ClassUtils.isPresent("org.ironrhino.core.remoting.ServiceRegistry", getClass().getClassLoader()))
			try {
				serviceRegistry = ctx.getBean(ServiceRegistry.class);
			} catch (NoSuchBeanDefinitionException e) {
			}
	}

	@Override
	protected Object doInvoke(MethodInvocation methodInvocation) throws Exception {
		Callable<Object> callable = () -> {
			int remainingAttempts = maxAttempts;
			do {
				try {
					return actualInvoke(methodInvocation);
				} catch (Exception e) {
					if (!IO_ERROR_PREDICATE.test(e) || NOT_RETRYABLE_PREDICATE.test(methodInvocation)
							|| remainingAttempts <= 1)
						throw e;
				}
			} while (--remainingAttempts > 0);
			throw new MaxAttemptsExceededException(maxAttempts);
		};
		Callable<Object> old = callable;
		callable = () -> Tracing.execute(ReflectionUtils.stringify(methodInvocation.getMethod()), old, "span.kind",
				"client", "component", "rest");
		return circuitBreakerRegistry != null
				? circuitBreakerRegistry.of(restApiClass.getName(), IO_ERROR_PREDICATE).executeCallable(callable)
				: callable.call();
	}

	@SuppressWarnings("unchecked")
	private Object actualInvoke(MethodInvocation methodInvocation) throws Exception {
		Method method = methodInvocation.getMethod();
		if (method.isAnnotationPresent(Lookup.class)) {
			if (method.getReturnType().isAssignableFrom(RestTemplate.class))
				return restTemplate;
		}
		RequestMapping classRequestMapping = AnnotatedElementUtils.findMergedAnnotation(restApiClass,
				RequestMapping.class);
		RequestMapping methodRequestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (classRequestMapping == null && methodRequestMapping == null)
			throw new UnsupportedOperationException("@RequestMapping should be present");
		String url = getRequestUrl(classRequestMapping, methodRequestMapping);
		String requestMethod = getRequestMethod(classRequestMapping, methodRequestMapping);

		Map<String, Object> pathVariables = new HashMap<>(8);
		MultiValueMap<String, String> headers = createHeaders(classRequestMapping, methodRequestMapping);
		MultiValueMap<String, Object> requestParams = null;
		Map<String, String> cookieValues = null;
		List<Object> requestParamsObjectCandidates = new ArrayList<>(1);
		Object body = null;
		String[] parameterNames = ReflectionUtils.getParameterNames(method);
		Object[] arguments = methodInvocation.getArguments();
		Annotation[][] array = method.getParameterAnnotations();
		for (int i = 0; i < array.length; i++) {
			Object argument = arguments[i];
			if (argument == null)
				continue;
			if (argument instanceof InputStream) {
				body = new InputStreamResource((InputStream) argument);
			} else if (argument instanceof byte[]) {
				body = new ByteArrayResource((byte[]) argument);
			}
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
						for (Object obj : (Collection<Object>) argument)
							if (obj != null)
								headers.add(name, obj.toString());
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
						for (Object obj : (Collection<Object>) argument)
							if (obj != null) {
								if (obj instanceof File)
									obj = new FileSystemResource((File) obj);
								else if (obj instanceof InputStream)
									obj = new InputStreamResource((InputStream) obj);
								else
									obj = obj.toString();
								requestParams.add(name, obj);
							}
					} else {
						Object obj = argument;
						if (obj instanceof File)
							obj = new FileSystemResource((File) obj);
						else if (obj instanceof InputStream)
							obj = new InputStreamResource((InputStream) obj);
						else
							obj = obj.toString();
						requestParams.add(name, obj);
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
					for (Object obj : (Collection<Object>) argument)
						if (obj != null)
							requestParams.add(name, obj.toString());
				} else {
					requestParams.add(name, argument.toString());
				}
			}
		}
		headers = addCookies(headers, cookieValues);
		url = resolvePathVariables(url, pathVariables);
		return exchange(method, createRequestEntity(url, requestMethod, headers, requestParams, body));
	}

	private String getRequestMethod(RequestMapping classRequestMapping, RequestMapping methodRequestMapping) {
		RequestMethod[] requestMethods = methodRequestMapping != null ? methodRequestMapping.method()
				: classRequestMapping.method();
		RequestMethod requestMethod = requestMethods.length > 0 ? requestMethods[0] : RequestMethod.GET;
		return requestMethod.name();
	}

	private String getRequestUrl(RequestMapping classRequestMapping, RequestMapping methodRequestMapping)
			throws Exception {
		ApplicationContext ctx = getApplicationContext();
		String pathFromClass = getPathFromRequestMapping(classRequestMapping);
		String pathFromMethod = getPathFromRequestMapping(methodRequestMapping);
		String baseUrl = apiBaseUrl;
		if (ctx != null) {
			pathFromClass = ctx.getEnvironment().resolvePlaceholders(pathFromClass);
			pathFromMethod = ctx.getEnvironment().resolvePlaceholders(pathFromMethod);
			if (StringUtils.isBlank(baseUrl) && pathFromClass.indexOf("://") < 0 && pathFromMethod.indexOf("://") < 0
					&& serviceRegistry != null && !(restTemplate instanceof RestClientTemplate)) {
				baseUrl = ((ServiceRegistry) serviceRegistry).discover(restApiClass.getName());
				if (baseUrl.indexOf("://") < 0)
					baseUrl = "http://" + baseUrl;
			}
		}
		return (baseUrl + pathFromClass + pathFromMethod).trim();
	}

	private MultiValueMap<String, String> createHeaders(RequestMapping classRequestMapping,
			RequestMapping methodRequestMapping) throws UnsupportedEncodingException {
		HttpHeaders headers = new HttpHeaders();
		requestHeaders.forEach((k, v) -> {
			headers.set(k, v);
		});
		if (classRequestMapping != null) {
			for (String s : classRequestMapping.headers()) {
				String[] arr = s.split("=", 2);
				if (arr.length == 2)
					headers.add(arr[0], arr[1]);
			}
		}
		if (methodRequestMapping != null) {
			for (String s : methodRequestMapping.headers()) {
				String[] arr = s.split("=", 2);
				if (arr.length == 2)
					headers.add(arr[0], arr[1]);
			}
		}
		return headers;
	}

	private String getPathFromRequestMapping(RequestMapping requestMapping) {
		return (requestMapping != null && requestMapping.value().length > 0) ? requestMapping.value()[0] : "";
	}

	private String resolvePathVariables(String url, Map<String, Object> pathVariables)
			throws UnsupportedEncodingException {
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
		return url;
	}

	private MultiValueMap<String, String> addCookies(MultiValueMap<String, String> headers,
			Map<String, String> cookieValues) throws UnsupportedEncodingException {
		if (cookieValues != null && !cookieValues.isEmpty()) {
			StringBuilder cookie = new StringBuilder();
			for (Map.Entry<String, String> entry : cookieValues.entrySet())
				cookie.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"))
						.append("; ");
			headers.set(HttpHeaders.COOKIE, cookie.toString());
		}
		return headers;
	}

	private RequestEntity<Object> createRequestEntity(String url, String requestMethod,
			MultiValueMap<String, String> headers, MultiValueMap<String, Object> requestParams, Object body)
			throws UnsupportedEncodingException {
		if (requestParams != null) {
			if (body == null && requestMethod.startsWith("P")) {
				boolean multipart = requestParams.entrySet().stream().flatMap(entry -> entry.getValue().stream())
						.anyMatch(e -> e instanceof Resource);
				if (multipart && !headers.containsKey(HttpHeaders.CONTENT_TYPE))
					headers.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
				body = requestParams;
			} else {
				StringBuilder temp = new StringBuilder(url);
				for (Map.Entry<String, List<Object>> entry : requestParams.entrySet())
					for (Object value : entry.getValue()) {
						String v = value != null ? value.toString() : null;
						if (v == null)
							continue;
						temp.append(temp.indexOf("?") < 0 ? '?' : '&').append(entry.getKey()).append("=")
								.append(URLEncoder.encode(v.toString(), "UTF-8"));
					}
				url = temp.toString();
			}
		}
		return new RequestEntity<>(body, headers, HttpMethod.valueOf(requestMethod), URI.create(url));
	}

	private Object exchange(Method method, RequestEntity<Object> requestEntity) throws Exception {
		try {
			Type type = method.getGenericReturnType();
			if (type instanceof TypeVariable)
				type = GenericTypeResolver.resolveType(type, restApiClass);
			if (method.getReturnType() == ResponseEntity.class) {
				type = ((ParameterizedType) type).getActualTypeArguments()[0];
				if (type == InputStream.class) {
					// no InputStreamHttpMessageConverter use ResourceHttpMessageConverter instead
					ResponseEntity<Resource> response = restTemplate.exchange(requestEntity, Resource.class);
					Resource resource = response.getBody();
					return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders())
							.body(resource != null ? resource.getInputStream() : null);
				} else {
					return exchange(requestEntity, type, method.getAnnotation(JsonPointer.class));
				}
			}
			if (type == Void.TYPE) {
				restTemplate.exchange(requestEntity, Resource.class);
				return null;
			}
			if (type == Boolean.TYPE && requestEntity.getMethod() == HttpMethod.HEAD) {
				try {
					restTemplate.exchange(requestEntity, Resource.class);
					return true;
				} catch (NotFound e) {
					return false;
				}
			}
			if (type == InputStream.class) {
				Resource resource = restTemplate.exchange(requestEntity, Resource.class).getBody();
				return resource != null ? resource.getInputStream() : null;
			}
			return exchange(requestEntity, type, method.getAnnotation(JsonPointer.class)).getBody();
		} catch (HttpStatusCodeException e) {
			if (e instanceof NotFound) {
				if (requestEntity.getMethod() == HttpMethod.GET && annotation != null
						&& annotation.treatNotFoundAsNull())
					return null;
			}
			try {
				JsonNode tree = objectMapper.readTree(e.getResponseBodyAsString());
				if (tree.has("code") && tree.has("status")) {
					RestStatus rs = objectMapper.readValue(objectMapper.treeAsTokens(tree), RestStatus.class);
					rs.initCause(e);
					throw rs;
				}
			} catch (JsonParseException jpe) {
			}
			throw e;
		}
	}

	private ResponseEntity<Object> exchange(RequestEntity<Object> requestEntity, Type type, JsonPointer pointer)
			throws Exception {
		if (pointer == null)
			return restTemplate.exchange(requestEntity, ParameterizedTypeReference.forType(type));
		ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode())
				.headers(response.getHeaders());
		JsonNode tree = response.getBody();
		Class<? extends JsonValidator> validatorClass = pointer.validator();
		if (validatorClass != JsonValidator.class) {
			JsonValidator validator = null;
			ApplicationContext ctx = getApplicationContext();
			if (ctx != null) {
				try {
					validator = ctx.getBean(validatorClass);
				} catch (NoSuchBeanDefinitionException e) {
				}
			}
			if (validator == null)
				validator = BeanUtils.instantiateClass(validatorClass);
			validator.validate(tree);
		}
		if (!pointer.value().isEmpty())
			tree = tree.at(pointer.value());
		Object body;
		if (type instanceof Class && ((Class<?>) type).isAssignableFrom(JsonNode.class))
			body = tree;
		else
			body = objectMapper.readValue(objectMapper.treeAsTokens(tree), objectMapper.constructType(type));
		return builder.body(body);
	}

	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> restApiClass) {
		return (T) new RestApiFactoryBean(restApiClass).getObject();
	}

	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> restApiClass, RestTemplate restTemplate) {
		return (T) new RestApiFactoryBean(restApiClass, restTemplate).getObject();
	}

}

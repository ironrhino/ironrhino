package org.ironrhino.rest.doc;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.Field;
import org.ironrhino.rest.doc.annotation.Fields;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiDoc implements Serializable {

	private static final long serialVersionUID = -3039539795219938302L;

	private String name;

	private String description;

	protected String[] requiredAuthorities;

	protected String url;

	protected String[] methods;

	protected List<FieldObject> pathVariables = new ArrayList<>();

	protected List<FieldObject> requestParams = new ArrayList<>();

	protected List<FieldObject> requestHeaders = new ArrayList<>();

	protected List<FieldObject> cookieValues = new ArrayList<>();

	protected List<FieldObject> requestBody;

	protected List<FieldObject> responseBody;

	protected boolean requestBodyRequired = true;

	protected String requestBodyType;

	protected String responseBodyType;

	protected String requestBodySample;

	protected String responseBodySample;

	public ApiDoc(Class<?> apiDocClazz, Method apiDocMethod, ObjectMapper objectMapper) throws Exception {

		Class<?> clazz = apiDocClazz.getSuperclass();
		Method method = clazz.getMethod(apiDocMethod.getName(), apiDocMethod.getParameterTypes());
		Object apiDocInstance = apiDocClazz.newInstance();

		Api api = apiDocMethod.getAnnotation(Api.class);
		this.name = api.value();
		this.description = api.description();

		Authorize authorize = method.getAnnotation(Authorize.class);
		if (authorize == null)
			authorize = clazz.getAnnotation(Authorize.class);
		if (authorize != null) {
			String[] ifAnyGranted = authorize.ifAnyGranted();
			if (ifAnyGranted != null && ifAnyGranted.length > 0) {
				Set<String> set = new LinkedHashSet<>();
				for (String s : ifAnyGranted) {
					String[] arr = s.split("\\s*,\\s*");
					for (String ss : arr)
						set.add(ss);
				}
				requiredAuthorities = set.toArray(new String[0]);
			}
		}

		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (requestMapping != null) {
			String murl = "";
			String curl = "";
			String[] values = requestMapping.value();
			if (values.length > 0)
				murl = values[0];
			RequestMapping requestMappingWithClass = AnnotatedElementUtils.findMergedAnnotation(clazz,
					RequestMapping.class);
			if (requestMappingWithClass != null) {
				values = requestMappingWithClass.value();
				if (values.length > 0)
					curl = values[0];
			}
			if (StringUtils.isNotBlank(murl) || StringUtils.isNotBlank(curl))
				url = curl + murl;
			RequestMethod[] rms = requestMapping.method();
			if (rms.length > 0) {
				methods = new String[rms.length];
				for (int i = 0; i < rms.length; i++)
					methods[i] = rms[i].name();
			}
		}

		Class<?> responseBodyClass = method.getReturnType();
		if (Collection.class.isAssignableFrom(responseBodyClass)) {
			responseBodyType = "collection";
		} else if (ResultPage.class.isAssignableFrom(responseBodyClass)) {
			responseBodyType = "resultPage";
		}
		if (method.getGenericReturnType() instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) method.getGenericReturnType();
			Type type = pt.getActualTypeArguments()[0];
			if (type instanceof Class) {
				responseBodyClass = (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				pt = (ParameterizedType) type;
				if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
					responseBodyType = "collection";
					responseBodyClass = (Class<?>) pt.getActualTypeArguments()[0];
				}
			}
		}
		if (responseBodyClass.getGenericSuperclass() instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) responseBodyClass.getGenericSuperclass();
			Type type = pt.getActualTypeArguments()[0];
			if (type instanceof Class) {
				responseBodyClass = (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				pt = (ParameterizedType) type;
				if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
					responseBodyType = "collection";
					responseBodyClass = (Class<?>) pt.getActualTypeArguments()[0];
				}
			}
		}

		Fields responseFields = apiDocMethod.getAnnotation(Fields.class);
		responseBody = FieldObject.createList(responseBodyClass, responseFields, false);

		Object responseSample = ApiDocHelper.generateSample(apiDocInstance, apiDocMethod, responseFields);
		if (responseSample instanceof String) {
			responseBodySample = (String) responseSample;
		} else if (responseSample != null) {
			Class<?> view = null;
			if (responseSample instanceof DeferredResult)
				responseSample = ((DeferredResult<?>) responseSample).getResult();
			else if (responseSample instanceof CompletableFuture)
				responseSample = ((CompletableFuture<?>) responseSample).get();
			else if (responseSample instanceof Callable)
				responseSample = ((Callable<?>) responseSample).call();
			else if (responseSample instanceof Future)
				responseSample = ((Future<?>) responseSample).get();
			else if (responseSample instanceof ResponseEntity)
				responseSample = ((ResponseEntity<?>) responseSample).getBody();
			else if (responseSample instanceof MappingJacksonValue) {
				view = ((MappingJacksonValue) responseSample).getSerializationView();
				responseSample = ((MappingJacksonValue) responseSample).getValue();
			}
			JsonView jsonView = method.getAnnotation(JsonView.class);
			if (view == null && jsonView != null)
				view = jsonView.value()[0];
			if (view == null)
				responseBodySample = objectMapper.writeValueAsString(responseSample);
			else
				responseBodySample = objectMapper.writerWithView(view).writeValueAsString(responseSample);
		}

		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length > 0) {
			String[] parameterNames = ReflectionUtils.getParameterNames(method);
			Annotation[][] apiDocParameterAnnotations = apiDocMethod.getParameterAnnotations();
			Annotation[][] array = method.getParameterAnnotations();
			for (int i = 0; i < parameterTypes.length; i++) {
				Annotation[] apiDocAnnotations = apiDocParameterAnnotations[i];
				Annotation[] annotations = array[i];
				Field fd = null;
				Fields requestFields = null;
				for (int j = 0; j < apiDocAnnotations.length; j++) {
					Annotation anno = apiDocAnnotations[j];
					if (anno instanceof Field) {
						fd = (Field) anno;
					}
				}
				for (int j = 0; j < apiDocAnnotations.length; j++) {
					Annotation anno = apiDocAnnotations[j];
					if (anno instanceof Fields) {
						requestFields = (Fields) anno;
					}
				}
				for (int j = 0; j < annotations.length; j++) {
					Annotation anno = annotations[j];
					if (anno instanceof RequestBody) {
						requestBodyRequired = ((RequestBody) anno).required();
						Class<?> requestBodyClass = parameterTypes[i];
						if (Collection.class.isAssignableFrom(requestBodyClass)) {
							requestBodyType = "collection";
						} else if (ResultPage.class.isAssignableFrom(requestBodyClass)) {
							requestBodyType = "resultPage";
						}
						Type gtype = method.getGenericParameterTypes()[i];
						if (gtype instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) gtype;
							requestBodyClass = (Class<?>) pt.getActualTypeArguments()[0];
						}
						if (requestBodyClass.getGenericSuperclass() instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) requestBodyClass.getGenericSuperclass();
							requestBodyClass = (Class<?>) pt.getActualTypeArguments()[0];
						}
						requestBody = FieldObject.createList(requestBodyClass, requestFields, true);
						if (requestFields != null) {
							Object requestSample = ApiDocHelper.generateSample(apiDocInstance, null, requestFields);
							if (requestSample instanceof String) {
								requestBodySample = (String) requestSample;
							} else if (requestSample != null) {
								requestBodySample = objectMapper.writeValueAsString(requestSample);
							}
						}
					}
					if (anno instanceof PathVariable) {
						PathVariable ann = (PathVariable) anno;
						pathVariables.add(FieldObject.create(
								StringUtils.isNotBlank(ann.value()) ? ann.value() : parameterNames[i],
								parameterTypes[i], true, null, fd));
					}
					if (anno instanceof RequestParam) {
						RequestParam ann = (RequestParam) anno;
						if (!Map.class.isAssignableFrom(parameterTypes[i]))
							requestParams.add(FieldObject.create(
									StringUtils.isNotBlank(ann.value()) ? ann.value() : parameterNames[i],
									parameterTypes[i], ann.required(), ann.defaultValue(), fd));
					}
					if (anno instanceof RequestHeader) {
						RequestHeader ann = (RequestHeader) anno;
						if (!Map.class.isAssignableFrom(parameterTypes[i]))
							requestHeaders.add(FieldObject.create(
									StringUtils.isNotBlank(ann.value()) ? ann.value() : parameterNames[i],
									parameterTypes[i], ann.required(), ann.defaultValue(), fd));
					}
					if (anno instanceof CookieValue) {
						CookieValue ann = (CookieValue) anno;
						if (!Map.class.isAssignableFrom(parameterTypes[i]))
							cookieValues.add(FieldObject.create(
									StringUtils.isNotBlank(ann.value()) ? ann.value() : parameterNames[i],
									parameterTypes[i], ann.required(), ann.defaultValue(), fd));
					}
				}
			}

		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String[] getRequiredAuthorities() {
		return requiredAuthorities;
	}

	public void setRequiredAuthorities(String[] requiredAuthorities) {
		this.requiredAuthorities = requiredAuthorities;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String[] getMethods() {
		return methods;
	}

	public void setMethods(String[] methods) {
		this.methods = methods;
	}

	public List<FieldObject> getPathVariables() {
		return pathVariables;
	}

	public void setPathVariables(List<FieldObject> pathVariables) {
		this.pathVariables = pathVariables;
	}

	public List<FieldObject> getRequestParams() {
		return requestParams;
	}

	public void setRequestParams(List<FieldObject> requestParams) {
		this.requestParams = requestParams;
	}

	public List<FieldObject> getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(List<FieldObject> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public List<FieldObject> getCookieValues() {
		return cookieValues;
	}

	public void setCookieValues(List<FieldObject> cookieValues) {
		this.cookieValues = cookieValues;
	}

	public List<FieldObject> getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(List<FieldObject> requestBody) {
		this.requestBody = requestBody;
	}

	public List<FieldObject> getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(List<FieldObject> responseBody) {
		this.responseBody = responseBody;
	}

	public boolean isRequestBodyRequired() {
		return requestBodyRequired;
	}

	public void setRequestBodyRequired(boolean requestBodyRequired) {
		this.requestBodyRequired = requestBodyRequired;
	}

	public String getRequestBodyType() {
		return requestBodyType;
	}

	public void setRequestBodyType(String requestBodyType) {
		this.requestBodyType = requestBodyType;
	}

	public String getResponseBodyType() {
		return responseBodyType;
	}

	public void setResponseBodyType(String responseBodyType) {
		this.responseBodyType = responseBodyType;
	}

	public String getRequestBodySample() {
		return requestBodySample;
	}

	public void setRequestBodySample(String requestBodySample) {
		this.requestBodySample = requestBodySample;
	}

	public String getResponseBodySample() {
		return responseBodySample;
	}

	public void setResponseBodySample(String responseBodySample) {
		this.responseBodySample = responseBodySample;
	}

}

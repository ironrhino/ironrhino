package org.ironrhino.rest.doc;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.Field;
import org.ironrhino.rest.doc.annotation.Fields;
import org.springframework.http.converter.json.MappingJacksonValue;
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

	protected List<FieldObject> pathVariables = new ArrayList<FieldObject>();

	protected List<FieldObject> requestParams = new ArrayList<FieldObject>();

	protected List<FieldObject> requestHeaders = new ArrayList<FieldObject>();

	protected List<FieldObject> requestBody;

	protected List<FieldObject> responseBody;

	protected String requestBodyType;

	protected String responseBodyType;

	protected String requestBodySample;

	protected String responseBodySample;

	public ApiDoc(Class<?> apiDocClazz, Method apiDocMethod,
			ObjectMapper objectMapper) throws Exception {
		Class<?> clazz = apiDocClazz.getSuperclass();
		Method method = clazz.getMethod(apiDocMethod.getName(),
				apiDocMethod.getParameterTypes());

		Api api = apiDocMethod.getAnnotation(Api.class);
		this.name = api.value();
		this.description = api.description();

		Class<?> responseBodyClass = method.getReturnType();
		if (Collection.class.isAssignableFrom(responseBodyClass)) {
			responseBodyType = "collection";
		} else if (ResultPage.class.isAssignableFrom(responseBodyClass)) {
			responseBodyType = "resultPage";
		}
		if (method.getGenericReturnType() instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) method
					.getGenericReturnType();
			Type type = pt.getActualTypeArguments()[0];
			if (type instanceof Class) {
				responseBodyClass = (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				pt = (ParameterizedType) type;
				if (Collection.class.isAssignableFrom((Class<?>) pt
						.getRawType())) {
					responseBodyType = "collection";
					responseBodyClass = (Class<?>) pt.getActualTypeArguments()[0];
				}
			}
		}
		if (responseBodyClass.getGenericSuperclass() instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) responseBodyClass
					.getGenericSuperclass();
			Type type = pt.getActualTypeArguments()[0];
			if (type instanceof Class) {
				responseBodyClass = (Class<?>) type;
			} else if (type instanceof ParameterizedType) {
				pt = (ParameterizedType) type;
				if (Collection.class.isAssignableFrom((Class<?>) pt
						.getRawType())) {
					responseBodyType = "collection";
					responseBodyClass = (Class<?>) pt.getActualTypeArguments()[0];
				}
			}
		}
		Fields returnFields = apiDocMethod.getAnnotation(Fields.class);
		responseBody = FieldObject.createList(responseBodyClass, returnFields,
				false);
		Object instance = apiDocClazz.newInstance();
		if (returnFields != null
				&& StringUtils.isNotBlank(returnFields.sampleMethodName())) {
			try {
				Method m = apiDocClazz.getDeclaredMethod(
						returnFields.sampleMethodName(), new Class[0]);
				m.setAccessible(true);
				Object requestObject = m.invoke(instance, new Object[0]);
				if (requestObject instanceof String)
					responseBodySample = (String) requestObject;
				else
					responseBodySample = objectMapper
							.writeValueAsString(requestObject);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		} else {
			Class<?>[] argTypes = apiDocMethod.getParameterTypes();
			Object[] args = new Object[argTypes.length];
			for (int i = 0; i < argTypes.length; i++) {
				Class<?> type = argTypes[i];
				if (type.isPrimitive()) {
					if (Number.class.isAssignableFrom(type))
						args[i] = 0;
					else if (type == Boolean.TYPE)
						args[i] = false;
					else if (type == Byte.TYPE)
						args[i] = (byte) 0;
				} else {
					args[i] = null;
				}
			}
			Object sample = apiDocMethod.invoke(instance, args);
			Class<?> view = null;
			if (sample instanceof DeferredResult)
				sample = ((DeferredResult<?>) sample).getResult();
			else if (sample instanceof Callable)
				sample = ((Callable<?>) sample).call();
			else if (sample instanceof MappingJacksonValue) {
				view = ((MappingJacksonValue) sample).getSerializationView();
				sample = ((MappingJacksonValue) sample).getValue();
			}
			JsonView jsonView = method.getAnnotation(JsonView.class);
			if (view == null && jsonView != null)
				view = jsonView.value()[0];
			if (view == null)
				responseBodySample = objectMapper.writeValueAsString(sample);
			else
				responseBodySample = objectMapper.writerWithView(view)
						.writeValueAsString(sample);
		}

		Authorize authorize = method.getAnnotation(Authorize.class);
		if (authorize == null)
			authorize = clazz.getAnnotation(Authorize.class);
		if (authorize != null) {
			String ifAnyGranted = authorize.ifAnyGranted();
			if (StringUtils.isNotBlank(ifAnyGranted))
				requiredAuthorities = ifAnyGranted.split("\\s*,\\s*");
		}

		RequestMapping requestMapping = method
				.getAnnotation(RequestMapping.class);
		if (requestMapping != null) {
			String murl = "";
			String curl = "";
			String[] values = requestMapping.value();
			if (values.length > 0)
				murl = values[0];
			RequestMapping requestMappingWithClass = clazz
					.getAnnotation(RequestMapping.class);
			if (requestMappingWithClass != null) {
				values = requestMappingWithClass.value();
				if (values.length > 0)
					curl = values[0];
			}
			if (StringUtils.isNotBlank(murl) | StringUtils.isNoneBlank(curl))
				url = curl + murl;
			RequestMethod[] rms = requestMapping.method();
			if (rms.length > 0) {
				methods = new String[rms.length];
				for (int i = 0; i < rms.length; i++)
					methods[i] = rms[i].name();
			}
		}

		Class<?>[] types = method.getParameterTypes();
		if (types.length > 0) {
			String[] names = ReflectionUtils.getParameterNames(method);
			Annotation[][] apiDocArray = apiDocMethod.getParameterAnnotations();
			Annotation[][] array = method.getParameterAnnotations();
			for (int i = 0; i < types.length; i++) {
				Annotation[] apiDocAnnotations = apiDocArray[i];
				Annotation[] annotations = array[i];
				Field fd = null;
				Fields fds = null;
				for (int j = 0; j < apiDocAnnotations.length; j++) {
					Annotation anno = apiDocAnnotations[j];
					if (anno instanceof Field) {
						fd = (Field) anno;
					}
				}
				for (int j = 0; j < apiDocAnnotations.length; j++) {
					Annotation anno = apiDocAnnotations[j];
					if (anno instanceof Fields) {
						fds = (Fields) anno;
					}
				}
				for (int j = 0; j < annotations.length; j++) {
					Annotation anno = annotations[j];
					if (anno instanceof RequestBody) {
						Class<?> requestBodyClass = types[i];
						if (Collection.class.isAssignableFrom(requestBodyClass)) {
							requestBodyType = "collection";
						} else if (ResultPage.class
								.isAssignableFrom(requestBodyClass)) {
							requestBodyType = "resultPage";
						}
						Type gtype = method.getGenericParameterTypes()[i];
						if (gtype instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) gtype;
							requestBodyClass = (Class<?>) pt
									.getActualTypeArguments()[0];
						}
						if (requestBodyClass.getGenericSuperclass() instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) requestBodyClass
									.getGenericSuperclass();
							requestBodyClass = (Class<?>) pt
									.getActualTypeArguments()[0];
						}
						requestBody = FieldObject.createList(requestBodyClass,
								fds, true);
						if (fds != null) {
							if (StringUtils.isNotBlank(fds.sampleMethodName()))
								try {
									Method m = apiDocClazz.getDeclaredMethod(
											fds.sampleMethodName(),
											new Class[0]);
									m.setAccessible(true);
									Object requestObject = m.invoke(instance,
											new Object[0]);
									if (requestObject instanceof String)
										requestBodySample = (String) requestObject;
									else
										requestBodySample = objectMapper
												.writeValueAsString(requestObject);
								} catch (NoSuchMethodException e) {
									e.printStackTrace();
								}
						}
					}
					if (anno instanceof PathVariable) {
						PathVariable ann = (PathVariable) anno;
						pathVariables.add(FieldObject.create(StringUtils
								.isNotBlank(ann.value()) ? ann.value()
								: names[i], types[i], true, null, fd));
					}
					if (anno instanceof RequestParam) {
						RequestParam ann = (RequestParam) anno;
						requestParams.add(FieldObject.create(StringUtils
								.isNotBlank(ann.value()) ? ann.value()
								: names[i], types[i], ann.required(), ann
								.defaultValue(), fd));
					}
					if (anno instanceof RequestHeader) {
						RequestHeader ann = (RequestHeader) anno;
						requestHeaders.add(FieldObject.create(StringUtils
								.isNotBlank(ann.value()) ? ann.value()
								: names[i], types[i], ann.required(), ann
								.defaultValue(), fd));
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

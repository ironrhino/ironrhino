package org.ironrhino.core.remoting.playground;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.impl.AbstractServiceRegistry;
import org.ironrhino.core.spring.configuration.StageConditional;
import org.ironrhino.core.spring.converter.CustomConversionService;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.SampleObjectCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
@StageConditional(Stage.DEVELOPMENT)
public class ServicePlayground {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private ServiceRegistry serviceRegistry;

	private Map<String, Object> services = new TreeMap<>();

	private Map<String, Collection<MethodInfo>> methods = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper = JsonSerializationUtils.createNewObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.setDateFormat(new SimpleDateFormat(DateUtils.DATETIME));

	public Collection<String> getServices() {
		if (services.isEmpty()) {
			synchronized (services) {
				List<String> names = new ArrayList<>();
				names.addAll(serviceRegistry.getExportedServices().keySet());
				if (serviceRegistry instanceof AbstractServiceRegistry)
					names.addAll(((AbstractServiceRegistry) serviceRegistry).getImportedServiceCandidates().keySet());
				for (String name : names) {
					try {
						services.put(name, ctx.getBean(Class.forName(name)));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return services.keySet();
	}

	public Collection<MethodInfo> getMethods(String service) {
		if (!getServices().contains(service))
			return Collections.emptyList();
		return methods.computeIfAbsent(service, key -> {
			List<MethodInfo> list = new ArrayList<>();
			try {
				Class<?> clazz = Class.forName(service);
				for (Method m : clazz.getMethods()) {
					MethodInfo mi = new MethodInfo();
					mi.setMethod(m);
					mi.setName(m.getName());
					mi.setReturnType(m.getGenericReturnType());
					String[] parameterNames = ReflectionUtils.getParameterNames(m);
					Type[] parameterTypes = m.getGenericParameterTypes();
					ParameterInfo[] parameters = new ParameterInfo[parameterNames.length];
					for (int i = 0; i < parameterNames.length; i++) {
						ParameterInfo pi = new ParameterInfo();
						pi.setName(parameterNames[i]);
						Type type = parameterTypes[i];
						pi.setType(type);
						try {
							Object sampleObject = SampleObjectCreator.getDefaultInstance().createSample(type);
							String sample = objectMapper.writeValueAsString(sampleObject);
							pi.setSample(sample);
						} catch (Throwable t) {
							t.printStackTrace();
						}
						parameters[i] = pi;
					}
					mi.setParameters(parameters);
					list.add(mi);
				}
			} catch (ClassNotFoundException e) {
				return Collections.emptyList();
			}
			list.sort(Comparator.comparing(MethodInfo::getName));
			return list;
		});
	}

	public String invoke(String service, String method, Map<String, String> params) throws Exception {
		Collection<MethodInfo> methods = getMethods(service);
		if (methods.isEmpty())
			throw new IllegalArgumentException("Unknown service: " + service);
		MethodInfo mi = methods.stream().filter(m -> m.getSignature().equals(method)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown method: " + method));
		Object result = mi.getMethod().invoke(services.get(service), convert(mi.getParameters(), params));
		if (result instanceof Optional)
			result = ((Optional<?>) result).orElse(null);
		else if (result instanceof Callable)
			result = ((Callable<?>) result).call();
		if (result instanceof Future)
			result = ((Future<?>) result).get();
		return mi.getMethod().getReturnType() != void.class ? objectMapper.writeValueAsString(result) : "";
	}

	protected Object[] convert(ParameterInfo[] parameters, Map<String, String> params) throws Exception {
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			String value = params.get(parameters[i].getName());
			if (value != null) {
				args[i] = convert(parameters[i].getType(), value);
			}
		}
		return args;
	}

	protected Object convert(Type type, String value) throws Exception {
		JavaType jt = objectMapper.constructType(type);
		try {
			return objectMapper.readValue(value, jt);
		} catch (JsonProcessingException e) {
			if (type == String.class)
				return value;
			else if (type instanceof Class<?>) {
				Class<?> clz = (Class<?>) type;
				if (Serializable.class.isAssignableFrom(clz))
					return CustomConversionService.getSharedInstance().convert(value, clz);
			}
			throw e;
		}
	}

}

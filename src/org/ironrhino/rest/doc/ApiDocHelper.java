package org.ironrhino.rest.doc;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ClassScanner;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.ironrhino.rest.doc.annotation.Fields;

import com.fasterxml.jackson.databind.ObjectMapper;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

public class ApiDocHelper {

	private static ClassPool classPool = ClassPool.getDefault();

	public static ObjectMapper objectMapper = JsonUtils.createNewObjectMapper();

	public static List<Method> findApiMethods(Class<?> apiDocClass) throws Exception {
		List<Method> methods = new ArrayList<>();
		for (Method m : apiDocClass.getMethods()) {
			if (m.getAnnotation(Api.class) == null)
				continue;
			methods.add(m);
		}
		classPool.insertClassPath(new ClassClassPath(apiDocClass));
		final CtClass cc = classPool.get(apiDocClass.getName());
		Collections.sort(methods, (o1, o2) -> {
			int line1 = 0;
			int line2 = 1;
			try {
				Class<?>[] types = o1.getParameterTypes();
				CtClass[] paramTypes = new CtClass[types.length];
				for (int i = 0; i < types.length; i++) {
					classPool.insertClassPath(new ClassClassPath(types[i]));
					paramTypes[i] = classPool.get(types[i].getName());
				}
				classPool.insertClassPath(new ClassClassPath(o1.getReturnType()));
				CtMethod method1 = cc.getMethod(o1.getName(),
						Descriptor.ofMethod(classPool.get(o1.getReturnType().getName()), paramTypes));
				line1 = method1.getMethodInfo().getLineNumber(0);
				types = o2.getParameterTypes();
				paramTypes = new CtClass[types.length];
				for (int i = 0; i < types.length; i++) {
					classPool.insertClassPath(new ClassClassPath(types[i]));
					paramTypes[i] = classPool.get(types[i].getName());
				}
				classPool.insertClassPath(new ClassClassPath(o2.getReturnType()));
				CtMethod method2 = cc.getMethod(o2.getName(),
						Descriptor.ofMethod(classPool.get(o2.getReturnType().getName()), paramTypes));
				line2 = method2.getMethodInfo().getLineNumber(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return line1 - line2;
		});
		return methods;
	}

	public static Map<String, List<ApiModuleObject>> getApiModules() {
		return getApiModules(ClassScanner.getAppPackages());
	}

	public static Map<String, List<ApiModuleObject>> getApiModules(String[] basePackages) {
		Map<String, List<ApiModuleObject>> map = new LinkedHashMap<>();
		Collection<Class<?>> classes = ClassScanner.scanAnnotated(basePackages, ApiModule.class);
		for (Class<?> clazz : classes) {
			ApiModule apiModule = clazz.getAnnotation(ApiModule.class);
			String category = apiModule.category().trim();
			List<ApiModuleObject> list = map.get(category);
			if (list == null) {
				list = new ArrayList<>();
				map.put(category, list);
			}
			String name = apiModule.value().trim();
			String description = apiModule.description();
			ApiModuleObject apiModuleObject = null;
			for (ApiModuleObject amo : list) {
				if (amo.getName().equals(name)) {
					apiModuleObject = amo;
					break;
				}
			}
			if (apiModuleObject == null) {
				apiModuleObject = new ApiModuleObject();
				apiModuleObject.setName(name);
				apiModuleObject.setDescription(description);
				list.add(apiModuleObject);
			}
			try {
				List<Method> methods = findApiMethods(clazz);
				for (Method m : methods)
					apiModuleObject.getApiDocs().add(new ApiDoc(clazz, m, objectMapper));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return map;
	}

	public static Object generateSample(Object apiDocInstance, Method apiDocMethod, Fields fields) throws Exception {
		if (fields != null) {
			if (StringUtils.isNotBlank(fields.sample()))
				return fields.sample();
			String sampleFileName = fields.sampleFileName();
			if (StringUtils.isNotBlank(sampleFileName)) {
				try (InputStream is = apiDocInstance.getClass().getResourceAsStream(sampleFileName)) {
					if (is == null) {
						throw new ErrorMessage(
								sampleFileName + " with " + apiDocInstance.getClass().getName() + " is not found!");
					}
					return StringUtils.join(IOUtils.readLines(is, StandardCharsets.UTF_8), "\n");
				}
			}
			String sampleMethodName = fields.sampleMethodName();
			if (StringUtils.isNotBlank(sampleMethodName)) {
				Method m = apiDocInstance.getClass().getDeclaredMethod(sampleMethodName, new Class[0]);
				m.setAccessible(true);
				return m.invoke(apiDocInstance, new Object[0]);
			}
		}
		if (apiDocMethod != null) {
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
			return apiDocMethod.invoke(apiDocInstance, args);
		}
		return null;

	}

}

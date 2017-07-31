package org.ironrhino.rest.doc;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ClassScanner;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.ironrhino.rest.doc.annotation.Fields;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.async.DeferredResult;

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
		Collections.sort(methods, (m1, m2) -> {
			int line1 = 0;
			int line2 = 1;
			try {
				Class<?>[] types = m1.getParameterTypes();
				CtClass[] paramTypes = new CtClass[types.length];
				for (int i = 0; i < types.length; i++) {
					classPool.insertClassPath(new ClassClassPath(types[i]));
					paramTypes[i] = classPool.get(types[i].getName());
				}
				classPool.insertClassPath(new ClassClassPath(m1.getReturnType()));
				CtMethod method1 = cc.getMethod(m1.getName(),
						Descriptor.ofMethod(classPool.get(m1.getReturnType().getName()), paramTypes));
				line1 = method1.getMethodInfo().getLineNumber(0);
				types = m2.getParameterTypes();
				paramTypes = new CtClass[types.length];
				for (int i = 0; i < types.length; i++) {
					classPool.insertClassPath(new ClassClassPath(types[i]));
					paramTypes[i] = classPool.get(types[i].getName());
				}
				classPool.insertClassPath(new ClassClassPath(m2.getReturnType()));
				CtMethod method2 = cc.getMethod(m2.getName(),
						Descriptor.ofMethod(classPool.get(m2.getReturnType().getName()), paramTypes));
				line2 = method2.getMethodInfo().getLineNumber(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return line1 - line2;
		});
		Collections.sort(methods, (m1, m2) -> {
			Order o1 = m1.getAnnotation(Order.class);
			int order1 = o1 != null ? o1.value() : methods.indexOf(m1) + 1;
			Order o2 = m2.getAnnotation(Order.class);
			int order2 = o2 != null ? o2.value() : methods.indexOf(m2) + 1;
			return order1 - order2;
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
			Object obj;
			try {
				obj = apiDocMethod.invoke(apiDocInstance, args);
			} catch (InvocationTargetException e) {
				obj = null;
			}
			if (obj == null) {
				obj = createSample(apiDocMethod.getGenericReturnType());
			}
			return obj;
		}
		return null;

	}

	public static Object createSample(Type returnType) {
		if (returnType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) returnType;
			if (!(pt.getRawType() instanceof Class) || pt.getActualTypeArguments().length != 1
					|| !(pt.getActualTypeArguments()[0] instanceof Class))
				return null;
			Class<?> raw = (Class<?>) pt.getRawType();
			Class<?> clazz = (Class<?>) pt.getActualTypeArguments()[0];
			if (DeferredResult.class.isAssignableFrom(raw) || Future.class.isAssignableFrom(raw)
					|| Callable.class.isAssignableFrom(raw) || ResponseEntity.class.isAssignableFrom(raw)) {
				return createSample(clazz);
			} else if (Set.class.isAssignableFrom(raw)) {
				Set<Object> set = new HashSet<>();
				set.add(createSample(clazz));
				return set;
			} else if (Collection.class.isAssignableFrom(raw)) {
				List<Object> list = new ArrayList<>();
				list.add(createSample(clazz));
				return list;
			}
			return null;
		} else if (returnType instanceof Class) {
			return createSample((Class<?>) returnType);
		} else {
			return null;
		}
	}

	private static Object createSample(Class<?> clazz) {
		if (clazz.isArray()) {
			return new Object[] { createObject(clazz.getComponentType()) };
		} else {
			return createObject(clazz);
		}
	}

	private static Object createObject(Class<?> clazz) {
		Object object = createValue(clazz, null, null);
		if (object != null)
			return object;
		try {
			final Object obj = BeanUtils.instantiateClass(clazz);
			ReflectionUtils.doWithFields(obj.getClass(), field -> {
				ReflectionUtils.makeAccessible(field);
				Object value;
				Type type = field.getGenericType();
				if (type instanceof Class) {
					if (!field.getType().isPrimitive() && field.get(obj) != null) {
						return;
					}
					value = createValue(field.getType(), field.getName(), clazz);
					if (value == null)
						value = createObject(field.getType());
				} else if (type instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) type;
					if (!(pt.getRawType() instanceof Class) || pt.getActualTypeArguments().length != 1
							|| !(pt.getActualTypeArguments()[0] instanceof Class))
						return;
					Class<?> raw = (Class<?>) pt.getRawType();
					Class<?> clazz2 = (Class<?>) pt.getActualTypeArguments()[0];
					if (Set.class.isAssignableFrom(raw)) {
						Set<Object> set = new HashSet<>();
						set.add(createSample(clazz2));
						value = set;
					} else if (Collection.class.isAssignableFrom(raw)) {
						List<Object> list = new ArrayList<>();
						list.add(createSample(clazz2));
						value = list;
					} else {
						return;
					}
				} else {
					return;
				}
				field.set(obj, value);
			}, field -> {
				if (field.getType() == clazz)
					return false;
				int mod = field.getModifiers();
				return !(Modifier.isFinal(mod) || Modifier.isStatic(mod));
			});
			return obj;
		} catch (Exception e) {
			return null;
		}
	}

	private static Object createValue(Class<?> type, String fieldName, Class<?> sampleClass) {
		if (String.class == type)
			return suggestStringValue(fieldName, sampleClass);
		if ((Boolean.TYPE == type) || (Boolean.class == type))
			return true;
		if ((Byte.TYPE == type) || (Byte.class == type))
			return 0;
		if ((Short.TYPE == type) || (Short.class == type))
			return 10;
		if ((Integer.TYPE == type) || (Integer.class == type))
			return 100;
		if ((Long.TYPE == type) || (Long.class == type))
			return 1000;
		if ((Float.TYPE == type) || (Float.class == type))
			return 9.9f;
		if ((Double.TYPE == type) || (Double.class == type) || (Number.class == type))
			return 12.12d;
		if (BigDecimal.class == type)
			return new BigDecimal("12.12");
		if (Date.class.isAssignableFrom(type))
			return new Date();
		if (type.isEnum()) {
			try {
				return ((Object[]) type.getMethod("values").invoke(null))[0];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (type == RestStatus.class)
			return RestStatus.OK;
		return null;
	}

	private static String suggestStringValue(String fieldName, Class<?> sampleClass) {
		if (fieldName == null)
			return "test";
		if (fieldName.toLowerCase().equals("id"))
			return CodecUtils.encodeBase62(CodecUtils.md5Hex(sampleClass.getName()));
		if (fieldName.toLowerCase().endsWith("email"))
			return "test@test.com";
		if (fieldName.toLowerCase().endsWith("username"))
			return "admin";
		if (fieldName.toLowerCase().endsWith("password"))
			return "********";
		if (fieldName.toLowerCase().endsWith("phone") || fieldName.toLowerCase().endsWith("mobile"))
			return "13888888888";
		return fieldName.toUpperCase();
	}

}

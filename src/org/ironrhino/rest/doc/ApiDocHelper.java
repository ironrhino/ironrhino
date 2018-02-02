package org.ironrhino.rest.doc;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.rest.RestStatus;
import org.ironrhino.rest.doc.annotation.Fields;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.async.DeferredResult;

public class ApiDocHelper {

	public static Object generateSample(Class<?> apiDocClazz, Method apiDocMethod, Fields fields) throws Exception {
		if (fields != null) {
			if (StringUtils.isNotBlank(fields.sample()))
				return fields.sample();
			String sampleFileName = fields.sampleFileName();
			if (StringUtils.isNotBlank(sampleFileName)) {
				try (InputStream is = apiDocClazz.getResourceAsStream(sampleFileName)) {
					if (is == null) {
						throw new ErrorMessage(sampleFileName + " with " + apiDocClazz.getName() + " is not found!");
					}
					return String.join("\n", IOUtils.readLines(is, StandardCharsets.UTF_8));
				}
			}
			String sampleMethodName = fields.sampleMethodName();
			if (StringUtils.isNotBlank(sampleMethodName)) {
				Method m = apiDocClazz.getDeclaredMethod(sampleMethodName, new Class[0]);
				m.setAccessible(true);
				return m.invoke(apiDocClazz.getConstructor().newInstance(), new Object[0]);
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
					args[i] = createSample(type);
				}
			}
			Object obj;
			try {
				obj = apiDocMethod.invoke(apiDocClazz.getConstructor().newInstance(), args);
			} catch (InvocationTargetException | IllegalArgumentException | NoSuchMethodException e) {
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
			Class<?> cls = clazz.getComponentType();
			Object array = Array.newInstance(cls, 1);
			Array.set(array, 0, createObject(cls));
			return array;
		} else {
			return createObject(clazz);
		}
	}

	private static Object createObject(Class<?> clazz) {
		return createObject(clazz, new HashSet<>());
	}

	private static Object createObject(Class<?> clazz, Set<Class<?>> references) {
		Object object = createValue(clazz, null, null);
		if (object != null)
			return object;
		try {
			final Object obj = BeanUtils.instantiateClass(clazz);
			references.add(clazz);
			ReflectionUtils.doWithFields(obj.getClass(), field -> {
				ReflectionUtils.makeAccessible(field);
				Object value;
				Type type = field.getGenericType();
				if (type instanceof Class) {
					if (!field.getType().isPrimitive() && field.get(obj) != null) {
						return;
					}
					if (type == clazz) {
						value = obj;
					} else {
						value = createValue(field.getType(), field.getName(), clazz);
						if (value == null) {
							if (!references.contains(clazz)) {
								value = createObject(field.getType(), references);
							} else {
								return;
							}
						}
					}
				} else if (type instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) type;
					if (!(pt.getRawType() instanceof Class) || pt.getActualTypeArguments().length != 1
							|| !(pt.getActualTypeArguments()[0] instanceof Class))
						return;
					Class<?> raw = (Class<?>) pt.getRawType();
					Class<?> clazz2 = (Class<?>) pt.getActualTypeArguments()[0];
					if (Set.class.isAssignableFrom(raw)) {
						Set<Object> set = new HashSet<>();
						set.add((clazz2 == clazz) ? obj : createSample(clazz2));
						value = set;
					} else if (Collection.class.isAssignableFrom(raw)) {
						List<Object> list = new ArrayList<>();
						list.add((clazz2 == clazz) ? obj : createSample(clazz2));
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
		else if (type == LocalDate.class)
			return LocalDate.now();
		else if (type == LocalDateTime.class)
			return LocalDateTime.now();
		else if (type == LocalTime.class)
			return LocalTime.now();
		else if (type == Duration.class)
			return Duration.ofSeconds(1);
		if (type.isEnum()) {
			try {
				return ((Object[]) type.getMethod("values").invoke(null))[0];
			} catch (Throwable e) {
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
		if (fieldName.toLowerCase().endsWith("code"))
			return "123456";
		return fieldName.toUpperCase();
	}

}

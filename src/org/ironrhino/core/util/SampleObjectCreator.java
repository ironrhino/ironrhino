package org.ironrhino.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import org.ironrhino.core.model.ResultPage;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.async.DeferredResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SampleObjectCreator {

	private static final SampleObjectCreator defaultInstance = new SampleObjectCreator();

	private final BiFunction<Class<?>, String, ?> suggestionFunction;

	public static SampleObjectCreator getDefaultInstance() {
		return defaultInstance;
	}

	public <T> SampleObjectCreator() {
		this(null);
	}

	public <T> SampleObjectCreator(BiFunction<Class<?>, String, ?> suggestionFunction) {
		this.suggestionFunction = suggestionFunction;
	}

	public Object createSample(Type returnType) {
		if (returnType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) returnType;
			if (!(pt.getRawType() instanceof Class) || pt.getActualTypeArguments().length != 1
					|| !(pt.getActualTypeArguments()[0] instanceof Class))
				return null;
			Class<?> raw = (Class<?>) pt.getRawType();
			Class<?> clazz = (Class<?>) pt.getActualTypeArguments()[0];
			if (Flux.class.isAssignableFrom(raw)) {
				return Flux.just(createSample(clazz));
			} else if (Mono.class.isAssignableFrom(raw)) {
				return Mono.just(createSample(clazz));
			} else if (DeferredResult.class.isAssignableFrom(raw) || Future.class.isAssignableFrom(raw)
					|| Callable.class.isAssignableFrom(raw) || ResponseEntity.class.isAssignableFrom(raw)) {
				return createSample(clazz);
			} else if (Set.class.isAssignableFrom(raw)) {
				Set<Object> set = new HashSet<>();
				set.add(createSample(clazz));
				return set;
			} else if (Iterable.class.isAssignableFrom(raw)) {
				List<Object> list = new ArrayList<>();
				list.add(createSample(clazz));
				return list;
			} else if (ResultPage.class.isAssignableFrom(raw)) {
				ResultPage<Object> page = new ResultPage<>();
				page.setTotalResults(1);
				List<Object> list = new ArrayList<>();
				list.add(createSample(clazz));
				page.setResult(list);
				return page;
			}
			return null;
		} else if (returnType instanceof Class) {
			return createSample((Class<?>) returnType);
		} else {
			return null;
		}
	}

	private Object createSample(Class<?> clazz) {
		if (clazz.isArray()) {
			Class<?> cls = clazz.getComponentType();
			Object array = Array.newInstance(cls, 1);
			Array.set(array, 0, createObject(cls));
			return array;
		} else {
			return createObject(clazz);
		}
	}

	private Object createObject(Class<?> clazz) {
		return createObject(clazz, new HashSet<>());
	}

	private Object createObject(Class<?> clazz, Set<Class<?>> references) {
		Object object = createValue(clazz, null, null);
		if (object != null)
			return object;
		if (clazz.isInterface()) {
			object = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, (proxy, method, args) -> {
				Type returnType = method.getGenericReturnType();
				if (returnType == void.class)
					return null;
				if (returnType == clazz)
					return null;
				if (returnType instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) returnType;
					for (Type t : pt.getActualTypeArguments())
						if (t == clazz)
							return null;
				}
				return createSample(returnType);
			});
			return object;
		}
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
						if (!references.contains(clazz2))
							set.add((clazz2 == clazz) ? obj : createObject(clazz2, references));
						value = set;
					} else if (Iterable.class.isAssignableFrom(raw)) {
						List<Object> list = new ArrayList<>();
						if (!references.contains(clazz2))
							list.add((clazz2 == clazz) ? obj : createObject(clazz2, references));
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
			System.err.println("SampleObjectCreator can not instantiate :" + clazz);
			return null;
		}
	}

	private Object createValue(Class<?> type, String fieldName, Class<?> sampleClass) {
		if (suggestionFunction != null) {
			Object value = suggestionFunction.apply(type, fieldName);
			if (value != null)
				return value;
		}
		if (GrantedAuthority.class == type)
			return new SimpleGrantedAuthority("role");
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
		else if (type == YearMonth.class)
			return YearMonth.now();
		else if (type == Duration.class)
			return Duration.ofSeconds(1);
		if (type.isEnum()) {
			try {
				return ((Object[]) type.getMethod("values").invoke(null))[0];
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String suggestStringValue(String fieldName, Class<?> sampleClass) {
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

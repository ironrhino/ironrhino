package org.ironrhino.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import org.ironrhino.common.model.Coordinate;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.validation.constraints.CitizenIdentificationNumber;
import org.ironrhino.core.validation.constraints.MobilePhoneNumber;
import org.ironrhino.core.validation.constraints.OrganizationCode;
import org.ironrhino.core.validation.constraints.SocialCreditIdentifier;
import org.ironrhino.core.validation.validators.CitizenIdentificationNumberValidator;
import org.ironrhino.core.validation.validators.MobilePhoneNumberValidator;
import org.ironrhino.core.validation.validators.OrganizationCodeValidator;
import org.ironrhino.core.validation.validators.SocialCreditIdentifierValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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

	private final Random random;

	public static SampleObjectCreator getDefaultInstance() {
		return defaultInstance;
	}

	public <T> SampleObjectCreator() {
		this(false);
	}

	public <T> SampleObjectCreator(BiFunction<Class<?>, String, ?> suggestionFunction) {
		this.suggestionFunction = suggestionFunction;
		this.random = null;

	}

	public <T> SampleObjectCreator(boolean random) {
		this.random = random ? new Random() : null;
		this.suggestionFunction = null;
	}

	public Object createSample(Type returnType) {
		if (returnType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) returnType;
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			if (!(pt.getRawType() instanceof Class)
					|| (actualTypeArguments.length != 1 && actualTypeArguments.length != 2)
					|| !(actualTypeArguments[0] instanceof Class))
				return null;
			Class<?> raw = (Class<?>) pt.getRawType();
			Class<?> clazz = (Class<?>) actualTypeArguments[0];
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
			} else if (Map.class.isAssignableFrom(raw)) {
				Map<Object, Object> map = new HashMap<>();
				map.put(createSample(clazz), createSample((Class<?>) actualTypeArguments[1]));
				return map;
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
				returnType = GenericTypeResolver.resolveType(returnType, clazz);
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
		Object o = null;
		try {
			o = BeanUtils.instantiateClass(clazz);
		} catch (Exception e) {
			Constructor<?> c = clazz.getConstructors()[0];
			Type[] types = c.getGenericParameterTypes();
			for (int i = 0; i < types.length; i++) {
				types[i] = GenericTypeResolver.resolveType(types[i], clazz);
			}
			if (types.length > 0) {
				Object[] arr = new Object[types.length];
				for (int i = 0; i < types.length; i++)
					arr[i] = createSample(types[i]);
				try {
					o = c.newInstance(arr);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
		if (o == null) {
			System.err.println("SampleObjectCreator can not instantiate :" + clazz);
			return null;
		}
		Object obj = o;
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
					value = createValue(field, clazz);
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

	}

	private Object createValue(Field field, Class<?> sampleClass) {
		if (random != null) {
			if (field.isAnnotationPresent(MobilePhoneNumber.class))
				return MobilePhoneNumberValidator.randomValue();
			if (field.isAnnotationPresent(CitizenIdentificationNumber.class))
				return CitizenIdentificationNumberValidator.randomValue();
			if (field.isAnnotationPresent(SocialCreditIdentifier.class))
				return SocialCreditIdentifierValidator.randomValue();
			if (field.isAnnotationPresent(OrganizationCode.class))
				return OrganizationCodeValidator.randomValue();
		}
		return createValue(field.getType(), field.getName(), sampleClass);
	}

	private Object createValue(Class<?> type, String fieldName, Class<?> sampleClass) {
		if (suggestionFunction != null) {
			Object value = suggestionFunction.apply(type, fieldName);
			if (value != null)
				return value;
		}
		if (GrantedAuthority.class == type)
			return new SimpleGrantedAuthority("role" + (random != null ? random.nextInt(1000000) : ""));
		if (Coordinate.class == type) {
			if (random != null)
				return new Coordinate(random.nextInt(89) + random.nextDouble(),
						random.nextInt(179) + random.nextDouble());
			else
				return new Coordinate("26.7011948,113.5207633");
		}
		if (InputStream.class == type) {
			if (random != null) {
				byte[] bytes = new byte[random.nextInt(256)];
				random.nextBytes(bytes);
				return new ByteArrayInputStream(bytes);
			} else {
				return new ByteArrayInputStream(new byte[0]);
			}
		}
		if (OutputStream.class == type)
			return new ByteArrayOutputStream();
		if (Resource.class == type)
			return new InputStreamResource((InputStream) createValue(InputStream.class, fieldName, sampleClass));
		if (String.class == type)
			return suggestStringValue(fieldName, sampleClass);
		if ((Boolean.TYPE == type) || (Boolean.class == type))
			return random != null ? random.nextBoolean() : true;
		if ((Byte.TYPE == type) || (Byte.class == type))
			return (byte) (random != null ? random.nextInt(256) : 0);
		if ((Short.TYPE == type) || (Short.class == type))
			return (short) (random != null ? random.nextInt(32768) : 10);
		if ((Integer.TYPE == type) || (Integer.class == type))
			return random != null ? random.nextInt() : 1100;
		if ((Long.TYPE == type) || (Long.class == type))
			return random != null ? random.nextLong() : 1000L;
		if ((Float.TYPE == type) || (Float.class == type))
			return random != null ? (float) (random.nextDouble() * random.nextInt(100)) : 9.9f;
		if ((Double.TYPE == type) || (Double.class == type) || (Number.class == type))
			return random != null ? (random.nextDouble() * random.nextInt(1000)) : 12.12d;
		if (BigDecimal.class == type)
			return new BigDecimal(
					Double.toString(random != null ? (random.nextDouble() * random.nextInt(1000)) : 12.12d));
		if (Date.class.isAssignableFrom(type)) {
			Date date = new Date();
			if (random != null)
				date = new Date(date.getTime() - random.nextInt(100000));
			return date;
		} else if (type == LocalDate.class) {
			LocalDate date = LocalDate.of(2018, 4, 25);
			if (random != null)
				date = date.minus(random.nextInt(100), ChronoUnit.DAYS);
			return date;
		} else if (type == LocalDateTime.class) {
			LocalDateTime datetime = LocalDateTime.of(2018, 4, 25, 15, 30, 0);
			if (random != null)
				datetime = datetime.minus(random.nextInt(100000), ChronoUnit.MILLIS);
			return datetime;
		} else if (type == LocalTime.class) {
			LocalTime time = LocalTime.of(15, 30);
			if (random != null)
				time = time.minus(random.nextInt(100000), ChronoUnit.MILLIS);
			return time;
		} else if (type == YearMonth.class) {
			return random != null ? YearMonth.of(1900 + random.nextInt(120), 1 + random.nextInt(12))
					: YearMonth.of(2018, 4);
		} else if (type == Duration.class)
			return Duration.ofSeconds(random != null ? random.nextInt(100000) : 1);
		if (type.isEnum()) {
			try {
				Object[] arr = (Object[]) type.getMethod("values").invoke(null);
				return arr[random != null ? random.nextInt(arr.length) : 0];
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private String suggestStringValue(String fieldName, Class<?> sampleClass) {
		if (fieldName == null)
			return "test" + (random != null ? random.nextInt(1000000) : "");
		if (fieldName.toLowerCase(Locale.ROOT).equals("id"))
			return random != null ? CodecUtils.nextId(22) : "4zIybvEyycOxwoKLWLStsG";
		if (fieldName.toLowerCase(Locale.ROOT).endsWith("email"))
			return "test " + (random != null ? random.nextInt(1000000) : "") + "@test.com";
		if (fieldName.toLowerCase(Locale.ROOT).endsWith("username"))
			return "admin" + (random != null ? random.nextInt(1000000) : "");
		if (fieldName.toLowerCase(Locale.ROOT).endsWith("password"))
			return random != null ? CodecUtils.nextId(48) : "********";
		if (fieldName.toLowerCase(Locale.ROOT).endsWith("phone")
				|| fieldName.toLowerCase(Locale.ROOT).endsWith("mobile"))
			return random != null ? MobilePhoneNumberValidator.randomValue() : "13888888888";
		if (fieldName.toLowerCase(Locale.ROOT).endsWith("code"))
			return random != null ? String.valueOf(random.nextInt(1000000000)) : "123456";
		return fieldName.toUpperCase(Locale.ROOT) + (random != null ? random.nextInt(1000000) : "");
	}

}

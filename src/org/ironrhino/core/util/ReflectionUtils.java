package org.ironrhino.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.metadata.Param;
import org.springframework.aop.framework.Advised;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

public class ReflectionUtils {

	public static ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

	public static Class<?> getGenericClass(Class<?> clazz) {
		return getGenericClass(clazz, 0);
	}

	public static Class<?> getGenericClass(Class<?> clazz, int index) {
		Type genType = clazz.getGenericSuperclass();
		if (genType instanceof ParameterizedType) {
			ParameterizedType pramType = (ParameterizedType) genType;
			Type[] params = pramType.getActualTypeArguments();
			if ((params != null) && (params.length > index))
				return params[index] instanceof Class ? (Class<?>) params[index]
						: null;
		}
		return null;
	}

	public static String[] getParameterNames(Constructor<?> ctor) {
		return getParameterNames(null, ctor);
	}

	public static String[] getParameterNames(Method method) {
		return getParameterNames(method, null);
	}

	private static String[] getParameterNames(Method method, Constructor<?> ctor) {
		Annotation[][] annotations = method != null ? method
				.getParameterAnnotations() : ctor.getParameterAnnotations();
		String[] names = new String[annotations.length];
		boolean allbind = true;
		loop: for (int i = 0; i < annotations.length; i++) {
			Annotation[] arr = annotations[i];
			for (Annotation a : arr) {
				if (a instanceof Param) {
					String s = ((Param) a).value();
					if (StringUtils.isNotBlank(s)) {
						names[i] = s;
						continue loop;
					}
				}
			}
			allbind = false;
		}
		if (!allbind) {
			String[] namesDiscovered = method != null ? parameterNameDiscoverer
					.getParameterNames(method) : parameterNameDiscoverer
					.getParameterNames(ctor);
			if (namesDiscovered == null)
				return null;
			for (int i = 0; i < names.length; i++)
				if (names[i] == null)
					names[i] = namesDiscovered[i];
		}
		return names;
	}

	public static String[] getParameterNames(JoinPoint jp) {
		if (!jp.getKind().equals(JoinPoint.METHOD_EXECUTION))
			return null;
		Class<?> clz = jp.getTarget().getClass();
		MethodSignature sig = (MethodSignature) jp.getSignature();
		Method method;
		try {
			method = clz.getDeclaredMethod(sig.getName(),
					sig.getParameterTypes());
			if (method.isBridge())
				method = BridgeMethodResolver.findBridgedMethod(method);
			return getParameterNames(method);
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Object o, String name) {
		try {
			Field f = o.getClass().getDeclaredField(name);
			f.setAccessible(true);
			return (T) f.get(o);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static void setFieldValue(Object o, String name, Object value) {
		try {
			Field f = o.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(o, value);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static Object getTargetObject(Object proxy) {
		while (proxy instanceof Advised) {
			try {
				return getTargetObject(((Advised) proxy).getTargetSource()
						.getTarget());
			} catch (Exception e) {
				e.printStackTrace();
				return proxy;
			}
		}
		return proxy;
	}

	public static void processCallback(Object obj,
			Class<? extends Annotation> callbackAnnotation) {
		Set<Method> methods = AnnotationUtils.getAnnotatedMethods(
				obj.getClass(), callbackAnnotation);
		for (Method m : methods) {
			if (m.getParameterTypes().length == 0
					&& m.getReturnType() == void.class
					&& Modifier.isPublic(m.getModifiers()))
				try {
					m.invoke(obj, new Object[0]);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e.getMessage(), e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e.getMessage(), e);
				} catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (cause != null) {
						if (cause instanceof RuntimeException)
							throw (RuntimeException) cause;
						else
							throw new RuntimeException(cause.getMessage(),
									cause);
					} else
						throw new RuntimeException(e.getMessage(), e);
				}
		}
	}

}

package org.ironrhino.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.metadata.Param;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.util.proxy.ProxyObject;

public class ReflectionUtils {

	public final static ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static ClassPool classPool = ClassPool.getDefault();

	public static List<String> getAllFields(Class<?> clazz) {
		if (clazz == Object.class || clazz.isPrimitive())
			return Collections.emptyList();
		try {
			classPool.insertClassPath(new ClassClassPath(clazz));
			CtClass cc = classPool.get(clazz.getName());
			List<CtClass> ctClasses = new ArrayList<>();
			ctClasses.add(cc);
			while (!(cc = cc.getSuperclass()).getName().equals(Object.class.getName()))
				ctClasses.add(0, cc);
			List<String> fields = new ArrayList<>();
			for (CtClass ctc : ctClasses) {
				for (CtField cf : ctc.getDeclaredFields()) {
					int accessFlag = cf.getModifiers();
					if (Modifier.isFinal(accessFlag) || Modifier.isStatic(accessFlag))
						continue;
					fields.add(cf.getName());
				}
			}
			return fields;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public static Class<?> getGenericClass(Class<?> clazz) {
		return getGenericClass(clazz, null, 0);
	}

	public static Class<?> getGenericClass(Class<?> clazz, int index) {
		return getGenericClass(clazz, null, index);
	}

	public static Class<?> getGenericClass(Class<?> clazz, Class<?> genericContainerClass) {
		return getGenericClass(clazz, genericContainerClass, 0);
	}

	public static Class<?> getGenericClass(Class<?> clazz, Class<?> genericContainerClass, int index) {
		Type t = clazz.getGenericSuperclass();
		while (t != null) {
			if (t instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) t;
				if (genericContainerClass == null || genericContainerClass == paramType.getRawType()) {
					return getGenericClass(paramType, index);
				} else {
					t = paramType.getRawType();
				}
			} else if (t instanceof Class) {
				t = ((Class<?>) t).getGenericSuperclass();
			}
		}
		return null;
	}

	public static Class<?> getGenericClass(Type genType, int index) {
		if (genType instanceof ParameterizedType) {
			ParameterizedType pramType = (ParameterizedType) genType;
			Type[] params = pramType.getActualTypeArguments();
			if ((params != null) && (params.length > index))
				return params[index] instanceof Class ? (Class<?>) params[index] : null;
		}
		return null;
	}

	public static Class<?> getActualClass(Object object) {
		return getActualClass(object.getClass());
	}

	public static Class<?> getActualClass(Class<?> clazz) {
		if (ProxyObject.class.isAssignableFrom(clazz) || SpringProxy.class.isAssignableFrom(clazz)
				|| clazz.getName().contains("$$EnhancerBySpringCGLIB$$")) {
			clazz = clazz.getSuperclass();
			return getActualClass(clazz);
		} else {
			return clazz;
		}
	}

	public static String[] getParameterNames(Constructor<?> ctor) {
		return doGetParameterNames(ctor);
	}

	public static String[] getParameterNames(Method method) {
		if (method.isBridge())
			method = BridgeMethodResolver.findBridgedMethod(method);
		return doGetParameterNames(method);
	}

	public static String[] getParameterNames(JoinPoint jp) {
		if (!jp.getKind().equals(JoinPoint.METHOD_EXECUTION))
			return null;
		Class<?> clz = jp.getTarget().getClass();
		MethodSignature sig = (MethodSignature) jp.getSignature();
		Method method;
		try {
			method = clz.getDeclaredMethod(sig.getName(), sig.getParameterTypes());
			return getParameterNames(method);
		} catch (Exception e) {
			return null;
		}
	}

	private static String[] doGetParameterNames(Executable executable) {
		Annotation[][] annotations = executable.getParameterAnnotations();
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
			String[] namesDiscovered;
			if ((executable instanceof Method))
				namesDiscovered = parameterNameDiscoverer.getParameterNames((Method) executable);
			else
				namesDiscovered = parameterNameDiscoverer.getParameterNames((Constructor<?>) executable);
			if (namesDiscovered == null)
				return null;
			for (int i = 0; i < names.length; i++)
				if (names[i] == null)
					names[i] = namesDiscovered[i];
		}
		return names;
	}

	public static Field getField(Class<?> clazz, String name) throws NoSuchFieldException {
		try {
			Field f = clazz.getDeclaredField(name);
			f.setAccessible(true);
			return f;
		} catch (NoSuchFieldException e) {
			if (clazz == Object.class)
				throw e;
			return getField(clazz.getSuperclass(), name);
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Object o, String name) {
		try {
			Field f = getField(o.getClass(), name);
			return (T) f.get(o);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static void setFieldValue(Object o, String name, Object value) {
		try {
			Field f = getField(o.getClass(), name);
			f.set(o, value);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static Object getTargetObject(Object proxy) {
		while (proxy instanceof Advised) {
			try {
				Object target = ((Advised) proxy).getTargetSource().getTarget();
				if (target == null)
					return proxy;
				else
					proxy = target;
			} catch (Exception e) {
				e.printStackTrace();
				return proxy;
			}
		}
		return proxy;
	}

	public static void processCallback(Object obj, Class<? extends Annotation> callbackAnnotation) {
		Set<Method> methods = AnnotationUtils.getAnnotatedMethods(obj.getClass(), callbackAnnotation);
		for (Method m : methods) {
			if (m.getParameterCount() == 0 && m.getReturnType() == void.class)
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
							throw new RuntimeException(cause.getMessage(), cause);
					} else
						throw new RuntimeException(e.getMessage(), e);
				}
		}
	}

	public static String getCurrentMethodName() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}

}

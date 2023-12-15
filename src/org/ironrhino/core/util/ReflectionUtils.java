package org.ironrhino.core.util;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.proxy.HibernateProxy;
import org.ironrhino.core.metadata.Param;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.util.proxy.ProxyObject;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ReflectionUtils {

	public final static ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static final boolean JDK9PLUS = ClassUtils.isPresent("java.lang.StackWalker",
			System.class.getClassLoader());

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

	public static Class<?> getActualClass(Object object) {
		return getActualClass(object.getClass());
	}

	public static Class<?> getActualClass(Class<?> clazz) {
		if (Proxy.isProxyClass(clazz))
			return clazz.getInterfaces()[0];
		if (ProxyObject.class.isAssignableFrom(clazz) || HibernateProxy.class.isAssignableFrom(clazz)
				|| SpringProxy.class.isAssignableFrom(clazz) || clazz.getName().contains("$$EnhancerBySpringCGLIB$$")) {
			clazz = clazz.getSuperclass();
			return getActualClass(clazz);
		} else {
			return clazz;
		}
	}

	public static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
		Set<Class<?>> set = new HashSet<>();
		if (clazz.isInterface()) {
			set.add(clazz);
			for (Class<?> intf : clazz.getInterfaces())
				set.addAll(getAllInterfaces(intf));
		} else {
			for (Class<?> intf : ClassUtils.getAllInterfacesForClassAsSet(clazz))
				set.addAll(getAllInterfaces(intf));
		}
		return set;
	}

	public static String[] getParameterNames(Constructor<?> ctor) {
		return doGetParameterNames(ctor);
	}

	public static String[] getParameterNames(Method method) {
		method = BridgeMethodResolver.findBridgedMethod(method);
		return doGetParameterNames(method);
	}

	public static String[] getParameterNames(JoinPoint jp) {
		MethodSignature sig = (MethodSignature) jp.getSignature();
		Class<?> clz = jp.getTarget().getClass();
		Method method;
		try {
			method = Proxy.isProxyClass(clz) ? sig.getMethod()
					: clz.getDeclaredMethod(sig.getName(), sig.getParameterTypes());
		} catch (NoSuchMethodException e) {
			method = sig.getMethod();
		}
		return getParameterNames(method);
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

	public static String stringify(Method method) {
		return stringify(method, false);
	}

	public static String stringify(Method method, boolean simpleParameterName) {
		return stringify(method, false, false);
	}

	public static String stringify(Method method, boolean fullParameterName, boolean excludeDeclaringClass) {
		StringBuilder sb = new StringBuilder();
		if (!excludeDeclaringClass)
			sb.append(method.getDeclaringClass().getName()).append(".");
		sb.append(method.getName()).append("(");
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			sb.append(fullParameterName ? parameterTypes[i].getName() : parameterTypes[i].getSimpleName());
			if (i < parameterTypes.length - 1)
				sb.append(',');
		}
		sb.append(")");
		return sb.toString();
	}

	public static Object invokeDefaultMethod(Object object, Method method, Object[] arguments) throws Throwable {
		if (!method.isDefault())
			throw new IllegalArgumentException("Method is not default: " + method);
		Class<?> objectType = method.getDeclaringClass();
		MethodHandle mh = defaultMethods.computeIfAbsent(new MethodCacheKey(object, method), key -> {
			try {
				Object o = key.getObject();
				Method m = key.getMethod();
				if (JDK9PLUS) {
					return MethodHandles.lookup()
							.findSpecial(objectType, m.getName(),
									MethodType.methodType(m.getReturnType(), m.getParameterTypes()), objectType)
							.bindTo(o);
				} else {
					Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
					constructor.setAccessible(true);
					return constructor.newInstance(objectType).in(objectType).unreflectSpecial(m, objectType).bindTo(o);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return mh.invokeWithArguments(arguments);
	}

	@Value
	private static class MethodCacheKey {
		Object object;
		Method method;
	}

	private static final Map<MethodCacheKey, MethodHandle> defaultMethods = new ConcurrentHashMap<>();

}

package org.ironrhino.core.exception;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ironrhino.core.util.NumberUtils;
import org.ironrhino.core.util.ReflectionUtils;

public class ExceptionCreators {

	private static final Map<Class<?>, Object> cache = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public static <T> T get(Class<T> interfaceClass) {
		return (T) cache.computeIfAbsent(interfaceClass, clz -> {
			if (!clz.isInterface()) {
				throw new IllegalArgumentException(clz + " should be an interface");
			}
			ExceptionCreator exceptionCreator = clz.getAnnotation(ExceptionCreator.class);
			if (exceptionCreator == null) {
				throw new IllegalArgumentException(clz + " should be annotated with @ExceptionCreator");
			}
			Class<?> exceptionType = exceptionCreator.type();
			Constructor<?> ctor;
			try {
				ctor = exceptionType.getConstructor(String.class, String.class);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(
						exceptionType + " should define public constructor accepts code and message as parameters");
			}
			return Proxy.newProxyInstance(clz.getClassLoader(), new Class[] { clz }, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					if (org.springframework.util.ReflectionUtils.isHashCodeMethod(method)) {
						return System.identityHashCode(proxy);
					}
					if (org.springframework.util.ReflectionUtils.isToStringMethod(method)) {
						return "Dynamic proxy for [" + clz.getName() + "]";
					}
					if (method.isDefault()) {
						return ReflectionUtils.invokeDefaultMethod(proxy, method, args);
					}
					ExceptionDetail exceptionDetail = method.getAnnotation(ExceptionDetail.class);
					if (exceptionDetail == null) {
						throw new IllegalArgumentException(method + " should be annotated with @ExceptionDetail");
					}
					String code = String.format("%s-%s-%s-%s", exceptionCreator.project(), exceptionCreator.module(),
							exceptionDetail.type(),
							NumberUtils.format(exceptionDetail.id(), exceptionCreator.length()));
					String message = String.format(exceptionDetail.message(), args);
					Exception exception = (Exception) ctor.newInstance(code, message);
					StackTraceElement[] stackTrace = exception.getStackTrace();
					int index = 0;
					for (int i = 0; i < stackTrace.length; i++) {
						if (stackTrace[i].getMethodName().equals(method.getName())) {
							index = i + 1;
							break;
						}
					}
					if (index > 0) {
						StackTraceElement[] subStackTrace = new StackTraceElement[stackTrace.length - index];
						System.arraycopy(stackTrace, index, subStackTrace, 0, subStackTrace.length);
						exception.setStackTrace(subStackTrace);
					}
					Class<?> returnType = method.getReturnType();
					if (returnType == void.class) {
						throw exception;
					} else if (returnType.isInstance(exception)) {
						return exception;
					} else {
						throw new IllegalArgumentException(method + " should return void or exception assignable from "
								+ exceptionCreator.type().getName());
					}
				}
			});
		});

	}
}

package org.ironrhino.rest.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

import org.ironrhino.rest.annotation.Api;

public class ReflectionHelper {

	private static ClassPool classPool = ClassPool.getDefault();

	public static List<Method> findApiMethods(Class<?> apiDocClass)
			throws Exception {
		List<Method> methods = new ArrayList<>();
		for (Method m : apiDocClass.getMethods()) {
			if (m.getAnnotation(Api.class) == null)
				continue;
			methods.add(m);
		}
		classPool.insertClassPath(new ClassClassPath(apiDocClass));
		final CtClass cc = classPool.get(apiDocClass.getName());
		Collections.sort(methods, new Comparator<Method>() {
			@Override
			public int compare(Method o1, Method o2) {
				int line1 = 0;
				int line2 = 1;
				try {
					Class<?>[] types = o1.getParameterTypes();
					CtClass[] paramTypes = new CtClass[types.length];
					for (int i = 0; i < types.length; i++) {
						classPool.insertClassPath(new ClassClassPath(types[i]));
						paramTypes[i] = classPool.get(types[i].getName());
					}
					classPool.insertClassPath(new ClassClassPath(o1
							.getReturnType()));
					CtMethod method1 = cc.getMethod(
							o1.getName(),
							Descriptor.ofMethod(
									classPool.get(o1.getReturnType().getName()),
									paramTypes));
					line1 = method1.getMethodInfo().getLineNumber(0);
					types = o2.getParameterTypes();
					paramTypes = new CtClass[types.length];
					for (int i = 0; i < types.length; i++) {
						classPool.insertClassPath(new ClassClassPath(types[i]));
						paramTypes[i] = classPool.get(types[i].getName());
					}
					classPool.insertClassPath(new ClassClassPath(o2
							.getReturnType()));
					CtMethod method2 = cc.getMethod(
							o2.getName(),
							Descriptor.ofMethod(
									classPool.get(o2.getReturnType().getName()),
									paramTypes));
					line2 = method2.getMethodInfo().getLineNumber(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return line1 - line2;
			}
		});
		return methods;
	}

}

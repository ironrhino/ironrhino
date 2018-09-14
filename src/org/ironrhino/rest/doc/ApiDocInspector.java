package org.ironrhino.rest.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.ClassScanner;
import org.ironrhino.rest.ApiConfigBase;
import org.ironrhino.rest.doc.annotation.Api;
import org.ironrhino.rest.doc.annotation.ApiModule;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestController;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

public class ApiDocInspector {

	private static ClassPool classPool = ClassPool.getDefault();

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private ApiConfigBase apiConfig;

	protected Map<String, List<ApiModuleObject>> apiModules;

	public Map<String, List<ApiModuleObject>> getApiModules() {
		if (apiModules == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			ComponentScan cs = AnnotatedElementUtils.getMergedAnnotation(apiConfig.getClass(), ComponentScan.class);
			String[] basePackages = cs != null ? cs.basePackages() : new String[0];
			if (basePackages.length == 0)
				basePackages = new String[] { apiConfig.getClass().getPackage().getName() };
			apiModules = getApiModules(basePackages);
		}
		return apiModules;
	}

	protected Map<String, List<ApiModuleObject>> getApiModules(String[] basePackages) {
		Map<String, List<ApiModuleObject>> map = new LinkedHashMap<>();
		Collection<Class<?>> classes = ClassScanner.scanAnnotated(basePackages, ApiModule.class);
		for (Class<?> clazz : classes) {
			Class<?> controllerClass = (AnnotationUtils.getAnnotation(clazz, RestController.class) != null
					|| clazz.isInterface()) ? clazz : clazz.getSuperclass();
			try {
				ctx.getBean(controllerClass);
			} catch (NoSuchBeanDefinitionException e) {
				continue;
			}
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
				for (Method m : methods) {
					ApiDoc ad = new ApiDoc(clazz, m, apiConfig.objectMapper());
					ad.setUrl(ctx.getEnvironment().resolvePlaceholders(ad.getUrl()));
					apiModuleObject.getApiDocs().add(ad);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return map;
	}

	protected static List<Method> findApiMethods(Class<?> apiDocClass) throws Exception {
		List<Method> methods = new ArrayList<>();
		for (Method m : apiDocClass.getMethods()) {
			if (AnnotationUtils.findAnnotation(m, Api.class) == null)
				continue;
			methods.add(m);
		}
		classPool.insertClassPath(new ClassClassPath(apiDocClass));
		final CtClass cc = classPool.get(apiDocClass.getName());
		methods.sort((m1, m2) -> {
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
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return line1 - line2;
		});
		methods.sort((m1, m2) -> {
			Order o1 = AnnotationUtils.findAnnotation(m1, Order.class);
			int order1 = o1 != null ? o1.value() : methods.indexOf(m1) + 1;
			Order o2 = AnnotationUtils.findAnnotation(m2, Order.class);
			int order2 = o2 != null ? o2.value() : methods.indexOf(m2) + 1;
			return order1 - order2;
		});
		return methods;
	}

}

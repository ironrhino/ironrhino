package org.ironrhino.core.spring;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.event.ExpressionEvent;
import org.ironrhino.core.metadata.PostPropertiesReset;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextConsole implements
		ApplicationListener<ExpressionEvent> {

	private static final String SET_PROPERTY_EXPRESSION_PATTERN = "^\\s*[a-zA-Z][a-zA-Z0-9_\\-]*\\.[a-zA-Z][a-zA-Z0-9_]*\\s*=\\s*.+\\s*$";

	private static final String SIMPLE_METHOD_INVOCATION_EXPRESSION_PATTERN = "^\\s*[a-zA-Z][a-zA-Z0-9_\\-]*\\.[a-zA-Z][a-zA-Z0-9_]*\\(\\s*\\)\\s*$";

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	@Autowired(required = false)
	private ServletContext servletContext;

	@Autowired
	private EventPublisher eventPublisher;

	private Map<String, Object> beans = new HashMap<String, Object>();

	private Map<String, Scope> triggers;

	public Map<String, Object> getBeans() {
		if (beans.isEmpty()) {
			if (servletContext != null)
				beans.put(
						"freemarkerConfiguration",
						servletContext
								.getAttribute(FreemarkerManager.CONFIG_SERVLET_CONTEXT_KEY));
			String[] beanNames = ctx.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				if (StringUtils.isAlphanumeric(beanName)
						&& ctx.isSingleton(beanName))
					beans.put(beanName, ctx.getBean(beanName));
			}
		}
		return beans;
	}

	public Map<String, Scope> getTriggers() {
		if (triggers == null) {
			triggers = new TreeMap<String, Scope>();
			triggers.put("freemarkerConfiguration.clearTemplateCache()",
					Scope.APPLICATION);
			String[] beanNames = ctx.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				if (StringUtils.isAlphanumeric(beanName)
						&& ctx.isSingleton(beanName)) {
					try {
						String beanClassName = ctx.getBeanDefinition(beanName)
								.getBeanClassName();
						Class<?> clz = beanClassName != null ? Class
								.forName(beanClassName) : ReflectionUtils
								.getTargetObject(ctx.getBean(beanName))
								.getClass();
						Set<Method> methods = AnnotationUtils
								.getAnnotatedMethods(clz, Trigger.class);
						for (Method m : methods) {
							int modifiers = m.getModifiers();
							if (Modifier.isPublic(modifiers)
									&& m.getParameterTypes().length == 0) {
								StringBuilder expression = new StringBuilder(
										beanName);
								expression.append(".").append(m.getName())
										.append("()");
								triggers.put(expression.toString(), m
										.getAnnotation(Trigger.class).scope());
							}
						}
					} catch (NoSuchBeanDefinitionException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return triggers;
	}

	public Object execute(String expression, Scope scope) throws Exception {
		Object value = null;
		if (expression.matches(SET_PROPERTY_EXPRESSION_PATTERN)) {
			executeSetProperty(expression);
		} else {
			value = executeMethodInvocation(expression);
		}
		if (scope != null && scope != Scope.LOCAL)
			eventPublisher.publish(new ExpressionEvent(expression),
					Scope.GLOBAL);
		return value;
	}

	private Object executeMethodInvocation(String expression) throws Exception {
		if (expression.matches(SIMPLE_METHOD_INVOCATION_EXPRESSION_PATTERN)) {
			String[] arr = expression.split("\\.");
			String beanName = arr[0].trim();
			String methodName = arr[1].trim();
			methodName = methodName.substring(0, methodName.indexOf('('));
			Object bean = getBeans().get(beanName);
			if (bean == null)
				throw new IllegalArgumentException("bean[" + beanName
						+ "] doesn't exists");
			try {
				Method m = bean.getClass().getMethod(methodName, new Class[0]);
				return m.invoke(bean, new Object[0]);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException("bean[" + beanName
						+ "] has no such method: " + arr[1]);
			}
		} else {
			return ExpressionUtils.evalExpression(expression, getBeans());
		}
	}

	private void executeSetProperty(String expression) throws Exception {
		try {
			Object bean = null;
			if (expression.indexOf('=') > 0) {
				bean = getBeans().get(
						expression.substring(0, expression.indexOf('.')));
			}
			ExpressionUtils.evalExpression(expression, getBeans());
			if (bean != null) {
				Method m = AnnotationUtils.getAnnotatedMethod(bean.getClass(),
						PostPropertiesReset.class);
				if (m != null)
					m.invoke(bean, new Object[0]);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void onApplicationEvent(ExpressionEvent event) {
		if (event.isLocal())
			return;
		String expression = event.getExpression();
		try {
			execute(expression, Scope.LOCAL);
		} catch (Exception e) {
			log.error("execute '" + expression + "' error", e);
		}
	}

}
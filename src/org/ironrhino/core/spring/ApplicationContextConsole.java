package org.ironrhino.core.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
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
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.mvel2.PropertyAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextConsole {

	private static final String SET_PROPERTY_EXPRESSION_PATTERN = "^[a-zA-Z][a-zA-Z0-9_\\-]*\\.[a-zA-Z][a-zA-Z0-9_]*\\s*=\\s*.+$";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	@Autowired(required = false)
	private ServletContext servletContext;

	@Autowired
	private EventPublisher eventPublisher;

	private volatile Map<String, Object> beans;

	private Map<String, Scope> triggers;

	public Map<String, Object> getBeans() {
		if (beans == null) {
			synchronized (this) {
				if (beans == null) {
					Map<String, Object> temp = new HashMap<>();
					if (servletContext != null)
						temp.put("freemarkerConfiguration",
								servletContext.getAttribute(FreemarkerManager.CONFIG_SERVLET_CONTEXT_KEY));
					String[] beanNames = ctx.getBeanDefinitionNames();
					for (String beanName : beanNames) {
						if (!ctx.isSingleton(beanName))
							continue;
						BeanDefinition bd = ctx.getBeanDefinition(beanName);
						if (bd.isAbstract())
							continue;
						if (StringUtils.isAlphanumeric(beanName.replaceAll("_", "")))
							temp.put(beanName, ctx.getBean(beanName));
						String[] aliases = ctx.getAliases(beanName);
						for (String alias : aliases)
							if (StringUtils.isAlphanumeric(alias.replaceAll("_", "")))
								temp.put(alias, ctx.getBean(beanName));
					}
					beans = Collections.unmodifiableMap(temp);
				}
			}
		}
		return beans;
	}

	public Map<String, Scope> getTriggers() {
		if (triggers == null) {
			Map<String, Scope> temp = new TreeMap<>();
			// triggers.put("freemarkerConfiguration.clearTemplateCache()",
			// Scope.APPLICATION);
			String[] beanNames = ctx.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				if (StringUtils.isAlphanumeric(beanName) && ctx.isSingleton(beanName)) {
					try {
						BeanDefinition bd = ctx.getBeanDefinition(beanName);
						if (bd.isAbstract())
							continue;
						String beanClassName = bd.getBeanClassName();
						Class<?> clz = beanClassName != null ? Class.forName(beanClassName)
								: ReflectionUtils.getTargetObject(ctx.getBean(beanName)).getClass();
						Set<Method> methods = AnnotationUtils.getAnnotatedMethods(clz, Trigger.class);
						for (Method m : methods) {
							if (m.getParameterCount() == 0) {
								StringBuilder expression = new StringBuilder(beanName);
								expression.append(".").append(m.getName()).append("()");
								temp.put(expression.toString(), m.getAnnotation(Trigger.class).scope());
							}
						}
					} catch (NoSuchBeanDefinitionException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
			triggers = Collections.unmodifiableMap(temp);
		}
		return triggers;
	}

	public Object execute(String expression, Scope scope) throws Exception {
		expression = expression.trim();
		Object value = null;
		try {
			if (expression.matches(SET_PROPERTY_EXPRESSION_PATTERN)) {
				executeSetProperty(expression);
			} else {
				try {
					value = ExpressionUtils.evalExpression(expression, getBeans());
				} catch (PropertyAccessException pe) {
					if (!expression.endsWith(")"))
						value = executeGetProperty(expression);
					else
						throw pe;
				}
			}
			if (scope != null && scope != Scope.LOCAL)
				eventPublisher.publish(new ExpressionEvent(expression), Scope.GLOBAL);
			return value;
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException("NoSuchFieldException: " + e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("NoSuchMethodException: " + e.getMessage());
		}
	}

	private void executeSetProperty(String expression) throws Exception {
		Object bean = null;
		String beanName = expression.substring(0, expression.indexOf('.'));
		bean = getBeans().get(beanName);
		if (bean == null)
			throw new IllegalArgumentException("bean[" + beanName + "] doesn't exist");
		try {
			ExpressionUtils.evalExpression(expression, getBeans());
		} catch (PropertyAccessException pe) {
			bean = ReflectionUtils.getTargetObject(bean);
			String fieldName = expression.substring(expression.indexOf('.') + 1, expression.indexOf('=')).trim();
			String value = expression.substring(expression.indexOf('=') + 1).trim();
			Field f = ReflectionUtils.getField(bean.getClass(), fieldName);
			if (f.getType() == String.class && (value.startsWith("\"") && value.endsWith("\"")
					|| value.startsWith("'") && value.endsWith("'"))) {
				value = value.substring(1, value.length() - 1);
			}
			Object v = ApplicationContextUtils.getBean(ConversionService.class).convert(value, f.getType());
			f.setAccessible(true);
			f.set(bean, v);
		}
		ReflectionUtils.processCallback(bean, PostPropertiesReset.class);
	}

	private Object executeGetProperty(String expression) throws Exception {
		Object bean = null;
		String beanName = expression.substring(0, expression.indexOf('.'));
		bean = getBeans().get(beanName);
		if (bean == null)
			throw new IllegalArgumentException("bean[" + beanName + "] doesn't exist");
		bean = ReflectionUtils.getTargetObject(bean);
		String fieldName = expression.substring(expression.indexOf('.') + 1).trim();
		Field f = ReflectionUtils.getField(bean.getClass(), fieldName);
		f.setAccessible(true);
		return f.get(bean);
	}

	@EventListener
	public void onApplicationEvent(ExpressionEvent event) {
		if (event.isLocal())
			return;
		String expression = event.getExpression();
		try {
			execute(expression, Scope.LOCAL);
		} catch (Throwable e) {
			logger.error("execute '" + expression + "' error", e);
		}
	}

}
package org.ironrhino.common.action;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.metadata.SetupParameter;
import org.ironrhino.core.model.Ordered;
import org.ironrhino.core.spring.ApplicationContextConsole;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;

@AutoConfig(namespace = "/")
public class SetupAction extends BaseAction {

	private static final long serialVersionUID = -9168529475332327922L;

	private static Logger logger = LoggerFactory.getLogger(SetupAction.class);

	private static final String SETUP_ENABLED_KEY = "setup.enabled";

	@Value("${" + SETUP_ENABLED_KEY + ":true}")
	private boolean enabled;

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	@Override
	@InputConfig(methodName = INPUT)
	public String execute() throws Exception {
		if (!canSetup())
			return NOTFOUND;
		executeSetup();
		targetUrl = "/";
		return REDIRECT;
	}

	@Override
	public String input() {
		if (!canSetup())
			return NOTFOUND;
		List<SetupParameterImpl> list;
		try {
			list = getSetupParameters();
			if (list.size() == 0) {
				executeSetup();
				targetUrl = "/";
				return REDIRECT;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return SUCCESS;
	}

	private boolean canSetup() {
		if (!enabled)
			return false;
		if (ctx.containsBean("settingControl"))
			try {
				ApplicationContextConsole console = ctx.getBean(ApplicationContextConsole.class);
				String expression = "settingControl.getBooleanValue(\"" + SETUP_ENABLED_KEY + "\",true)";
				return (Boolean) console.execute(expression, Scope.LOCAL);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		return true;
	}

	private void executeSetup() throws Exception {
		doSetup();
		if (ctx.containsBean("settingControl")) {
			ApplicationContextConsole console = ctx.getBean(ApplicationContextConsole.class);
			String expression = "settingControl.setValue(\"" + SETUP_ENABLED_KEY + "\",\"false\",true,true)";
			console.execute(expression, Scope.LOCAL);
		}
	}

	private List<SetupParameterImpl> setupParameters;

	public List<SetupParameterImpl> getSetupParameters() throws Exception {
		if (setupParameters == null) {
			Set<String> names = new HashSet<>();
			setupParameters = new ArrayList<>();
			String[] beanNames = ctx.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				if (StringUtils.isAlphanumeric(beanName) && ctx.isSingleton(beanName)) {
					BeanDefinition bd = ctx.getBeanDefinition(beanName);
					if (bd.isAbstract())
						continue;
					String beanClassName = bd.getBeanClassName();
					Class<?> clz = beanClassName != null ? Class.forName(beanClassName)
							: ReflectionUtils.getTargetObject(ctx.getBean(beanName)).getClass();
					Set<Method> methods = AnnotationUtils.getAnnotatedMethods(clz, Setup.class);
					for (Method m : methods) {
						if (m.getParameterCount() == 0)
							continue;
						String[] parameterNames = ReflectionUtils.parameterNameDiscoverer.getParameterNames(m);
						Class<?>[] parameterTypes = m.getParameterTypes();
						Annotation[][] annotationArrays = m.getParameterAnnotations();
						for (int i = 0; i < annotationArrays.length; i++) {
							Annotation[] arr = annotationArrays[i];
							SetupParameter sp = null;
							for (Annotation ann : arr)
								if (ann instanceof SetupParameter) {
									sp = (SetupParameter) ann;
									break;
								}
							Class<?> type = parameterTypes[i];
							String _type = StringUtils.uncapitalize(type.getSimpleName());
							if (type.isEnum())
								_type = "enum";
							else if (_type.equals("int") || _type.equals("long"))
								_type = "integer";
							else if (_type.equals("float") || _type.equals("bigdecimal"))
								_type = "double";
							if (names.contains(parameterNames[i]))
								continue;
							names.add(parameterNames[i]);
							setupParameters.add(new SetupParameterImpl(type, parameterNames[i], _type, sp));
						}
					}
				}
			}
			Collections.sort(setupParameters);
		}
		return setupParameters;
	}

	public void doSetup() throws Exception {
		logger.info("setup started");
		String[] beanNames = ctx.getBeanDefinitionNames();
		Map<Method, Object> methods = new TreeMap<Method, Object>((m1, m2) -> {
			int order1 = org.springframework.core.Ordered.LOWEST_PRECEDENCE,
					order2 = org.springframework.core.Ordered.LOWEST_PRECEDENCE;
			Order o = m1.getAnnotation(Order.class);
			if (o != null)
				order1 = o.value();
			o = m2.getAnnotation(Order.class);
			if (o != null)
				order2 = o.value();
			return order1 == order2 ? m1.toString().compareTo(m2.toString()) : order1 < order2 ? -1 : 1;
		});
		for (String beanName : beanNames)
			if (StringUtils.isAlphanumeric(beanName) && ctx.isSingleton(beanName)) {
				BeanDefinition bd = ctx.getBeanDefinition(beanName);
				if (bd.isAbstract())
					continue;
				String beanClassName = bd.getBeanClassName();
				Class<?> clz = beanClassName != null ? Class.forName(beanClassName)
						: ReflectionUtils.getTargetObject(ctx.getBean(beanName)).getClass();
				Object bean = ctx.getBean(beanName);
				for (Method m : AnnotationUtils.getAnnotatedMethods(clz, Setup.class))
					methods.put(m, bean);
			}
		for (Map.Entry<Method, Object> entry : methods.entrySet()) {
			Method m = entry.getKey();
			logger.info("executing {}", m);
			if (m.getParameterCount() == 0) {
				m.invoke(entry.getValue(), new Object[0]);
			} else {
				String[] parameterNames = ReflectionUtils.parameterNameDiscoverer.getParameterNames(m);
				Class<?>[] parameterTypes = m.getParameterTypes();
				Object[] value = new Object[parameterNames.length];
				for (int i = 0; i < parameterNames.length; i++) {
					String pvalue = ServletActionContext.getRequest().getParameter(parameterNames[i]);
					Class<?> type = parameterTypes[i];
					value[i] = ctx.getConversionService().convert(pvalue, type);
				}
				Object o = m.invoke(entry.getValue(), value);
				if (o instanceof UserDetails)
					AuthzUtils.autoLogin((UserDetails) o);
			}
		}
		logger.info("setup finished");
	}

	public static class SetupParameterImpl implements Serializable, Ordered<SetupParameterImpl> {

		private static final long serialVersionUID = -3004203941981232510L;

		private Class<?> parameterType;

		private String name;

		private String type = "string";

		private String label;

		private String defaultValue;

		private String placeholder;

		private boolean required;

		private Set<String> cssClasses = new ConcurrentSkipListSet<>();

		private Map<String, String> dynamicAttributes = new ConcurrentHashMap<>(0);

		private int displayOrder;

		public SetupParameterImpl() {

		}

		public SetupParameterImpl(Class<?> parameterType, String name, String type, SetupParameter setupParameter) {
			this();
			this.parameterType = parameterType;
			this.name = name;
			if (StringUtils.isNotBlank(type))
				this.type = type;
			if (setupParameter != null) {
				this.label = setupParameter.label();
				this.defaultValue = setupParameter.defaultValue();
				this.placeholder = setupParameter.placeholder();
				this.required = setupParameter.required();
				if (StringUtils.isNotBlank(setupParameter.cssClass()))
					this.cssClasses.addAll(Arrays.asList(setupParameter.cssClass().split("\\s")));
				if (Date.class.isAssignableFrom(parameterType))
					this.cssClasses.add("date");
				if (StringUtils.isNotBlank(setupParameter.dynamicAttributes()))
					try {
						this.dynamicAttributes = JsonUtils.fromJson(setupParameter.dynamicAttributes(),
								JsonUtils.STRING_MAP_TYPE);
					} catch (Exception e) {
						e.printStackTrace();
					}
				this.displayOrder = setupParameter.displayOrder();
			}
		}

		public Class<?> getParameterType() {
			return parameterType;
		}

		public void setParameterType(Class<?> parameterType) {
			this.parameterType = parameterType;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		public String getPlaceholder() {
			return placeholder;
		}

		public void setPlaceholder(String placeholder) {
			this.placeholder = placeholder;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public String getCssClass() {
			if (required)
				addCssClass("required");
			return StringUtils.join(cssClasses, " ");
		}

		public void addCssClass(String cssClass) {
			this.cssClasses.add(cssClass);
		}

		public Set<String> getCssClasses() {
			return cssClasses;
		}

		public Map<String, String> getDynamicAttributes() {
			return dynamicAttributes;
		}

		public void setDynamicAttributes(Map<String, String> dynamicAttributes) {
			this.dynamicAttributes = dynamicAttributes;
		}

		@Override
		public int getDisplayOrder() {
			return displayOrder;
		}

		public void setDisplayOrder(int displayOrder) {
			this.displayOrder = displayOrder;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}
}

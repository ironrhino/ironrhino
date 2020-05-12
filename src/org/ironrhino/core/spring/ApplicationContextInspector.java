package org.ironrhino.core.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.ironrhino.core.util.ClassScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.SingletonSupplier;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import com.opensymphony.xwork2.ActionSupport;

@Component
public class ApplicationContextInspector {

	@Autowired
	private ConfigurableListableBeanFactory ctx;

	@Autowired
	private ConfigurableEnvironment env;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private SingletonSupplier<Map<String, ApplicationProperty>> overridedPropertiesSupplier = SingletonSupplier
			.of(() -> {
				Map<String, ApplicationProperty> overridedProperties = new TreeMap<>();
				for (PropertySource<?> ps : env.getPropertySources()) {
					addOverridedProperties(overridedProperties, ps);
				}
				return Collections.unmodifiableMap(overridedProperties);
			});

	private SingletonSupplier<Map<String, ApplicationProperty>> defaultPropertiesSupplier = SingletonSupplier.of(() -> {
		Map<String, String> props = new HashMap<>();
		for (String s : ctx.getBeanDefinitionNames()) {
			BeanDefinition bd = ctx.getBeanDefinition(s);
			String clz = bd.getBeanClassName();
			if (clz == null) {
				continue;
			}
			try {
				Class<?> clazz = Class.forName(clz);
				ReflectionUtils.doWithFields(clazz, field -> {
					props.put(field.getAnnotation(Value.class).value(), formatClassName(clazz));
				}, field -> {
					return field.isAnnotationPresent(Value.class);
				});
				ReflectionUtils.doWithMethods(Class.forName(clz), method -> {
					props.put(method.getAnnotation(Value.class).value(), formatClassName(clazz));
				}, method -> {
					return method.isAnnotationPresent(Value.class);
				});
			} catch (NoClassDefFoundError e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		try {
			for (Resource resource : resourcePatternResolver
					.getResources("classpath*:resources/spring/applicationContext-*.xml"))
				add(resource, props);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (Class<?> clazz : ClassScanner.scanAssignable(ClassScanner.getAppPackages(), ActionSupport.class)) {
			ReflectionUtils.doWithFields(clazz, field -> {
				props.put(field.getAnnotation(Value.class).value(), formatClassName(clazz));
			}, field -> {
				return field.isAnnotationPresent(Value.class);
			});
		}
		Map<String, ApplicationProperty> defaultProperties = new TreeMap<>();
		props.forEach((k, v) -> {
			int start = k.indexOf("${");
			int end = k.lastIndexOf("}");
			if (start > -1 && end > start) {
				k = k.substring(start + 2, end);
				String[] arr = k.split(":", 2);
				if (arr.length > 1)
					defaultProperties.put(arr[0], new ApplicationProperty(arr[1], v));
			}
		});

		ctx.getBeanProvider(DefaultPropertiesProvider.class).forEach(p -> p.getDefaultProperties().forEach(
				(k, v) -> defaultProperties.put(k, new ApplicationProperty(v, formatClassName(p.getClass())))));
		return Collections.unmodifiableMap(defaultProperties);
	});

	public Map<String, ApplicationProperty> getOverridedProperties() {
		return overridedPropertiesSupplier.obtain();
	}

	private void addOverridedProperties(Map<String, ApplicationProperty> properties, PropertySource<?> propertySource) {
		String name = propertySource.getName();
		if (name != null && name.startsWith("servlet"))
			return;
		if (propertySource instanceof EnumerablePropertySource) {
			EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) propertySource;
			for (String s : ps.getPropertyNames()) {
				if (!(propertySource instanceof ResourcePropertySource) && !getDefaultProperties().containsKey(s))
					continue;
				if (!properties.containsKey(s))
					properties.put(s, new ApplicationProperty(
							s.endsWith(".password") ? "********" : String.valueOf(ps.getProperty(s)), name));
			}
		} else if (propertySource instanceof CompositePropertySource) {
			for (PropertySource<?> ps : ((CompositePropertySource) propertySource).getPropertySources()) {
				addOverridedProperties(properties, ps);
			}
		}
	}

	public Map<String, ApplicationProperty> getDefaultProperties() {
		return defaultPropertiesSupplier.obtain();
	}

	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

	private void add(Resource resource, Map<String, String> props) {
		if (resource.isReadable())
			try (InputStream is = resource.getInputStream()) {
				Document doc = builderFactory.newDocumentBuilder().parse(new InputSource(is));
				add(resource, doc.getDocumentElement(), props);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	private void add(Resource resource, Element element, Map<String, String> props) {
		if (element.getTagName().equals("import")) {
			try {
				Resource[] resources = resourcePatternResolver
						.getResources(env.resolvePlaceholders(element.getAttribute("resource")));
				for (Resource r : resources)
					add(r, props);
			} catch (IOException e) {
			}
			return;
		}
		if ("org.springframework.batch.core.configuration.support.ClasspathXmlApplicationContextsFactoryBean"
				.equals(element.getAttribute("class"))) {
			for (int i = 0; i < element.getChildNodes().getLength(); i++) {
				Node node = element.getChildNodes().item(i);
				if (node instanceof Element) {
					Element ele = (Element) node;
					if ("resources".equals(ele.getAttribute("name"))) {
						try {
							Resource[] resources = resourcePatternResolver
									.getResources(env.resolvePlaceholders(ele.getAttribute("value")));
							for (Resource r : resources)
								add(r, props);
						} catch (IOException e) {
						}
						return;
					}
				}
			}
		}
		NamedNodeMap map = element.getAttributes();
		for (int i = 0; i < map.getLength(); i++) {
			Attr attr = (Attr) map.item(i);
			if (attr.getValue().contains("${"))
				props.put(attr.getValue(), resource.toString());
		}
		for (int i = 0; i < element.getChildNodes().getLength(); i++) {
			Node node = element.getChildNodes().item(i);
			if (node instanceof Text) {
				Text text = (Text) node;
				if (text.getTextContent().contains("${"))
					props.put(text.getTextContent(), resource.toString());
			} else if (node instanceof Element) {
				add(resource, (Element) node, props);
			}
		}
	}

	private static String formatClassName(Class<?> clazz) {
		return String.format("class [%s]",
				org.ironrhino.core.util.ReflectionUtils.getActualClass(clazz).getCanonicalName());
	}

	@lombok.Value
	public static class ApplicationProperty {
		private String value;
		private String source;
	}

}
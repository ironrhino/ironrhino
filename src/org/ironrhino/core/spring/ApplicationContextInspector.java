package org.ironrhino.core.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	private SingletonSupplier<Map<String, String>> overridedPropertiesSupplier = SingletonSupplier.of(() -> {
		Map<String, String> overridedProperties = new TreeMap<>();
		for (PropertySource<?> ps : env.getPropertySources()) {
			addOverridedProperties(overridedProperties, ps);
		}
		return Collections.unmodifiableMap(overridedProperties);
	});

	private SingletonSupplier<Map<String, String>> defaultPropertiesSupplier = SingletonSupplier.of(() -> {
		List<String> list = new ArrayList<>();
		for (String s : ctx.getBeanDefinitionNames()) {
			BeanDefinition bd = ctx.getBeanDefinition(s);
			String clz = bd.getBeanClassName();
			if (clz == null) {
				continue;
			}
			try {
				ReflectionUtils.doWithFields(Class.forName(clz), field -> {
					list.add(field.getAnnotation(Value.class).value());
				}, field -> {
					return field.isAnnotationPresent(Value.class);
				});
				ReflectionUtils.doWithMethods(Class.forName(clz), method -> {
					list.add(method.getAnnotation(Value.class).value());
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
				add(resource, list);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for (Class<?> clazz : ClassScanner.scanAssignable(ClassScanner.getAppPackages(), ActionSupport.class)) {
			ReflectionUtils.doWithFields(clazz, field -> {
				list.add(field.getAnnotation(Value.class).value());
			}, field -> {
				return field.isAnnotationPresent(Value.class);
			});
		}
		Map<String, String> defaultProperties = new TreeMap<>();
		for (String str : list) {
			int start = str.indexOf("${");
			int end = str.lastIndexOf("}");
			if (start > -1 && end > start) {
				str = str.substring(start + 2, end);
				String[] arr = str.split(":", 2);
				if (arr.length > 1)
					defaultProperties.put(arr[0], arr[1]);
			}
		}
		ctx.getBeanProvider(DefaultPropertiesProvider.class)
				.forEach(p -> defaultProperties.putAll(p.getDefaultProperties()));
		return Collections.unmodifiableMap(defaultProperties);
	});

	public Map<String, String> getOverridedProperties() {
		return overridedPropertiesSupplier.obtain();
	}

	private void addOverridedProperties(Map<String, String> properties, PropertySource<?> propertySource) {
		String name = propertySource.getName();
		if (name != null && name.startsWith("servlet"))
			return;
		if (propertySource instanceof EnumerablePropertySource) {
			EnumerablePropertySource<?> ps = (EnumerablePropertySource<?>) propertySource;
			for (String s : ps.getPropertyNames()) {
				if (!(propertySource instanceof ResourcePropertySource) && !getDefaultProperties().containsKey(s))
					continue;
				if (!properties.containsKey(s))
					properties.put(s, s.endsWith(".password") ? "********" : String.valueOf(ps.getProperty(s)));

			}
		} else if (propertySource instanceof CompositePropertySource) {
			for (PropertySource<?> ps : ((CompositePropertySource) propertySource).getPropertySources()) {
				addOverridedProperties(properties, ps);
			}
		}
	}

	public Map<String, String> getDefaultProperties() {
		return defaultPropertiesSupplier.obtain();
	}

	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

	private void add(Resource resource, List<String> list) {
		if (resource.isReadable())
			try (InputStream is = resource.getInputStream()) {
				Document doc = builderFactory.newDocumentBuilder().parse(new InputSource(is));
				add(doc.getDocumentElement(), list);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	private void add(Element element, List<String> list) {
		if (element.getTagName().equals("import")) {
			add(resourcePatternResolver.getResource(element.getAttribute("resource")), list);
			return;
		}
		NamedNodeMap map = element.getAttributes();
		for (int i = 0; i < map.getLength(); i++) {
			Attr attr = (Attr) map.item(i);
			if (attr.getValue().contains("${"))
				list.add(attr.getValue());
		}
		for (int i = 0; i < element.getChildNodes().getLength(); i++) {
			Node node = element.getChildNodes().item(i);
			if (node instanceof Text) {
				Text text = (Text) node;
				if (text.getTextContent().contains("${"))
					list.add(text.getTextContent());
			} else if (node instanceof Element) {
				add((Element) node, list);
			}
		}

	}

}
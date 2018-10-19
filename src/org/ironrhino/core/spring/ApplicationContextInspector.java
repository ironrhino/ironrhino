package org.ironrhino.core.spring;

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
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
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

	private volatile Map<String, String> overridedProperties;

	private volatile Map<String, String> defaultProperties;

	public Map<String, String> getOverridedProperties() {
		Map<String, String> temp = overridedProperties;
		if (temp == null) {
			synchronized (this) {
				temp = overridedProperties;
				if (temp == null) {
					Map<String, String> map = new TreeMap<>();
					for (PropertySource<?> ps : env.getPropertySources()) {
						addOverridedProperties(map, ps);
					}
					overridedProperties = temp = Collections.unmodifiableMap(map);
				}
			}
		}
		return temp;
	}

	private static void addOverridedProperties(Map<String, String> properties, PropertySource<?> propertySource) {
		if (propertySource instanceof ResourcePropertySource) {
			ResourcePropertySource rps = (ResourcePropertySource) propertySource;
			for (String s : rps.getPropertyNames()) {
				if (!properties.containsKey(s))
					properties.put(s, s.endsWith(".password") ? "********" : String.valueOf(rps.getProperty(s)));
			}
		} else if (propertySource instanceof CompositePropertySource) {
			for (PropertySource<?> ps : ((CompositePropertySource) propertySource).getPropertySources()) {
				addOverridedProperties(properties, ps);
			}
		}
	}

	public Map<String, String> getDefaultProperties() throws Exception {
		Map<String, String> temp = defaultProperties;
		if (temp == null) {
			synchronized (this) {
				temp = defaultProperties;
				if (temp == null) {
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
						} catch (NoClassDefFoundError e) {
							e.printStackTrace();
						}
					}

					for (Resource resource : resourcePatternResolver
							.getResources("classpath*:resources/spring/applicationContext-*.xml"))
						add(resource, list);

					for (Class<?> clazz : ClassScanner.scanAssignable(ClassScanner.getAppPackages(),
							ActionSupport.class)) {
						ReflectionUtils.doWithFields(clazz, field -> {
							list.add(field.getAnnotation(Value.class).value());
						}, field -> {
							return field.isAnnotationPresent(Value.class);
						});
					}

					Map<String, String> map = new TreeMap<>();
					for (String str : list) {
						int start = str.indexOf("${");
						int end = str.lastIndexOf("}");
						if (start > -1 && end > start) {
							str = str.substring(start + 2, end);
							String[] arr = str.split(":", 2);
							if (arr.length > 1)
								map.put(arr[0], arr[1]);
						}
					}
					ctx.getBeanProvider(DefaultPropertiesProvider.class)
							.forEach(p -> map.putAll(p.getDefaultProperties()));
					defaultProperties = temp = Collections.unmodifiableMap(map);
				}
			}
		}
		return temp;
	}

	DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

	private void add(Resource resource, List<String> list) throws Exception {
		if (resource.isReadable())
			try (InputStream is = resource.getInputStream()) {
				Document doc = builderFactory.newDocumentBuilder().parse(new InputSource(is));
				add(doc.getDocumentElement(), list);
			}
	}

	private void add(Element element, List<String> list) throws Exception {
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
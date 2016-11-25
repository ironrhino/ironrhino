package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.configuration.ConditionTypeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public class ClassScanner {

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final List<TypeFilter> includeFilters = new LinkedList<>();

	private final List<TypeFilter> excludeFilters = new LinkedList<>();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
			this.resourcePatternResolver);

	public ClassScanner() {
		if (AppInfo.getExcludeFilterRegex() != null)
			addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(AppInfo.getExcludeFilterRegex())));
		addIncludeFilter(ConditionTypeFilter.INSTANCE);
	}

	@Autowired(required = false)
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	public final ResourceLoader getResourceLoader() {
		return this.resourcePatternResolver;
	}

	public void addIncludeFilter(TypeFilter includeFilter) {
		this.includeFilters.add(includeFilter);
	}

	public void addExcludeFilter(TypeFilter excludeFilter) {
		this.excludeFilters.add(0, excludeFilter);
	}

	public void resetFilters(boolean useDefaultFilters) {
		this.includeFilters.clear();
		this.excludeFilters.clear();
	}

	@SafeVarargs
	public static Collection<Class<?>> scanAnnotated(String basePackage, Class<? extends Annotation>... annotations) {
		ClassScanner cs = new ClassScanner();
		for (Class<? extends Annotation> anno : annotations)
			cs.addIncludeFilter(new AnnotationTypeFilter(anno));
		return cs.doScan(basePackage);
	}

	public static Collection<Class<?>> scanAnnotated(String[] basePackages, Class<? extends Annotation> annotation) {
		ClassScanner cs = new ClassScanner();
		cs.addIncludeFilter(new AnnotationTypeFilter(annotation));
		List<Class<?>> classes = new ArrayList<>();
		for (String s : basePackages)
			classes.addAll(cs.doScan(s));
		Collections.sort(classes, ClassComparator.INSTANCE);
		return classes;
	}

	@SafeVarargs
	public static Collection<Class<?>> scanAnnotated(String[] basePackages,
			Class<? extends Annotation>... annotations) {
		ClassScanner cs = new ClassScanner();
		for (Class<? extends Annotation> anno : annotations)
			cs.addIncludeFilter(new AnnotationTypeFilter(anno));
		List<Class<?>> classes = new ArrayList<>();
		for (String s : basePackages)
			classes.addAll(cs.doScan(s));
		Collections.sort(classes, ClassComparator.INSTANCE);
		return classes;
	}

	public static Collection<Class<?>> scanAssignable(String basePackage, Class<?>... classes) {
		ClassScanner cs = new ClassScanner();
		for (Class<?> clz : classes)
			cs.addIncludeFilter(new AssignableTypeFilter(clz));
		List<Class<?>> list = new ArrayList<>();
		list.addAll(cs.doScan(basePackage));
		Collections.sort(list, ClassComparator.INSTANCE);
		return list;
	}

	public static Collection<Class<?>> scanAssignable(String[] basePackages, Class<?>... classes) {
		ClassScanner cs = new ClassScanner();
		for (Class<?> clz : classes)
			cs.addIncludeFilter(new AssignableTypeFilter(clz));
		List<Class<?>> list = new ArrayList<>();
		for (String s : basePackages)
			list.addAll(cs.doScan(s));
		Collections.sort(list, ClassComparator.INSTANCE);
		return list;
	}

	public static Collection<Class<?>> scanAnnotatedPackage(String basePackage,
			Class<? extends Annotation> annotation) {
		ClassScanner cs = new ClassScanner();
		cs.addIncludeFilter(new AnnotationTypeFilter(annotation));
		return cs.doScan(basePackage, "/**/*/package-info.class");
	}

	@SafeVarargs
	public static Collection<Class<?>> scanAnnotatedPackage(String basePackage,
			Class<? extends Annotation>... annotations) {
		ClassScanner cs = new ClassScanner();
		for (Class<? extends Annotation> anno : annotations)
			cs.addIncludeFilter(new AnnotationTypeFilter(anno));
		return cs.doScan(basePackage, "/**/*/package-info.class");
	}

	public Collection<Class<?>> doScan(String basePackage) {
		return doScan(basePackage, null);
	}

	public Collection<Class<?>> doScan(String basePackage, String pattern) {
		if (org.apache.commons.lang3.StringUtils.isBlank(pattern))
			pattern = "/**/*.class";
		List<Class<?>> classes = new ArrayList<>();
		Resource resource = null;
		try {
			String searchPath = new StringBuilder(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)
					.append(org.springframework.util.ClassUtils.convertClassNameToResourcePath(basePackage))
					.append(pattern).toString();
			Resource[] resources = this.resourcePatternResolver.getResources(searchPath);
			for (int i = 0; i < resources.length; i++) {
				resource = resources[i];
				if (resource.isReadable()) {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
					if ((includeFilters.size() == 0 && excludeFilters.size() == 0) || matches(metadataReader)) {
						try {
							classes.add(Class.forName(metadataReader.getClassMetadata().getClassName()));
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}

					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		Collections.sort(classes, ClassComparator.INSTANCE);
		return classes;
	}

	protected boolean matches(MetadataReader metadataReader) throws IOException {
		for (TypeFilter tf : this.includeFilters)
			try {
				if (!tf.match(metadataReader, this.metadataReaderFactory))
					return false;
			} catch (Exception e) {
				return false;
			}
		for (TypeFilter tf : this.excludeFilters)
			try {
				return !tf.match(metadataReader, this.metadataReaderFactory);
			} catch (Exception e) {
				return false;
			}
		return true;
	}

	public static String[] getAppPackages() {
		return getAppPackages(true);
	}

	public static String[] getAppPackages(boolean strict) {
		if (strict) {
			String appBasePackage = AppInfo.getAppBasePackage();
			if (StringUtils.isNotBlank(appBasePackage)) {
				if (!appBasePackage.contains("org.ironrhino"))
					appBasePackage = "org.ironrhino," + appBasePackage;
			} else {
				appBasePackage = "org.ironrhino";
			}
			String[] arr = appBasePackage.split(",+");
			Set<String> packages = new TreeSet<>();
			Collection<Class<?>> componentScans = scanAnnotated(arr, ComponentScan.class);
			for (Class<?> c : componentScans) {
				ComponentScan cs = AnnotatedElementUtils.getMergedAnnotation(c, ComponentScan.class);
				packages.addAll(Arrays.asList(cs.value()));
			}
			packages.addAll(Arrays.asList(arr));
			return packages.toArray(new String[0]);
		} else {
			Set<String> packages = new TreeSet<>();
			for (Package p : Package.getPackages()) {
				String name = p.getName();
				if (isExcludePackage(name))
					continue;
				int deep = name.split("\\.").length;
				if (deep <= 2)
					packages.add(name);
				else
					packages.add(name.substring(0, name.indexOf(".", name.indexOf(".") + 1)));
			}
			return packages.toArray(new String[0]);
		}
	}

	private static boolean isExcludePackage(String name) {
		if (name.equals("net") || name.equals("com") || name.equals("org")) {
			return true;
		}
		for (String s : excludePackages) {
			if (name.equals(s) || name.startsWith(s + '.'))
				return true;
		}
		return false;
	}

	private static String[] excludePackages = new String[] { "java", "javax", "com.sun", "sun", "org.w3c", "org.xml",
			"antlr", "com.bea", "com.caucho", "com.chenlb", "com.fasterxml", "com.google", "com.ibm", "com.jolbox",
			"com.microsoft", "com.mongodb", "com.mysql", "com.opensymphony", "com.oracle", "com.rabbitmq", "com.taobao",
			"com.vmware", "freemarker", "javassist", "jsr166y", "net.htmlparser", "net.sf", "net.sourceforge", "ognl",
			"oracle", "org.antlr", "org.aopalliance", "org.apache", "org.aspectj", "org.bson", "org.cloudfoundry",
			"org.codehaus", "org.elasticsearch", "org.dom4j", "org.eclipse", "org.hibernate", "org.ietf", "org.jboss",
			"org.jcp", "org.mvel2", "org.postgresql", "org.slf4j", "org.springframework", "org.tartarus",
			"org.ironrhino.core", "redis", "weblogic" };

}
package org.ironrhino.core.spring.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

public class YamlPropertySourceFactory implements PropertySourceFactory {

	private final static boolean snakeyamlPresent = ClassUtils.isPresent("org.yaml.snakeyaml.Yaml",
			YamlPropertySourceFactory.class.getClassLoader());

	@Override
	public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException {
		if (!snakeyamlPresent) {
			if (!resource.getResource().exists()) {
				throw new FileNotFoundException(resource.getResource() + " cannot be opened because it does not exist");
			}
			throw new IllegalArgumentException("missing snakeyaml"); // for ignoreResourceNotFound
		}
		String sourceName = name != null ? name : resource.getResource().getFilename();
		if (sourceName == null)
			sourceName = "Unknown";
		Properties propertiesFromYaml = loadYamlIntoProperties(resource);
		return new PropertiesPropertySource(sourceName, propertiesFromYaml);
	}

	private Properties loadYamlIntoProperties(EncodedResource resource) throws FileNotFoundException {
		try {
			YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
			factory.setResources(resource.getResource());
			factory.afterPropertiesSet();
			return factory.getObject();
		} catch (IllegalStateException e) {
			// for ignoreResourceNotFound
			Throwable cause = e.getCause();
			if (cause instanceof FileNotFoundException)
				throw (FileNotFoundException) e.getCause();
			throw e;
		}
	}
}
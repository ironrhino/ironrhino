package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.classreading.AnnotationMetadataReadingVisitor;

class ServiceImplementationCondition implements Condition {

	private static Properties services = new Properties();

	static {
		Resource resource = new ClassPathResource("services.properties");
		if (resource.exists()) {
			try (InputStream is = resource.getInputStream()) {
				services.load(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if (context.getEnvironment() != null) {
			Map<String, Object> attrs = metadata
					.getAnnotationAttributes(ServiceImplementationConditional.class.getName());
			if (attrs != null) {
				String className = ((AnnotationMetadataReadingVisitor) metadata).getClassName();
				Class<?> serviceInterface = (Class<?>) attrs.get("serviceInterface");
				if (serviceInterface == void.class) {
					try {
						Class<?> clazz = Class.forName(className);
						Class<?>[] interfaces = clazz.getInterfaces();
						if (interfaces.length > 0)
							serviceInterface = interfaces[0];
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
				if (serviceInterface != null && serviceInterface != void.class) {
					String implementationClassName = services.getProperty(serviceInterface.getName());
					if (implementationClassName != null)
						return implementationClassName.equals(className);
				}
				String[] profiles = (String[]) attrs.get("profiles");
				if (profiles.length == 0 || context.getEnvironment().acceptsProfiles(profiles)) {
					return true;
				}
				return false;
			}
		}
		return true;
	}

}

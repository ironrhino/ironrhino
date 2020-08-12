package org.ironrhino.core.spring;

import java.util.Map;

@FunctionalInterface
public interface DefaultPropertiesProvider {

	Map<String, String> getDefaultProperties();

}

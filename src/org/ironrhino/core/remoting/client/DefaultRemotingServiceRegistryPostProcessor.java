package org.ironrhino.core.remoting.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.springframework.stereotype.Component;

@Component
public class DefaultRemotingServiceRegistryPostProcessor extends RemotingServiceRegistryPostProcessor {

	public static final String KEY_IMPORT_BASE_PACKAGES = "remoting.import.basePackages";

	public static final String KEY_IMPORT_INCLUDE_CLASSES = "remoting.import.includeClasses";

	public static final String KEY_IMPORT_EXCLUDE_CLASSES = "remoting.import.excludeClasses";

	public String[] getBasePackages() {
		String str = AppInfo.getApplicationContextProperties().getProperty(KEY_IMPORT_BASE_PACKAGES);
		return StringUtils.isNotBlank(str) ? str.trim().split(",") : null;
	}

	public Collection<Class<?>> getIncludeClasses() {
		String str = AppInfo.getApplicationContextProperties().getProperty(KEY_IMPORT_INCLUDE_CLASSES);
		if (StringUtils.isNotBlank(str)) {
			String arr[] = str.trim().split(",");
			Collection<Class<?>> classes = new ArrayList<>(arr.length);
			for (String clz : arr) {
				try {
					classes.add(Class.forName(clz));
				} catch (ClassNotFoundException e) {
					logger.error(e.getMessage(), e);
				}
			}
			return classes;
		}
		return Collections.emptySet();
	}

	public Collection<Class<?>> getExcludeClasses() {
		String str = AppInfo.getApplicationContextProperties().getProperty(KEY_IMPORT_EXCLUDE_CLASSES);
		if (StringUtils.isNotBlank(str)) {
			String arr[] = str.trim().split(",");
			Collection<Class<?>> classes = new ArrayList<>(arr.length);
			for (String clz : arr) {
				try {
					classes.add(Class.forName(clz));
				} catch (ClassNotFoundException e) {
					logger.error(e.getMessage(), e);
				}
			}
			return classes;
		}
		return Collections.emptySet();
	}

}
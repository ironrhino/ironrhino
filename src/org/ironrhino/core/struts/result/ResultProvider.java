package org.ironrhino.core.struts.result;

import java.util.HashMap;
import java.util.Map;

import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.config.entities.ResultTypeConfig;

public interface ResultProvider {

	Logger logger = LoggerFactory.getLogger(ResultProvider.class);

	default void registerResults(PackageConfig config) {
		Map<String, ResultTypeConfig> resultTypeConfigs = new HashMap<>(config.getResultTypeConfigs());
		getResultTypeConfigs().entrySet().stream().forEach(entry -> {
			if (!resultTypeConfigs.containsKey(entry.getKey())) {
				resultTypeConfigs.put(entry.getKey(), entry.getValue());
				logger.info("Register result type [{}] to package [{}]", entry.getKey(), config.getName());
			} else {
				logger.warn("Skip override result type [{}] of package [{}]", entry.getKey(), config.getName());
			}
		});
		ReflectionUtils.setFieldValue(config, "resultTypeConfigs", resultTypeConfigs);
		Map<String, ResultConfig> globalResultConfigs = new HashMap<>(config.getGlobalResultConfigs());
		getGlobalResultConfigs().entrySet().stream().forEach(entry -> {
			if (!globalResultConfigs.containsKey(entry.getKey())) {
				globalResultConfigs.put(entry.getKey(), entry.getValue());
				logger.info("Register global result [{}] to package [{}]", entry.getKey(), config.getName());
			} else {
				logger.warn("Skip override global result [{}] of package [{}]", entry.getKey(), config.getName());
			}
		});
		ReflectionUtils.setFieldValue(config, "globalResultConfigs", globalResultConfigs);
	}

	Map<String, ResultTypeConfig> getResultTypeConfigs();

	Map<String, ResultConfig> getGlobalResultConfigs();

}

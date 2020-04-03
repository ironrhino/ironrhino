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
		getResultTypeConfigs().forEach((k, v) -> {
			if (!resultTypeConfigs.containsKey(k)) {
				resultTypeConfigs.put(k, v);
				logger.info("Register result type [{}] to package [{}]", k, config.getName());
			} else {
				logger.warn("Skip override result type [{}] of package [{}]", k, config.getName());
			}
		});
		ReflectionUtils.setFieldValue(config, "resultTypeConfigs", resultTypeConfigs);
		Map<String, ResultConfig> globalResultConfigs = new HashMap<>(config.getGlobalResultConfigs());
		getGlobalResultConfigs().forEach((k, v) -> {
			if (!globalResultConfigs.containsKey(k)) {
				globalResultConfigs.put(k, v);
				logger.info("Register global result [{}] to package [{}]", k, config.getName());
			} else {
				logger.warn("Skip override global result [{}] of package [{}]", k, config.getName());
			}
		});
		ReflectionUtils.setFieldValue(config, "globalResultConfigs", globalResultConfigs);
	}

	Map<String, ResultTypeConfig> getResultTypeConfigs();

	Map<String, ResultConfig> getGlobalResultConfigs();

}

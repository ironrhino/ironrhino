package org.ironrhino.core.metrics;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@Component
@ApplicationContextPropertiesConditional(key = "prometheus.enabled", value = "true")
@ClassPresentConditional("io.micrometer.prometheus.PrometheusMeterRegistry")
public class PrometheusMeterRegistryProvider extends AccessHandler implements MeterRegistryProvider {

	public static final String DEFAULT_METRICS_PATH = "/metrics";

	@Value("${prometheus.metricsPath:" + DEFAULT_METRICS_PATH + "}")
	private String metricsPath = DEFAULT_METRICS_PATH;

	private PrometheusMeterRegistry prometheusMeterRegistry;

	@Override
	public Optional<PrometheusMeterRegistry> get() {
		this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		return Optional.of(prometheusMeterRegistry);
	}

	@Override
	public String getPattern() {
		return metricsPath;
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		response.getWriter().write(prometheusMeterRegistry.scrape());
		return true;
	}

}

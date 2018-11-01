package org.ironrhino.core.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;

import org.ironrhino.core.spring.DefaultPropertiesProvider;
import org.ironrhino.core.spring.configuration.AddressAvailabilityCondition;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.NameableThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.lang.Nullable;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import io.micrometer.influx.InfluxNamingConvention;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@ClassPresentConditional("io.micrometer.influx.InfluxMeterRegistry")
@Slf4j
public class InfluxMeterRegistryProvider implements MeterRegistryProvider, DefaultPropertiesProvider {

	public static final String DEFAULT_DB = "metrics";

	public static final String DEFAULT_STEP = "PT10S";

	@Autowired
	private Environment environment;

	private ThreadFactory threadFactory = new NameableThreadFactory("metrics");

	private InfluxConfig influxConfig;

	private InfluxMeterRegistry influxMeterRegistry;

	@Getter
	private final Map<String, String> defaultProperties;

	public InfluxMeterRegistryProvider() {
		Map<String, String> map = new HashMap<>();
		String prefix = InfluxConfig.DEFAULT.prefix();
		map.put(prefix + ".db", DEFAULT_DB);
		map.put(prefix + ".step", DEFAULT_STEP);
		map.put(prefix + ".uri", InfluxConfig.DEFAULT.uri());
		defaultProperties = Collections.unmodifiableMap(map);
	}

	@Override
	public Optional<InfluxMeterRegistry> get() {
		return createMeterRegistry(getConfig(Collections.emptyMap()), false);
	}

	private InfluxConfig getConfig(Map<String, String> overrides) {
		return key -> {
			String suffix = key.substring(key.lastIndexOf('.') + 1);
			if (overrides != null && overrides.containsKey(suffix))
				return overrides.get(suffix);
			return environment.getProperty(key, defaultProperties.get(key));
		};
	}

	private Optional<InfluxMeterRegistry> createMeterRegistry(InfluxConfig config, boolean forUpdate) {
		if (AddressAvailabilityCondition.check(config.uri(), 2000)) {
			if (forUpdate)
				log.info("Add influx metrics registry {} with db '{}'", config.uri(), config.db());
			this.influxConfig = config;
			this.influxMeterRegistry = InfluxMeterRegistry.builder(config).threadFactory(threadFactory).build();
			// revert https://github.com/micrometer-metrics/micrometer/issues/693
			this.influxMeterRegistry.config().namingConvention(new InfluxNamingConvention() {

				@Override
				public String name(String name, Meter.Type type, @Nullable String baseUnit) {
					return format(name.replace("=", "_"));
				}

				private String format(String name) {
					// https://docs.influxdata.com/influxdb/v1.3/write_protocols/line_protocol_reference/#special-characters
					return name.replace(",", "\\,").replace(" ", "\\ ").replace("=", "\\=").replace("\"", "\\\"");
				}
			});
			return Optional.of(influxMeterRegistry);
		} else {
			log.warn("Skip influx metrics registry {} with db '{}'", config.uri(), config.db());
			return Optional.empty();
		}
	}

	public void updateStep(String step) {
		InfluxMeterRegistry previousMeterRegistry = influxMeterRegistry;
		InfluxConfig previousConfig = influxConfig;
		if (previousMeterRegistry == null || previousConfig == null) {
			log.warn("InfluxMeterRegistry is not registered");
			return;
		}
		String previousStep = previousConfig.step().toString();
		if (step.equals(previousStep)) {
			log.warn("Updating step is equals to current step");
			return;
		}
		InfluxConfig config = getConfig(Collections.singletonMap("step", step));
		createMeterRegistry(config, true).ifPresent(registry -> {
			Metrics.removeRegistry(previousMeterRegistry);
			previousMeterRegistry.close();
			Metrics.addRegistry(registry);
			log.info("Updated influx metrics registry step from {} to {}", previousStep, step);
		});
	}

}

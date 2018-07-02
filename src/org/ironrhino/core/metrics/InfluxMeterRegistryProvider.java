package org.ironrhino.core.metrics;

import java.util.Optional;

import org.ironrhino.core.spring.configuration.AddressAvailabilityCondition;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.NameableThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.lang.Nullable;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import io.micrometer.influx.InfluxNamingConvention;
import lombok.extern.slf4j.Slf4j;

@Component
@ClassPresentConditional("io.micrometer.influx.InfluxMeterRegistry")
@Slf4j
public class InfluxMeterRegistryProvider implements MeterRegistryProvider {

	public static final String DEFAULT_DB = "metrics";

	@Autowired
	private Environment environment;

	@Override
	public Optional<MeterRegistry> get() {
		InfluxConfig config = key -> environment.getProperty(key, key.endsWith(".db") ? DEFAULT_DB : (String) null);
		if (AddressAvailabilityCondition.check(config.uri(), 2000)) {
			log.info("Add influx metrics registry {} with db '{}'", config.uri(), config.db());
			MeterRegistry meterRegistry = new InfluxMeterRegistry(config, Clock.SYSTEM,
					new NameableThreadFactory("metrics"));
			// revert https://github.com/micrometer-metrics/micrometer/issues/693
			meterRegistry.config().namingConvention(new InfluxNamingConvention() {

				@Override
				public String name(String name, Meter.Type type, @Nullable String baseUnit) {
					return format(name.replace("=", "_"));
				}

				private String format(String name) {
					// https://docs.influxdata.com/influxdb/v1.3/write_protocols/line_protocol_reference/#special-characters
					return name.replace(",", "\\,").replace(" ", "\\ ").replace("=", "\\=").replace("\"", "\\\"");
				}
			});
			return Optional.of(meterRegistry);
		} else {
			log.warn("Skip influx metrics registry {} with db '{}'", config.uri(), config.db());
			return Optional.empty();
		}
	}

}

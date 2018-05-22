package org.ironrhino.core.metrics;

import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;

public interface MeterRegistryProvider {

	public Optional<MeterRegistry> get();

}

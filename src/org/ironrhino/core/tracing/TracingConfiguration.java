package org.ironrhino.core.tracing;

import java.net.URI;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.configuration.AddressAvailabilityCondition;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jaegertracing.internal.Constants;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.samplers.GuaranteedThroughputSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.util.GlobalTracer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ClassPresentConditional("io.jaegertracing.Configuration")
@Slf4j
public class TracingConfiguration {

	public final static String SPAN_CONTEXT_KEY = "x-trace-id";

	public final static String BAGGAG_EPREFIX = "x-trace-ctx-";

	private static final int ONE_MB_IN_BYTES = 1048576;

	@Value("${jaeger.collector.uri:http://localhost:14268}")
	private URI uri;

	@Value("${jaeger.reporter.flushInterval:5000}")
	private int flushInterval;

	@Value("${jaeger.reporter.maxQueueSize:1000}")
	private int maxQueueSize;

	@Value("${jaeger.reporter.minDurationInMs:5}")
	private int minDurationInMs;

	@Value("${jaeger.sampler.samplingRate:0.01}")
	private double samplingRate;

	@Value("${jaeger.sampler.lowerBound:10}")
	private double lowerBound;

	private JaegerTracer tracer;

	@Getter
	private Sampler sampler;

	@Getter
	private Reporter reporter;

	@PostConstruct
	public void init() {
		if (uri == null) {
			Tracing.disable();
			return;
		}
		String scheme = uri.getScheme();
		if (scheme == null || !scheme.equals("udp") && !scheme.equals("http") && !scheme.equals("https"))
			throw new IllegalArgumentException("uri scheme should be one of udp http https");
		Sender sender;
		if (scheme.equals("udp")) {
			sender = new UdpSender(uri.getHost(), uri.getPort(), ONE_MB_IN_BYTES);
		} else {
			String s = uri.toString();
			if (!s.endsWith("/api/traces"))
				s += "/api/traces";
			if (!AddressAvailabilityCondition.check(s, 2000)) {
				log.warn("Skip jaeger tracer with {}", s);
				Tracing.disable();
				return;
			}
			sender = new HttpSender(s, ONE_MB_IN_BYTES);
		}
		boolean production = AppInfo.getStage() == Stage.PRODUCTION;
		Reporter reporter = new RemoteReporter.Builder().withSender(sender)
				.withFlushInterval(production ? flushInterval : 1000).withMaxQueueSize(maxQueueSize).build();
		this.reporter = reporter;
		if (production) {
			sampler = new GuaranteedThroughputSampler(samplingRate, lowerBound);
			if (minDurationInMs > 0)
				reporter = new DelegatingReporter(reporter, span -> span.getDuration() >= minDurationInMs * 1000);
		} else {
			sampler = new ConstSampler(true);
		}
		TextMapCodec codec = new TextMapCodec.Builder().withSpanContextKey(SPAN_CONTEXT_KEY)
				.withBaggagePrefix(BAGGAG_EPREFIX).withUrlEncoding(true).build();
		JaegerTracer.Builder builder = new JaegerTracer.Builder(AppInfo.getAppName()).withTraceId128Bit()
				.withSampler(sampler).withReporter(reporter).withTag("java.version", System.getProperty("java.version"))
				.withTag("instance", AppInfo.getInstanceId())
				.withTag(Constants.TRACER_HOSTNAME_TAG_KEY, AppInfo.getHostName())
				.withTag(Constants.TRACER_IP_TAG_KEY, AppInfo.getHostAddress())
				.registerExtractor(Builtin.HTTP_HEADERS, codec).registerInjector(Builtin.HTTP_HEADERS, codec)
				.registerExtractor(Builtin.TEXT_MAP, codec).registerInjector(Builtin.TEXT_MAP, codec);
		String ironrhinoVersion = AppInfo.getIronrhinoVersion();
		if (StringUtils.isNotBlank(ironrhinoVersion))
			builder.withTag("ironrhino.version", ironrhinoVersion);
		String server = AppInfo.getServerInfo();
		if (StringUtils.isNotBlank(server))
			builder.withTag("server", server);
		tracer = builder.build();
		GlobalTracer.registerIfAbsent(tracer);
		log.info("Register jaeger tracer with {}", uri);
	}

	@Bean
	public TracingAspect tracingAspect() {
		return new TracingAspect();
	}

	@PreDestroy
	public void destroy() {
		if (tracer != null) {
			tracer.close();
		}
	}

}

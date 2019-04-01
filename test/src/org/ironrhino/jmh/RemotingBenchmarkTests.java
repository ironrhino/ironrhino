package org.ironrhino.jmh;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.serializer.FstHttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.JavaHttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.JsonHttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.SmileHttpInvokerSerializer;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.security.domain.User;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class RemotingBenchmarkTests {

	private AnnotationConfigApplicationContext ctx;
	private HttpInvokerClient httpInvokerClient;
	private TestService testService;

	@Setup(Level.Trial)
	public void setup() {
		AppInfo.initialize();
		ctx = new AnnotationConfigApplicationContext(Config.class);
		httpInvokerClient = ctx.getBean(HttpInvokerClient.class);
		testService = ctx.getBean("testService", TestService.class);
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		ctx.close();
	}

	@Benchmark
	public User baseline() {
		httpInvokerClient.getHttpInvokerRequestExecutor().setSerializer(JavaHttpInvokerSerializer.INSTANCE);
		return testService.loadUserByUsername("admin");
	}

	@Benchmark
	public User measureRemotingWithJsonSerializer() {
		httpInvokerClient.getHttpInvokerRequestExecutor().setSerializer(JsonHttpInvokerSerializer.INSTANCE);
		return testService.loadUserByUsername("admin");
	}

	@Benchmark
	public User measureRemotingWithFstSerializer() {
		httpInvokerClient.getHttpInvokerRequestExecutor().setSerializer(FstHttpInvokerSerializer.INSTANCE);
		return testService.loadUserByUsername("admin");
	}

	@Benchmark
	public User measureRemotingWithSmileSerializer() {
		httpInvokerClient.getHttpInvokerRequestExecutor().setSerializer(SmileHttpInvokerSerializer.INSTANCE);
		return testService.loadUserByUsername("admin");
	}

	@Configuration
	static class Config {

		@Bean
		public HttpInvokerClient testService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(TestService.class);
			hic.setHost("localhost");
			return hic;
		}

	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include(RemotingBenchmarkTests.class.getName()).shouldFailOnError(true)
				.build();
		new Runner(opt).run();
	}
}

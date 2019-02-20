package org.ironrhino.core.remoting;

import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.BaseHttpInvokerTest.HttpInvokerConfiguration;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.client.RemotingServiceRegistryPostProcessor;
import org.ironrhino.core.remoting.impl.StandaloneServiceRegistry;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.server.HttpInvokerServer;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestServiceImpl;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = HttpInvokerConfiguration.class)
public abstract class BaseHttpInvokerTest {

	public static MockHttpServletRequest mockHttpServletRequest;
	public static MockHttpServletResponse mockHttpServletResponse;
	public static MockAsyncContext mockAsyncContext;

	@Autowired
	protected ServiceRegistry serviceRegistry;
	@Autowired
	protected TestService testService;
	@Autowired
	protected TestService mockTestService;
	@Autowired
	protected FooService fooService;
	@Autowired
	protected FooService mockFooService;
	@Autowired
	protected HttpInvokerServer httpInvokerServer;
	@Autowired
	protected HttpInvokerRequestExecutor mockHttpInvokerRequestExecutor;

	@Value("${httpInvoker.serializationType:}")
	protected String serializationType;

	protected HttpInvokerSerializer serializer;

	protected String serviceUrl(Class<?> serviceClazz) {
		return "http://localhost:8080/remoting/httpinvoker/" + serviceClazz.getName();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		serializer = HttpInvokerSerializers.ofSerializationType(serializationType);
	}

	@Before
	public void reset() {
		Mockito.reset(mockHttpInvokerRequestExecutor, mockTestService);
		if (mockHttpServletRequest != null)
			Mockito.reset(mockHttpServletRequest);
		if (mockHttpServletResponse != null)
			Mockito.reset(mockHttpServletResponse);
		if (mockAsyncContext != null)
			Mockito.reset(mockAsyncContext);

	}

	public static MockHttpServletRequest mockHttpServletRequest() {
		return mockHttpServletRequest = spy(new MockAsyncHttpServletRequest());
	}

	public static MockHttpServletResponse mockHttpServletResponse() {
		return mockHttpServletResponse = spy(new MockHttpServletResponse());
	}

	public static MockAsyncContext mockAsyncContext(MockHttpServletRequest request, MockHttpServletResponse response) {
		return mockAsyncContext = spy(new MockAsyncContext(request, response));
	}

	@Configuration
	static class HttpInvokerConfiguration {

		@Bean
		public RemotingServiceRegistryPostProcessor remotingServiceRegistryPostProcessor() {
			RemotingServiceRegistryPostProcessor registryPostProcessor = new RemotingServiceRegistryPostProcessor();
			registryPostProcessor.setAnnotatedClasses(new Class[] { TestService.class, FooService.class });
			return registryPostProcessor;
		}

		@Bean
		public ServiceRegistry serviceRegistry() {
			return new StandaloneServiceRegistry() {
				@Override
				@EventListener
				public void onApplicationEvent(ContextRefreshedEvent event) {
					init();
				}
			};
		}

		@Bean
		public HttpInvokerServer httpInvokerServer() {
			return new HttpInvokerServer();
		}

		@Bean
		public HttpInvokerClient testService() {
			HttpInvokerClient httpInvokerClient = new HttpInvokerClient();
			httpInvokerClient.setServiceInterface(TestService.class);
			httpInvokerClient.setHost("localhost");
			return httpInvokerClient;
		}

		@Bean
		public HttpInvokerClient fooService() {
			HttpInvokerClient httpInvokerClient = new HttpInvokerClient();
			httpInvokerClient.setServiceInterface(FooService.class);
			httpInvokerClient.setHost("localhost");
			return httpInvokerClient;
		}

		@Bean
		public TestService mockTestService() {
			return spy(new TestServiceImpl());
		}

		@Bean
		public FooServiceFactoryBean mockFooService() {
			return new FooServiceFactoryBean();
		}

		@Bean
		public HttpInvokerRequestExecutor mockHttpInvokerRequestExecutor() {
			return spy(new MockHttpInvokerRequestExecutor());
		}
	}

	static class MockAsyncHttpServletRequest extends MockHttpServletRequest {
		@Override
		public AsyncContext startAsync(ServletRequest request, @Nullable ServletResponse response) {
			this.setAsyncSupported(true);
			if (this.getAsyncContext() == null) {
				this.setAsyncContext(new MockAsyncContext(request, response));
			}
			return this.getAsyncContext();
		}
	}

	static class MockHttpInvokerRequestExecutor extends HttpInvokerRequestExecutor {

		@Autowired
		HttpInvokerServer httpInvokerServer;

		@Override
		protected RemoteInvocationResult doExecuteRequest(String serviceUrl, MethodInvocation methodInvocation,
				ByteArrayOutputStream baos) throws Exception {

			MockHttpServletRequest request = mockHttpServletRequest();
			MockHttpServletResponse response = mockHttpServletResponse();
			request.setRequestURI(serviceUrl);
			request.addHeader(HttpHeaders.CONTENT_TYPE, this.getSerializer().getContentType());
			request.setContent(baos.toByteArray());
			request.setAsyncContext(mockAsyncContext(request, response));

			httpInvokerServer.handleRequest(request, response);
			byte[] content;
			// wait async task finish
			while ((content = response.getContentAsByteArray()).length == 0 && request.isAsyncSupported()) {
				Thread.sleep(50);
			}
			return this.getSerializer().readRemoteInvocationResult(methodInvocation, new ByteArrayInputStream(content));
		}
	}

	// must public
	public static class FooServiceImpl implements FooService {

		@Override
		public String test(String value) {
			return value;
		}
	}

	static class FooServiceFactoryBean implements FactoryBean<FooService> {

		public FooService service;

		public FooServiceFactoryBean() {
			this.service = spy(new FooServiceImpl());
		}

		@Override
		public FooService getObject() throws Exception {
			return service;
		}

		@Override
		public Class<?> getObjectType() {
			return FooService.class;
		}
	}
}

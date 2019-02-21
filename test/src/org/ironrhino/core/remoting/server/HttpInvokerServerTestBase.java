package org.ironrhino.core.remoting.server;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.client.RemotingServiceRegistryPostProcessor;
import org.ironrhino.core.remoting.impl.StandaloneServiceRegistry;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.server.HttpInvokerServerTestBase.HttpInvokerConfiguration;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestServiceImpl;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HttpInvokerConfiguration.class)
public abstract class HttpInvokerServerTestBase {

	protected static MockHttpServletRequest mockHttpServletRequest;
	protected static MockHttpServletResponse mockHttpServletResponse;
	protected static MockAsyncContext mockAsyncContext;

	protected static String serviceUrl(Class<?> serviceClass) {
		return "http://" + AppInfo.getHostAddress() + ':'
				+ (AppInfo.getHttpPort() > 0 ? AppInfo.getHttpPort() : ServiceRegistry.DEFAULT_HTTP_PORT)
				+ "/remoting/httpinvoker/" + serviceClass.getName();
	}

	@Configuration
	static class HttpInvokerConfiguration {

		@Value("${httpInvoker.serializationType:}")
		protected String serializationType;

		@Bean
		public RemotingServiceRegistryPostProcessor remotingServiceRegistryPostProcessor() {
			RemotingServiceRegistryPostProcessor registryPostProcessor = new RemotingServiceRegistryPostProcessor();
			registryPostProcessor.setAnnotatedClasses(new Class[] { TestService.class, FooService.class });
			return registryPostProcessor;
		}

		@Bean
		public ServiceRegistry serviceRegistry() {
			return new StandaloneServiceRegistry();
		}

		@Bean
		public HttpInvokerServer httpInvokerServer() {
			return new HttpInvokerServer();
		}

		@Bean
		public HttpInvokerClient testService() {
			HttpInvokerClient httpInvokerClient = new HttpInvokerClient();
			httpInvokerClient.setServiceInterface(TestService.class);
			return httpInvokerClient;
		}

		@Bean
		public HttpInvokerClient fooService() {
			HttpInvokerClient httpInvokerClient = new HttpInvokerClient();
			httpInvokerClient.setServiceInterface(FooService.class);
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

	static class MockHttpInvokerRequestExecutor extends HttpInvokerRequestExecutor {

		@Autowired
		private HttpInvokerServer httpInvokerServer;

		@Value("${httpInvoker.serializationType:}")
		private void setSerializationType(String serializationType) {
			setSerializer(HttpInvokerSerializers.ofSerializationType(serializationType));
		}

		@Override
		protected RemoteInvocationResult doExecuteRequest(String serviceUrl, MethodInvocation methodInvocation,
				ByteArrayOutputStream baos) throws Exception {

			mockHttpServletRequest = spy(new MockHttpServletRequest());
			mockHttpServletResponse = spy(new MockHttpServletResponse());
			mockAsyncContext = spy(new MockAsyncContext(mockHttpServletRequest, mockHttpServletResponse));
			URI uri = URI.create(serviceUrl);
			mockHttpServletRequest.setServerName(uri.getHost());
			mockHttpServletRequest.setServerPort(uri.getPort());
			mockHttpServletRequest.setRequestURI(uri.getPath());
			mockHttpServletRequest.addHeader(HttpHeaders.CONTENT_TYPE, this.getSerializer().getContentType());
			mockHttpServletRequest.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, CodecUtils.nextId());
			mockHttpServletRequest.setContent(baos.toByteArray());
			mockHttpServletRequest.setAsyncSupported(true);
			doAnswer(i -> {
				mockHttpServletRequest.setAsyncStarted(true);
				return mockAsyncContext;
			}).when(mockHttpServletRequest).startAsync();

			httpInvokerServer.handleRequest(mockHttpServletRequest, mockHttpServletResponse);
			byte[] content;
			// wait async task finish
			while ((content = mockHttpServletResponse.getContentAsByteArray()).length == 0
					&& mockHttpServletRequest.isAsyncStarted()) {
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
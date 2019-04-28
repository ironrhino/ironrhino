package org.ironrhino.rest;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.ironrhino.core.spring.ExecutorServiceFactoryBean;
import org.ironrhino.core.spring.security.password.MixedPasswordEncoder;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.client.RestClientConfiguration.MyJsonValidator;
import org.ironrhino.rest.component.AuthorizeAspect;
import org.ironrhino.rest.component.RestExceptionHandler;
import org.ironrhino.sample.api.controller.UserController;
import org.ironrhino.security.service.UserManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@EnableWebSecurity
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class UserRestApiConfig implements WebMvcConfigurer {

	@Bean
	public MockMvc mockMvc(WebApplicationContext wac) {
		return MockMvcBuilders.webAppContextSetup(wac).build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return mock(UserDetailsService.class);
	}

	@Bean
	public UserManager userManager() {
		return mock(UserManager.class);
	}

	@Bean
	public ExecutorServiceFactoryBean executorService() {
		return new ExecutorServiceFactoryBean();
	}

	@Bean
	public UserController userController() {
		return new UserController();
	}

	@Bean
	public MixedPasswordEncoder passwordEncoder() {
		return new MixedPasswordEncoder();
	}

	@Bean
	public AuthorizeAspect authorizeAspect() {
		return new AuthorizeAspect();
	}

	@Bean
	public RestExceptionHandler restExceptionHandler() {
		return new RestExceptionHandler();
	}

	@Bean
	public MyJsonValidator myJsonValidator() {
		return new MyJsonValidator();
	}

	@Bean
	public DeferredResultProcessingInterceptor deferredResultProcessingInterceptor() {
		return mock(DeferredResultProcessingInterceptor.class);
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.registerDeferredResultInterceptors(deferredResultProcessingInterceptor());
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		MappingJackson2HttpMessageConverter jackson2 = new MappingJackson2HttpMessageConverter() {

			@Override
			protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
					throws IOException, HttpMessageNotWritableException {
				super.writeInternal(object, type, outputMessage);
				if (!(outputMessage instanceof ServerHttpResponse)
						|| outputMessage instanceof ServletServerHttpResponse) {
					// don't close MediaType.TEXT_EVENT_STREAM
					outputMessage.getBody().close();
				}
			}

		};
		jackson2.setObjectMapper(JsonUtils.createNewObjectMapper());
		converters.add(jackson2);
		StringHttpMessageConverter string = new StringHttpMessageConverter(StandardCharsets.UTF_8) {

			@Override
			protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage)
					throws IOException {
				try {
					return super.readInternal(clazz, inputMessage);
				} finally {
					inputMessage.getBody().close();
				}
			}

			@Override
			protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
				super.writeInternal(str, outputMessage);
				if (!(outputMessage instanceof ServerHttpResponse)
						|| outputMessage instanceof ServletServerHttpResponse) {
					// don't close MediaType.TEXT_EVENT_STREAM
					outputMessage.getBody().close();
				}
			}

		};
		string.setWriteAcceptCharset(false);
		converters.add(string);
	}

}

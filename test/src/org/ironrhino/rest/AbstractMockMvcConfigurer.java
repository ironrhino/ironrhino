package org.ironrhino.rest;

import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.ironrhino.core.servlet.MainAppInitializer;
import org.ironrhino.core.spring.converter.DateConverter;
import org.ironrhino.core.spring.converter.LocalDateConverter;
import org.ironrhino.core.spring.converter.LocalDateTimeConverter;
import org.ironrhino.core.spring.converter.LocalTimeConverter;
import org.ironrhino.core.spring.converter.MonthDayConverter;
import org.ironrhino.core.spring.converter.OffsetDateTimeConverter;
import org.ironrhino.core.spring.converter.YearMonthConverter;
import org.ironrhino.core.spring.converter.ZonedDateTimeConverter;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.component.RestExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public abstract class AbstractMockMvcConfigurer implements WebMvcConfigurer {

	@Bean
	public MockMvc mockMvc(WebApplicationContext wac) {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		MainAppInitializer.SERVLET_CONTEXT = mockMvc.getDispatcherServlet().getServletContext();
		return mockMvc;
	}

	@Bean
	public DeferredResultProcessingInterceptor deferredResultProcessingInterceptor() {
		return mock(DeferredResultProcessingInterceptor.class);
	}

	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.registerDeferredResultInterceptors(deferredResultProcessingInterceptor());
	}

	@Bean
	public RestExceptionHandler restExceptionHandler() {
		return new RestExceptionHandler();
	}

	@Override
	public void addFormatters(FormatterRegistry formatterRegistry) {
		formatterRegistry.addConverter(new DateConverter());
		formatterRegistry.addConverter(new LocalDateConverter());
		formatterRegistry.addConverter(new LocalDateTimeConverter());
		formatterRegistry.addConverter(new LocalTimeConverter());
		formatterRegistry.addConverter(new ZonedDateTimeConverter());
		formatterRegistry.addConverter(new OffsetDateTimeConverter());
		formatterRegistry.addConverter(new YearMonthConverter());
		formatterRegistry.addConverter(new MonthDayConverter());
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		MappingJackson2HttpMessageConverter jackson2 = new MappingJackson2HttpMessageConverter();
		jackson2.setObjectMapper(JsonUtils.createNewObjectMapper());
		converters.add(jackson2);
		StringHttpMessageConverter string = new StringHttpMessageConverter(StandardCharsets.UTF_8);
		string.setWriteAcceptCharset(false);
		converters.add(string);
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());
	}
}

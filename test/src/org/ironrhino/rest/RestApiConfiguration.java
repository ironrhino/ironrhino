package org.ironrhino.rest;

import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.ironrhino.core.spring.converter.DateConverter;
import org.ironrhino.core.spring.converter.LocalDateConverter;
import org.ironrhino.core.spring.converter.LocalDateTimeConverter;
import org.ironrhino.core.spring.converter.LocalTimeConverter;
import org.ironrhino.core.spring.converter.YearMonthConverter;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.component.RestExceptionHandler;
import org.ironrhino.sample.api.controller.ArticleController;
import org.ironrhino.sample.api.controller.UploadController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class RestApiConfiguration implements WebMvcConfigurer {

	@Bean
	public ArticleController articleController() {
		return spy(new ArticleController());
	}

	@Bean
	public UploadController uploadController() {
		return new UploadController();
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
		formatterRegistry.addConverter(new YearMonthConverter());
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

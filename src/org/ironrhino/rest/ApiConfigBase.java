package org.ironrhino.rest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.freemarker.FreemarkerConfigurer;
import org.ironrhino.core.metrics.MicrometerPresent;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.spring.converter.DateConverter;
import org.ironrhino.core.spring.converter.LocalDateConverter;
import org.ironrhino.core.spring.converter.LocalDateTimeConverter;
import org.ironrhino.core.spring.converter.LocalTimeConverter;
import org.ironrhino.core.spring.converter.MonthDayConverter;
import org.ironrhino.core.spring.converter.OffsetDateTimeConverter;
import org.ironrhino.core.spring.converter.YearMonthConverter;
import org.ironrhino.core.spring.converter.ZonedDateTimeConverter;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.component.AuthorizeInstrument;
import org.ironrhino.rest.component.MetricsInstrument;
import org.ironrhino.rest.component.RestExceptionHandler;
import org.ironrhino.rest.component.TracingInstrument;
import org.ironrhino.rest.doc.ApiDocInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@EnableAspectJAutoProxy(proxyTargetClass = true)
public abstract class ApiConfigBase extends WebMvcConfigurationSupport {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private SpringValidatorAdapter validator;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@PostConstruct
	private void init() {
		Class<?> clazz = ReflectionUtils.getActualClass(getClass());
		if (clazz.isAnnotationPresent(PropertySources.class) || clazz.isAnnotationPresent(EnableAspectJAutoProxy.class)
				|| clazz.isAnnotationPresent(ControllerAdvice.class)) {
			logger.warn(
					"It seems you are running with legacy code, please remove @PropertySources and @EnableAspectJAutoProxy and @ControllerAdvice from "
							+ clazz.getName());
		}
		Map<String, ? extends ServletRegistration> map = servletContext.getServletRegistrations();
		for (ServletRegistration sr : map.values()) {
			if (ReflectionUtils.getActualClass(this).getName()
					.equals(sr.getInitParameter(ContextLoader.CONFIG_LOCATION_PARAM))) {
				String mapping = getServletMapping();
				if (!sr.getMappings().contains(mapping))
					sr.addMapping(mapping);
				break;
			}
		}
	}

	protected String getServletMapping() {
		String version = getVersion();
		return "/api" + (StringUtils.isNotBlank(version) ? "/" + version : "") + "/*";
	}

	public String getVersion() {
		return "";
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = JsonUtils.createNewObjectMapper();
		return objectMapper;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setUseSuffixPatternMatch(false);
		configurer.setUseTrailingSlashMatch(false);
	}

	@Override
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
		configurer.useRegisteredExtensionsOnly(true);
		configurer.replaceMediaTypes(Collections.emptyMap());
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		MappingJackson2HttpMessageConverter jackson2 = new MappingJackson2HttpMessageConverter() {

			@Override
			public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException {
				try {
					return super.read(type, contextClass, inputMessage);
				} finally {
					inputMessage.getBody().close();
					// for LoggingBodyHttpServletRequest.ContentCachingInputStream.close()
				}
			}

			@Override
			public Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
				try {
					return super.readInternal(clazz, inputMessage);
				} finally {
					inputMessage.getBody().close();
					// for LoggingBodyHttpServletRequest.ContentCachingInputStream.close()
				}
			}
		};
		jackson2.setObjectMapper(objectMapper());
		converters.add(jackson2);
		StringHttpMessageConverter string = new StringHttpMessageConverter(StandardCharsets.UTF_8) {

			@Override
			protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage)
					throws IOException {
				try {
					return super.readInternal(clazz, inputMessage);
				} finally {
					inputMessage.getBody().close();
					// for LoggingBodyHttpServletRequest.ContentCachingInputStream.close()
				}
			}
		};
		string.setWriteAcceptCharset(false);
		converters.add(string);
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());
	}

	@Override
	protected Map<String, MediaType> getDefaultMediaTypes() {
		Map<String, MediaType> map = new HashMap<>();
		map.put("json", MediaType.APPLICATION_JSON);
		map.put("txt", MediaType.TEXT_PLAIN);
		return map;
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
	protected void configureViewResolvers(ViewResolverRegistry registry) {
		registry.freeMarker();
	}

	@Override
	protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setDefaultTimeout(30000);
		configurer.setTaskExecutor(taskExecutor);
	}

	@Bean
	public FreeMarkerViewResolver freeMarkerViewResolver() {
		FreeMarkerViewResolver freeMarkerViewResolver = new FreeMarkerViewResolver();
		freeMarkerViewResolver.setContentType("text/html; charset=UTF-8");
		freeMarkerViewResolver.setSuffix(".ftl");
		return freeMarkerViewResolver;
	}

	@Bean
	public FreeMarkerConfig freeMarkerConfig(FreemarkerConfigurer freemarkerConfigurer) {
		FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer() {
			@Override
			protected Configuration newConfiguration() throws IOException, TemplateException {
				return freemarkerConfigurer.createConfiguration();
			}

		};
		freeMarkerConfigurer.setTemplateLoaderPath("classpath:/resources/view");
		freeMarkerConfigurer.setDefaultEncoding("UTF-8");
		return freeMarkerConfigurer;
	}

	@Bean
	public MultipartResolver multipartResolver() {
		StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
		return multipartResolver;
	}

	@Bean
	protected RestExceptionHandler restExceptionHandler() {
		return new RestExceptionHandler();
	}

	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
		if (ClassUtils.isPresent("org.ironrhino.security.oauth.server.component.OAuthAuthorizationArgumentResolver",
				null)) {
			argumentResolvers
					.add(new org.ironrhino.security.oauth.server.component.OAuthAuthorizationArgumentResolver());
		}
		super.addArgumentResolvers(argumentResolvers);
	}

	@Override
	public Validator getValidator() {
		return this.validator;
	}

	@Bean
	protected MethodValidationPostProcessor methodValidationPostProcessor() {
		MethodValidationPostProcessor mvpp = new MethodValidationPostProcessor();
		mvpp.setProxyTargetClass(true);
		mvpp.setValidator(this.validator);
		return mvpp;
	}

	@Bean
	protected AuthorizeInstrument authorizeInstrument() {
		return new AuthorizeInstrument();
	}

	@Bean
	@MicrometerPresent
	protected MetricsInstrument metricsInstrument() {
		return new MetricsInstrument(getServletMapping());
	}

	@Bean
	@ClassPresentConditional("io.opentracing.Tracer")
	protected TracingInstrument tracingInstrument() {
		return new TracingInstrument(getServletMapping());
	}

	@Bean
	public ApiDocInspector apiDocInspector() {
		return new ApiDocInspector();
	}

	@Bean
	@ClassPresentConditional("org.ironrhino.core.remoting.ServiceRegistry")
	protected ApiRegistrant apiRegistrant() {
		return new ApiRegistrant(getServletMapping());
	}

}
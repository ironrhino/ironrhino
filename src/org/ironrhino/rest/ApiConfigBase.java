package org.ironrhino.rest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.freemarker.FreemarkerConfigurer;
import org.ironrhino.core.spring.converter.DateConverter;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.rest.component.AuthorizeAspect;
import org.ironrhino.rest.component.JsonpAdvice;
import org.ironrhino.rest.component.RestExceptionHandler;
import org.ironrhino.rest.doc.ApiDocInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
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

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private SpringValidatorAdapter validator;

	@PostConstruct
	private void init() {
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

	@Override
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
		configurer.favorPathExtension(false);
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		MappingJackson2HttpMessageConverter jackson2 = new MappingJackson2HttpMessageConverter() {

			@Override
			protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
					throws IOException, HttpMessageNotWritableException {
				super.writeInternal(object, type, outputMessage);
				outputMessage.getBody().close();
			}

		};
		jackson2.setObjectMapper(objectMapper());
		converters.add(jackson2);
		StringHttpMessageConverter string = new StringHttpMessageConverter(StandardCharsets.UTF_8) {

			@Override
			protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
				super.writeInternal(str, outputMessage);
				outputMessage.getBody().close();
			}

		};
		string.setWriteAcceptCharset(false);
		converters.add(string);
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
	}

	@Override
	protected void configureViewResolvers(ViewResolverRegistry registry) {
		registry.freeMarker();
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
	protected BeanPostProcessor methodValidationPostProcessor() {
		return new MethodValidationPostProcessor();
	}

	@Bean
	protected JsonpAdvice jsonpAdvice() {
		return new JsonpAdvice();
	}

	@Bean
	protected AuthorizeAspect authorizeAspect() {
		return new AuthorizeAspect();
	}

	@Bean
	public ApiDocInspector apiDocInspector() {
		return new ApiDocInspector();
	}

}
package org.ironrhino.security.oauth.server.controller;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Order(0)
@Configuration
@ComponentScan
public class OAuth2Config extends WebMvcConfigurationSupport {

	@Override
	protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
		configurer.favorPathExtension(false);
	}

	@Bean
	protected OAuth2ExceptionHandler exceptionHandler() {
		return new OAuth2ExceptionHandler();
	}

	@Override
	protected void addFormatters(FormatterRegistry registry) {
		registry.addConverter(new Converter<String, GrantType>() {

			@Override
			public GrantType convert(String input) {
				if (StringUtils.isBlank(input))
					return null;
				try {
					return GrantType.valueOf(input);
				} catch (IllegalArgumentException e) {
					if (input.equals(GrantType.JWT_BEARER))
						return GrantType.jwt_bearer;
					throw e;
				}
			}

		});
	}

}
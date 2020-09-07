package org.ironrhino.rest.client;

import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RootUriTemplateHandler implements UriTemplateHandler {

	private final Supplier<String> rootUriSupplier;

	private final UriTemplateHandler handler;

	public RootUriTemplateHandler(Supplier<String> rootUriSupplier) {
		this(rootUriSupplier, new DefaultUriBuilderFactory());
	}

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		return this.handler.expand(apply(uriTemplate), uriVariables);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		return this.handler.expand(apply(uriTemplate), uriVariables);
	}

	private String apply(String uriTemplate) {
		return uriTemplate.startsWith("/") ? rootUriSupplier.get() + uriTemplate : uriTemplate;
	}

}

package org.ironrhino.rest.client;

import org.apache.http.client.methods.HttpUriRequest;

public class HttpComponentsClientHttpRequestFactory
		extends org.ironrhino.core.spring.http.client.HttpComponentsClientHttpRequestFactory {

	private final RestClient client;

	public HttpComponentsClientHttpRequestFactory(RestClient client) {
		super();
		this.client = client;
	}

	public HttpComponentsClientHttpRequestFactory(RestClient client, boolean trustAllHosts) {
		super(trustAllHosts);
		this.client = client;
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		if (client != null)
			request.addHeader("Authorization", client.getAuthorizationHeader());
		super.postProcessHttpRequest(request);
	}

}

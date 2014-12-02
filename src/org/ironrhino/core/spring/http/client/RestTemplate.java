package org.ironrhino.core.spring.http.client;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;

@Component
public class RestTemplate extends org.springframework.web.client.RestTemplate {

	@Value("${restTemplate.connectTimeout:5000}")
	private int connectTimeout;

	@Value("${restTemplate.readTimeout:5000}")
	private int readTimeout;

	public RestTemplate() {
		super();
		setRequestFactory(new SimpleClientHttpRequestFactory());
	}

	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		super();
		setRequestFactory(requestFactory);
	}

	@PostConstruct
	public void init() {
		ClientHttpRequestFactory chrf = getRequestFactory();
		if (chrf instanceof org.springframework.http.client.SimpleClientHttpRequestFactory) {
			org.springframework.http.client.SimpleClientHttpRequestFactory scrf = (org.springframework.http.client.SimpleClientHttpRequestFactory) chrf;
			scrf.setConnectTimeout(connectTimeout);
			scrf.setReadTimeout(readTimeout);
		} else if (chrf instanceof org.springframework.http.client.HttpComponentsClientHttpRequestFactory) {
			org.springframework.http.client.HttpComponentsClientHttpRequestFactory hccrf = (org.springframework.http.client.HttpComponentsClientHttpRequestFactory) chrf;
			hccrf.setConnectTimeout(connectTimeout);
			hccrf.setReadTimeout(readTimeout);
		}
	}
}

package org.ironrhino.core.spring.http.client;

import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
public class RestTemplate extends org.springframework.web.client.RestTemplate {

	@Getter
	@Value("${restTemplate.connectTimeout:5000}")
	private int connectTimeout;

	@Getter
	@Value("${restTemplate.readTimeout:10000}")
	private int readTimeout;

	@Getter
	@Setter
	@Value("${restTemplate.trustAllHosts:false}")
	private boolean trustAllHosts;

	public RestTemplate() {
		super();
		setRequestFactory(new HttpComponentsClientHttpRequestFactory(trustAllHosts));
	}

	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		super();
		setRequestFactory(requestFactory);
	}

	@PostConstruct
	public void init() {
		Iterator<HttpMessageConverter<?>> it = getMessageConverters().iterator();
		while (it.hasNext()) {
			HttpMessageConverter<?> mc = it.next();
			if (mc instanceof MappingJackson2XmlHttpMessageConverter)
				it.remove();
			else if (mc instanceof MappingJackson2HttpMessageConverter)
				((MappingJackson2HttpMessageConverter) mc).setObjectMapper(JsonUtils.createNewObjectMapper());
		}
		setConnectTimeout(connectTimeout);
		setReadTimeout(readTimeout);
	}

	public void setConnectTimeout(int connectTimeout) {
		ClientHttpRequestFactory chrf = getRequestFactory();
		if (chrf instanceof org.springframework.http.client.SimpleClientHttpRequestFactory) {
			org.springframework.http.client.SimpleClientHttpRequestFactory scrf = (org.springframework.http.client.SimpleClientHttpRequestFactory) chrf;
			scrf.setConnectTimeout(connectTimeout);
		} else if (chrf instanceof org.springframework.http.client.HttpComponentsClientHttpRequestFactory) {
			org.springframework.http.client.HttpComponentsClientHttpRequestFactory hccrf = (org.springframework.http.client.HttpComponentsClientHttpRequestFactory) chrf;
			hccrf.setConnectTimeout(connectTimeout);
		}
	}

	public void setReadTimeout(int readTimeout) {
		ClientHttpRequestFactory chrf = getRequestFactory();
		if (chrf instanceof org.springframework.http.client.SimpleClientHttpRequestFactory) {
			org.springframework.http.client.SimpleClientHttpRequestFactory scrf = (org.springframework.http.client.SimpleClientHttpRequestFactory) chrf;
			scrf.setReadTimeout(readTimeout);
		} else if (chrf instanceof org.springframework.http.client.HttpComponentsClientHttpRequestFactory) {
			org.springframework.http.client.HttpComponentsClientHttpRequestFactory hccrf = (org.springframework.http.client.HttpComponentsClientHttpRequestFactory) chrf;
			hccrf.setReadTimeout(readTimeout);
		}
	}
}

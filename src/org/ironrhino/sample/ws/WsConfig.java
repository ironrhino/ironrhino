package org.ironrhino.sample.ws;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.jaxws.SimpleJaxWsServiceExporter;

@Configuration
@ComponentScan
public class WsConfig {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${ws.baseAddress:}")
	private String baseAddress;

	@Bean
	public SimpleJaxWsServiceExporter simpleJaxWsServiceExporter() {
		SimpleJaxWsServiceExporter exporter = new SimpleJaxWsServiceExporter();
		if (StringUtils.isBlank(baseAddress))
			baseAddress = new StringBuilder("http://").append(AppInfo.getHostAddress()).append(":8081/").toString();
		logger.info("set ws.baseAddress: {}", baseAddress);
		exporter.setBaseAddress(baseAddress);
		return exporter;
	}

}
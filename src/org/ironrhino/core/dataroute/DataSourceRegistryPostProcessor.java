package org.ironrhino.core.dataroute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.Assert;

public class DataSourceRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	public static final String KEY_SHARDING_HOSTS = "routingDataSource.shardingHosts";

	public static final String KEY_SHARDINGS_PER_HOST = "routingDataSource.shardingsPerHost";

	public static final String KEY_SHARDING_VERSION = "routingDataSource.shardingGeneration";

	public static final String KEY_JDBC_URL_FORMAT = "routingDataSource.jdbcUrlFormat";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected String routingDataSourceName = "dataSource";

	protected String defaultRouterName;

	protected String shardingParentName = "abstractDataSource";

	protected String shardingNamePrefix;

	protected List<String> shardingHosts;

	protected int shardingsPerHost;

	protected int shardingGeneration;

	protected String jdbcUrlFormat;

	public void setRoutingDataSourceName(String routingDataSourceName) {
		this.routingDataSourceName = routingDataSourceName;
	}

	public void setDefaultRouterName(String defaultRouterName) {
		this.defaultRouterName = defaultRouterName;
	}

	public void setShardingParentName(String shardingParentName) {
		this.shardingParentName = shardingParentName;
	}

	public void setShardingNamePrefix(String shardingNamePrefix) {
		this.shardingNamePrefix = shardingNamePrefix;
	}

	public void setShardingHosts(List<String> shardingHosts) {
		this.shardingHosts = shardingHosts;
	}

	public void setShardingsPerHost(int shardingsPerHost) {
		this.shardingsPerHost = shardingsPerHost;
	}

	public void setShardingGeneration(int shardingGeneration) {
		this.shardingGeneration = shardingGeneration;
	}

	public void setJdbcUrlFormat(String jdbcUrlFormat) {
		this.jdbcUrlFormat = jdbcUrlFormat;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Assert.hasText(routingDataSourceName);
		Assert.hasText(shardingParentName);
		Assert.hasText(shardingNamePrefix);
		String str = AppInfo.getApplicationContextProperties().getProperty(KEY_SHARDING_HOSTS);
		if (str != null)
			shardingHosts = parseHosts(str);
		Assert.notEmpty(shardingHosts);
		str = AppInfo.getApplicationContextProperties().getProperty(KEY_SHARDINGS_PER_HOST);
		if (str != null)
			shardingsPerHost = Integer.valueOf(str);
		if (shardingsPerHost <= 0)
			shardingsPerHost = 1;
		str = AppInfo.getApplicationContextProperties().getProperty(KEY_SHARDING_VERSION);
		if (str != null)
			shardingGeneration = Integer.valueOf(str);
		str = AppInfo.getApplicationContextProperties().getProperty(KEY_JDBC_URL_FORMAT);
		if (str != null)
			jdbcUrlFormat = str;
		Assert.hasText(jdbcUrlFormat);
		Map<String, String> jdbcUrls = buildJdbcUrls();
		StringBuilder dataSourceMapping = new StringBuilder();
		for (Map.Entry<String, String> entry : jdbcUrls.entrySet()) {
			String beanName = entry.getKey();
			String jdbcUrl = entry.getValue();
			ChildBeanDefinition beanDefinition = new ChildBeanDefinition(shardingParentName);
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			String url = AppInfo.getApplicationContextProperties().getProperty(beanName + ".jdbc.url");
			if (url != null)
				jdbcUrl = url;
			propertyValues.addPropertyValue("jdbcUrl", jdbcUrl);
			dataSourceMapping.append(beanName).append("\t= ").append(jdbcUrl).append("\n");
			String username = AppInfo.getApplicationContextProperties().getProperty(beanName + ".jdbc.username");
			if (username != null)
				propertyValues.addPropertyValue("username", username);
			String password = AppInfo.getApplicationContextProperties().getProperty(beanName + ".jdbc.password");
			if (password != null)
				propertyValues.addPropertyValue("password", password);
			beanDefinition.setPropertyValues(propertyValues);
			registry.registerBeanDefinition(beanName, beanDefinition);
		}
		logger.info("Register dataSources:\n\n{}", dataSourceMapping.toString());
		RootBeanDefinition beanDefinition = new RootBeanDefinition(RoutingDataSource.class);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		if (StringUtils.isNotBlank(defaultRouterName))
			propertyValues.addPropertyValue("defaultRouter", new RuntimeBeanReference(defaultRouterName));
		propertyValues.addPropertyValue("shardingNames", jdbcUrls.keySet());
		beanDefinition.setPropertyValues(propertyValues);
		registry.registerBeanDefinition(routingDataSourceName, beanDefinition);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	protected Map<String, String> buildJdbcUrls() {
		Map<String, String> map = new LinkedHashMap<>();
		List<Integer> seq = seq(shardingHosts.size(), shardingsPerHost, shardingGeneration);
		for (int hostIndex = 0; hostIndex < shardingHosts.size(); hostIndex++) {
			for (int databaseIndex = 0; databaseIndex < shardingsPerHost; databaseIndex++) {
				int instanceIndex = hostIndex * shardingsPerHost + databaseIndex;
				String beanName = shardingNamePrefix + (instanceIndex + 1);
				String databaseName = beanName;
				if (shardingsPerHost == 1 && shardingGeneration <= 1)
					databaseName = shardingNamePrefix;
				String host = shardingHosts.get(seq.indexOf(instanceIndex) / shardingsPerHost);
				String jdbcUrl = buildJdbcUrl(host, databaseName, databaseIndex);
				map.put(beanName, jdbcUrl);
			}
		}
		return map;
	}

	protected String buildJdbcUrl(String host, String databaseName, int databaseIndex) {
		return String.format(jdbcUrlFormat, host, databaseName);
	}

	protected static List<String> parseHosts(String hosts) {
		LinkedHashSet<String> set = new LinkedHashSet<>();
		String[] arr = hosts.split("\\s*,\\s*");
		for (String s : arr) {
			int i = s.indexOf('-');
			if (i > 0) {
				String beginHost = s.substring(0, i);
				String begin = beginHost.substring(beginHost.lastIndexOf('.') + 1);
				String prefix = beginHost.substring(0, beginHost.lastIndexOf('.') + 1);
				String end = s.substring(i + 1);
				if (end.indexOf('.') > 0)
					end = end.substring(end.lastIndexOf('.') + 1);
				int low = Integer.parseInt(begin);
				int high = Integer.parseInt(end);
				for (int j = low; j <= high; j++)
					set.add(prefix + j);
			} else {
				set.add(s);
			}
		}
		return new ArrayList<>(set);
	}

	protected static List<Integer> seq(int shardingHosts, int shardingsPerHost, int shardingGeneration) {
		if (shardingGeneration <= 1) {
			List<Integer> seq = new ArrayList<>();
			for (int i = 0; i < shardingHosts * shardingsPerHost; i++)
				seq.add(i);
			return seq;
		} else {
			if (shardingHosts % 2 != 0)
				throw new IllegalArgumentException("hosts should be even number but is " + shardingHosts);
			int lastShardingHosts = shardingHosts / 2;
			int lastShardingsPerHost = shardingsPerHost * 2;
			int lastShardingGeneration = shardingGeneration - 1;
			List<Integer> lastSeq = seq(lastShardingHosts, lastShardingsPerHost, lastShardingGeneration);
			List<Integer> seq = new ArrayList<>(lastSeq.size());
			for (int i = 0; i < lastShardingHosts; i++) {
				for (int j = 0; j < shardingsPerHost; j++) {
					seq.add(lastSeq.get(i * lastShardingsPerHost) + j);
				}
			}
			for (int i = 0; i < lastShardingHosts; i++) {
				for (int j = shardingsPerHost; j < shardingsPerHost * 2; j++) {
					seq.add(lastSeq.get(i * lastShardingsPerHost) + j);
				}
			}
			return seq;
		}
	}

}
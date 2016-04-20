package org.ironrhino.core.dataroute;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
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

	public static final String KEY_JDBC_URL_FORMAT = "routingDataSource.jdbcUrlFormat";

	private String routingDataSourceName = "dataSource";

	private String defaultRouterName;

	private String shardingParentName = "abstractDataSource";

	private String shardingNamePrefix;

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

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Assert.hasText(routingDataSourceName);
		Assert.hasText(shardingParentName);
		Assert.hasText(shardingNamePrefix);
		List<String> shardingHosts = getShardingHosts();
		int index = 1;
		List<String> shardingNames = new ArrayList<>();
		int shardingsPerHost = getShardingsPerHost();
		String jdbcUrlFormat = getJdbcUrlFormat();
		for (String host : shardingHosts) {
			for (int i = 0; i < shardingsPerHost; i++) {
				String beanName = shardingNamePrefix + (index);
				shardingNames.add(beanName);
				String databaseName = shardingNamePrefix;
				if (shardingsPerHost > 1)
					databaseName += (i + 1);
				ChildBeanDefinition beanDefinition = new ChildBeanDefinition(shardingParentName);
				MutablePropertyValues propertyValues = new MutablePropertyValues();
				propertyValues.addPropertyValue("jdbcUrl",
						"${" + beanName + ".jdbc.url:" + String.format(jdbcUrlFormat, host, databaseName) + "}");
				String username = AppInfo.getApplicationContextProperties().getProperty(beanName + ".jdbc.username");
				if (username != null)
					propertyValues.addPropertyValue("username", username);
				String password = AppInfo.getApplicationContextProperties().getProperty(beanName + ".jdbc.password");
				if (password != null)
					propertyValues.addPropertyValue("password", password);
				beanDefinition.setPropertyValues(propertyValues);
				registry.registerBeanDefinition(beanName, beanDefinition);
				index++;
			}
		}
		RootBeanDefinition beanDefinition = new RootBeanDefinition(RoutingDataSource.class);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		if (StringUtils.isNotBlank(defaultRouterName))
			propertyValues.addPropertyValue("defaultRouter", new RuntimeBeanReference(defaultRouterName));
		propertyValues.addPropertyValue("shardingNames", shardingNames);
		beanDefinition.setPropertyValues(propertyValues);
		registry.registerBeanDefinition(routingDataSourceName, beanDefinition);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

	public List<String> getShardingHosts() {
		String str = AppInfo.getApplicationContextProperties().getProperty(KEY_SHARDING_HOSTS, "localhost");
		return parseHosts(str);
	}

	public int getShardingsPerHost() {
		String str = AppInfo.getApplicationContextProperties().getProperty(KEY_SHARDINGS_PER_HOST, "4");
		return Integer.valueOf(str);
	}

	public String getJdbcUrlFormat() {
		return AppInfo.getApplicationContextProperties().getProperty(KEY_JDBC_URL_FORMAT,
				"jdbc:mysql://%s/%s?autoReconnectForPools=true&useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true&useSSL=false");
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

	public static void main(String[] args) {
		System.out.println(String.format("this is a %s %s test", "A", "B"));
	}

}
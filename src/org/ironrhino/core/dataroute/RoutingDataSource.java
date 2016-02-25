package org.ironrhino.core.dataroute;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.Assert;

import com.google.common.hash.Hashing;

public class RoutingDataSource extends AbstractDataSource implements InitializingBean, BeanFactoryAware {

	protected BeanFactory beanFactory;

	protected DataSource defaultNode;

	protected Map<String, DataSource> shardings;

	protected String defaultName;

	protected List<String> shardingNames;

	public void setDefaultName(String defaultName) {
		this.defaultName = defaultName;
	}

	public void setShardingNames(List<String> shardings) {
		this.shardingNames = shardings;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(shardingNames);
		shardings = new LinkedHashMap<>();
		for (String sharding : shardingNames)
			shardings.put(sharding, beanFactory.getBean(sharding, DataSource.class));
		if (defaultName != null)
			defaultNode = beanFactory.getBean(defaultName, DataSource.class);
		else
			defaultNode = beanFactory.getBean(shardingNames.get(0), DataSource.class);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		DataSource ds = null;
		String routingKey = DataRouteContext.getRoutingKey();
		String nodeName = DataRouteContext.getNodeName();
		if (routingKey != null) {
			ds = shardings.get(route(routingKey));
		} else if (nodeName != null) {
			ds = shardings.get(nodeName);
			if (ds == null)
				ds = beanFactory.getBean(nodeName, DataSource.class);
			if (ds == null)
				throw new IllegalArgumentException("dataSource '" + nodeName + "' not found");
		} else {
			ds = defaultNode;
		}
		return (username == null) ? ds.getConnection() : ds.getConnection(username, password);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(null, null);
	}

	public String route(String routingKey) {
		int i = Hashing.consistentHash(Hashing.murmur3_32().hashString(routingKey, Charset.defaultCharset()),
				shardingNames.size());
		return shardingNames.get(i);
	}

}
package org.ironrhino.core.dataroute;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.Assert;

import com.google.common.hash.Hashing;

public class RoutingDataSource extends AbstractDataSource implements InitializingBean, BeanFactoryAware {

	protected BeanFactory beanFactory;

	protected DataSource defaultNode;

	protected List<DataSource> shardings;

	protected String defaultName;

	protected List<String> shardingNames;

	protected Router defaultRouter;

	@Autowired(required = false)
	protected Map<String, Router> routers;

	public Router getDefaultRouter() {
		return defaultRouter;
	}

	public void setDefaultRouter(Router defaultRouter) {
		this.defaultRouter = defaultRouter;
	}

	public Map<String, Router> getRouters() {
		return routers;
	}

	public List<DataSource> getShardings() {
		return shardings;
	}

	public void setDefaultName(String defaultName) {
		this.defaultName = defaultName;
	}

	public List<String> getShardingNames() {
		return shardingNames;
	}

	public void setShardingNames(List<String> shardingNames) {
		this.shardingNames = shardingNames;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(shardingNames);
		Assert.isTrue(shardingNames.size() > 1, "shardings should more than 1");
		Set<String> set = new HashSet<>();
		set.addAll(shardingNames);
		Assert.isTrue(set.size() == shardingNames.size(), "sharding should not be duplicated");
		shardings = new ArrayList<>(shardingNames.size());
		for (String sharding : shardingNames)
			shardings.add(beanFactory.getBean(sharding, DataSource.class));
		if (defaultName != null)
			defaultNode = beanFactory.getBean(defaultName, DataSource.class);
		else
			defaultNode = beanFactory.getBean(shardingNames.get(0), DataSource.class);
		if (defaultRouter == null)
			defaultRouter = new DefaultRouter();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		DataSource ds = null;
		Object routingKey = DataRouteContext.getRoutingKey();
		String nodeName = DataRouteContext.getNodeName();
		if (routingKey != null) {
			Router router = defaultRouter;
			String routerName = DataRouteContext.getRouterName();
			if (routerName != null) {
				if (routers == null)
					throw new IllegalArgumentException("no routers found");
				router = routers.get(routerName);
				if (router == null)
					throw new IllegalArgumentException("router '" + routerName + "' not found");
			}
			ds = shardings.get(router.route(shardingNames, routingKey));
		} else if (nodeName != null) {
			int index = shardingNames.indexOf(nodeName);
			if (index > -1)
				ds = shardings.get(index);
			else
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

	private static class DefaultRouter implements Router {

		@Override
		public int route(List<String> nodes, Object routingKey) {
			int i = Hashing.consistentHash(
					Hashing.murmur3_32().hashString(routingKey.toString(), Charset.defaultCharset()), nodes.size());
			return i;
		}

	}

}
package org.ironrhino.core.dataroute;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.Assert;

import com.google.common.hash.Hashing;

public class RoutingDataSource extends AbstractDataSource implements InitializingBean {

	private DataSource defaultDataSource;

	private Map<String, DataSource> routingMap;

	private String[] nodeNames;

	public void setDefaultDataSource(DataSource defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	public void setNodes(List<GroupedDataSource> nodes) {
		routingMap = new LinkedHashMap<>();
		for (GroupedDataSource gds : nodes)
			routingMap.put(gds.getGroupName(), gds);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(routingMap);
		Assert.notEmpty(routingMap);
		nodeNames = routingMap.keySet().toArray(new String[0]);
		if (defaultDataSource == null)
			defaultDataSource = routingMap.values().iterator().next();
		Assert.notNull(defaultDataSource);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		DataSource ds = null;
		String routingKey = DataRouteContext.getRoutingKey();
		String nodeName = DataRouteContext.getNodeName();
		if (routingKey != null) {
			ds = routingMap.get(route(routingKey));
		} else if (nodeName != null) {
			ds = routingMap.get(nodeName);
			if (ds == null)
				throw new IllegalArgumentException("dataSource '" + nodeName + "' not found");
		} else {
			ds = defaultDataSource;
		}
		return ds.getConnection(username, password);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(null, null);
	}

	public void check() {
		if (defaultDataSource instanceof GroupedDataSource)
			((GroupedDataSource) defaultDataSource).checkDeadDataSources();
		for (DataSource ds : routingMap.values())
			if (ds instanceof GroupedDataSource)
				((GroupedDataSource) ds).checkDeadDataSources();
	}

	protected String route(String routingKey) {
		int i = Hashing.consistentHash(Hashing.murmur3_32().hashString(routingKey, Charset.defaultCharset()),
				nodeNames.length);
		return nodeNames[i];
	}

}
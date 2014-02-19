package org.ironrhino.core.dataroute;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDataSource;

public class RoutingDataSource extends AbstractDataSource {

	private DataSource mainGroup;

	private Map<String, DataSource> routingMap;

	public DataSource getMainGroup() {
		if (mainGroup == null) {
			mainGroup = routingMap.values().iterator().next();
		}
		return mainGroup;
	}

	// inject DataSource with groupName
	public void setRoutingMap(Map<String, DataSource> map) {
		if (routingMap != null)
			throw new IllegalArgumentException("already injected by groupList");
		this.routingMap = map;
	}

	// inject GroupedDataSource included groupName
	public void setGroups(List<GroupedDataSource> list) {
		if (routingMap != null)
			throw new IllegalArgumentException("already injected by routingMap");
		routingMap = new LinkedHashMap<String, DataSource>();
		for (GroupedDataSource gds : list)
			routingMap.put(gds.getGroupName(), gds);
		mainGroup = list.get(0);
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		DataSource ds = null;
		String groupName = DataRouteContext.getName();
		if (groupName != null) {
			ds = routingMap.get(groupName);
		} else {
			ds = getMainGroup();
		}
		if (ds == null)
			throw new IllegalArgumentException("group name '" + groupName
					+ "' not found");
		return ds.getConnection(username, password);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(null, null);
	}

	public void check() {
		if (mainGroup instanceof GroupedDataSource)
			((GroupedDataSource) mainGroup).checkDeadDataSources();
		for (DataSource ds : routingMap.values())
			if (ds instanceof GroupedDataSource)
				((GroupedDataSource) ds).checkDeadDataSources();
	}

}
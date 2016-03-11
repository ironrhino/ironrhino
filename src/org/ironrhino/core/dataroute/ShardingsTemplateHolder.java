package org.ironrhino.core.dataroute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class ShardingsTemplateHolder implements InitializingBean {

	protected RoutingDataSource routingDataSource;

	protected List<TemplateHolder> templateHolders;

	public ShardingsTemplateHolder(DataSource dataSource) {
		if (!(dataSource instanceof RoutingDataSource))
			throw new IllegalArgumentException("dataSource should be RoutingDataSource");
		this.routingDataSource = (RoutingDataSource) dataSource;
	}

	@Override
	public void afterPropertiesSet() {
		templateHolders = new ArrayList<>();
		for (DataSource ds : routingDataSource.getShardings()) {
			JdbcTemplate jt = new JdbcTemplate(ds);
			jt.afterPropertiesSet();
			NamedParameterJdbcTemplate njt = new NamedParameterJdbcTemplate(jt);
			DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
			tm.afterPropertiesSet();
			TransactionTemplate tt = new TransactionTemplate(tm);
			tt.afterPropertiesSet();
			templateHolders.add(new TemplateHolder(jt, njt, tt));
		}
	}

	public Collection<TemplateHolder> list() {
		return templateHolders;
	}

	public TemplateHolder route(String routingKey) {
		return templateHolders
				.get(routingDataSource.getDefaultRouter().route(routingDataSource.getShardingNames(), routingKey));
	}

	public TemplateHolder route(String routingKey, String routerName) {
		Router router = routingDataSource.getRouters().get(routerName);
		if (router == null)
			throw new IllegalArgumentException("router '" + routerName + "' not found");
		return templateHolders.get(router.route(routingDataSource.getShardingNames(), routingKey));
	}

	public static class TemplateHolder {

		public final JdbcTemplate jdbc;

		public final NamedParameterJdbcTemplate namedJdbc;

		public final TransactionTemplate transaction;

		public TemplateHolder(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc,
				TransactionTemplate transaction) {
			this.jdbc = jdbc;
			this.namedJdbc = namedJdbc;
			this.transaction = transaction;
		}

	}

}
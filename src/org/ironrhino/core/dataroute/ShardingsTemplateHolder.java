package org.ironrhino.core.dataroute;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
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
		for (DataSource ds : routingDataSource.getShardings().values()) {
			JdbcTemplate jt = new JdbcTemplate(ds);
			jt.afterPropertiesSet();
			DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
			tm.afterPropertiesSet();
			TransactionTemplate tt = new TransactionTemplate(tm);
			tt.afterPropertiesSet();
			templateHolders.add(new TemplateHolder(jt, tt));
		}
	}

	public List<TemplateHolder> get() {
		return templateHolders;
	}

	public static class TemplateHolder {

		public final JdbcTemplate jdbc;

		public final TransactionTemplate transaction;

		public TemplateHolder(JdbcTemplate jdbc, TransactionTemplate transaction) {
			this.jdbc = jdbc;
			this.transaction = transaction;
		}

	}

}
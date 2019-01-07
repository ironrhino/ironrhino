package org.ironrhino.core.hibernate;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.ironrhino.core.jdbc.DatabaseProduct;

public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

	private static final long serialVersionUID = 1028300647776161253L;

	private final DataSource dataSource;

	private final boolean useCatalog;

	private final String defaultSchema;

	public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
		this.dataSource = dataSource;
		try (Connection conn = dataSource.getConnection()) {
			DatabaseProduct dp = DatabaseProduct.parse(conn.getMetaData().getDatabaseProductName());
			this.useCatalog = (dp == DatabaseProduct.MYSQL);
			this.defaultSchema = useCatalog ? conn.getCatalog() : conn.getSchema();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return (ConnectionProvider.class.equals(unwrapType)) || (DataSource.class.isAssignableFrom(unwrapType));
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		if (ConnectionProvider.class.equals(unwrapType)) {
			return (T) this;
		}
		if (DataSource.class.isAssignableFrom(unwrapType)) {
			return (T) dataSource;
		}
		throw new UnknownUnwrapTypeException(unwrapType);
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		Connection connection = dataSource.getConnection();
		if (StringUtils.isNotBlank(tenantIdentifier)) {
			if (useCatalog)
				connection.setCatalog(tenantIdentifier);
			else
				connection.setSchema(tenantIdentifier);
		}
		return connection;
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		if (useCatalog)
			connection.setCatalog(defaultSchema);
		else
			connection.setSchema(defaultSchema);
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

}

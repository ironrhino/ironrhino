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

	private static final String CLIENT_INFO_KEY_IDENTIFIER = "IDENTIFIER";

	private static final String CLIENT_INFO_NULL_IDENTIFIER = "$$NULL$$";

	private DataSource dataSource;

	private boolean useCatalog;

	public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
		this.dataSource = dataSource;
		try (Connection conn = dataSource.getConnection()) {
			DatabaseProduct dp = DatabaseProduct.parse(conn.getMetaData().getDatabaseProductName());
			this.useCatalog = (dp == DatabaseProduct.MYSQL);
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
			if (useCatalog) {
				String catalog = connection.getCatalog();
				connection.setClientInfo(CLIENT_INFO_KEY_IDENTIFIER, catalog);
				connection.setCatalog(tenantIdentifier);
			} else {
				String schema = connection.getSchema();
				connection.setClientInfo(CLIENT_INFO_KEY_IDENTIFIER, schema);
				connection.setSchema(tenantIdentifier);
			}
		}
		return connection;
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		String oldTenantIdentifier = connection.getClientInfo(CLIENT_INFO_KEY_IDENTIFIER);
		if (oldTenantIdentifier != null && !CLIENT_INFO_NULL_IDENTIFIER.equals(oldTenantIdentifier)) {
			if (useCatalog) {
				connection.setCatalog(oldTenantIdentifier);
			} else {
				connection.setSchema(oldTenantIdentifier);
			}
			connection.setClientInfo(CLIENT_INFO_KEY_IDENTIFIER, CLIENT_INFO_NULL_IDENTIFIER);
		}
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

}

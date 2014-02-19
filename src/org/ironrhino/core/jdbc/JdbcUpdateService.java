package org.ironrhino.core.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

public class JdbcUpdateService {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private DatabaseProduct databaseProduct;

	private int databaseMajorVersion;

	private int databaseMinorVersion;

	@Value("${jdbcQueryService.restricted:true}")
	private boolean restricted = true;

	@Value("${jdbcQueryService.queryTimeout:0}")
	private int queryTimeout;

	private String catalog;

	private String schema;

	private String quoteString = "\"";

	private boolean supportsBatchUpdates;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setDatabaseProduct(DatabaseProduct databaseProduct) {
		this.databaseProduct = databaseProduct;
	}

	public DatabaseProduct getDatabaseProduct() {
		return databaseProduct;
	}

	public int getDatabaseMajorVersion() {
		return databaseMajorVersion;
	}

	public void setDatabaseMajorVersion(int databaseMajorVersion) {
		this.databaseMajorVersion = databaseMajorVersion;
	}

	public int getDatabaseMinorVersion() {
		return databaseMinorVersion;
	}

	public void setDatabaseMinorVersion(int databaseMinorVersion) {
		this.databaseMinorVersion = databaseMinorVersion;
	}

	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	public boolean isRestricted() {
		return restricted;
	}

	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
	}

	@PostConstruct
	public void init() {
		if (queryTimeout > 0)
			jdbcTemplate.setQueryTimeout(queryTimeout);
		namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(
				jdbcTemplate);
		Connection con = DataSourceUtils.getConnection(jdbcTemplate
				.getDataSource());
		try {
			catalog = con.getCatalog();
			try {
				schema = con.getSchema();
			} catch (Throwable t) {

			}
			DatabaseMetaData dbmd = con.getMetaData();
			supportsBatchUpdates = dbmd.supportsBatchUpdates();
			if (databaseProduct == null)
				databaseProduct = DatabaseProduct.parse(dbmd
						.getDatabaseProductName());
			if (databaseMajorVersion == 0)
				databaseMajorVersion = dbmd.getDatabaseMajorVersion();
			if (databaseMinorVersion == 0)
				databaseMinorVersion = dbmd.getDatabaseMinorVersion();
			String str = dbmd.getIdentifierQuoteString();
			if (StringUtils.isNotBlank(str))
				quoteString = str.trim().substring(0, 1);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		} finally {
			DataSourceUtils
					.releaseConnection(con, jdbcTemplate.getDataSource());
		}
	}

	@Transactional
	public void validate(String sql) {
		sql = SqlUtils.trim(sql);
		Set<String> names = SqlUtils.extractParameters(sql);
		Map<String, Object> paramMap = new HashMap<String, Object>();
		for (String name : names)
			paramMap.put(name, "0");
		validateAndConvertTypes(sql, paramMap);
		if (restricted) {
			for (String table : SqlUtils.extractTables(sql, quoteString,
					"update")) {
				if (table.indexOf('.') < 0)
					continue;
				if (table.startsWith(quoteString)
						&& table.endsWith(quoteString)
						&& !table.substring(1, table.length() - 1).contains(
								quoteString))
					continue;
				String[] arr = table.split("\\.");
				if (arr.length == 2) {
					String prefix = arr[0].replaceAll(quoteString, "");
					if (!prefix.equalsIgnoreCase(catalog)
							&& !prefix.equalsIgnoreCase(schema)) {
						throw new ErrorMessage("query.access.denied",
								new Object[] { table });
					}
				} else if (arr.length > 2) {
					String prefix1 = arr[0].replaceAll(quoteString, "");
					String prefix2 = arr[1].replaceAll(quoteString, "");
					if (!prefix1.equalsIgnoreCase(catalog)
							&& !prefix2.equalsIgnoreCase(schema)) {
						throw new ErrorMessage("query.access.denied",
								new Object[] { table });
					}
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void validateAndConvertTypes(String sql, Map paramMap) {
		try {
			update(appendFalseClause(sql), paramMap);
		} catch (BadSqlGrammarException bse) {
			Throwable t = bse.getCause();
			if (t.getClass().getSimpleName().equals("PSQLException")) {
				String error = t.getMessage().toLowerCase();
				if ((error.indexOf("smallint") > 0
						|| error.indexOf("bigint") > 0
						|| error.indexOf("bigserial") > 0
						|| error.indexOf("integer") > 0
						|| error.indexOf("serial") > 0
						|| error.indexOf("numeric") > 0
						|| error.indexOf("decimal") > 0
						|| error.indexOf("real") > 0
						|| error.indexOf("double precision") > 0
						|| error.indexOf("money") > 0
						|| error.indexOf("timestamp") > 0
						|| error.indexOf("date") > 0 || error.indexOf("time") > 0)
						&& error.indexOf("character varying") > 0
						&& error.indexOf("：") > 0) {
					int location = Integer.valueOf(error.substring(
							error.lastIndexOf("：") + 1).trim());
					String paramName = sql.substring(location);
					paramName = paramName.substring(paramName.indexOf(":") + 1);
					paramName = paramName.split("\\s")[0].split("\\)")[0];
					Object object = paramMap.get(paramName);
					if (object != null) {
						String value = object.toString();
						if (error.indexOf("small") > 0)
							paramMap.put(paramName, Short.valueOf(value));
						else if (error.indexOf("bigint") > 0
								|| error.indexOf("bigserial") > 0)
							paramMap.put(paramName, Long.valueOf(value));
						else if (error.indexOf("integer") > 0
								|| error.indexOf("serial") > 0)
							paramMap.put(paramName, Integer.valueOf(value));
						else if (error.indexOf("numeric") > 0
								|| error.indexOf("decimal") > 0
								|| error.indexOf("real") > 0
								|| error.indexOf("double precision") > 0
								|| error.indexOf("money") > 0)
							paramMap.put(paramName, new BigDecimal(value));
						else if (error.indexOf("timestamp") > 0
								|| error.indexOf("date") > 0
								|| error.indexOf("time") > 0)
							paramMap.put(
									paramName,
									value.equals("0") ? new Date() : DateUtils
											.parse(value));
						validateAndConvertTypes(sql, paramMap);
						return;
					}
				}
			}
			String cause = "";
			if (t instanceof SQLException)
				cause = t.getMessage();
			throw new ErrorMessage("query.bad.sql.grammar",
					new Object[] { cause });
		} catch (DataAccessException e) {
			throw new ErrorMessage(e.getMessage());
		}
	}

	@Transactional
	public int[] update(String sql, Map<String, ?> paramMap) {
		int[] result = new int[2];
		long time = System.currentTimeMillis();
		result[0] = namedParameterJdbcTemplate.update(sql, paramMap);
		result[1] = (int) (System.currentTimeMillis() - time);
		return result;
	}

	@Transactional
	public int[][] update(String[] sql, Map<String, ?> paramMap) {
		int queryTimeout = jdbcTemplate.getQueryTimeout();
		int[][] result = new int[sql.length][2];
		for (int i = 0; i < sql.length; i++) {
			jdbcTemplate.setQueryTimeout(queryTimeout);
			result[i] = update(sql[i], paramMap);
		}
		return result;
	}

	@Transactional
	public int[] executeBatch(String[] sql) {
		boolean batch = supportsBatchUpdates;
		if (batch
				&& (databaseProduct == DatabaseProduct.SYBASE || databaseProduct == DatabaseProduct.SQLSERVER)) {
			for (int i = 0; i < sql.length; i++) {
				if (i > 0
						&& CREATE_OR_ALTER_PROCEDURE_OR_FUNCTION_PATTERN
								.matcher(sql[i]).find()) {
					// create/alter procedure/function must be the first command
					batch = false;
					break;
				}
			}
		}
		if (batch)
			return jdbcTemplate.batchUpdate(sql);
		else {
			int[] result = new int[sql.length];
			for (int i = 0; i < sql.length; i++)
				result[i] = jdbcTemplate.update(sql[i]);
			return result;
		}
	}

	private static String appendFalseClause(String sql) {
		if (!sql.endsWith("1=0")) {
			boolean where = sql.toLowerCase().contains(" where ");
			if (where) {
				StringBuilder sb = new StringBuilder(sql.length() + 9);
				sb.append(sql).append("\n").append(" and 1=0");
				sql = sb.toString();
			} else {
				StringBuilder sb = new StringBuilder(sql.length() + 11);
				sb.append(sql).append("\n").append(" where 1=0");
				sql = sb.toString();
			}
		}
		return sql;
	}

	private static final Pattern CREATE_OR_ALTER_PROCEDURE_OR_FUNCTION_PATTERN = Pattern
			.compile("(create|alter)\\s+(procedure|function)",
					Pattern.CASE_INSENSITIVE);

}

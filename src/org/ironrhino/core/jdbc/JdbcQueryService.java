package org.ironrhino.core.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.hibernate.HibernateEnabled;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@HibernateEnabled
@Slf4j
public class JdbcQueryService {

	@Getter
	@Setter
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Getter
	@Setter
	private DatabaseProduct databaseProduct;

	@Getter
	@Setter
	private int databaseMajorVersion;

	@Getter
	@Setter
	private int databaseMinorVersion;

	@Getter
	@Setter
	@Value("${jdbcQueryService.restricted:true}")
	private boolean restricted = true;

	@Setter
	@Value("${jdbcQueryService.queryTimeout:0}")
	private int queryTimeout;

	private String catalog;

	private String schema;

	private String quoteString = "\"";

	private List<String> keywords = new ArrayList<>();

	@Value("${csv.maxRows:0}")
	private int csvMaxRows;

	public int getCsvMaxRows() {
		return csvMaxRows > 0 ? csvMaxRows : 1000 * ResultPage.DEFAULT_PAGE_SIZE;
	}

	@NonNull
	public DataSource getDataSource() {
		Assert.notNull(jdbcTemplate, "JdbcTemplate should be present");
		DataSource dataSource = jdbcTemplate.getDataSource();
		Assert.notNull(dataSource, "DataSource should be present");
		return dataSource;
	}

	@PostConstruct
	public void init() {
		Assert.notNull(jdbcTemplate, "JdbcTemplate should be present");
		if (queryTimeout > 0)
			jdbcTemplate.setQueryTimeout(queryTimeout);
		namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		Connection con = DataSourceUtils.getConnection(getDataSource());
		try {
			catalog = con.getCatalog();
			try {
				schema = con.getSchema();
			} catch (Throwable t) {

			}
			DatabaseMetaData dbmd = con.getMetaData();
			if (databaseProduct == null)
				databaseProduct = DatabaseProduct.parse(dbmd.getDatabaseProductName());
			if (databaseMajorVersion == 0)
				databaseMajorVersion = dbmd.getDatabaseMajorVersion();
			if (databaseMinorVersion == 0)
				databaseMinorVersion = dbmd.getDatabaseMinorVersion();
			String str = dbmd.getIdentifierQuoteString();
			if (StringUtils.isNotBlank(str))
				quoteString = str.trim().substring(0, 1);
			keywords = new ArrayList<>(databaseProduct.getKeywords());
			if (keywords.isEmpty()) {
				str = dbmd.getSQLKeywords();
				if (StringUtils.isNotBlank(str))
					keywords.addAll(Arrays.asList(str.toUpperCase(Locale.ROOT).split("\\s*,\\s*")));
			}
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			DataSourceUtils.releaseConnection(con, jdbcTemplate.getDataSource());
		}
	}

	@Transactional(readOnly = true)
	public List<String> getTables() {
		List<String> tables = new ArrayList<>();
		Connection con = DataSourceUtils.getConnection(getDataSource());
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			ResultSet rs = dbmd.getTables(catalog, schema, "%", new String[] { "TABLE" });
			while (rs.next()) {
				String table = rs.getString(3);
				if (keywords.contains(table.toUpperCase(Locale.ROOT)))
					table = new StringBuilder(table.length() + 2).append(quoteString).append(table).append(quoteString)
							.toString();
				tables.add(table);
			}
			rs.close();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			DataSourceUtils.releaseConnection(con, jdbcTemplate.getDataSource());
		}
		return tables;
	}

	@Transactional(readOnly = true)
	public void validate(String sql) {
		sql = SqlUtils.trim(sql);
		Map<String, String> parameters = SqlUtils.extractParametersWithType(sql, getDataSource());
		Map<String, Object> paramMap = new HashMap<>();
		parameters.forEach((name, type) -> {
			Object value = "19700101";
			if ("date".equals(type)) {
				value = new java.sql.Date(DateUtils.parseDate8("19700101").getTime());
			} else if ("datetime".equals(type) || "timestamp".equals(type)) {
				value = new java.sql.Timestamp(DateUtils.parseDatetime("1970-01-01 00:00:00").getTime());
			} else if ("time".equals(type)) {
				value = new java.sql.Time(DateUtils.parseDatetime("1970-01-01 00:00:00").getTime());
			} else if ("integer".equals(type) || "long".equals(type)) {
				value = 19700101;
			} else if ("double".equals(type)) {
				value = 197001.01;
			} else if ("decimal".equals(type)) {
				value = new BigDecimal("197001.01");
			} else if ("boolean".equals(type)) {
				value = false;
			} else if ("bit".equals(type)) {
				value = 0;
			}
			paramMap.put(name, value);
		});
		validateAndConvertTypes(sql, paramMap);
		if (restricted) {
			for (String table : SqlUtils.extractTables(sql, quoteString)) {
				if (table.indexOf('.') < 0)
					continue;
				if (table.startsWith(quoteString) && table.endsWith(quoteString)
						&& !table.substring(1, table.length() - 1).contains(quoteString))
					continue;
				String[] arr = table.split("\\.");
				if (arr.length == 2) {
					String prefix = arr[0].replaceAll(quoteString, "");
					if (!prefix.equalsIgnoreCase(catalog) && !prefix.equalsIgnoreCase(schema)) {
						throw new ErrorMessage("query.access.denied", new Object[] { table });
					}
				} else if (arr.length > 2) {
					String prefix1 = arr[0].replaceAll(quoteString, "");
					String prefix2 = arr[1].replaceAll(quoteString, "");
					if (!prefix1.equalsIgnoreCase(catalog) && !prefix2.equalsIgnoreCase(schema)) {
						throw new ErrorMessage("query.access.denied", new Object[] { table });
					}
				}
			}
		}
	}

	private void validateAndConvertTypes(String sql, Map<String, Object> paramMap) {
		try {
			query(sql, paramMap, 1);
		} catch (BadSqlGrammarException bse) {
			Throwable t = bse.getCause();
			if (t.getClass().getSimpleName().equals("PSQLException")) {
				String error = t.getMessage().toLowerCase(Locale.ROOT);
				if ((error.indexOf("smallint") > 0 || error.indexOf("bigint") > 0 || error.indexOf("bigserial") > 0
						|| error.indexOf("integer") > 0 || error.indexOf("serial") > 0 || error.indexOf("numeric") > 0
						|| error.indexOf("decimal") > 0 || error.indexOf("real") > 0
						|| error.indexOf("double precision") > 0 || error.indexOf("money") > 0
						|| error.indexOf("timestamp") > 0 || error.indexOf("date") > 0 || error.indexOf("time") > 0)
						&& error.indexOf("character varying") > 0 && error.indexOf("：") > 0) {
					int location = Integer.parseInt(error.substring(error.lastIndexOf("：") + 1).trim());
					String paramName = sql.substring(location);
					paramName = paramName.substring(paramName.indexOf(":") + 1);
					paramName = paramName.split("\\s")[0].split("\\)")[0];
					Object object = paramMap.get(paramName);
					if (object != null) {
						String value = object.toString();
						if (error.indexOf("small") > 0)
							paramMap.put(paramName, Short.valueOf(value));
						else if (error.indexOf("bigint") > 0 || error.indexOf("bigserial") > 0)
							paramMap.put(paramName, Long.valueOf(value));
						else if (error.indexOf("integer") > 0 || error.indexOf("serial") > 0)
							paramMap.put(paramName, Integer.valueOf(value));
						else if (error.indexOf("numeric") > 0 || error.indexOf("decimal") > 0
								|| error.indexOf("real") > 0 || error.indexOf("double precision") > 0
								|| error.indexOf("money") > 0)
							paramMap.put(paramName, new BigDecimal(value));
						else if (error.indexOf("timestamp") > 0 || error.indexOf("date") > 0
								|| error.indexOf("time") > 0)
							paramMap.put(paramName, value.equals("19700101") ? new Date() : DateUtils.parse(value));
						validateAndConvertTypes(sql, paramMap);
						return;
					}
				}
			}
			String cause = "";
			if (t instanceof SQLException)
				cause = t.getMessage();
			throw new ErrorMessage("query.bad.sql.grammar", new Object[] { cause });
		}
	}

	@Transactional(readOnly = true)
	public long count(String sql, Map<String, ?> paramMap) {
		sql = SqlUtils.trim(sql);
		String alias = "tfc";
		while (sql.contains(alias))
			alias += "0";
		StringBuilder sb = new StringBuilder("select count(*) from (\n").append(SqlUtils.trimOrderby(sql))
				.append("\n) ").append(alias);
		Long count = namedParameterJdbcTemplate.queryForObject(sb.toString(), paramMap, Long.class);
		if (count == null)
			throw new RuntimeException("Unexpected null");
		return count;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> query(String sql, Map<String, ?> paramMap) {
		return namedParameterJdbcTemplate.queryForList(sql, paramMap);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> query(String sql, Map<String, ?> paramMap, final int limit) {
		return query(sql, paramMap, limit, 0);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> query(String sql, Map<String, ?> paramMap, final int limit, final int offset) {
		sql = SqlUtils.trim(sql);
		if (hasLimit(sql))
			return namedParameterJdbcTemplate.queryForList(sql, paramMap);
		sql = new StringBuilder(sql.length() + 2).append('\n').append(sql).append('\n').toString();

		if (databaseProduct == DatabaseProduct.ORACLE && databaseMajorVersion >= 12
				|| databaseProduct == DatabaseProduct.POSTGRESQL
						&& (databaseMajorVersion > 8 || databaseMajorVersion == 8 && databaseMinorVersion >= 4)
				|| databaseProduct == DatabaseProduct.DERBY) {
			// SQL 2008 Standards
			StringBuilder sb = new StringBuilder(sql.length() + offset > 0 ? 45 : 30);
			sb.append(sql);
			if (offset == 0)
				sb.append(" fetch first ");
			else
				sb.append(" offset ").append(offset).append(" rows fetch next ");
			sb.append(limit).append(" rows only");
			return namedParameterJdbcTemplate.queryForList(sb.toString(), paramMap);
		}

		if (databaseProduct == DatabaseProduct.SQLSERVER && databaseMajorVersion >= 11) {
			// https://technet.microsoft.com/en-us/library/gg699618(v=sql.110).aspx
			// OFFSET-FETCH can be used only with the ORDER BY clause.
			StringBuilder sb = new StringBuilder(sql.length() + 45);
			sb.append(sql);
			if (!sql.trim().toLowerCase(Locale.ROOT).matches(".+\\s+order\\s+by\\s+.+"))
				sb.append(" order by (select 0)");
			sb.append(" offset ").append(offset).append(" rows fetch ").append(offset > 0 ? "next " : "first ");
			sb.append(limit).append(" rows only");
			return namedParameterJdbcTemplate.queryForList(sb.toString(), paramMap);
		}

		if (databaseProduct == DatabaseProduct.MYSQL || databaseProduct == DatabaseProduct.MARIADB
				|| databaseProduct == DatabaseProduct.POSTGRESQL || databaseProduct == DatabaseProduct.H2) {
			StringBuilder sb = new StringBuilder(sql.length() + 20);
			sb.append(sql);
			sb.append(" limit ");
			sb.append(limit);
			if (offset > 0) {
				sb.append(" offset ");
				sb.append(offset);
			}
			return namedParameterJdbcTemplate.queryForList(sb.toString(), paramMap);
		} else if (databaseProduct == DatabaseProduct.HSQL) {
			StringBuilder sb = new StringBuilder(sql.length() + 20);
			sb.append(sql);
			if (offset > 0) {
				sb.append(" offset ");
				sb.append(offset);
			}
			sb.append(" limit ");
			sb.append(limit);
			return namedParameterJdbcTemplate.queryForList(sb.toString(), paramMap);
		} else if (databaseProduct == DatabaseProduct.ORACLE) {
			StringBuilder sb = new StringBuilder(sql.length() + 100);
			if (offset > 0) {
				sb.append("select * from ( select row_.*, rownum rownum_9527 from ( ");
			} else {
				sb.append("select * from ( ");
			}
			sb.append(sql);
			if (offset > 0) {
				sb.append(" ) row_ ) where rownum_9527 <= " + (limit + offset) + " and rownum_9527 > " + offset);
			} else {
				sb.append(" ) where rownum <= " + limit);
			}
			List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sb.toString(), paramMap);
			if (offset > 0) {
				String columnName = null;
				for (Map<String, Object> map : list) {
					if (columnName == null)
						for (String s : map.keySet())
							if (s.equalsIgnoreCase("rownum_9527")) {
								columnName = s;
								break;
							}
					map.remove(columnName);
				}
			}
			return list;
		} else if (databaseProduct == DatabaseProduct.DB2) {
			StringBuilder sb = new StringBuilder(sql.length() + offset > 0 ? 200 : 20);
			if (offset > 0) {
				sb.append(
						"select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( ")
						.append(sql).append(" fetch first ").append(limit + offset)
						.append(" rows only ) as inner2_ ) as inner1_ where rownumber_ > ").append(offset)
						.append(" order by rownumber_");
			} else {
				sb.append(sql);
				sb.append(" fetch first ");
				sb.append(limit);
				sb.append(" rows only");
			}
			List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sb.toString(), paramMap);
			if (offset > 0) {
				String columnName = null;
				for (Map<String, Object> map : list) {
					if (columnName == null)
						for (String s : map.keySet())
							if (s.equalsIgnoreCase("rownumber_")) {
								columnName = s;
								break;
							}
					map.remove(columnName);
				}
			}
			return list;
		}

		if (databaseProduct == DatabaseProduct.SQLSERVER || databaseProduct == DatabaseProduct.SYBASE) {
			int selectIndex = sql.toLowerCase(Locale.ROOT).indexOf("select");
			int selectDistinctIndex = sql.toLowerCase(Locale.ROOT).indexOf("select distinct");
			int position = selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);
			sql = new StringBuilder(sql.length() + 8).append(sql)
					.insert(position, " top " + (offset > 0 ? offset + limit : limit)).toString();
			if (offset <= 0)
				return namedParameterJdbcTemplate.queryForList(sql, paramMap);
		}
		if (databaseProduct == DatabaseProduct.INFORMIX) {
			int selectIndex = sql.toLowerCase(Locale.ROOT).indexOf("select");
			int selectDistinctIndex = sql.toLowerCase(Locale.ROOT).indexOf("select distinct");
			int position = selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);
			if (databaseMajorVersion >= 11) {
				sql = new StringBuilder(sql.length() + 8).append(sql)
						.insert(position, " skip " + offset + " first " + limit).toString();
				return namedParameterJdbcTemplate.queryForList(sql, paramMap);
			}
			sql = new StringBuilder(sql.length() + 8).append(sql)
					.insert(position, " first " + (offset > 0 ? offset + limit : limit)).toString();
			if (offset <= 0)
				return namedParameterJdbcTemplate.queryForList(sql, paramMap);
		}

		final ColumnMapRowMapper crm = new ColumnMapRowMapper();
		return namedParameterJdbcTemplate.execute(sql, paramMap, preparedStatement -> {
			preparedStatement.setMaxRows(offset + limit);
			ResultSet rs = preparedStatement.executeQuery();
			List<Map<String, Object>> result = new ArrayList<>(limit);
			int i = 0;
			while (rs.next()) {
				if (i >= offset)
					result.add(crm.mapRow(rs, i));
				i++;
			}
			rs.close();
			return result;
		});
	}

	@Transactional(readOnly = true)
	public ResultPage<Map<String, Object>> query(ResultPage<Map<String, Object>> resultPage) {
		int queryTimeout = jdbcTemplate.getQueryTimeout();
		QueryCriteria criteria = resultPage.getCriteria();
		String sql = criteria.getQuery();
		Map<String, Object> paramMap = criteria.getParameters();
		sql = SqlUtils.trim(sql);
		validateAndConvertTypes(sql, paramMap);
		boolean hasLimit = hasLimit(sql);
		resultPage.setPaged(!hasLimit);
		jdbcTemplate.setQueryTimeout(queryTimeout);
		resultPage.setTotalResults(count(sql, paramMap));
		if (resultPage.getTotalResults() > getCsvMaxRows() && (hasLimit || !(databaseProduct == DatabaseProduct.MYSQL
				|| databaseProduct == DatabaseProduct.MARIADB || databaseProduct == DatabaseProduct.POSTGRESQL
				|| databaseProduct == DatabaseProduct.H2 || databaseProduct == DatabaseProduct.HSQL
				|| databaseProduct == DatabaseProduct.ORACLE || databaseProduct == DatabaseProduct.DB2
				|| databaseProduct == DatabaseProduct.INFORMIX && databaseMajorVersion >= 11
				|| databaseProduct == DatabaseProduct.SQLSERVER && databaseMajorVersion >= 11
				|| databaseProduct == DatabaseProduct.DERBY)))
			throw new ErrorMessage("query.result.number.exceed", new Object[] { getCsvMaxRows() });
		long time = System.nanoTime();
		jdbcTemplate.setQueryTimeout(queryTimeout);
		resultPage.setResult(query(sql, paramMap, resultPage.getPageSize(),
				(resultPage.getPageNo() - 1) * resultPage.getPageSize()));
		resultPage.setTookInMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time));
		return resultPage;
	}

	@Transactional(readOnly = true)
	public void query(String sql, Map<String, ?> paramMap, final LineHandler lh) {
		query(sql, paramMap, new RowHandler() {

			@Override
			public boolean isWithHeader() {
				return lh.isWithHeader();
			}

			@Override
			public void handleRow(int index, Object[] row) {
				String seperator = lh.getColumnSeperator();
				String[] arr = new String[row.length];
				for (int i = 0; i < arr.length; i++) {
					Object value = row[i];
					String text = value != null ? value.toString() : "";
					if (text.contains(String.valueOf(seperator)) || text.contains("\"") || text.contains("\n")) {
						if (text.contains("\""))
							text = text.replaceAll("\"", "\"\"");
						text = new StringBuilder(text.length() + 2).append("\"").append(text).append("\"").toString();
					}
					arr[i] = text;
				}
				lh.handleLine(index, String.join(seperator, arr));
			}
		});
	}

	@Transactional(readOnly = true)
	public void query(String sql, Map<String, ?> paramMap, final RowHandler rh) {
		final AtomicInteger ai = new AtomicInteger(0);
		namedParameterJdbcTemplate.query(sql, paramMap, new RowCallbackHandler() {

			private int columnCount;

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				int index = ai.getAndIncrement();
				if (index == 0) {
					ResultSetMetaData rsmd = rs.getMetaData();
					columnCount = rsmd.getColumnCount();
					if (rh.isWithHeader()) {
						String[] arr = new String[columnCount];
						for (int i = 1; i <= columnCount; i++)
							arr[i - 1] = JdbcUtils.lookupColumnName(rsmd, i);
						rh.handleRow(-1, arr);
					}
				}
				Object[] arr = new Object[columnCount];
				for (int i = 1; i <= columnCount; i++)
					arr[i - 1] = JdbcUtils.getResultSetValue(rs, i);
				rh.handleRow(index, arr);
			}
		});
	}

	private boolean hasLimit(String sql) {
		sql = SqlUtils.clearComments(sql);
		if (databaseProduct == DatabaseProduct.MYSQL || databaseProduct == DatabaseProduct.MARIADB
				|| databaseProduct == DatabaseProduct.H2 || databaseProduct == DatabaseProduct.HSQL) {
			return LIMIT_PATTERN.matcher(sql).find();
		} else if (databaseProduct == DatabaseProduct.POSTGRESQL) {
			return LIMIT_PATTERN.matcher(sql).find()
					|| (databaseMajorVersion > 8 || databaseMajorVersion == 8 && databaseMinorVersion >= 4)
							&& (FIRST_PATTERN.matcher(sql).find() || NEXT_PATTERN.matcher(sql).find());
		} else if (databaseProduct == DatabaseProduct.DB2 || databaseProduct == DatabaseProduct.INFORMIX) {
			return FIRST_PATTERN.matcher(sql).find();
		} else if (databaseProduct == DatabaseProduct.DERBY) {
			return FIRST_PATTERN.matcher(sql).find() || NEXT_PATTERN.matcher(sql).find();
		} else if (databaseProduct == DatabaseProduct.SQLSERVER) {
			return TOP_PATTERN.matcher(sql).find() || databaseMajorVersion >= 11
					&& (FIRST_PATTERN.matcher(sql).find() || NEXT_PATTERN.matcher(sql).find());
		} else if (databaseProduct == DatabaseProduct.SYBASE) {
			return TOP_PATTERN.matcher(sql).find();
		} else if (databaseProduct == DatabaseProduct.ORACLE) {
			return ROWNUM_PATTERN.matcher(sql).find() || databaseMajorVersion >= 12
					&& (FIRST_PATTERN.matcher(sql).find() || NEXT_PATTERN.matcher(sql).find());
		}
		return false;
	}

	private static final Pattern LIMIT_PATTERN = Pattern.compile("\\s+limit\\s+\\d+", Pattern.CASE_INSENSITIVE);

	private static final Pattern TOP_PATTERN = Pattern.compile("\\s+top\\s+\\d+\\s+", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIRST_PATTERN = Pattern.compile("\\s+first\\s+\\d+\\s+", Pattern.CASE_INSENSITIVE);
	private static final Pattern NEXT_PATTERN = Pattern.compile("\\s+next\\s+\\d+\\s+", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROWNUM_PATTERN = Pattern.compile("\\s+rownum", Pattern.CASE_INSENSITIVE);

}

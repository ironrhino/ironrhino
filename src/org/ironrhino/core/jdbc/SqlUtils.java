package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqlUtils {

	public static String buildInsertStatement(Class<?> beanClass) {
		return buildInsertStatement(beanClass, null);
	}

	public static String buildInsertStatement(Class<?> beanClass, String tableName, String... ignoreProperties) {
		List<String> properties = new ArrayList<>();
		Set<String> ignorePropertiesSet = new HashSet<>();
		if (ignoreProperties.length > 0)
			ignorePropertiesSet.addAll(Arrays.asList(ignoreProperties));
		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(beanClass)) {
			if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
				String name = pd.getName();
				if (!ignorePropertiesSet.contains(name)) {
					String columnName = name;
					Column column = pd.getReadMethod().getAnnotation(Column.class);
					if (column == null) {
						try {
							column = ReflectionUtils.getField(beanClass, name).getAnnotation(Column.class);
						} catch (NoSuchFieldException e) {
						}
					}
					if (column != null && StringUtils.isNotBlank(column.name()))
						columnName = column.name();
					properties.add(columnName);
				}
			}
		}
		if (properties.isEmpty())
			throw new IllegalArgumentException(beanClass + " has no properties");
		if (tableName == null) {
			Table table = beanClass.getAnnotation(Table.class);
			if (table != null)
				tableName = table.name();
			if (StringUtils.isBlank(tableName))
				tableName = beanClass.getSimpleName();
		}
		StringBuilder sb = new StringBuilder("insert into ");
		sb.append(tableName);
		sb.append("(");
		sb.append(String.join(",", properties));
		sb.append(") values (");
		for (int i = 0; i < properties.size(); i++) {
			sb.append(":").append(properties.get(i));
			if (i < properties.size() - 1)
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}

	public static String buildUpdateStatement(String tableName, List<String> setColumns, List<String> whereColumns) {
		StringBuilder sb = new StringBuilder("update ");
		sb.append(tableName);
		sb.append(" set ");
		for (int i = 0; i < setColumns.size(); i++) {
			String name = setColumns.get(i);
			sb.append(name).append("=:").append(name);
			if (i < setColumns.size() - 1)
				sb.append(",");
		}
		if (whereColumns != null && !whereColumns.isEmpty()) {
			sb.append(" where ");
			for (int i = 0; i < whereColumns.size(); i++) {
				String name = whereColumns.get(i);
				sb.append(name).append("=:").append(name);
				if (i < whereColumns.size() - 1)
					sb.append(" and ");
			}
		}
		return sb.toString();
	}

	public static String trim(String sql) {
		sql = sql.trim();
		while (sql.endsWith(";"))
			sql = sql.substring(0, sql.length() - 1);
		return sql;
	}

	public static String clearComments(String sql) {
		if (StringUtils.isBlank(sql))
			return sql;
		return LINE_COMMENTS_PATTERN.matcher(BLOCK_COMMENTS_PATTERN.matcher(sql).replaceAll("")).replaceAll("\n")
				.replaceAll("\n+", "\n").trim();
	}

	public static Set<String> extractParameters(String sql) {
		if (StringUtils.isBlank(sql))
			return Collections.emptySet();
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		if (parsedSql != null) {
			List<String> names = ReflectionUtils.getFieldValue(parsedSql, "parameterNames");
			if (names != null)
				return new LinkedHashSet<>(names);
		}
		sql = clearComments(sql);
		Set<String> names = new LinkedHashSet<>();
		Matcher m = PARAMETER_PATTERN.matcher(sql);
		while (m.find()) {
			String name = m.group(1).substring(1);
			if (name.startsWith("{") && name.endsWith("}"))
				name = name.substring(1, name.length() - 1);
			names.add(name);
		}
		return names;
	}

	public static Map<String, String> extractParametersWithType(String sql) {
		return extractParametersWithType(sql, (Connection) null);
	}

	public static Map<String, String> extractParametersWithType(String sql, DataSource ds) {
		try (Connection con = ds.getConnection()) {
			return extractParametersWithType(sql, con);
		} catch (SQLException e) {
			return extractParametersWithType(sql, (Connection) null);
		}
	}

	public static Map<String, String> extractParametersWithType(String sql, Connection con) {
		Map<String, String> map = new LinkedHashMap<>();
		for (String name : extractParameters(sql)) {
			String param = ":" + name;
			String type = "";
			int index = sql.indexOf(param);
			String s;
			if (index < 0) {
				param = ":{" + name + "}";
				index = sql.indexOf(param);
			}
			s = sql.substring(index + param.length()).trim();
			int i = s.indexOf("/*--");
			int j = s.indexOf("--*/");
			if (i == 0 && j > i)
				type = s.substring(i + 4, j).trim().toLowerCase();

			map.put(name, type);
		}
		if (con != null) {
			boolean untyped = false;
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (StringUtils.isBlank(entry.getValue())) {
					untyped = true;
					break;
				}
			}
			if (untyped) {
				sql = clearComments(sql);
				Map<String, Integer> index = new LinkedHashMap<>();
				for (String name : map.keySet()) {
					String p = ":" + name;
					int i = sql.indexOf(p);
					if (i < 0) {
						p = ":{" + name + "}";
						i = sql.indexOf(p);
					}
					index.put(name, StringUtils.countMatches(sql.substring(0, i), '?') + 1);
					while (i > 0) {
						sql = StringUtils.replace(sql, p, "?");
						i = sql.indexOf(p);
					}
				}
				try (PreparedStatement ps = con.prepareStatement(sql)) {
					ParameterMetaData pmd = ps.getParameterMetaData();
					for (Map.Entry<String, String> entry : map.entrySet())
						if (StringUtils.isBlank(entry.getValue()))
							map.put(entry.getKey(), convertType(pmd.getParameterClassName(index.get(entry.getKey()))));
				} catch (SQLException e) {
				}
			}
		}
		return map;
	}

	private static String convertType(String className) {
		if (className != null) {
			try {
				Class<?> clazz = Class.forName(className);
				if (clazz == boolean.class || clazz == Boolean.class)
					return "boolean";
				else if (clazz == Timestamp.class || clazz == java.util.Date.class || clazz == LocalDateTime.class)
					return "datetime";
				else if (clazz == Date.class || clazz == LocalDate.class)
					return "date";
				else if (clazz == Time.class || clazz == LocalTime.class)
					return "time";
				else if (clazz == int.class || clazz == short.class || clazz == Integer.class || clazz == Short.class)
					return "integer";
				else if (clazz == long.class || clazz == Long.class)
					return "long";
				else if (clazz == double.class || clazz == float.class || clazz == Double.class || clazz == Float.class)
					return "double";
				else if (clazz == BigDecimal.class)
					return "decimal";
			} catch (ClassNotFoundException e) {

			}
		}
		return "";
	}

	public static Map<String, Object> convertParameters(Map<String, String> paramMap, Map<String, String> paramTypes) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : paramMap.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();
			if (value == null)
				continue;
			String type = paramTypes.get(name);
			Object v = value;
			if ("date".equals(type) || "datetime".equals(type) || "timestamp".equals(type)) {
				v = DateUtils.parse(value.toString());
			} else if ("int".equals(type) || "integer".equals(type)) {
				v = Integer.valueOf(value.toString());
			} else if ("long".equals(type)) {
				v = Long.valueOf(value.toString());
			} else if ("double".equals(type)) {
				v = Double.valueOf(value.toString());
			} else if ("decimal".equals(type)) {
				v = new BigDecimal(value.toString());
			} else if ("boolean".equals(type)) {
				v = Boolean.valueOf(value.toString());
			} else if ("bit".equals(type)) {
				v = Integer.valueOf(value.toString());
			}
			result.put(name, v);
		}
		return result;
	}

	public static Set<String> extractTables(String sql, String quoteString) {
		return extractTables(sql, quoteString, "from");
	}

	public static Set<String> extractTables(String sql, String quoteString, String frontKeyword) {
		if (StringUtils.isBlank(sql))
			return Collections.emptySet();
		sql = clearComments(sql);
		Pattern tablePattern = Pattern.compile(frontKeyword + "\\s+([\\w\\." + quoteString + ",\\s]+)",
				Pattern.CASE_INSENSITIVE);
		Set<String> names = new LinkedHashSet<>();
		Matcher m = tablePattern.matcher(sql);
		while (m.find()) {
			String arr[] = m.group(1).split(",");
			for (String s : arr) {
				names.add(s.trim().split("\\s+")[0]);
			}
		}
		return names;
	}

	public static String trimOrderby(String sql) {
		Matcher m = ORDERBY_PATTERN.matcher(sql);
		return m.replaceAll("");
	}

	static String expandCollectionParameter(String sql, String paramName, int size) {
		if (size < 1 || size > 100)
			throw new IllegalArgumentException("invalid size: " + size);
		boolean padding = true; // https://hibernate.atlassian.net/browse/HHH-12469
		int count = padding ? (1 << -Integer.numberOfLeadingZeros(size - 1)) : size;
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i < count; i++) {
			sb.append(":").append(paramName).append('[').append(i < size ? i : size - 1).append(']');
			if (i != count - 1)
				sb.append(',');
		}
		sb.append(')');
		String regex = "\\(\\s*:" + paramName + "\\s*\\)";
		return sql.replaceAll(regex, sb.toString());
	}

	static String appendLimitingClause(DatabaseProduct databaseProduct, int databaseMajorVersion,
			int databaseMinorVersion, String sql, String limitingParameterName, Limiting limiting) {
		switch (databaseProduct) {
		case MYSQL:
		case POSTGRESQL:
		case DB2:
		case H2:
			return appendLimitOffset(sql, limitingParameterName, limiting, false);
		case ORACLE:
			if (databaseMajorVersion >= 12) {
				return appendOffsetFetch(sql, limitingParameterName, limiting);
			} else {
				StringBuilder sb = new StringBuilder(sql.length() + 100);
				if (limiting.getOffset() > 0) {
					sb.append("select * from ( select row_.*, rownum rownum_9527 from ( ");
				} else {
					sb.append("select * from ( ");
				}
				sb.append(sql);
				if (limiting.getOffset() > 0) {
					sb.append(" ) row_ ) where rownum_9527 <= " + (limiting.getLimit() + limiting.getOffset())
							+ " and rownum_9527 > " + limiting.getOffset());
				} else {
					sb.append(" ) where rownum <= " + limiting.getLimit());
				}
				return sb.toString();
			}
		case SQLSERVER:
			if (databaseMajorVersion >= 11)
				return appendOffsetFetch(sql, limitingParameterName, limiting);
		case DERBY:
			return appendOffsetFetch(sql, limitingParameterName, limiting);
		case HSQL:
			return appendLimitOffset(sql, limitingParameterName, limiting, true);
		default:
		}
		throw new UnsupportedOperationException(databaseProduct + "" + databaseMajorVersion + "." + databaseMinorVersion
				+ " doesn't support offset limiting");
	}

	private static String appendLimitOffset(String sql, String limitingParameterName, Limiting limiting,
			boolean offsetBeforeLimit) {
		String[] arr = sql.split("\\s+");
		if (arr.length > 2 && arr[arr.length - 2].equalsIgnoreCase("limit")
				|| arr.length > 4 && arr[arr.length - 4].equalsIgnoreCase("limit"))
			return sql;
		StringBuilder sb = new StringBuilder(sql);
		if (offsetBeforeLimit) {
			if (limiting.getOffset() > 0)
				sb.append(" offset :").append(limitingParameterName).append(".offset");
			sb.append(" limit :").append(limitingParameterName).append(".limit");
		} else {
			sb.append(" limit :").append(limitingParameterName).append(".limit");
			if (limiting.getOffset() > 0)
				sb.append(" offset :").append(limitingParameterName).append(".offset");
		}
		return sb.toString();
	}

	private static String appendOffsetFetch(String sql, String limitingParameterName, Limiting limiting) {
		String[] arr = sql.split("\\s+");
		if (arr[arr.length - 1].equalsIgnoreCase("only"))
			return sql;
		StringBuilder sb = new StringBuilder(sql);
		if (limiting.getOffset() > 0)
			sb.append(" offset :").append(limitingParameterName).append(".offset rows");
		sb.append(" fetch ").append(limiting.getOffset() > 0 ? "next" : "first").append(" :")
				.append(limitingParameterName).append(".limit rows only");
		return sb.toString();
	}

	private static final Pattern ORDERBY_PATTERN = Pattern.compile("\\s+order\\s+by\\s+.+$", Pattern.CASE_INSENSITIVE);

	private static final Pattern PARAMETER_PATTERN = Pattern
			.compile("(:(\\{\\s*)?(\\w|[^'\\)\\sx00-xff])*(\\s*\\})?)(,|;|\\)|\\s|\\||\\+|$)");

	private static final Pattern BLOCK_COMMENTS_PATTERN = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/");

	private static final Pattern LINE_COMMENTS_PATTERN = Pattern.compile("\r?\n?([ \\t]*--.*)\r?(\n|$)");

}
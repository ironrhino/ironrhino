package org.ironrhino.core.jdbc;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanUtils;

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
		sb.append(StringUtils.join(properties, ","));
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

	public static String highlight(String sql) {
		if (StringUtils.isBlank(sql))
			return sql;
		return PARAMETER_PATTERN.matcher(LINE_COMMENTS_PATTERN
				.matcher(BLOCK_COMMENTS_PATTERN.matcher(sql).replaceAll("<span class=\"comment\">$0</span>"))
				.replaceAll("\n<span class=\"comment\">$1</span>\n")).replaceAll("<strong>$0</strong>");
	}

	public static Set<String> extractParameters(String sql) {
		if (StringUtils.isBlank(sql))
			return Collections.emptySet();
		sql = clearComments(sql);
		Set<String> names = new LinkedHashSet<>();
		Matcher m = PARAMETER_PATTERN.matcher(sql);
		while (m.find())
			names.add(m.group(1).substring(1));
		return names;
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
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i < size; i++) {
			sb.append(":").append(paramName).append('[').append(i).append(']');
			if (i != size - 1)
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
			.compile("(:(\\w|[^'\\)\\sx00-xff])*)(,|;|\\)|\\s|\\||\\+|$)");

	private static final Pattern BLOCK_COMMENTS_PATTERN = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/");

	private static final Pattern LINE_COMMENTS_PATTERN = Pattern.compile("\r?\n?([ \\t]*--.*)\r?(\n|$)");

}
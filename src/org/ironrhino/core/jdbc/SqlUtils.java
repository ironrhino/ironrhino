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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

public class SqlUtils {

	public static String buildInsertStatement(Class<?> beanClass) {
		return buildInsertStatement(beanClass, null);
	}

	public static String buildInsertStatement(Class<?> beanClass, String tableName, String... ignoreProperties) {
		List<String> properties = new ArrayList<>();
		Set<String> set = new HashSet<>();
		if (ignoreProperties.length > 0)
			set.addAll(Arrays.asList(ignoreProperties));
		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(beanClass)) {
			if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
				String name = pd.getName();
				if (!set.contains(name))
					properties.add(name);
			}
		}
		if (properties.isEmpty())
			throw new IllegalArgumentException(beanClass + " has no properties");
		if (tableName == null)
			tableName = beanClass.getSimpleName();
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

	private static final Pattern ORDERBY_PATTERN = Pattern.compile("\\s+order\\s+by\\s+.+$", Pattern.CASE_INSENSITIVE);

	private static final Pattern PARAMETER_PATTERN = Pattern
			.compile("(:(\\w|[^'\\)\\sx00-xff])*)(,|;|\\)|\\s|\\||\\+|$)");

	private static final Pattern BLOCK_COMMENTS_PATTERN = Pattern.compile("/\\*(?:.|[\\n\\r])*?\\*/");

	private static final Pattern LINE_COMMENTS_PATTERN = Pattern.compile("\r?\n?([ \\t]*--.*)\r?(\n|$)");

}
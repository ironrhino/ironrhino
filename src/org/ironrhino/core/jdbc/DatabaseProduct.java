package org.ironrhino.core.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public enum DatabaseProduct {

	MYSQL {

		@Override
		public int getDefaultPort() {
			return 3306;
		}

		@Override
		public String getDefaultDriverClass() {
			return "com.mysql.cj.jdbc.Driver";
		}

		@Override
		protected String getRecommendedJdbcUrlQueryString() {
			return "createDatabaseIfNotExist=true&serverTimezone=" + TimeZone.getDefault().getID()
					+ "&autoReconnectForPools=true&useUnicode=true&useServerPrepStmts=true&cachePrepStmts=true&tinyInt1isBit=false&socketTimeout=60000&sslMode=DISABLED";
		}

	},
	POSTGRESQL {
		@Override
		public int getDefaultPort() {
			return 5432;
		}

		@Override
		public String getDefaultDriverClass() {
			return "org.postgresql.Driver";
		}
	},
	ORACLE {
		@Override
		public int getDefaultPort() {
			return 1521;
		}

		@Override
		public String getDefaultDriverClass() {
			return "oracle.jdbc.OracleDriver";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append(":thin:@//");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append("/").append(databaseName);
			return sb.toString();
		}

		@Override
		public String getValidationQuery() {
			return "SELECT 1 FROM DUAL";
		}
	},
	DB2 {
		@Override
		public int getDefaultPort() {
			return 50000;
		}

		@Override
		public String getDefaultDriverClass() {
			return "com.ibm.db2.jcc.DB2Driver";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append("://");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append("/").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				params = params.replaceAll("&", ";");
				if (!params.startsWith(":"))
					sb.append(":");
				sb.append(params);
				if (!params.endsWith(";"))
					sb.append(";");
			}
			return sb.toString();
		}

		@Override
		public String polishJdbcUrl(String jdbcUrl) {
			String url = polishJdbcUrl(jdbcUrl, ":", ";");
			if (url.indexOf(':', url.lastIndexOf('/')) > 0 && !url.endsWith(";"))
				url = url + ";";
			return url;
		}

		@Override
		public String appendJdbcUrlProperties(String jdbcUrl, Map<String, String> properties) {
			StringBuilder sb = new StringBuilder(jdbcUrl);
			boolean hasDelimiter = jdbcUrl.endsWith(";");
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				if (!hasDelimiter) {
					sb.append(':');
					hasDelimiter = true;
				}
				sb.append(entry.getKey()).append('=').append(entry.getValue()).append(";");
			}
			return sb.toString();
		}

		@Override
		public String getValidationQuery() {
			return "VALUES 1";
		}
	},
	INFORMIX {
		@Override
		public int getDefaultPort() {
			return 1533;
		}

		@Override
		public String getDefaultDriverClass() {
			return "com.informix.jdbc.IfxDriver";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append("-sqli://");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append("/").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				params = params.replaceAll("&", ":");
				if (!params.startsWith(":"))
					sb.append(":");
				sb.append(params);
			}
			return sb.toString();
		}

		@Override
		public String getValidationQuery() {
			return "SELECT FIRST 1 CURRENT FROM SYSTABLES";
		}
	},
	SQLSERVER {
		@Override
		public int getDefaultPort() {
			return 1433;
		}

		@Override
		public String getDefaultDriverClass() {
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append("://");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append(";DatabaseName=").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				params = params.replaceAll("&", ";");
				if (!params.startsWith(";"))
					sb.append(";");
				sb.append(params);
				if (!params.endsWith(";"))
					sb.append(";");
			}
			return sb.toString();
		}

		@Override
		protected String getRecommendedJdbcUrlQueryString() {
			return "sendStringParametersAsUnicode=true";
		}

		@Override
		public String polishJdbcUrl(String jdbcUrl) {
			return polishJdbcUrl(jdbcUrl, ";", ";");
		}

		@Override
		public String appendJdbcUrlProperties(String jdbcUrl, Map<String, String> properties) {
			StringBuilder sb = new StringBuilder(jdbcUrl);
			boolean hasDelimiter = jdbcUrl.endsWith(";");
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				if (!hasDelimiter) {
					sb.append(';');
					hasDelimiter = true;
				}
				sb.append(entry.getKey()).append('=').append(entry.getValue()).append(";");
			}
			return sb.toString();
		}

	},
	SYBASE {
		@Override
		public int getDefaultPort() {
			return 4100;
		}

		@Override
		public String getDefaultDriverClass() {
			return "com.sybase.jdbc4.jdbc.SybDriver";
		}

		@Override
		public String getJdbcUrlPrefix() {
			return "jdbc:sybase:Tds";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append(":");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			sb.append(":").append(port);
			sb.append("/").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				if (!params.startsWith("?"))
					sb.append("?");
				sb.append(params);
			}
			return sb.toString();
		}
	},
	H2 {
		@Override
		public int getDefaultPort() {
			return 9092;
		}

		@Override
		public String getDefaultDriverClass() {
			return "org.h2.Driver";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append(":tcp://");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append("/").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				params = params.replaceAll("&", ";");
				if (!params.startsWith(";"))
					sb.append(";");
				sb.append(params);
			}
			return sb.toString();
		}
	},
	HSQL {
		@Override
		public int getDefaultPort() {
			return 9001;
		}

		@Override
		public String getDefaultDriverClass() {
			return "org.hsqldb.jdbc.JDBCDriver";
		}

		@Override
		public String getJdbcUrlPrefix() {
			return "jdbc:hsqldb";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append(":hsql://");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append("/").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				params = params.replaceAll("&", ";");
				if (!params.startsWith(";"))
					sb.append(";");
				sb.append(params);
			}
			return sb.toString();
		}

		@Override
		public String getValidationQuery() {
			return "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";
		}
	},
	DERBY {
		@Override
		public int getDefaultPort() {
			return 1527;
		}

		@Override
		public String getDefaultDriverClass() {
			return "org.apache.derby.jdbc.ClientDriver";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName, String params) {
			StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
			sb.append("://");
			sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
			if (port > 0 && port != getDefaultPort())
				sb.append(":").append(port);
			sb.append("/").append(databaseName);
			if (StringUtils.isNotBlank(params)) {
				params = params.replaceAll("&", ";");
				if (!params.startsWith(";"))
					sb.append(";");
				sb.append(params);
			}
			return sb.toString();
		}

		@Override
		public String getValidationQuery() {
			return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
		}
	};

	public static DatabaseProduct parse(String nameOrUrl) {
		nameOrUrl = nameOrUrl.trim();
		if (nameOrUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
			for (DatabaseProduct p : values())
				if (nameOrUrl.startsWith(p.getJdbcUrlPrefix()))
					return p;
			return null;
		} else {
			if (nameOrUrl.toLowerCase(Locale.ROOT).contains("mysql"))
				return MYSQL;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("postgres"))
				return POSTGRESQL;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("oracle"))
				return ORACLE;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).startsWith("db2"))
				return DB2;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("informix"))
				return INFORMIX;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("microsoft"))
				return SQLSERVER;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("sql server")
					|| nameOrUrl.equals("Adaptive Server Enterprise") || nameOrUrl.equals("ASE"))
				return SYBASE;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).equals("h2"))
				return H2;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("hsql"))
				return HSQL;
			else if (nameOrUrl.toLowerCase(Locale.ROOT).contains("derby"))
				return DERBY;
		}
		return null;
	}

	public abstract int getDefaultPort();

	public abstract String getDefaultDriverClass();

	public List<String> getKeywords() {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(getClass().getResourceAsStream("keywords.txt"), StandardCharsets.UTF_8))) {
			List<String> lines = br.lines().collect(Collectors.toList());
			for (String line : lines) {
				if (line.startsWith(name() + "=")) {
					String s = line.substring(line.indexOf("=") + 1);
					return Arrays.asList(s.split("\\s*,\\s*"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	public String getValidationQuery() {
		return "SELECT 1";
	}

	public String getJdbcUrlPrefix() {
		return "jdbc:" + name().toLowerCase(Locale.ROOT);
	}

	public String getJdbcUrl(String host, int port, String databaseName, String params) {
		StringBuilder sb = new StringBuilder(getJdbcUrlPrefix());
		sb.append("://");
		sb.append(StringUtils.isNotBlank(host) ? host : "localhost");
		if (port > 0 && port != getDefaultPort())
			sb.append(":").append(port);
		sb.append("/").append(databaseName);
		if (StringUtils.isNotBlank(params)) {
			if (!params.startsWith("?"))
				sb.append("?");
			sb.append(params);
		}
		return sb.toString();
	}

	public String polishJdbcUrl(String jdbcUrl) {
		return polishJdbcUrl(jdbcUrl, "?", "&");
	}

	public String appendJdbcUrlProperties(String jdbcUrl, Map<String, String> properties) {
		StringBuilder sb = new StringBuilder(jdbcUrl);
		boolean hasDelimiter = jdbcUrl.indexOf('?') > 0;
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			if (hasDelimiter) {
				sb.append('&');
			} else {
				sb.append('?');
				hasDelimiter = true;
			}
			sb.append(entry.getKey()).append('=').append(entry.getValue());
		}
		return sb.toString();
	}

	protected String polishJdbcUrl(String jdbcUrl, String delimiter, String separator) {
		String qs = getRecommendedJdbcUrlQueryString();
		if (qs == null)
			return jdbcUrl;
		int i = jdbcUrl.indexOf(delimiter, jdbcUrl.lastIndexOf('/'));
		if (i > 0) {
			String uri = jdbcUrl.substring(0, i);
			String params = jdbcUrl.substring(i + 1);
			Map<String, String> map = new LinkedHashMap<>();
			for (String s : (qs + separator + params).split(separator)) {
				String[] arr = s.split("=", 2);
				if (arr.length == 2)
					map.put(arr[0], arr[1]);
			}
			StringBuilder sb = new StringBuilder();
			if (map.size() > 0) {
				for (Map.Entry<String, String> entry : map.entrySet())
					sb.append(entry.getKey()).append("=").append(entry.getValue()).append(separator);
				sb.deleteCharAt(sb.length() - 1);
			}
			return uri + delimiter + sb.toString();
		} else {
			return jdbcUrl + delimiter + qs;
		}
	}

	protected String getRecommendedJdbcUrlQueryString() {
		return null;
	}

}

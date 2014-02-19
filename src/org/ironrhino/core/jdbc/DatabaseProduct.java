package org.ironrhino.core.jdbc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public enum DatabaseProduct {

	MYSQL {
		@Override
		public int getDefaultPort() {
			return 3306;
		}

	},
	POSTGRESQL {
		@Override
		public int getDefaultPort() {
			return 5432;
		}

	},
	ORACLE {
		@Override
		public int getDefaultPort() {
			return 1521;
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
			return 446;
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
			return "VALUES 1";
		}
	},
	INFORMIX {
		@Override
		public int getDefaultPort() {
			return 1533;
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
	},
	SYBASE {
		@Override
		public int getDefaultPort() {
			return 4100;
		}

		@Override
		public String getJdbcUrlPrefix() {
			return "jdbc:sybase:Tds";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
		public String getJdbcUrlPrefix() {
			return "jdbc:hsqldb";
		}

		@Override
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
		public String getJdbcUrl(String host, int port, String databaseName,
				String params) {
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
		if (nameOrUrl.toLowerCase().startsWith("jdbc:")) {
			for (DatabaseProduct p : values())
				if (nameOrUrl.startsWith(p.getJdbcUrlPrefix()))
					return p;
			return null;
		} else {
			if (nameOrUrl.toLowerCase().contains("mysql"))
				return MYSQL;
			else if (nameOrUrl.toLowerCase().contains("postgres"))
				return POSTGRESQL;
			else if (nameOrUrl.toLowerCase().contains("oracle"))
				return ORACLE;
			else if (nameOrUrl.toLowerCase().startsWith("db2"))
				return DB2;
			else if (nameOrUrl.toLowerCase().contains("informix"))
				return INFORMIX;
			else if (nameOrUrl.toLowerCase().contains("microsoft"))
				return SQLSERVER;
			else if (nameOrUrl.toLowerCase().contains("sql server")
					|| nameOrUrl.equals("Adaptive Server Enterprise")
					|| nameOrUrl.equals("ASE"))
				return SYBASE;
			else if (nameOrUrl.toLowerCase().equals("h2"))
				return H2;
			else if (nameOrUrl.toLowerCase().contains("hsql"))
				return HSQL;
			else if (nameOrUrl.toLowerCase().contains("derby"))
				return DERBY;
		}
		return null;
	}

	public abstract int getDefaultPort();

	public List<String> getKeywords() {
		try {
			List<String> lines = IOUtils.readLines(getClass()
					.getResourceAsStream("keywords.txt"));
			for (String line : lines) {
				if (line.startsWith(name() + "=")) {
					String s = line.substring(line.indexOf("=") + 1);
					return Arrays.asList(s.split(","));
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
		return "jdbc:" + name().toLowerCase();
	}

	public String getJdbcUrl(String host, int port, String databaseName,
			String params) {
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

}

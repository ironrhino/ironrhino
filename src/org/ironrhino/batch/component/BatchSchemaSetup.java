package org.ironrhino.batch.component;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.metadata.Setup;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BatchSchemaSetup {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobRepositoryFactoryBean jobRepositoryFactoryBean;

	@Setup
	public void setup() throws Exception {
		String tablePrefix = ReflectionUtils.getFieldValue(jobRepositoryFactoryBean, "tablePrefix");
		int maxVarCharLength = ReflectionUtils.getFieldValue(jobRepositoryFactoryBean, "maxVarCharLength");
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData dbmd = conn.getMetaData();
			String tableName = tablePrefix + "JOB_EXECUTION";
			ResultSet rs = dbmd.getTables(null, null, "%", null);
			while (rs.next()) {
				if (tableName.equalsIgnoreCase(rs.getString(3))) {
					return;
				}
			}
			DatabaseProduct dp = DatabaseProduct.parse(dbmd.getDatabaseProductName());
			if (maxVarCharLength > 65535)
				throw new IllegalArgumentException("Max varchar length is 65535");
			if (dp == DatabaseProduct.ORACLE && maxVarCharLength > 4000)
				throw new IllegalArgumentException("Max varchar length is 4000 for oracle");
			if (dp == DatabaseProduct.DB2 && maxVarCharLength > 32704)
				throw new IllegalArgumentException("Max varchar length is 32704 for db2");
			if ((dp == DatabaseProduct.SQLSERVER || dp == DatabaseProduct.SYBASE) && maxVarCharLength > 8000)
				throw new IllegalArgumentException("Max varchar length is 8000 for sqlserver or sybase");
			String file = "schema-" + (dp == DatabaseProduct.ORACLE ? "oracle10g" : dp.name().toLowerCase()) + ".sql";
			try (InputStream is = Job.class.getResourceAsStream(file)) {
				if (is == null)
					throw new UnsupportedOperationException("Database " + dp.name() + " is not supported");
				String[] arr = StringUtils.join(IOUtils.readLines(is), "\n").split(";");
				try (Statement stmt = conn.createStatement()) {
					for (String sql : arr) {
						if (!tablePrefix.equalsIgnoreCase("BATCH_")) {
							sql = sql.replaceAll("BATCH_", tablePrefix);
						}
						if (maxVarCharLength != 2500)
							sql = sql.replaceAll("2500", String.valueOf(maxVarCharLength));
						stmt.execute(sql);
					}
				}
			}
		}
	}

}

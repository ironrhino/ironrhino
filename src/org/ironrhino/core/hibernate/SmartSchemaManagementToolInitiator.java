package org.ironrhino.core.hibernate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.ironrhino.core.jdbc.DatabaseProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@SuppressWarnings("rawtypes")
@Component
@HibernateEnabled
public class SmartSchemaManagementToolInitiator implements StandardServiceInitiator<SchemaManagementTool> {

	@Autowired
	private DataSource dataSource;

	@Value("${hibernate.convert_foreign_key_to_index:true}")
	private boolean convertForeignKeyToIndex = true;

	@Override
	public SchemaManagementTool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		if (!convertForeignKeyToIndex) {
			return SchemaManagementToolInitiator.INSTANCE.initiateService(configurationValues, registry);
		} else {
			dropExistsForeignKey();
			return new HibernateSchemaManagementTool() {
				private static final long serialVersionUID = 1L;

				@Override
				public SchemaMigrator getSchemaMigrator(Map options) {
					SchemaMigrator sm = super.getSchemaMigrator(options);
					return (metadata, executionOptions, targetDescriptor) -> {
						convertForeignKeyToIndex(metadata.getDatabase());
						sm.doMigration(metadata, executionOptions, targetDescriptor);
					};
				}

				@Override
				public SchemaCreator getSchemaCreator(Map options) {
					SchemaCreator sc = super.getSchemaCreator(options);
					return (metadata, executionOptions, sourceDescriptor, targetDescriptor) -> {
						convertForeignKeyToIndex(metadata.getDatabase());
						sc.doCreation(metadata, executionOptions, sourceDescriptor, targetDescriptor);
					};
				}

				private void convertForeignKeyToIndex(Database database) {
					try (Connection conn = dataSource.getConnection()) {
						DatabaseMetaData dbmd = conn.getMetaData();
						String catalog = conn.getCatalog();
						String schema = null;
						try {
							schema = conn.getSchema();
						} catch (Throwable t) {
							schema = "%";
						}
						Set<String> existedTables = new HashSet<>();
						try (ResultSet rs = dbmd.getTables(catalog, schema, "%", new String[] { "TABLE" })) {
							while (rs.next())
								existedTables.add(rs.getString(3));
						}
						for (Namespace namespace : database.getNamespaces()) {
							for (Table table : namespace.getTables()) {
								Set<String> existedIndexes = new HashSet<>();
								String tableName = table.getName();
								String existedTableName = tableName;
								for (String s : existedTables) {
									if (s.equalsIgnoreCase(existedTableName)) {
										existedTableName = s;
										break;
									}
								}
								try (ResultSet rs = dbmd.getIndexInfo(catalog, schema, existedTableName, false,
										false)) {
									while (rs.next()) {
										String indexName = rs.getString("INDEX_NAME");
										if (indexName != null)
											existedIndexes.add(indexName);
									}
								} catch (SQLException e) {
								}
								@SuppressWarnings("unchecked")
								Iterator<ForeignKey> foreignKeys = table.getForeignKeyIterator();
								loop: while (foreignKeys.hasNext()) {
									ForeignKey foreignKey = foreignKeys.next();
									foreignKeys.remove();
									for (String existedIndex : existedIndexes) {
										if (foreignKey.getName().equalsIgnoreCase(existedIndex))
											continue loop;
									}
									List<Column> columns = foreignKey.getColumns();
									if (columns.size() == 1) {
										// test @MapsId
										KeyValue id = foreignKey.getTable().getIdentifierValue();
										if (id instanceof SimpleValue) {
											SimpleValue simpleId = (SimpleValue) id;
											if (columns.get(0).getName()
													.equalsIgnoreCase(simpleId.getColumnIterator().next().getText()))
												continue;
										}
									}

									Index index = new Index();
									index.setTable(foreignKey.getTable());
									for (Column col : columns)
										index.addColumn(col);
									if (foreignKey.getName() != null) {
										index.setName(foreignKey.getName());
										table.addIndex(index);
									}
								}
							}
						}
					} catch (SQLException ex) {
						ex.printStackTrace();
					}
				}
			};
		}
	}

	private void dropExistsForeignKey() {
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData dbmd = conn.getMetaData();
			DatabaseProduct dp = DatabaseProduct.parse(dbmd.getDatabaseProductName());
			Map<String, String> foreignKeys = new HashMap<String, String>();
			String catalog = conn.getCatalog();
			String schema = null;
			try {
				schema = conn.getSchema();
			} catch (Throwable t) {
				schema = "%";
			}
			ResultSet tables = dbmd.getTables(catalog, schema, "%", new String[] { "TABLE" });
			while (tables.next()) {
				String table = tables.getString(3);
				try (ResultSet importedKeys = dbmd.getImportedKeys(conn.getCatalog(), schema, table)) {
					while (importedKeys.next()) {
						String fkName = importedKeys.getString("FK_NAME");
						if (fkName != null && fkName.length() == 27 && fkName.toLowerCase(Locale.ROOT).startsWith("fk"))
							foreignKeys.put(fkName, importedKeys.getString("FKTABLE_NAME"));
					}
				}
			}
			if (foreignKeys.isEmpty())
				return;
			try (Statement stmt = conn.createStatement()) {
				for (Map.Entry<String, String> entry : foreignKeys.entrySet()) {
					StringBuilder dropSql = new StringBuilder();
					dropSql.append("alter table ");
					dropSql.append(entry.getValue());
					dropSql.append(" drop ");
					dropSql.append(dp == DatabaseProduct.MYSQL ? "foreign key " : "constraint ");
					dropSql.append(entry.getKey());
					stmt.addBatch(dropSql.toString());
				}
				stmt.executeBatch();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Class<SchemaManagementTool> getServiceInitiated() {
		return SchemaManagementTool.class;
	}

}
package org.ironrhino.core.hibernate;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Table.ForeignKeyKey;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("rawtypes")
public class SchemaManagementToolInitiator extends org.hibernate.tool.schema.internal.SchemaManagementToolInitiator {

	@Value("${hibernate.convert_foreign_key_to_index:true}")
	private boolean convertForeignKeyToIndex = true;

	@Override
	public SchemaManagementTool initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		if (!convertForeignKeyToIndex)
			return super.initiateService(configurationValues, registry);
		else
			return new HibernateSchemaManagementTool() {
				private static final long serialVersionUID = 1L;

				@Override
				public SchemaMigrator getSchemaMigrator(Map options) {
					SchemaMigrator sm = super.getSchemaMigrator(options);
					return new SchemaMigrator() {

						@Override
						public void doMigration(Metadata metadata, ExecutionOptions executionOptions,
								TargetDescriptor targetDescriptor) {
							convertForeignKeyToIndex(metadata.getDatabase());
							sm.doMigration(metadata, executionOptions, targetDescriptor);
						}
					};

				}

				@Override
				public SchemaCreator getSchemaCreator(Map options) {
					SchemaCreator sc = super.getSchemaCreator(options);
					return new SchemaCreator() {

						@Override
						public void doCreation(Metadata metadata, ExecutionOptions executionOptions,
								SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
							convertForeignKeyToIndex(metadata.getDatabase());
							sc.doCreation(metadata, executionOptions, sourceDescriptor, targetDescriptor);
						}
					};
				}

				private void convertForeignKeyToIndex(Database database) {
					for (Namespace namespace : database.getNamespaces()) {
						for (Table table : namespace.getTables()) {
							Map<ForeignKeyKey, ForeignKey> foreignKeys = ReflectionUtils.getFieldValue(table,
									"foreignKeys");
							for (ForeignKey foreignKey : foreignKeys.values()) {
								Index index = new Index();
								index.setTable(foreignKey.getTable());
								for (Column col : foreignKey.getColumns())
									index.addColumn(col);
								if (foreignKey.getName() != null) {
									index.setName(foreignKey.getName());
									table.addIndex(index);
								}
							}
							foreignKeys.clear();
						}
					}
				}
			};
	}

}
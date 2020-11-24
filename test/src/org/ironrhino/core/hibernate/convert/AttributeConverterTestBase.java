package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.Serializable;

import javax.persistence.AttributeConverter;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.AbstractStandardBasicType;
import org.ironrhino.core.model.Displayable;

public abstract class AttributeConverterTestBase {

	StandardServiceRegistry buildStandardServiceRegistry() {
		return new StandardServiceRegistryBuilder().applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
				.applySetting(AvailableSettings.XML_MAPPING_ENABLED, String.valueOf(false))
				.applySetting(AvailableSettings.URL, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
				.applySetting(AvailableSettings.SHOW_SQL, "true").build();
	}

	MetadataImplementor buildMetadata(StandardServiceRegistry ssr, Class<?> entity) {
		return (MetadataImplementor) new MetadataSources(ssr).addAnnotatedClass(entity).getMetadataBuilder().build();
	}

	MetadataImplementor buildMetadata(StandardServiceRegistry ssr, Class<?> entity,
			Class<? extends AttributeConverter<?, ?>> converter) {
		return (MetadataImplementor) new MetadataSources(ssr).addAnnotatedClass(entity).getMetadataBuilder()
				.applyAttributeConverter(converter).build();
	}

	void assertDetermineType(MetadataImplementor metadata, Class<?> entity, String fieldName, int expectedType) {
		PersistentClass tester = metadata.getEntityBinding(entity.getName());
		SimpleValue simpleValue = (SimpleValue) tester.getProperty(fieldName).getValue();
		AbstractStandardBasicType<?> basicType = (AbstractStandardBasicType<?>) simpleValue.getType();
		assertThat(basicType.getSqlTypeDescriptor().getSqlType(), is(expectedType));
	}

	void assertNotDetermineType(MetadataImplementor metadata, Class<?> entity, String fieldName) {
		PersistentClass tester = metadata.getEntityBinding(entity.getName());
		SimpleValue simpleValue = (SimpleValue) tester.getProperty(fieldName).getValue();
		try {
			simpleValue.getType();
			fail("Expected mappingException");
		} catch (MappingException e) {
			// ignore
		}
	}

	void persist(SessionFactory sf, Object entity) {
		Session s = sf.openSession();
		s.getTransaction().begin();
		s.persist(entity);
		s.getTransaction().commit();
		s.close();
	}

	<T> T get(SessionFactory sf, Class<T> clazz, Serializable id) {
		Session s = sf.openSession();
		s.getTransaction().begin();
		T entity = s.get(clazz, id);
		s.getTransaction().commit();
		s.close();
		return entity;
	}

	void delete(SessionFactory sf, Object entity) {
		Session s = sf.openSession();
		s.getTransaction().begin();
		s.delete(entity);
		s.getTransaction().commit();
		s.close();
	}

	enum TestEnum implements Displayable {
		BAR, BAZ, FOO;

		@Override
		public String toString() {
			return getDisplayName();
		}
	}
}

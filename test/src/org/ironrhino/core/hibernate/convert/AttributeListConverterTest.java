package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.ironrhino.core.model.Attribute;
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AttributeListConverterTest extends AttributeConverterTestBase {

	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "attributeList");

			metadata = buildMetadata(ssr, TestEntity.class, AttributeListConverter.class);
			assertDetermineType(metadata, TestEntity.class, "attributeList", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				List<Attribute> attributeList = Arrays.asList(new Attribute("1", "1"), new Attribute("2", "2"));
				persist(sf, new TestEntity(1L, attributeList));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getAttributeList(), is(attributeList));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getAttributeList(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptyList()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getAttributeList(), is(Collections.EMPTY_LIST));
				delete(sf, entity);
			}
		} finally {
			StandardServiceRegistryBuilder.destroy(ssr);
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Entity(name = "TABLE_ATTRIBUTE_LIST")
	static class TestEntity {
		@Id
		private Long id;
		private List<Attribute> attributeList;
	}
}
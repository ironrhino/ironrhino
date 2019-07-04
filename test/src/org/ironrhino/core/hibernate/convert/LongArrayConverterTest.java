package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class LongArrayConverterTest extends AttributeConverterTestBase {

	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertDetermineType(metadata, TestEntity.class, "longArray", Types.VARBINARY);

			metadata = buildMetadata(ssr, TestEntity.class, LongArrayConverter.class);
			assertDetermineType(metadata, TestEntity.class, "longArray", Types.VARCHAR);

			try (final SessionFactory sf = metadata.buildSessionFactory()) {
				persist(sf, new TestEntity(1L, new Long[]{1L, 2L, 3L, 4L, 5L}));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getLongArray(), is(new Long[]{1L, 2L, 3L, 4L, 5L}));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getLongArray(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, new Long[0]));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getLongArray(), is(new Long[0]));
				delete(sf, entity);
			}
		} finally {
			StandardServiceRegistryBuilder.destroy(ssr);
		}
	}

	@Setter
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Entity(name = "TABLE_LONG_ARRAY")
	static class TestEntity {
		@Id
		private Long id;
		private Long[] longArray;
	}

}
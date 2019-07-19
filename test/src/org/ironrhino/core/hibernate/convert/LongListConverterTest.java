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
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class LongListConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "longList");

			metadata = buildMetadata(ssr, TestEntity.class, LongListConverter.class);
			assertDetermineType(metadata, TestEntity.class, "longList", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				persist(sf, new TestEntity(1L, Arrays.asList(1L, 2L, 3L, 4L, 5L)));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getLongList(), is(Arrays.asList(1L, 2L, 3L, 4L, 5L)));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getLongList(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptyList()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getLongList(), is(Collections.EMPTY_LIST));
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
	@Entity(name = "TABLE_LONG_LIST")
	static class TestEntity {
		@Id
		private Long id;
		private List<Long> longList;
	}
}

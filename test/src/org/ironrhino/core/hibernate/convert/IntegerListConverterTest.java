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

public class IntegerListConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "integerList");

			metadata = buildMetadata(ssr, TestEntity.class, IntegerListConverter.class);
			assertDetermineType(metadata, TestEntity.class, "integerList", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				persist(sf, new TestEntity(1L, Arrays.asList(1, 2, 3, 4, 5)));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getIntegerList(), is(Arrays.asList(1, 2, 3, 4, 5)));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getIntegerList(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptyList()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getIntegerList(), is(Collections.EMPTY_LIST));
				delete(sf, entity);
			}
		} finally {
			StandardServiceRegistryBuilder.destroy(ssr);
		}
	}

	@Setter
	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	@Entity(name = "TABLE_INTEGER_LIST")
	static class TestEntity {
		@Id
		private Long id;
		private List<Integer> integerList;
	}
}

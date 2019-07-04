package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

public class IntegerSetConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "integerSet");

			metadata = buildMetadata(ssr, TestEntity.class, IntegerSetConverter.class);
			assertDetermineType(metadata, TestEntity.class, "integerSet", Types.VARCHAR);

			try (final SessionFactory sf = metadata.buildSessionFactory()) {
				Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
				persist(sf, new TestEntity(1L, set));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getIntegerSet(), is(set));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getIntegerSet(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptySet()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getIntegerSet(), is(Collections.EMPTY_SET));
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
	@Entity(name = "TABLE_INTEGER_SET")
	static class TestEntity {
		@Id
		private Long id;
		private Set<Integer> integerSet;
	}
}

package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

public class StringMapConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "stringMap");

			metadata = buildMetadata(ssr, TestEntity.class, StringMapConverter.class);
			assertDetermineType(metadata, TestEntity.class, "stringMap", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				Map<String, String> map = new HashMap<>();
				map.put("1", "1");
				map.put("2", "2");
				persist(sf, new TestEntity(1L, map));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getStringMap(), is(map));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getStringMap(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptyMap()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getStringMap(), is(Collections.EMPTY_MAP));
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
	@Entity(name = "TABLE_STRING_MAP")
	static class TestEntity {
		@Id
		private Long id;
		private Map<String, String> stringMap;
	}
}

package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Converter;
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

public class EnumSetConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "enumSet");

			metadata = buildMetadata(ssr, TestEntity.class, TestEnumSetConverter.class);
			assertDetermineType(metadata, TestEntity.class, "enumSet", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				Set<TestEnum> set = new HashSet<>(Arrays.asList(TestEnum.BAR, TestEnum.BAZ, TestEnum.FOO));
				persist(sf, new TestEntity(1L, set));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getEnumSet(), is(set));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getEnumSet(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptySet()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getEnumSet(), is(Collections.EMPTY_SET));
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
	@Entity(name = "TABLE_ENUM_SET")
	static class TestEntity {
		@Id
		private Long id;
		private Set<TestEnum> enumSet;
	}

	@Converter(autoApply = true)
	static class TestEnumSetConverter extends EnumSetConverter<TestEnum> {
	}
}
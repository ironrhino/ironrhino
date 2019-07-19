package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;

import javax.persistence.AttributeConverter;
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

public class EnumArrayConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "enumArray");

			metadata = buildMetadata(ssr, TestEntity.class, TestEnumArrayConverter.class);
			assertDetermineType(metadata, TestEntity.class, "enumArray", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				persist(sf, new TestEntity(1L, new TestEnum[] { TestEnum.BAR, TestEnum.BAZ, TestEnum.FOO }));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getEnumArray(), is(new TestEnum[] { TestEnum.BAR, TestEnum.BAZ, TestEnum.FOO }));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getEnumArray(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, new TestEnum[0]));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getEnumArray(), is(new TestEnum[0]));
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
	@Entity(name = "TABLE_ENUM_ARRAY")
	static class TestEntity {
		@Id
		private Long id;
		private TestEnum[] enumArray;
	}

	@Converter(autoApply = true)
	static class TestEnumArrayConverter extends EnumArrayConverter<TestEnum>
			implements AttributeConverter<TestEnum[], String> {

	}
}
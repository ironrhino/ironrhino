package org.ironrhino.core.hibernate.convert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Types;
import java.time.YearMonth;

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

public class YearMonthConverterTest extends AttributeConverterTestBase {
	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertDetermineType(metadata, TestEntity.class, "yearMonth", Types.VARBINARY);

			metadata = buildMetadata(ssr, TestEntity.class, YearMonthConverter.class);
			assertDetermineType(metadata, TestEntity.class, "yearMonth", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				persist(sf, new TestEntity(1L, YearMonth.of(2019, 7)));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getYearMonth(), is(YearMonth.of(2019, 7)));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getYearMonth(), is(nullValue()));
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
	@Entity(name = "TABLE_YEAR_MONTH")
	static class TestEntity {
		@Id
		private Long id;
		private YearMonth yearMonth;
	}
}
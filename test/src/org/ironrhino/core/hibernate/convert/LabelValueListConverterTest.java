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
import org.ironrhino.core.model.LabelValue;
import org.junit.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class LabelValueListConverterTest extends AttributeConverterTestBase {

	@Test
	public void testConverter() {
		final StandardServiceRegistry ssr = buildStandardServiceRegistry();
		try {
			MetadataImplementor metadata = buildMetadata(ssr, TestEntity.class);
			assertNotDetermineType(metadata, TestEntity.class, "labelValueList");

			metadata = buildMetadata(ssr, TestEntity.class, LabelValueListConverter.class);
			assertDetermineType(metadata, TestEntity.class, "labelValueList", Types.VARCHAR);

			try (SessionFactory sf = metadata.buildSessionFactory()) {
				List<LabelValue> labelValueList = Arrays.asList(new LabelValue("1", "1"), new LabelValue("2", "2"));
				persist(sf, new TestEntity(1L, labelValueList));
				TestEntity entity = get(sf, TestEntity.class, 1L);
				assertThat(entity.getLabelValueList(), is(labelValueList));
				delete(sf, entity);

				persist(sf, new TestEntity(2L, null));
				entity = get(sf, TestEntity.class, 2L);
				assertThat(entity.getLabelValueList(), is(nullValue()));
				delete(sf, entity);

				persist(sf, new TestEntity(3L, Collections.emptyList()));
				entity = get(sf, TestEntity.class, 3L);
				assertThat(entity.getLabelValueList(), is(Collections.EMPTY_LIST));
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
	@Entity(name = "TABLE_LABEL_VALUE_LIST")
	static class TestEntity {
		@Id
		private Long id;
		private List<LabelValue> labelValueList;
	}
}
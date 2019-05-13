package org.ironrhino.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import lombok.Getter;
import lombok.Setter;

public class AnnotationUtilsTest {

	@MappedSuperclass
	@Getter
	@Setter
	public static class Base implements Serializable, Persistable<String> {

		private static final long serialVersionUID = 1616212908678942555L;
		protected String id;
		private List<String> names;
		private Set<String> tags;

		@UiConfig(hidden = true)
		private Map<String, String> attributes;

		@Override
		public boolean isNew() {
			return id == null;
		}

		@UiConfig(hidden = true)
		public List<String> getNames() {
			return names;
		}

		@PrePersist
		private void validate() {

		}

		@PreUpdate
		private void validateUpdate() {

		}

	}

	@Getter
	@Setter
	public static class User extends Base {

		private static final long serialVersionUID = -1634488900558289348L;

		@UiConfig
		private String username;
		@NotInCopy
		private String password;

		@UiConfig
		public String getPassword() {
			return password;
		}

		@PrePersist
		public void validate1() {

		}

		@PrePersist
		protected void validate2() {

		}

		@PrePersist
		private void validate3() {

		}

	}

	@Test
	public void testGetAnnotatedMethod() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedMethod();
	}

	private void doTestGetAnnotatedMethod() {
		assertThat(AnnotationUtils.getAnnotatedMethod(User.class, PreUpdate.class).getName(),
				equalTo("validateUpdate"));
		assertThat(AnnotationUtils.getAnnotatedMethod(User.class, Trigger.class), nullValue());
	}

	@Test
	public void testGetAnnotatedMethods() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedMethods();
	}

	public void doTestGetAnnotatedMethods() {
		assertThat(AnnotationUtils.getAnnotatedMethods(User.class, PrePersist.class).size(), equalTo(4));
		assertThat(AnnotationUtils.getAnnotatedMethods(User.class, Trigger.class).isEmpty(), equalTo(true));
	}

	@Test
	public void testGetAnnotatedPropertyNames() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedPropertyNames();
	}

	public void doTestGetAnnotatedPropertyNames() {
		assertThat(AnnotationUtils.getAnnotatedPropertyNames(User.class, UiConfig.class).size(), equalTo(4));
		assertThat(AnnotationUtils.getAnnotatedMethods(User.class, Trigger.class).isEmpty(), equalTo(true));
	}

	@Test
	public void testGetAnnotatedPropertyNameAndValues() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedPropertyNameAndValues();
	}

	public void doTestGetAnnotatedPropertyNameAndValues() {
		User user = new User();
		user.setUsername("username");
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(user, UiConfig.class);
		assertThat(map.size(), equalTo(4));
		assertThat(map.get("username"), equalTo("username"));
		assertThat(AnnotationUtils.getAnnotatedPropertyNameAndValues(user, Trigger.class).isEmpty(), equalTo(true));
	}

	@Test
	public void testGetAnnotatedPropertyNameAndAnnnotations() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedPropertyNameAndAnnnotations();
	}

	public void doTestGetAnnotatedPropertyNameAndAnnnotations() {
		Map<String, UiConfig> map = AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(User.class, UiConfig.class);
		assertThat(map.size(), equalTo(4));
		assertThat(map.get("attributes").hidden(), equalTo(true));
		assertThat(AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(User.class, Trigger.class).isEmpty(),
				equalTo(true));
	}

	@Test
	public void testGetAnnotationsByType() throws IOException {
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
		Resource resource = resourcePatternResolver
				.getResource("org/ironrhino/core/spring/configuration/RedisConfiguration.class");
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
		AnnotatedTypeMetadata metadata = metadataReader.getAnnotationMetadata();
		assertThat(AnnotationUtils.getAnnotationsByType(metadata, ClassPresentConditional.class).length, equalTo(1));
		assertThat(AnnotationUtils.getAnnotationsByType(metadata, UiConfig.class).length, equalTo(0));
	}

}

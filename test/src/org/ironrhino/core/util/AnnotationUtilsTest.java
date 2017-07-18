package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

public class AnnotationUtilsTest {

	@MappedSuperclass
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

		@Override
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@UiConfig(hidden = true)
		public List<String> getNames() {
			return names;
		}

		public void setNames(List<String> names) {
			this.names = names;
		}

		public Set<String> getTags() {
			return tags;
		}

		public void setTags(Set<String> tags) {
			this.tags = tags;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public void setAttributes(Map<String, String> attributes) {
			this.attributes = attributes;
		}

		@PrePersist
		private void validate() {

		}

		@PreUpdate
		private void validateUpdate() {

		}

	}

	public static class User extends Base {

		private static final long serialVersionUID = -1634488900558289348L;

		@UiConfig
		private String username;
		@NotInCopy
		private String password;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		@UiConfig
		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
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
		assertEquals("validateUpdate", AnnotationUtils.getAnnotatedMethod(User.class, PreUpdate.class).getName());
		assertNull(AnnotationUtils.getAnnotatedMethod(User.class, Trigger.class));
	}

	@Test
	public void testGetAnnotatedMethods() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedMethods();
	}

	public void doTestGetAnnotatedMethods() {
		assertEquals(4, AnnotationUtils.getAnnotatedMethods(User.class, PrePersist.class).size());
		assertTrue(AnnotationUtils.getAnnotatedMethods(User.class, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotatedPropertyNames() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedPropertyNames();
	}

	public void doTestGetAnnotatedPropertyNames() {
		assertEquals(4, AnnotationUtils.getAnnotatedPropertyNames(User.class, UiConfig.class).size());
		assertTrue(AnnotationUtils.getAnnotatedMethods(User.class, Trigger.class).isEmpty());
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
		assertEquals(4, map.size());
		assertEquals("username", map.get("username"));
		assertTrue(AnnotationUtils.getAnnotatedPropertyNameAndValues(user, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotatedPropertyNameAndAnnnotations() {
		for (int i = 0; i < 100; i++)
			doTestGetAnnotatedPropertyNameAndAnnnotations();
	}

	public void doTestGetAnnotatedPropertyNameAndAnnnotations() {
		Map<String, UiConfig> map = AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(User.class, UiConfig.class);
		assertEquals(4, map.size());
		assertTrue(map.get("attributes").hidden());
		assertTrue(AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(User.class, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotationsByType() throws IOException {
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
		Resource resource = resourcePatternResolver
				.getResource("org/ironrhino/core/spring/configuration/RedisConfiguration.class");
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
		AnnotatedTypeMetadata metadata = metadataReader.getAnnotationMetadata();
		assertEquals(1, AnnotationUtils.getAnnotationsByType(metadata, ClassPresentConditional.class).length);
		assertEquals(0, AnnotationUtils.getAnnotationsByType(metadata, UiConfig.class).length);
	}

}

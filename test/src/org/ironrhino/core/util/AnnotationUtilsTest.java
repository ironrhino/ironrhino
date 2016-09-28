package org.ironrhino.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.MappedSuperclass;

import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.junit.Test;

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

	}

	@Test
	public void testGetAnnotatedMethod() {
		assertEquals("getNames", AnnotationUtils.getAnnotatedMethod(User.class, UiConfig.class).getName());
		assertNull(AnnotationUtils.getAnnotatedMethod(User.class, Trigger.class));
	}

	@Test
	public void testGetAnnotatedMethods() {
		assertEquals("getNames",
				AnnotationUtils.getAnnotatedMethods(User.class, UiConfig.class).iterator().next().getName());
		assertTrue(AnnotationUtils.getAnnotatedMethods(User.class, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotatedPropertyNames() {
		assertEquals(4, AnnotationUtils.getAnnotatedPropertyNames(User.class, UiConfig.class).size());
		assertTrue(AnnotationUtils.getAnnotatedMethods(User.class, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotatedPropertyNameAndValues() {
		User user = new User();
		user.setUsername("username");
		Map<String, Object> map = AnnotationUtils.getAnnotatedPropertyNameAndValues(user, UiConfig.class);
		assertEquals(4, map.size());
		assertEquals("username", map.get("username"));
		assertTrue(AnnotationUtils.getAnnotatedPropertyNameAndValues(user, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotatedPropertyNameAndAnnnotations() {
		Map<String, UiConfig> map = AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(User.class, UiConfig.class);
		assertEquals(4, map.size());
		assertTrue(map.get("attributes").hidden());
		assertTrue(AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(User.class, Trigger.class).isEmpty());
	}

	@Test
	public void testGetAnnotation() {
		assertTrue(AnnotationUtils.getAnnotation(User.class, UiConfig.class, "getNames").hidden());
		assertNotNull(AnnotationUtils.getAnnotation(User.class, MappedSuperclass.class));
	}

}

package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.junit.Test;

import lombok.Getter;
import lombok.Setter;

public class NestedPathMapSqlParameterSourceTest {

	static enum Grade {
		A, B, C;
	}

	@Getter
	@Setter
	static class Person {
		private String name;
		private int age;
		private Grade grade1;
		private Person self = this;

		@Column(name = "g2")
		@Enumerated(EnumType.STRING)
		private Grade grade2;

		public List<Person> getSilbings() {
			return Collections.singletonList(this);
		}

	}

	@Test
	public void test() {
		Person p = new Person();
		p.setName("name");
		p.setAge(12);
		p.setGrade1(Grade.A);
		p.setGrade2(Grade.B);
		Map<String, Person> map = new HashMap<>();
		map.put("person", p);
		NestedPathMapSqlParameterSource source = new NestedPathMapSqlParameterSource();
		source.addValue("p", p);
		source.addValue("array", new String[] { "A", "B" });
		source.addValue("list", Arrays.asList(p));
		source.addValue("map", map);
		assertTrue(source.hasValue("p"));
		assertTrue(source.hasValue("p.name"));
		assertTrue(source.hasValue("p.age"));
		assertTrue(source.hasValue("p.grade1"));
		assertTrue(source.hasValue("p.grade2"));
		assertTrue(source.hasValue("p.g2"));
		assertEquals(p, source.getValue("p"));
		assertEquals("name", source.getValue("p.name"));
		assertEquals(12, source.getValue("p.age"));
		assertEquals(0, source.getValue("p.grade1"));
		assertEquals("B", source.getValue("p.grade2"));
		assertEquals("B", source.getValue("p.g2"));
		assertTrue(source.hasValue("array"));
		assertTrue(source.hasValue("array[0]"));
		assertTrue(source.hasValue("array[1]"));
		assertEquals("A", source.getValue("array[0]"));
		assertEquals("B", source.getValue("array[1]"));
		assertFalse(source.hasValue("array[2]"));
		assertTrue(source.hasValue("list"));
		assertTrue(source.hasValue("list[0]"));
		assertFalse(source.hasValue("list[1]"));
		assertEquals(p, source.getValue("list[0]"));
		assertEquals("name", source.getValue("list[0].name"));
		assertTrue(source.hasValue("list[0].self.name"));
		assertEquals("name", source.getValue("list[0].self.name"));
		assertTrue(source.hasValue("list[0].self.silbings[0].name"));
		assertEquals("name", source.getValue("list[0].self.silbings[0].name"));
		assertTrue(source.hasValue("map"));
		assertTrue(source.hasValue("map['person']"));
		assertTrue(source.hasValue("map[\"person\"]"));
		assertFalse(source.hasValue("map['person2']"));
		assertTrue(source.hasValue("map['person'].self.silbings[0].name"));
		assertEquals("name", source.getValue("map['person'].self.silbings[0].name"));

	}

}

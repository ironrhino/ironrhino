package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.junit.Test;

public class EntityBeanPropertySqlParameterSourceTest {

	static enum Grade {
		A, B, C;
	}

	static class Person {
		private String name;
		private int age;
		private Grade grade1;

		@Column(name = "g2")
		@Enumerated(EnumType.STRING)
		private Grade grade2;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Grade getGrade1() {
			return grade1;
		}

		public void setGrade1(Grade grade1) {
			this.grade1 = grade1;
		}

		public Grade getGrade2() {
			return grade2;
		}

		public void setGrade2(Grade grade2) {
			this.grade2 = grade2;
		}

	}

	@Test
	public void test() {
		Person p = new Person();
		p.setName("name");
		p.setAge(12);
		p.setGrade1(Grade.A);
		p.setGrade2(Grade.B);
		EntityBeanPropertySqlParameterSource ebpsps = new EntityBeanPropertySqlParameterSource(p);
		assertTrue(ebpsps.hasValue("name"));
		assertTrue(ebpsps.hasValue("age"));
		assertTrue(ebpsps.hasValue("grade1"));
		assertTrue(ebpsps.hasValue("grade2"));
		assertTrue(ebpsps.hasValue("g2"));
		assertEquals("name", ebpsps.getValue("name"));
		assertEquals(12, ebpsps.getValue("age"));
		assertEquals(0, ebpsps.getValue("grade1"));
		assertEquals("B", ebpsps.getValue("grade2"));
		assertEquals("B", ebpsps.getValue("g2"));
	}

}

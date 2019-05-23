package org.ironrhino.core.jdbc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.junit.Test;

import lombok.Data;

public class EntityBeanPropertySqlParameterSourceTest {

	static enum Grade {
		A, B, C;
	}

	static @Data class Person {
		private String name;
		private int age;
		private Grade grade1;

		@Column(name = "g2")
		@Enumerated(EnumType.STRING)
		private Grade grade2;

	}

	@Test
	public void test() {
		Person p = new Person();
		p.setName("name");
		p.setAge(12);
		p.setGrade1(Grade.A);
		p.setGrade2(Grade.B);
		EntityBeanPropertySqlParameterSource ebpsps = new EntityBeanPropertySqlParameterSource(p);
		assertThat(ebpsps.hasValue("name"), is(true));
		assertThat(ebpsps.hasValue("age"), is(true));
		assertThat(ebpsps.hasValue("grade1"), is(true));
		assertThat(ebpsps.hasValue("grade2"), is(true));
		assertThat(ebpsps.hasValue("g2"), is(true));
		assertThat(ebpsps.getValue("name"), is("name"));
		assertThat(ebpsps.getValue("age"), is(12));
		assertThat(ebpsps.getValue("grade1"), is(0));
		assertThat(ebpsps.getValue("grade2"), is("B"));
		assertThat(ebpsps.getValue("g2"), is("B"));
	}

}

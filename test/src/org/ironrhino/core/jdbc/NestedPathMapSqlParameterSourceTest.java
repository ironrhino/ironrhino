package org.ironrhino.core.jdbc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
		assertThat(source.hasValue("p"), is(true));
		assertThat(source.hasValue("p.name"), is(true));
		assertThat(source.hasValue("p.age"), is(true));
		assertThat(source.hasValue("p.grade1"), is(true));
		assertThat(source.hasValue("p.grade2"), is(true));
		assertThat(source.hasValue("p.g2"), is(true));
		assertThat(source.getValue("p"), is(p));
		assertThat(source.getValue("p.name"), is("name"));
		assertThat(source.getValue("p.age"), is(12));
		assertThat(source.getValue("p.grade1"), is(0));
		assertThat(source.getValue("p.grade2"), is("B"));
		assertThat(source.getValue("p.g2"), is("B"));
		assertThat(source.hasValue("array"), is(true));
		assertThat(source.hasValue("array[0]"), is(true));
		assertThat(source.hasValue("array[1]"), is(true));
		assertThat(source.getValue("array[0]"), is("A"));
		assertThat(source.getValue("array[1]"), is("B"));
		assertThat(source.hasValue("array[2]"), is(false));
		assertThat(source.hasValue("list"), is(true));
		assertThat(source.hasValue("list[0]"), is(true));
		assertThat(source.hasValue("list[1]"), is(false));
		assertThat(source.getValue("list[0]"), is(p));
		assertThat(source.getValue("list[0].name"), is("name"));
		assertThat(source.hasValue("list[0].self.name"), is(true));
		assertThat(source.getValue("list[0].self.name"), is("name"));
		assertThat(source.hasValue("list[0].self.silbings[0].name"), is(true));
		assertThat(source.getValue("list[0].self.silbings[0].name"), is("name"));
		assertThat(source.hasValue("map"), is(true));
		assertThat(source.hasValue("map['person']"), is(true));
		assertThat(source.hasValue("map[\"person\"]"), is(true));
		assertThat(source.hasValue("map['person2']"), is(false));
		assertThat(source.hasValue("map['person'].self.silbings[0].name"), is(true));
		assertThat(source.getValue("map['person'].self.silbings[0].name"), is("name"));

	}

}
